package filtros;

import datos.PaqueteDatos;
import datos.TipoDato;
import interfaces.IFiltroMultiple;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ID3 - FiltroImagenes
 *
 * Recibe UNA imagen o MÚLTIPLES imágenes (directorio) y aplica 4 transformaciones
 * en paralelo (un hilo por filtro), devolviendo imágenes resultado en LISTA_IMAGEN.
 *
 * IN:  IMAGEN       → bytes PNG de una imagen (1 imagen → 4 resultados)
 *      LISTA_IMAGEN → lista de imágenes desde un directorio (N imágenes → 4×N resultados)
 * OUT: LISTA_IMAGEN → lista con las imágenes filtradas:
 *        Por cada imagen: [gris, reducción 50%, brillo +50%, rotación 90°]
 *
 * Procesamiento concurrente: 4 hilos por imagen, una por transformación.
 * Reutiliza API de Java AWT/ImageIO (sin librerías externas).
 */
public class FiltroImagenes implements IFiltroMultiple {

    @Override
    public PaqueteDatos procesar(PaqueteDatos entrada) {
        // Si recibe una lista de imágenes (directorio), procesar todas
        if (entrada.esLista()) {
            return procesarMultiples(entrada);
        }
        return procesarUnaImagen(entrada);
    }

    /** Procesa una única imagen aplicando 4 filtros en paralelo. */
    private PaqueteDatos procesarUnaImagen(PaqueteDatos entrada) {
        // ── Leer la imagen de los bytes de entrada ────────────────
        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(entrada.getDatos()));
            if (original == null) {
                System.err.println("  [ID3] ERROR: No se pudo leer la imagen (formato no soportado).");
                return new PaqueteDatos(TipoDato.LISTA_IMAGEN, Collections.emptyList());
            }
        } catch (IOException e) {
            System.err.println("  [ID3] ERROR leyendo imagen: " + e.getMessage());
            return new PaqueteDatos(TipoDato.LISTA_IMAGEN, Collections.emptyList());
        }

        System.out.println("  [ID3] Imagen cargada: " + original.getWidth()
                + "x" + original.getHeight() + " px. Aplicando 4 filtros con hilos...");

        List<byte[]> resultados = aplicar4Filtros(original, "");
        List<String> nombres = new ArrayList<>();
        String[] sufijos = {"gris", "reduccion", "brillo", "rotacion"};
        for (String s : sufijos) nombres.add(s);

        System.out.println("  [ID3] 4 imágenes generadas correctamente.");
        return new PaqueteDatos(TipoDato.LISTA_IMAGEN, resultados, nombres);
    }

    /** Procesa múltiples imágenes de un directorio, aplicando 4 filtros a cada una. */
    private PaqueteDatos procesarMultiples(PaqueteDatos entrada) {
        List<byte[]> imagenes = entrada.getLista();
        List<String> nombresEntrada = entrada.getNombres();

        System.out.println("  [ID3] Procesando " + imagenes.size()
                + " imágenes del directorio...");

        List<byte[]> todosResultados = new ArrayList<>();
        List<String> todosNombres = new ArrayList<>();
        String[] sufijos = {"_gris", "_reduccion", "_brillo", "_rotacion"};

        for (int imgIdx = 0; imgIdx < imagenes.size(); imgIdx++) {
            byte[] imgBytes = imagenes.get(imgIdx);
            String nombreBase = (nombresEntrada != null && imgIdx < nombresEntrada.size())
                    ? nombresEntrada.get(imgIdx).replaceFirst("\\.[^.]+$", "")
                    : "imagen_" + imgIdx;

            BufferedImage original;
            try {
                original = ImageIO.read(new ByteArrayInputStream(imgBytes));
                if (original == null) {
                    System.err.println("  [ID3] No se pudo leer la imagen: " + nombreBase);
                    continue;
                }
            } catch (IOException e) {
                System.err.println("  [ID3] Error leyendo " + nombreBase + ": " + e.getMessage());
                continue;
            }

            System.out.println("  [ID3] Procesando: " + nombreBase + " ("
                    + original.getWidth() + "x" + original.getHeight() + " px)");

            List<byte[]> resultados4 = aplicar4Filtros(original, nombreBase);
            for (int j = 0; j < 4; j++) {
                if (resultados4.get(j) != null) {
                    todosResultados.add(resultados4.get(j));
                    todosNombres.add(nombreBase + sufijos[j]);
                }
            }
        }

        System.out.println("  [ID3] Total: " + todosResultados.size()
                + " imágenes generadas de " + imagenes.size() + " originales.");
        return new PaqueteDatos(TipoDato.LISTA_IMAGEN, todosResultados, todosNombres);
    }

    /**
     * Aplica los 4 filtros a una imagen usando hilos concurrentes.
     * @return Lista de 4 byte[] (gris, reducción, brillo, rotación).
     */
    private List<byte[]> aplicar4Filtros(BufferedImage original, String label) {
        List<byte[]> resultados = new ArrayList<>(Collections.nCopies(4, null));
        ExecutorService pool = Executors.newFixedThreadPool(getNumHilos());
        List<Future<?>> futures = new ArrayList<>();

        // Hilo 0 — Escala de grises
        futures.add(pool.submit(() -> {
            try {
                resultados.set(0, toBytes(aplicarGris(original)));
                System.out.println("  [ID3] " + label + " Hilo-0: Escala de grises ✔");
            } catch (Exception e) {
                System.err.println("  [ID3] Hilo-0 ERROR: " + e.getMessage());
            }
        }));

        // Hilo 1 — Reducción de tamaño 50%
        futures.add(pool.submit(() -> {
            try {
                resultados.set(1, toBytes(reducirTamanio(original)));
                System.out.println("  [ID3] " + label + " Hilo-1: Reducción de tamaño ✔");
            } catch (Exception e) {
                System.err.println("  [ID3] Hilo-1 ERROR: " + e.getMessage());
            }
        }));

        // Hilo 2 — Brillo aumentado
        futures.add(pool.submit(() -> {
            try {
                resultados.set(2, toBytes(ajustarBrillo(original)));
                System.out.println("  [ID3] " + label + " Hilo-2: Brillo ✔");
            } catch (Exception e) {
                System.err.println("  [ID3] Hilo-2 ERROR: " + e.getMessage());
            }
        }));

        // Hilo 3 — Rotación 90°
        futures.add(pool.submit(() -> {
            try {
                resultados.set(3, toBytes(rotar(original, 90)));
                System.out.println("  [ID3] " + label + " Hilo-3: Rotación 90° ✔");
            } catch (Exception e) {
                System.err.println("  [ID3] Hilo-3 ERROR: " + e.getMessage());
            }
        }));

        // ── Esperar a que terminen todos los hilos ─────────────────
        for (Future<?> f : futures) {
            try { f.get(); }
            catch (Exception e) { System.err.println("  [ID3] Error esperando hilo: " + e.getMessage()); }
        }
        pool.shutdown();
        return resultados;
    }

    // ── Transformaciones de imagen (reutiliza java.awt built-in) ──

    /** Convierte a escala de grises usando TYPE_BYTE_GRAY. */
    private BufferedImage aplicarGris(BufferedImage original) {
        BufferedImage gris = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gris.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return gris;
    }

    /** Reduce el tamaño al 50% con interpolación bilineal. */
    private BufferedImage reducirTamanio(BufferedImage original) {
        int nw = Math.max(1, original.getWidth() / 2);
        int nh = Math.max(1, original.getHeight() / 2);
        BufferedImage reducida = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = reducida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, nw, nh, null);
        g.dispose();
        return reducida;
    }

    /**
     * Aumenta el brillo con RescaleOp (factor 1.5).
     * Reutiliza java.awt.image.RescaleOp (built-in Java).
     */
    private BufferedImage ajustarBrillo(BufferedImage original) {
        // RescaleOp requiere imagen RGB (no ARGB con canal alpha)
        BufferedImage rgb = convertirARGB(original);
        float[] scales  = {1.5f, 1.5f, 1.5f};
        float[] offsets = {0f, 0f, 0f};
        RescaleOp op = new RescaleOp(scales, offsets, null);
        return op.filter(rgb, null);
    }

    /**
     * Rota la imagen el ángulo indicado (sentido horario).
     * Soporta 45°, 90°, 180° y cualquier ángulo.
     * Calcula el nuevo tamaño para que la imagen completa quede visible.
     */
    private BufferedImage rotar(BufferedImage original, double anguloDeg) {
        double rad = Math.toRadians(anguloDeg);
        double sin = Math.abs(Math.sin(rad));
        double cos = Math.abs(Math.cos(rad));
        int w = original.getWidth();
        int h = original.getHeight();
        int nw = (int) Math.round(w * cos + h * sin);
        int nh = (int) Math.round(h * cos + w * sin);

        BufferedImage rotada = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rotada.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.translate(nw / 2.0, nh / 2.0);
        g.rotate(rad);
        g.translate(-w / 2.0, -h / 2.0);
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return rotada;
    }

    /** Convierte a TYPE_INT_RGB para compatibilidad con RescaleOp. */
    private BufferedImage convertirARGB(BufferedImage original) {
        if (original.getType() == BufferedImage.TYPE_INT_RGB) return original;
        BufferedImage rgb = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return rgb;
    }

    /** Serializa un BufferedImage a bytes PNG. */
    private byte[] toBytes(BufferedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    @Override public int      getId()          { return 3; }
    @Override public String   getNombre()       { return "FiltroImagenes"; }
    @Override public String   getDescripcion()  {
        return "Aplica 4 filtros a una imagen o directorio de imágenes: grises, reducción, brillo, rotación 90°.";
    }
    @Override public TipoDato getTipoEntrada()  { return TipoDato.IMAGEN; }
    @Override public TipoDato getTipoSalida()   { return TipoDato.LISTA_IMAGEN; }
    @Override public int      getNumHilos()     { return 4; }
}
