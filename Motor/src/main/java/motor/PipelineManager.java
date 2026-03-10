package motor;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de pipelines a nivel de sistema operativo.
 *
 * Ejecuta una cadena de filtros como procesos independientes,
 * comunicándose mediante stdin/stdout usando el protocolo binario
 * de {@link ProtocoloPipe}.
 *
 * Cada filtro se ejecuta como un proceso Java independiente (JAR ejecutable).
 * La comunicación se realiza de forma secuencial:
 *   entrada → proceso1 → resultado1 → proceso2 → resultado2 → ... → resultado final
 *
 * Los mensajes de log de cada filtro van a stderr para no interferir
 * con los datos binarios de la tubería (que van por stdout).
 *
 * Patrón: Pipe-and-Filter a nivel de SO (inter-process communication).
 */
public class PipelineManager {

    /**
     * Representa un paso del pipeline: un JAR ejecutable con argumentos opcionales.
     */
    public static class PasoFiltro {
        private final String jarPath;
        private final List<String> argumentos;

        public PasoFiltro(String jarPath) {
            this.jarPath = jarPath;
            this.argumentos = new ArrayList<>();
        }

        public PasoFiltro(String jarPath, String... args) {
            this.jarPath = jarPath;
            this.argumentos = new ArrayList<>();
            for (String a : args) {
                if (a != null && !a.isEmpty()) argumentos.add(a);
            }
        }

        public String getJarPath()          { return jarPath; }
        public List<String> getArgumentos() { return argumentos; }
    }

    // ══════════════════════════════════════════════════════════════
    // Ejecución del pipeline
    // ══════════════════════════════════════════════════════════════

    /**
     * Ejecuta un pipeline de filtros como procesos independientes.
     *
     * @param entradaInicial PaqueteDatos con los datos de entrada.
     * @param pasos          Lista ordenada de filtros (JARs) a ejecutar.
     * @return PaqueteDatos resultado de la última etapa del pipeline.
     * @throws IOException          Si hay error de E/S en algún proceso.
     * @throws InterruptedException Si algún proceso es interrumpido.
     * @throws RuntimeException     Si algún proceso termina con código ≠ 0.
     */
    public PaqueteDatos ejecutar(PaqueteDatos entradaInicial, List<PasoFiltro> pasos)
            throws IOException, InterruptedException {

        if (pasos == null || pasos.isEmpty()) {
            throw new IllegalArgumentException("El pipeline debe tener al menos un paso.");
        }

        System.err.println("\n╔══════════════════════════════════════════════════╗");
        System.err.println("║  Pipeline Manager — " + pasos.size() + " filtro(s)");
        System.err.println("╚══════════════════════════════════════════════════╝");

        PaqueteDatos actual = entradaInicial;

        for (int i = 0; i < pasos.size(); i++) {
            PasoFiltro paso = pasos.get(i);
            System.err.println("\n── Paso " + (i + 1) + "/" + pasos.size()
                    + " — " + paso.getJarPath() + " ──");

            actual = ejecutarPaso(actual, paso);

            System.err.println("  → Resultado: " + actual.getTipo()
                    + " (" + actual.getTamanioBytes() + " bytes)");
        }

        System.err.println("\n✔ Pipeline completado exitosamente.");
        return actual;
    }

