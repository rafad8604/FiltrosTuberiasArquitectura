package datos;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Protocolo binario para serializar/deserializar {@link PaqueteDatos}
 * a través de stdin/stdout (tuberías a nivel de SO).
 *
 * Formato del protocolo (big-endian):
 * <pre>
 *   MAGIC       : 6 bytes  "TFPIPE"
 *   VERSION     : 1 byte   (actualmente 1)
 *   TIPO        : 4 bytes  (ordinal del TipoDato)
 *   PARAM_LEN   : 4 bytes  (longitud de parámetros, -1 si null)
 *   PARAM_DATA  : N bytes  (UTF-8, solo si PARAM_LEN >= 0)
 *   MODE        : 1 byte   (0=vacío, 1=datos únicos, 2=lista)
 *
 *   Si MODE=1 (datos únicos):
 *     DATA_LEN  : 4 bytes
 *     DATA      : N bytes
 *
 *   Si MODE=2 (lista):
 *     COUNT     : 4 bytes  (número de elementos)
 *     HAS_NAMES : 1 byte   (0=sin nombres, 1=con nombres)
 *     Por cada elemento:
 *       [Si HAS_NAMES: NAME_LEN (4 bytes) + NAME (UTF-8)]
 *       ITEM_LEN : 4 bytes
 *       ITEM     : N bytes
 * </pre>
 *
 * Uso típico en un filtro standalone:
 * <pre>
 *   PaqueteDatos entrada  = ProtocoloPipe.leer(System.in);
 *   PaqueteDatos resultado = filtro.procesar(entrada);
 *   ProtocoloPipe.escribir(System.out, resultado);
 * </pre>
 */
public class ProtocoloPipe {

    private static final byte[] MAGIC   = "TFPIPE".getBytes(StandardCharsets.US_ASCII);
    private static final byte   VERSION = 1;

    private ProtocoloPipe() { } // utilidad — no instanciar

    // ══════════════════════════════════════════════════════════════
    // Escritura
    // ══════════════════════════════════════════════════════════════

    /**
     * Escribe un PaqueteDatos en el OutputStream dado usando el protocolo binario.
     *
     * @param out     Stream de salida (p.ej. System.out o process.getOutputStream()).
     * @param paquete Paquete a serializar.
     * @throws IOException Si ocurre un error de E/S.
     */
    public static void escribir(OutputStream out, PaqueteDatos paquete) throws IOException {
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(out));

        // ── Cabecera ─────────────────────────────────────────────
        dos.write(MAGIC);
        dos.writeByte(VERSION);
        dos.writeInt(paquete.getTipo().ordinal());

        // ── Parámetros ───────────────────────────────────────────
        String params = paquete.getParametros();
        if (params == null || params.isEmpty()) {
            dos.writeInt(-1);
        } else {
            byte[] paramBytes = params.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(paramBytes.length);
            dos.write(paramBytes);
        }

        // ── Datos ────────────────────────────────────────────────
        if (paquete.getLista() != null) {
            // MODE = 2 (lista)
            dos.writeByte(2);
            List<byte[]> lista   = paquete.getLista();
            List<String> nombres = paquete.getNombres();
            boolean tieneNombres = nombres != null && !nombres.isEmpty();

            dos.writeInt(lista.size());
            dos.writeByte(tieneNombres ? 1 : 0);

            for (int i = 0; i < lista.size(); i++) {
                if (tieneNombres) {
                    String nombre = (i < nombres.size()) ? nombres.get(i) : "";
                    byte[] nombreBytes = nombre.getBytes(StandardCharsets.UTF_8);
                    dos.writeInt(nombreBytes.length);
                    dos.write(nombreBytes);
                }
                byte[] item = lista.get(i);
                if (item == null) {
                    dos.writeInt(0);
                } else {
                    dos.writeInt(item.length);
                    dos.write(item);
                }
            }

        } else if (paquete.getDatos() != null) {
            // MODE = 1 (datos únicos)
            dos.writeByte(1);
            dos.writeInt(paquete.getDatos().length);
            dos.write(paquete.getDatos());

        } else {
            // MODE = 0 (vacío)
            dos.writeByte(0);
        }

        dos.flush();
    }

    // ══════════════════════════════════════════════════════════════
    // Lectura
    // ══════════════════════════════════════════════════════════════

    /**
     * Lee un PaqueteDatos desde el InputStream dado usando el protocolo binario.
     *
     * @param in Stream de entrada (p.ej. System.in o process.getInputStream()).
     * @return PaqueteDatos deserializado.
     * @throws IOException Si ocurre un error de E/S o el formato es inválido.
     */
    public static PaqueteDatos leer(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(in));

        // ── Cabecera ─────────────────────────────────────────────
        byte[] magic = new byte[MAGIC.length];
        dis.readFully(magic);
        for (int i = 0; i < MAGIC.length; i++) {
            if (magic[i] != MAGIC[i]) {
                throw new IOException("Magic header inválido. "
                        + "Se esperaba 'TFPIPE', se recibió: '"
                        + new String(magic, StandardCharsets.US_ASCII) + "'");
            }
        }

        byte version = dis.readByte();
        if (version != VERSION) {
            throw new IOException("Versión de protocolo no soportada: " + version
                    + " (se esperaba " + VERSION + ")");
        }

        // ── Tipo ─────────────────────────────────────────────────
        int tipoOrdinal = dis.readInt();
        TipoDato[] valores = TipoDato.values();
        if (tipoOrdinal < 0 || tipoOrdinal >= valores.length) {
            throw new IOException("Ordinal de TipoDato fuera de rango: " + tipoOrdinal);
        }
        TipoDato tipo = valores[tipoOrdinal];

        // ── Parámetros ───────────────────────────────────────────
        int paramLen = dis.readInt();
        String parametros = null;
        if (paramLen >= 0) {
            byte[] paramBytes = new byte[paramLen];
            dis.readFully(paramBytes);
            parametros = new String(paramBytes, StandardCharsets.UTF_8);
        }

        // ── Datos ────────────────────────────────────────────────
        byte mode = dis.readByte();
        PaqueteDatos paquete;

        switch (mode) {
            case 0: // vacío
                paquete = new PaqueteDatos(tipo, (byte[]) null);
                break;

            case 1: { // datos únicos
                int dataLen = dis.readInt();
                byte[] data = new byte[dataLen];
                dis.readFully(data);
                paquete = new PaqueteDatos(tipo, data);
                break;
            }

            case 2: { // lista
                int count = dis.readInt();
                boolean tieneNombres = dis.readByte() == 1;
                List<byte[]> lista   = new ArrayList<>(count);
                List<String> nombres = tieneNombres ? new ArrayList<>(count) : null;

                for (int i = 0; i < count; i++) {
                    if (tieneNombres) {
                        int nameLen = dis.readInt();
                        byte[] nameBytes = new byte[nameLen];
                        dis.readFully(nameBytes);
                        nombres.add(new String(nameBytes, StandardCharsets.UTF_8));
                    }
                    int itemLen = dis.readInt();
                    byte[] item = new byte[itemLen];
                    dis.readFully(item);
                    lista.add(item);
                }
                paquete = tieneNombres
                        ? new PaqueteDatos(tipo, lista, nombres)
                        : new PaqueteDatos(tipo, lista);
                break;
            }

            default:
                throw new IOException("Modo desconocido: " + mode);
        }

        // ── Asignar parámetros leídos ────────────────────────────
        if (parametros != null && !parametros.isEmpty()) {
            paquete.setParametros(parametros);
        }

        return paquete;
    }
}
