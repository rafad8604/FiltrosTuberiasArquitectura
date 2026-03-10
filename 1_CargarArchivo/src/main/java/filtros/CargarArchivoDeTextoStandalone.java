package filtros;

import datos.PaqueteDatos;
import datos.ProtocoloPipe;

/**
 * Programa standalone para el filtro ID1 — Cargar Archivo de Texto.
 *
 * Lee un PaqueteDatos (tipo PATH) desde stdin, ejecuta el filtro,
 * y escribe el resultado (tipo TEXTO) en stdout.
 *
 * Uso en pipeline:
 *   echo datos | java -jar 1-CargarArchivo.jar
 */
public class CargarArchivoDeTextoStandalone {

    public static void main(String[] args) {
        try {
            // Guardar stdout real y redirigir System.out → stderr
            // para que los println internos del filtro no corrompan el protocolo
            java.io.PrintStream stdoutReal = System.out;
            System.setOut(System.err);

            System.err.println("[Filtro 1] Iniciando — Cargar Archivo de Texto");

            // Leer entrada desde stdin
            PaqueteDatos entrada = ProtocoloPipe.leer(System.in);
            System.err.println("[Filtro 1] Entrada recibida: " + entrada.getTipo()
                    + " (" + entrada.getTamanioBytes() + " bytes)");

            // Procesar
            CargarArchivoDeTexto filtro = new CargarArchivoDeTexto();
            PaqueteDatos resultado = filtro.procesar(entrada);

            System.err.println("[Filtro 1] Resultado: " + resultado.getTipo()
                    + " (" + resultado.getTamanioBytes() + " bytes)");

            // Escribir salida al stdout real (datos binarios del protocolo)
            ProtocoloPipe.escribir(stdoutReal, resultado);
            System.err.println("[Filtro 1] Salida escrita correctamente.");

        } catch (Exception e) {
            System.err.println("[Filtro 1] ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
