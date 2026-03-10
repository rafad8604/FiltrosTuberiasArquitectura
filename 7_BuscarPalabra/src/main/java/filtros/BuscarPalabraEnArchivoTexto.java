package filtros;

import datos.PaqueteDatos;
import datos.TipoDato;
import interfaces.IFiltroMultiple;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ID7 — BuscarPalabraEnArchivoTexto
 *
 * Recibe un archivo de texto y una palabra.
 * Devuelve un mensaje indicando si la palabra EXISTE o NO en el texto.
 *
 * IN:  TEXTO  (o LISTA_TEXTO — busca en todos y agrupa)
 * OUT: TEXTO  ("SÍ / NO" con detalle de líneas)
 *
 * La palabra se pasa por constructor; el Motor y la VistaEscritorio
 * crean la instancia con la palabra que el usuario ingresó.
 */
public class BuscarPalabraEnArchivoTexto implements IFiltroMultiple {

    private final String palabraBuscar;

    /**
     * @param palabraBuscar Palabra a buscar (insensible a mayúsculas).
     */
    public BuscarPalabraEnArchivoTexto(String palabraBuscar) {
        this.palabraBuscar = palabraBuscar.trim().toLowerCase();
    }

    @Override
    public PaqueteDatos procesar(PaqueteDatos entrada) {
        // Auto-wrap texto simple → lista para procesamiento uniforme
        PaqueteDatos lista = entrada.esLista() ? entrada : entrada.aLista();
        List<byte[]> textos = lista.getLista();
        List<String> nombres = lista.getNombres();

        System.out.println("  [ID7] Buscando \"" + palabraBuscar + "\" en "
                + textos.size() + " texto(s) con " + getNumHilos() + " hilos...");

        // ── Buscar en paralelo ──────────────────────────────────────
        ExecutorService pool = Executors.newFixedThreadPool(
                Math.min(getNumHilos(), Math.max(1, textos.size())));

        List<String> parciales = new java.util.ArrayList<>();
        for (int i = 0; i < textos.size(); i++) parciales.add(null);

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < textos.size(); i++) {
            final int idx = i;
            final byte[] bytes = textos.get(i);
            final String nombreArchivo = (nombres != null && i < nombres.size())
                    ? nombres.get(i) : null;
            futures.add(pool.submit(() -> {
                String texto = new String(bytes).toLowerCase();
                int ocurrencias = contarOcurrencias(texto);
                List<Integer> lineas = buscarLineas(new String(bytes));

                String resultado = (ocurrencias > 0)
                        ? "✔ SÍ — La palabra \"" + palabraBuscar
                          + "\" fue encontrada. Ocurrencias: " + ocurrencias
                          + (lineas.isEmpty() ? "" : " | Líneas: " + lineas)
                        : "✘ NO — La palabra \"" + palabraBuscar
                          + "\" no fue encontrada en el texto.";

                parciales.set(idx, resultado);
                String label = nombreArchivo != null ? nombreArchivo : "Hilo-" + idx;
                System.out.println("  [ID7] " + label + ": " + resultado);
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); }
            catch (Exception e) { System.err.println("  [ID7] Error hilo: " + e.getMessage()); }
        }
        pool.shutdown();

        // ── Ensamblar respuesta ─────────────────────────────────────
        StringBuilder sb = new StringBuilder();
        sb.append("Búsqueda de \"").append(palabraBuscar).append("\"\n");
        sb.append("─".repeat(50)).append("\n");
        for (int i = 0; i < parciales.size(); i++) {
            if (nombres != null && i < nombres.size()) {
                sb.append("Archivo: ").append(nombres.get(i)).append("\n  → ");
            } else if (parciales.size() > 1) {
                sb.append("Texto #").append(i + 1).append(": ");
            }
            sb.append(parciales.get(i) != null ? parciales.get(i) : "[error]").append("\n");
            if (nombres != null) sb.append("\n");
        }

        return new PaqueteDatos(TipoDato.TEXTO, sb.toString().getBytes());
    }

    // ── Utilidades ────────────────────────────────────────────────

    private int contarOcurrencias(String textoEnMinusculas) {
        int count = 0, idx = 0;
        while ((idx = textoEnMinusculas.indexOf(palabraBuscar, idx)) != -1) {
            count++;
            idx += palabraBuscar.length();
        }
        return count;
    }

    private List<Integer> buscarLineas(String texto) {
        List<Integer> lineas = new java.util.ArrayList<>();
        String[] partes = texto.split("\n");
        for (int i = 0; i < partes.length; i++) {
            if (partes[i].toLowerCase().contains(palabraBuscar)) lineas.add(i + 1);
        }
        return lineas;
    }

    @Override public int      getId()          { return 7; }
    @Override public String   getNombre()       { return "BuscarPalabraEnArchivoTexto"; }
    @Override public String   getDescripcion()  {
        return "Busca una palabra en un archivo o directorio de archivos (indica línea y archivo).";
    }
    @Override public TipoDato getTipoEntrada()  { return TipoDato.TEXTO; }
    @Override public TipoDato getTipoSalida()   { return TipoDato.TEXTO; }
    @Override public int      getNumHilos()     { return 4; }
}
