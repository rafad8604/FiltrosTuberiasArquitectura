package filtros;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;

/**
 * Programa standalone para el filtro ID3 — Filtro de Imágenes.
 *
 * Lee un PaqueteDatos (tipo IMAGEN o LISTA_IMAGEN) desde stdin,
 * aplica los 4 filtros de imagen en paralelo, y escribe el resultado
 * (tipo LISTA_IMAGEN) en stdout.
 *
 * Uso en pipeline:
 *   ... | java -jar 3-FiltroImagenes.jar | ...
 */
public class FiltroImagenesStandalone {

    public static void main(String[] args) {
        try {
            java.io.PrintStream stdoutReal = System.out;
            System.setOut(System.err);

            System.err.println("[Filtro 3] Iniciando — Filtro de Imágenes (4 hilos)");

            PaqueteDatos entrada = ProtocoloPipe.leer(System.in);
            System.err.println("[Filtro 3] Entrada recibida: " + entrada.getTipo()
                    + " (" + entrada.getTamanioBytes() + " bytes)");

            FiltroImagenes filtro = new FiltroImagenes();
            PaqueteDatos resultado = filtro.procesar(entrada);

            System.err.println("[Filtro 3] Resultado: " + resultado.getTipo()
                    + " (" + resultado.getTamanioBytes() + " bytes, "
                    + (resultado.getLista() != null ? resultado.getLista().size() : 0)
                    + " imágenes)");

            ProtocoloPipe.escribir(stdoutReal, resultado);
            System.err.println("[Filtro 3] Salida escrita correctamente.");

        } catch (Exception e) {
            System.err.println("[Filtro 3] ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
