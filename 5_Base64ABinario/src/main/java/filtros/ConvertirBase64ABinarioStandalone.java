package filtros;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;

/**
 * Programa standalone para el filtro ID5 — Convertir Base64 a Binario.
 *
 * Lee un PaqueteDatos (tipo BASE64) desde stdin, decodifica,
 * y escribe el resultado (tipo BINARIO) en stdout.
 *
 * Uso en pipeline:
 *   ... | java -jar 5-Base64ABinario.jar | ...
 */
public class ConvertirBase64ABinarioStandalone {

    public static void main(String[] args) {
        try {
            java.io.PrintStream stdoutReal = System.out;
            System.setOut(System.err);

            System.err.println("[Filtro 5] Iniciando — Convertir Base64 a Binario");

            PaqueteDatos entrada = ProtocoloPipe.leer(System.in);
            System.err.println("[Filtro 5] Entrada recibida: " + entrada.getTipo()
                    + " (" + entrada.getTamanioBytes() + " bytes)");

            ConvertirBase64ABinario filtro = new ConvertirBase64ABinario();
            PaqueteDatos resultado = filtro.procesar(entrada);

            System.err.println("[Filtro 5] Resultado: " + resultado.getTipo()
                    + " (" + resultado.getTamanioBytes() + " bytes)");

            ProtocoloPipe.escribir(stdoutReal, resultado);
            System.err.println("[Filtro 5] Salida escrita correctamente.");

        } catch (Exception e) {
            System.err.println("[Filtro 5] ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
