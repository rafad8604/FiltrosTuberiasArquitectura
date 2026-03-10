package filtros;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;

/**
 * Programa standalone para el filtro ID2 — Convertir Texto a Binario.
 *
 * Lee un PaqueteDatos (tipo TEXTO) desde stdin, ejecuta el filtro,
 * y escribe el resultado (tipo BINARIO) en stdout.
 *
 * Uso en pipeline:
 *   ... | java -jar 2-TextoABinario.jar | ...
 */
public class ConvertirTextoABinarioStandalone {

    public static void main(String[] args) {
        try {
            java.io.PrintStream stdoutReal = System.out;
            System.setOut(System.err);

            System.err.println("[Filtro 2] Iniciando — Convertir Texto a Binario");

            PaqueteDatos entrada = ProtocoloPipe.leer(System.in);
            System.err.println("[Filtro 2] Entrada recibida: " + entrada.getTipo()
                    + " (" + entrada.getTamanioBytes() + " bytes)");

            ConvertirTextoABinario filtro = new ConvertirTextoABinario();
            PaqueteDatos resultado = filtro.procesar(entrada);

            System.err.println("[Filtro 2] Resultado: " + resultado.getTipo()
                    + " (" + resultado.getTamanioBytes() + " bytes)");

            ProtocoloPipe.escribir(stdoutReal, resultado);
            System.err.println("[Filtro 2] Salida escrita correctamente.");

        } catch (Exception e) {
            System.err.println("[Filtro 2] ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