    /**
     * Ejecuta un único paso del pipeline como proceso Java independiente.
     *
     * @param entrada Datos de entrada para este paso.
     * @param paso    Definición del filtro JAR a ejecutar.
     * @return PaqueteDatos resultado de este paso.
     */
    private PaqueteDatos ejecutarPaso(PaqueteDatos entrada, PasoFiltro paso)
            throws IOException, InterruptedException {

        // ── Construir el comando ─────────────────────────────────
        List<String> comando = new ArrayList<>();
        comando.add("java");
        comando.add("-jar");
        comando.add(paso.getJarPath());
        comando.addAll(paso.getArgumentos());

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(false); // stderr separado para leer logs

        // ── Iniciar proceso ──────────────────────────────────────
        Process proceso = pb.start();

        // ── Escribir entrada al stdin del proceso (en hilo aparte para evitar deadlock) ──
        Thread escritor = new Thread(() -> {
            try {
                ProtocoloPipe.escribir(proceso.getOutputStream(), entrada);
                proceso.getOutputStream().close();
            } catch (IOException e) {
                System.err.println("  [PipelineManager] Error escribiendo al proceso: "
                        + e.getMessage());
            }
        }, "pipe-writer");
        escritor.start();

        // ── Leer stderr del proceso (logs) en hilo aparte ────────
        StringBuilder stderrLog = new StringBuilder();
        Thread lectorErr = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(proceso.getErrorStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    stderrLog.append("  ").append(linea).append("\n");
                }
            } catch (IOException ignored) { }
        }, "pipe-stderr-reader");
        lectorErr.start();

        // ── Leer resultado del stdout del proceso ─────────────────
        PaqueteDatos resultado = ProtocoloPipe.leer(proceso.getInputStream());

        // ── Esperar a que termine ─────────────────────────────────
        escritor.join();
        lectorErr.join();
        int exitCode = proceso.waitFor();

        // Mostrar logs del filtro
        if (stderrLog.length() > 0) {
            System.err.print(stderrLog);
        }

        if (exitCode != 0) {
            throw new RuntimeException(
                    "El filtro '" + paso.getJarPath()
                    + "' terminó con código de error: " + exitCode);
        }

        return resultado;
    }

    // ══════════════════════════════════════════════════════════════
    // Catálogo de JARs
    // ══════════════════════════════════════════════════════════════

    /** Nombres de los JARs generados por maven-shade-plugin. */
    private static final String[] JAR_NAMES = {
        "1-CargarArchivo-1.0-SNAPSHOT.jar",       // ID1
        "2-TextoABinario-1.0-SNAPSHOT.jar",       // ID2
        "3-FiltroImagenes-1.0-SNAPSHOT.jar",      // ID3
        "4-BinarioABase64-1.0-SNAPSHOT.jar",      // ID4
        "5-Base64ABinario-1.0-SNAPSHOT.jar",      // ID5
        "6-EncriptarSHA256-1.0-SNAPSHOT.jar",     // ID6
        "7-BuscarPalabra-1.0-SNAPSHOT.jar",       // ID7
        "8-ContarOcurrencias-1.0-SNAPSHOT.jar",   // ID8
    };

    /**
     * Devuelve el nombre del JAR para un filtro dado su ID (1–8).
     *
     * @param idFiltro ID del filtro (1–8).
     * @return Nombre del archivo JAR.
     */
    public static String getJarName(int idFiltro) {
        if (idFiltro < 1 || idFiltro > JAR_NAMES.length) {
            throw new IllegalArgumentException("ID de filtro inválido: " + idFiltro);
        }
        return JAR_NAMES[idFiltro - 1];
    }

    /**
     * Busca el directorio base que contenga los JARs de los filtros.
     * Busca en el directorio dado y en los subdirectorios target/ de cada módulo.
     *
     * @param baseDir Directorio raíz del proyecto.
     * @return Mapa de rutas a los JARs encontrados.
     */
    public static String resolverJarPath(String baseDir, int idFiltro)
            throws FileNotFoundException {
        String jarName = getJarName(idFiltro);

        // Nombres de carpetas de módulo por ID
        String[] moduleDirs = {
            "1_CargarArchivo", "2_TextoABinario", "3_FiltroImagenes",
            "4_BinarioABase64", "5_Base64ABinario", "6_EncriptarSHA256",
            "7_BuscarPalabra", "8_ContarOcurrencias"
        };

        String moduleDir = moduleDirs[idFiltro - 1];
        File jarFile = new File(baseDir, moduleDir + "/target/" + jarName);

        if (jarFile.exists()) {
            return jarFile.getAbsolutePath();
        }

        // Buscar en el directorio base directamente
        File directJar = new File(baseDir, jarName);
        if (directJar.exists()) {
            return directJar.getAbsolutePath();
        }

        throw new FileNotFoundException("No se encontró el JAR: " + jarName
                + "\nBuscado en:\n  " + jarFile.getAbsolutePath()
                + "\n  " + directJar.getAbsolutePath()
                + "\nEjecuta 'mvn clean package' primero.");
    }
}
