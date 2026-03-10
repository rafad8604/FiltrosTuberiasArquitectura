package filtros;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;

/**
 * Programa standalone para el filtro ID4 — Convertir Binario a Base64.
 *
 * Lee un PaqueteDatos (tipo BINARIO o LISTA_BINARIO) desde stdin,
 * codifica en Base64, y escribe el resultado (tipo LISTA_BASE64) en stdout.
 *
 * Uso en pipeline:
 *   ... | java -jar 4-BinarioABase64.jar | ...
 */
public class ConvertirBinarioABase64Standalone {

    public static void main(String[] args) {
        try {
            java.io.PrintStream stdoutReal = System.out;
            System.setOut(System.err);

            System.err.println("[Filtro 4] Iniciando — Convertir Binario a Base64");

            PaqueteDatos entrada = ProtocoloPipe.leer(System.in);
            System.err.println("[Filtro 4] Entrada recibida: " + entrada.getTipo()
                    + " (" + entrada.getTamanioBytes() + " bytes)");

            ConvertirBinarioABase64 filtro = new ConvertirBinarioABase64();
            PaqueteDatos resultado = filtro.procesar(entrada);

            System.err.println("[Filtro 4] Resultado: " + resultado.getTipo()
                    + " (" + resultado.getTamanioBytes() + " bytes)");

            ProtocoloPipe.escribir(stdoutReal, resultado);
            System.err.println("[Filtro 4] Salida escrita correctamente.");

        } catch (Exception e) {
            System.err.println("[Filtro 4] ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
