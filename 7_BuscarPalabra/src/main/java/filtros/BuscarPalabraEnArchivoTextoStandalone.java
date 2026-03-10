package filtros;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;

/**
 * Programa standalone para el filtro ID7 — Buscar Palabra en Texto.
 *
 * Lee un PaqueteDatos (tipo TEXTO) desde stdin, busca la palabra,
 * y escribe el resultado (tipo TEXTO) en stdout.
 *
 * La palabra a buscar se obtiene de (en orden de prioridad):
 *   1. Primer argumento de línea de comandos: args[0]
 *   2. Campo 'parametros' del PaqueteDatos de entrada
 *   3. Valor por defecto: "la"
 *
 * Uso en pipeline:
 *   ... | java -jar 7-BuscarPalabra.jar "hola" | ...
 */
public class BuscarPalabraEnArchivoTextoStandalone {

    public static void main(String[] args) {
        try {
            java.io.PrintStream stdoutReal = System.out;
            System.setOut(System.err);

            System.err.println("[Filtro 7] Iniciando — Buscar Palabra en Texto");

            PaqueteDatos entrada = ProtocoloPipe.leer(System.in);
            System.err.println("[Filtro 7] Entrada recibida: " + entrada.getTipo()
                    + " (" + entrada.getTamanioBytes() + " bytes)");

            // Determinar la palabra a buscar
            String palabra;
            if (args.length > 0 && !args[0].trim().isEmpty()) {
                palabra = args[0].trim();
            } else if (entrada.getParametros() != null && !entrada.getParametros().isEmpty()) {
                palabra = entrada.getParametros();
            } else {
                palabra = "la";
            }
            System.err.println("[Filtro 7] Palabra a buscar: \"" + palabra + "\"");

            BuscarPalabraEnArchivoTexto filtro = new BuscarPalabraEnArchivoTexto(palabra);
            PaqueteDatos resultado = filtro.procesar(entrada);

            System.err.println("[Filtro 7] Resultado: " + resultado.getTipo()
                    + " (" + resultado.getTamanioBytes() + " bytes)");

            ProtocoloPipe.escribir(stdoutReal, resultado);
            System.err.println("[Filtro 7] Salida escrita correctamente.");

        } catch (Exception e) {
            System.err.println("[Filtro 7] ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
