package filtros;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;

/**
 * Programa standalone para el filtro ID8 — Contar Ocurrencias de Palabra.
 *
 * Lee un PaqueteDatos (tipo TEXTO) desde stdin, cuenta las ocurrencias,
 * y escribe el resultado (tipo TEXTO) en stdout.
 *
 * La palabra a buscar se obtiene de (en orden de prioridad):
 *   1. Primer argumento de línea de comandos: args[0]
 *   2. Campo 'parametros' del PaqueteDatos de entrada
 *   3. Valor por defecto: "la"
 *
 * Uso en pipeline:
 *   ... | java -jar 8-ContarOcurrencias.jar "hola" | ...
 */
public class ListarPalabrasFrecuenciaDeOcurrenciaStandalone {

    public static void main(String[] args) {
        try {
            java.io.PrintStream stdoutReal = System.out;
            System.setOut(System.err);

            System.err.println("[Filtro 8] Iniciando — Contar Ocurrencias de Palabra");

            PaqueteDatos entrada = ProtocoloPipe.leer(System.in);
            System.err.println("[Filtro 8] Entrada recibida: " + entrada.getTipo()
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
            System.err.println("[Filtro 8] Palabra a buscar: \"" + palabra + "\"");

            ListarPalabrasFrecuenciaDeOcurrencia filtro =
                    new ListarPalabrasFrecuenciaDeOcurrencia(palabra);
            PaqueteDatos resultado = filtro.procesar(entrada);

            System.err.println("[Filtro 8] Resultado: " + resultado.getTipo()
                    + " (" + resultado.getTamanioBytes() + " bytes)");

            ProtocoloPipe.escribir(stdoutReal, resultado);
            System.err.println("[Filtro 8] Salida escrita correctamente.");

        } catch (Exception e) {
            System.err.println("[Filtro 8] ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
