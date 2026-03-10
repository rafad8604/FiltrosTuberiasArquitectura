package datos;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenedor de datos que fluye entre filtros a través de la tubería.
 *
 * Puede contener:
 *   - Un único elemento  → byte[] datos
 *   - Múltiples elementos → List<byte[]> lista
 *
 * Incluye métodos de conversión automática:
 *   - aLista()         → envuelve un elemento único en una lista
 *   - primerElemento() → extrae el primer elemento de una lista
 *
 * Patrón Builder para construcción fluida.
 */
public class PaqueteDatos {

    private final TipoDato     tipo;
    private final byte[]       datos;    // elemento único
    private final List<byte[]> lista;    // múltiples elementos
    private final List<String> nombres;  // nombres de archivo opcionales

    // ── Constructores ─────────────────────────────────────────────

    /** Crea un paquete con un único elemento. */
    public PaqueteDatos(TipoDato tipo, byte[] datos) {
        this.tipo    = tipo;
        this.datos   = datos;
        this.lista   = null;
        this.nombres = null;
    }

    /** Crea un paquete con múltiples elementos. */
    public PaqueteDatos(TipoDato tipo, List<byte[]> lista) {
        this.tipo    = tipo;
        this.datos   = null;
        this.lista   = lista;
        this.nombres = null;
    }

    /** Crea un paquete con múltiples elementos y nombres de archivo. */
    public PaqueteDatos(TipoDato tipo, List<byte[]> lista, List<String> nombres) {
        this.tipo    = tipo;
        this.datos   = null;
        this.lista   = lista;
        this.nombres = nombres;
    }

    // ── Getters ───────────────────────────────────────────────────

    public TipoDato     getTipo()    { return tipo; }
    public byte[]       getDatos()   { return datos; }
    public List<byte[]> getLista()   { return lista; }
    public List<String> getNombres() { return nombres; }
    public boolean      esLista()    { return lista != null; }

    // ── Conversiones automáticas ──────────────────────────────────

    /**
     * Envuelve el elemento único en una lista de un elemento.
     * Si ya es lista, devuelve this sin cambios.
     */
    public PaqueteDatos aLista() {
        if (esLista()) return this;
        List<byte[]> nuevaLista = new ArrayList<>();
        nuevaLista.add(datos);
        return new PaqueteDatos(tipo.aLista(), nuevaLista);
    }

    /**
     * Extrae el primer elemento de la lista como elemento único.
     * Si ya es elemento único, devuelve this sin cambios.
     */
    public PaqueteDatos primerElemento() {
        if (!esLista() || lista == null || lista.isEmpty()) return this;
        return new PaqueteDatos(tipo.aSingle(), lista.get(0));
    }

    // ── Utilidades ────────────────────────────────────────────────

    /** Convierte los datos a String (útil para TEXTO, BINARIO, BASE64). */
    public String getDatosComoString() {
        if (datos != null) return new String(datos);
        if (lista != null && !lista.isEmpty() && lista.get(0) != null)
            return new String(lista.get(0));
        return "";
    }

    /** Tamaño total de datos en bytes. */
    public long getTamanioBytes() {
        if (datos != null) return datos.length;
        if (lista != null) return lista.stream().mapToLong(b -> b != null ? b.length : 0).sum();
        return 0;
    }

    @Override
    public String toString() {
        if (esLista()) {
            return "PaqueteDatos{tipo=" + tipo
                    + ", elementos=" + (lista != null ? lista.size() : 0)
                    + ", bytesTotal=" + getTamanioBytes() + "}";
        }
        return "PaqueteDatos{tipo=" + tipo
                + ", bytes=" + (datos != null ? datos.length : 0) + "}";
    }
}
