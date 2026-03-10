package filtros;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;

/**
 * Programa standalone para el filtro ID6 — Encriptar SHA-256.
 *
 * Lee un PaqueteDatos (tipo TEXTO) desde stdin, genera el hash SHA-256,
 * y escribe el resultado (tipo TEXTO) en stdout.
 *
 * Uso en pipeline:
 *   ... | java -jar 6-EncriptarSHA256.jar | ...
 */
public class EncriptarArchivoTextoSHA256Standalone {

    public static void main(String[] args) {
        try {
            java.io.PrintStream stdoutReal = System.out;
            System.setOut(System.err);

            System.err.println("[Filtro 6] Iniciando — Encriptar SHA-256");

            PaqueteDatos entrada = ProtocoloPipe.leer(System.in);
            System.err.println("[Filtro 6] Entrada recibida: " + entrada.getTipo()
                    + " (" + entrada.getTamanioBytes() + " bytes)");

            EncriptarArchivoTextoSHA256 filtro = new EncriptarArchivoTextoSHA256();
            PaqueteDatos resultado = filtro.procesar(entrada);

            System.err.println("[Filtro 6] Resultado: " + resultado.getTipo()
                    + " (" + resultado.getTamanioBytes() + " bytes)");

            ProtocoloPipe.escribir(stdoutReal, resultado);
            System.err.println("[Filtro 6] Salida escrita correctamente.");

        } catch (Exception e) {
            System.err.println("[Filtro 6] ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
