package filtros;

import datos.PaqueteDatos;
import datos.TipoDato;
import interfaces.IFiltroMultiple;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ID4 - ConvertirBinarioABase64
 *
 * Codifica en Base64 cada elemento de una lista de datos binarios.
 * Cada elemento se procesa en un hilo independiente (concurrente).
 *
 * IN:  LISTA_BINARIO → lista de byte[] con datos binarios
 * OUT: LISTA_BASE64  → lista de byte[] con los datos en Base64
 *
 * Si recibe un BINARIO simple (único), lo auto-envuelve en lista de 1.
 * Usa java.util.Base64 (built-in Java 8+). Sin dependencias externas.
 */
public class ConvertirBinarioABase64 implements IFiltroMultiple {

    @Override
    public PaqueteDatos procesar(PaqueteDatos entrada) {
        // Auto-wrap: si recibe elemento único, conviértelo en lista
        PaqueteDatos paqueteLista = entrada.esLista() ? entrada : entrada.aLista();
        List<byte[]> entradas = paqueteLista.getLista();
        List<String> nombresEntrada = paqueteLista.getNombres();

        System.out.println("  [ID4] Codificando " + entradas.size() + " elemento(s) a Base64 con hilos...");

        List<byte[]> resultados = new ArrayList<>(Collections.nCopies(entradas.size(), null));
        List<String> nombresResultado = new ArrayList<>(Collections.nCopies(entradas.size(), null));
        ExecutorService pool = Executors.newFixedThreadPool(
                Math.min(getNumHilos(), entradas.size() > 0 ? entradas.size() : 1));
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < entradas.size(); i++) {
            final int idx  = i;
            final byte[] item = entradas.get(i);
            final String nombreOrig = (nombresEntrada != null && i < nombresEntrada.size())
                    ? nombresEntrada.get(i) : null;
            futures.add(pool.submit(() -> {
                byte[] encoded = Base64.getEncoder().encode(item);
                resultados.set(idx, encoded);
                String nombreBase = (nombreOrig != null)
                        ? nombreOrig.replaceFirst("\\.[^.]+$", "") + "_base64"
                        : "elemento_" + idx + "_base64";
                nombresResultado.set(idx, nombreBase);
                System.out.println("  [ID4] Hilo-" + idx + ": "
                        + (nombreOrig != null ? nombreOrig + " " : "")
                        + "codificado (" + encoded.length + " bytes) ✔");
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); }
            catch (Exception e) { System.err.println("  [ID4] Error en hilo: " + e.getMessage()); }
        }
        pool.shutdown();

        return new PaqueteDatos(TipoDato.LISTA_BASE64, resultados, nombresResultado);
    }

    @Override public int      getId()          { return 4; }
    @Override public String   getNombre()       { return "ConvertirBinarioABase64"; }
    @Override public String   getDescripcion()  { return "Codifica datos binarios (archivo o directorio) a Base64 usando hilos concurrentes."; }
    @Override public TipoDato getTipoEntrada()  { return TipoDato.LISTA_BINARIO; }
    @Override public TipoDato getTipoSalida()   { return TipoDato.LISTA_BASE64; }
    @Override public int      getNumHilos()     { return 4; }
}
