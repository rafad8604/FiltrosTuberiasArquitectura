package inicio;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;
import datos.TipoDato;
import motor.PipelineManager;
import motor.PipelineManager.PasoFiltro;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Punto de entrada para ejecutar pipelines como procesos independientes
 * comunicándose mediante stdin/stdout (tuberías a nivel de SO).
 *
 * A diferencia de InicioAplicacion (que abre la GUI Swing),
 * MainPipeline permite ejecutar cadenas de filtros desde la línea
 * de comandos, donde cada filtro corre como un proceso Java separado.
 *
 * Uso:
 *   java -jar TuberiasFiltros-pipeline.jar &lt;archivo&gt; &lt;filtro1&gt; [filtro2...] [--palabra=X]
 *
 * Ejemplos:
 *   java -jar TuberiasFiltros-pipeline.jar archivo.txt 1 2
 *     → Carga el archivo (filtro 1) y convierte a binario (filtro 2)
 *
 *   java -jar TuberiasFiltros-pipeline.jar archivo.txt 1 6
 *     → Carga el archivo y genera hash SHA-256
 *
 *   java -jar TuberiasFiltros-pipeline.jar archivo.txt 1 7 --palabra=hola
 *     → Carga el archivo y busca la palabra "hola"
 *
 *   java -jar TuberiasFiltros-pipeline.jar imagen.png 3
 *     → Aplica los 4 filtros de imagen
 *
 * Los resultados se guardan automáticamente en output/
 */
public class MainPipeline {

