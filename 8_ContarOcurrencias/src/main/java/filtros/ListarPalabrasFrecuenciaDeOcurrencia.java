package filtros;

import datos.PaqueteDatos;
import datos.TipoDato;
import interfaces.IFiltroMultiple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ID8 — ContarOcurrenciasDePalabra
 *
 * Recibe un archivo de texto y una palabra.
 * Devuelve el número EXACTO de veces que aparece esa palabra en el texto.
 *
 * IN:  TEXTO  (o LISTA_TEXTO — cuenta en todos y da total)
 * OUT: TEXTO  ("La palabra '...' aparece N veces.")
 *
 * Diferencia con ID7:
 *   ID7 → SÍ / NO (¿existe la palabra?)
 *   ID8 → CONTEO  (¿cuántas veces aparece?)
 *
 * La palabra se pasa por constructor (igual que ID7).
 */
public class ListarPalabrasFrecuenciaDeOcurrencia implements IFiltroMultiple {

    private final String palabraBuscar;

    /**
     * @param palabraBuscar Palabra cuyas ocurrencias se contarán (insensible a mayúsculas).
     */
    public ListarPalabrasFrecuenciaDeOcurrencia(String palabraBuscar) {
        this.palabraBuscar = palabraBuscar.trim().toLowerCase();
    }

    @Override
    public PaqueteDatos procesar(PaqueteDatos entrada) {
        // Auto-wrap texto simple → lista
        PaqueteDatos lista = entrada.esLista() ? entrada : entrada.aLista();
        List<byte[]> textos = lista.getLista();
        List<String> nombres = lista.getNombres();

        System.out.println("  [ID8] Contando \"" + palabraBuscar + "\" en "
                + textos.size() + " texto(s) con " + getNumHilos() + " hilos...");

        // ── Contar en paralelo ──────────────────────────────────────
        AtomicInteger totalGlobal = new AtomicInteger(0);
        List<int[]> conteoPorTexto = new ArrayList<>();
        for (int i = 0; i < textos.size(); i++) conteoPorTexto.add(new int[]{0});

        ExecutorService pool = Executors.newFixedThreadPool(
                Math.min(getNumHilos(), Math.max(1, textos.size())));
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < textos.size(); i++) {
            final int idx = i;
            final byte[] bytes = textos.get(i);
            final String nombreArchivo = (nombres != null && i < nombres.size())
                    ? nombres.get(i) : null;
            futures.add(pool.submit(() -> {
                int count = contarOcurrencias(new String(bytes).toLowerCase());
                conteoPorTexto.get(idx)[0] = count;
                totalGlobal.addAndGet(count);
                String label = nombreArchivo != null ? nombreArchivo : "Hilo-" + idx;
                System.out.println("  [ID8] " + label + ": " + count + " ocurrencia(s) ✔");
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); }
            catch (Exception e) { System.err.println("  [ID8] Error hilo: " + e.getMessage()); }
        }
        pool.shutdown();

        // ── Ensamblar respuesta ─────────────────────────────────────
        StringBuilder sb = new StringBuilder();
        sb.append("Conteo de \"").append(palabraBuscar).append("\"\n");
        sb.append("─".repeat(50)).append("\n");

        if (textos.size() == 1) {
            int c = conteoPorTexto.get(0)[0];
            if (nombres != null && !nombres.isEmpty()) {
                sb.append("Archivo: ").append(nombres.get(0)).append("\n");
            }
            sb.append(c > 0
                    ? "La palabra \"" + palabraBuscar + "\" aparece " + c + " veces en el texto."
                    : "La palabra \"" + palabraBuscar + "\" no aparece en el texto.");
        } else {
            for (int i = 0; i < conteoPorTexto.size(); i++) {
                String label = (nombres != null && i < nombres.size())
                        ? "Archivo: " + nombres.get(i)
                        : "Texto #" + (i + 1);
                sb.append(label).append(": ")
                  .append(conteoPorTexto.get(i)[0]).append(" ocurrencia(s)\n");
            }
            sb.append("─".repeat(50)).append("\n");
            sb.append("Total: ").append(totalGlobal.get()).append(" ocurrencia(s)");
        }

        return new PaqueteDatos(TipoDato.TEXTO, sb.toString().getBytes());
    }

    private int contarOcurrencias(String textoEnMinusculas) {
        int count = 0, idx = 0;
        while ((idx = textoEnMinusculas.indexOf(palabraBuscar, idx)) != -1) {
            count++;
            idx += palabraBuscar.length();
        }
        return count;
    }

    @Override public int      getId()          { return 8; }
    @Override public String   getNombre()       { return "ContarOcurrenciasDePalabra"; }
    @Override public String   getDescripcion()  {
        return "Cuenta ocurrencias de una palabra en un archivo o directorio de archivos.";
    }
    @Override public TipoDato getTipoEntrada()  { return TipoDato.TEXTO; }
    @Override public TipoDato getTipoSalida()   { return TipoDato.TEXTO; }
    @Override public int      getNumHilos()     { return 4; }
}