    public static void main(String[] args) {
        if (args.length < 2) {
            mostrarAyuda();
            System.exit(1);
        }

        try {
            // ── Parsear argumentos ───────────────────────────────
            String archivoEntrada = args[0];
            List<Integer> filtroIds   = new ArrayList<>();
            String palabra = null;

            for (int i = 1; i < args.length; i++) {
                if (args[i].startsWith("--palabra=")) {
                    palabra = args[i].substring("--palabra=".length());
                } else {
                    try {
                        int id = Integer.parseInt(args[i]);
                        if (id < 1 || id > 8) {
                            System.err.println("✘ ID de filtro inválido: " + id + " (debe ser 1-8)");
                            System.exit(1);
                        }
                        filtroIds.add(id);
                    } catch (NumberFormatException e) {
                        System.err.println("✘ Argumento no reconocido: " + args[i]);
                        System.exit(1);
                    }
                }
            }

            if (filtroIds.isEmpty()) {
                System.err.println("✘ Debes especificar al menos un filtro (1-8).");
                System.exit(1);
            }

            // ── Verificar archivo de entrada ─────────────────────
            File archivo = new File(archivoEntrada);
            if (!archivo.exists()) {
                System.err.println("✘ Archivo no encontrado: " + archivoEntrada);
                System.exit(1);
            }

            // ── Determinar directorio base del proyecto ──────────
            // Buscar los JARs relativos al directorio de ejecución
            String baseDir = System.getProperty("user.dir");
            System.err.println("Directorio base: " + baseDir);

            // ── Construir la entrada inicial ─────────────────────
            int primerFiltro = filtroIds.get(0);
            PaqueteDatos entradaInicial = construirEntrada(archivo, primerFiltro);

            // Establecer parámetros (palabra de búsqueda)
            if (palabra != null && !palabra.isEmpty()) {
                entradaInicial.setParametros(palabra);
            }

            System.err.println("\n═══ Pipeline: " + archivoEntrada + " → "
                    + filtroIds + " ═══");
            System.err.println("Entrada: " + entradaInicial.getTipo()
                    + " (" + entradaInicial.getTamanioBytes() + " bytes)");

            // ── Construir pasos del pipeline ─────────────────────
            List<PasoFiltro> pasos = new ArrayList<>();
            for (int id : filtroIds) {
                String jarPath = PipelineManager.resolverJarPath(baseDir, id);
                if (id == 7 || id == 8) {
                    // Pasar palabra como argumento al proceso
                    String pal = (palabra != null) ? palabra : "la";
                    pasos.add(new PasoFiltro(jarPath, pal));
                } else {
                    pasos.add(new PasoFiltro(jarPath));
                }
            }

            // ── Ejecutar pipeline ────────────────────────────────
            PipelineManager manager = new PipelineManager();
            PaqueteDatos resultado = manager.ejecutar(entradaInicial, pasos);

            // ── Guardar resultado ────────────────────────────────
            File outputDir = new File("output");
            outputDir.mkdirs();

            guardarResultado(resultado, outputDir, filtroIds);

            // ── Mostrar resultado textual ────────────────────────
            if (resultado.getDatos() != null
                    && (resultado.getTipo() == TipoDato.TEXTO
                        || resultado.getTipo() == TipoDato.BINARIO
                        || resultado.getTipo() == TipoDato.BASE64)) {
                System.err.println("\n═══ Resultado ═══");
                String texto = new String(resultado.getDatos(), StandardCharsets.UTF_8);
                // Solo mostrar los primeros 2000 caracteres en consola
                if (texto.length() > 2000) {
                    System.err.println(texto.substring(0, 2000) + "\n... (truncado)");
                } else {
                    System.err.println(texto);
                }
            }

        } catch (Exception e) {
            System.err.println("\n✘ Error en el pipeline: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Construye el PaqueteDatos inicial leyendo el archivo de entrada.
     * El tipo se infiere según el primer filtro del pipeline.
     */
    private static PaqueteDatos construirEntrada(File archivo, int primerFiltroId)
            throws IOException {

        // Para el filtro 1 (CargarArchivo), basta con enviar la ruta
        if (primerFiltroId == 1) {
            return new PaqueteDatos(TipoDato.PATH,
                    archivo.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
        }

        byte[] bytes = Files.readAllBytes(archivo.toPath());

        switch (primerFiltroId) {
            case 3:  return new PaqueteDatos(TipoDato.IMAGEN,  bytes);
            case 4:  return new PaqueteDatos(TipoDato.BINARIO, bytes);
            case 5:  return new PaqueteDatos(TipoDato.BASE64,  bytes);
            default: return new PaqueteDatos(TipoDato.TEXTO,   bytes);
        }
    }

    /**
     * Guarda el resultado del pipeline en el directorio output/.
     */
    private static void guardarResultado(PaqueteDatos resultado, File outputDir,
                                          List<Integer> filtroIds)
            throws IOException {

        String suffix = filtroIds.toString().replace(", ", "-")
                .replace("[", "").replace("]", "");
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());

        if (resultado.getLista() != null) {
            List<byte[]> lista = resultado.getLista();
            List<String> nombres = resultado.getNombres();
            String ext = extensionPara(resultado.getTipo());

            for (int i = 0; i < lista.size(); i++) {
                String nombre;
                if (nombres != null && i < nombres.size()) {
                    nombre = "pipeline_" + suffix + "_" + nombres.get(i) + "_" + ts + ext;
                } else {
                    nombre = "pipeline_" + suffix + "_item" + i + "_" + ts + ext;
                }
                File f = new File(outputDir, nombre);
                Files.write(f.toPath(), lista.get(i));
                System.err.println("💾 Guardado: " + f.getAbsolutePath());
            }
        } else if (resultado.getDatos() != null) {
            String ext = extensionPara(resultado.getTipo());
            File f = new File(outputDir, "pipeline_" + suffix + "_" + ts + ext);
            Files.write(f.toPath(), resultado.getDatos());
            System.err.println("💾 Guardado: " + f.getAbsolutePath());
        }
    }

    private static String extensionPara(TipoDato tipo) {
        switch (tipo) {
            case BINARIO:
            case LISTA_BINARIO: return ".bin";
            case BASE64:
            case LISTA_BASE64:  return ".b64";
            case IMAGEN:
            case LISTA_IMAGEN:  return ".png";
            default:            return ".txt";
        }
    }

    private static void mostrarAyuda() {
        System.err.println("╔══════════════════════════════════════════════════════════════╗");
        System.err.println("║  Tuberías y Filtros — Pipeline por línea de comandos        ║");
        System.err.println("╚══════════════════════════════════════════════════════════════╝");
        System.err.println();
        System.err.println("Uso:");
        System.err.println("  java -jar TuberiasFiltros-pipeline.jar <archivo> <filtro1> [filtro2...] [--palabra=X]");
        System.err.println();
        System.err.println("Filtros disponibles:");
        System.err.println("  1 — Cargar archivo de texto     (PATH → TEXTO)");
        System.err.println("  2 — Convertir texto a binario   (TEXTO → BINARIO)");
        System.err.println("  3 — Filtro de imágenes (4 hilos)(IMAGEN → LISTA_IMAGEN)");
        System.err.println("  4 — Binario a Base64            (BINARIO → LISTA_BASE64)");
        System.err.println("  5 — Base64 a Binario            (BASE64 → BINARIO)");
        System.err.println("  6 — Encriptar SHA-256           (TEXTO → TEXTO)");
        System.err.println("  7 — Buscar palabra              (TEXTO → TEXTO)");
        System.err.println("  8 — Contar ocurrencias          (TEXTO → TEXTO)");
        System.err.println();
        System.err.println("Ejemplos:");
        System.err.println("  java -jar TuberiasFiltros-pipeline.jar mi_texto.txt 1 2");
        System.err.println("  java -jar TuberiasFiltros-pipeline.jar mi_texto.txt 1 6");
        System.err.println("  java -jar TuberiasFiltros-pipeline.jar mi_texto.txt 1 7 --palabra=hola");
        System.err.println("  java -jar TuberiasFiltros-pipeline.jar imagen.png 3");
    }
}
