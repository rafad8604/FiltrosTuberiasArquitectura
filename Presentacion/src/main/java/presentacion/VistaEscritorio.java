package presentacion;

import datos.PaqueteDatos;
import datos.TipoDato;
import fabrica.FabricaFiltros;
import interfaces.IFiltro;
import motor.GestorTuberias;
import motor.Tuberia;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Vista de Escritorio (Swing) — Tuberías y Filtros.
 *
 * Flujo simple de 3 pantallas (CardLayout):
 *
 *   [PANTALLA 1] Selección de filtro
 *     → 8 cards mostrando cada filtro.
 *     → Si hay resultado previo, solo se resaltan los compatibles.
 *
 *   [PANTALLA 2] Configurar entrada
 *     → Campo para subir archivo o escribir ruta.
 *     → Si hay resultado previo, se usa automáticamente.
 *     → Campo de palabra (solo ID7 e ID8).
 *     → Botón PROCESAR.
 *
 *   [PANTALLA 3] Resultado
 *     → Muestra texto o las 4 imágenes generadas.
 *     → Indicador de guardado automático en output/.
 *     → Cards de los filtros compatibles para seguir procesando.
 *     → Botón "Nuevo proceso".
 *
 * Los resultados se guardan automáticamente en: Tuberias_Filtros/output/
 */
public class VistaEscritorio extends JFrame {

    // ── Colores por ID de filtro ──────────────────────────────────
    private static final Color[] COLOR_ID = {
            new Color(52, 152, 219),   // ID1 azul
            new Color(231, 76,  60),   // ID2 rojo
            new Color(155, 89, 182),   // ID3 morado
            new Color(26,  188, 156),  // ID4 verde-agua
            new Color(243, 156,  18),  // ID5 naranja
            new Color(46,  204, 113),  // ID6 verde
            new Color(52,  73,   94),  // ID7 gris-azul oscuro
            new Color(230, 126,  34),  // ID8 naranja oscuro
    };
    private static final Color COLOR_AZUL  = new Color(30, 60, 114);
    private static final Color COLOR_BG    = new Color(245, 246, 250);
    private static final Color COLOR_GRIS  = new Color(180, 180, 180);

    // ── Dependencias ─────────────────────────────────────────────
    private final FabricaFiltros fabrica;
    private final GestorTuberias gestor;
    private final File           outputDir;

    // ── Estado de la sesión ───────────────────────────────────────
    private PaqueteDatos datosPrevios    = null;  // null = sin datos previos
    private PaqueteDatos ultimoResultado = null;
    private IFiltro      filtroActual    = null;

    // ── CardLayout ────────────────────────────────────────────────
    private CardLayout cardLayout;
    private JPanel     panelCards;

    // Wrapper de la pantalla selección (se reconstruye dinámicamente)
    private JPanel wrapperSeleccion;

    // ── Componentes de ENTRADA ────────────────────────────────────
    private JLabel     lblFiltroTitulo;
    private JLabel     lblFiltroDesc;
    private JLabel     lblFiltroTipos;
    private JPanel     panelPrevio;        // visible cuando hay datosPrevios
    private JLabel     lblPrevioTipo;
    private JPanel     panelArchivo;       // visible cuando NO hay datosPrevios
    private JTextField campoPath;
    private JPanel     panelPalabra;       // visible solo para ID7 e ID8
    private JTextField campoPalabra;

    // ── Componentes de RESULTADO ──────────────────────────────────
    private JTextArea  areaTexto;
    private JPanel     gridImagenes;
    private CardLayout cardTipoResultado;
    private JPanel     contenedorTipoResultado;
    private JLabel     lblGuardado;
    private JPanel     panelCompatibles;

    // ── Barra de estado ───────────────────────────────────────────
    private JLabel lblHeader;
    private JLabel lblEstado;

    // ══════════════════════════════════════════════════════════════
    // Constructor
    // ══════════════════════════════════════════════════════════════

    public VistaEscritorio(FabricaFiltros fabrica, GestorTuberias gestor) {
        this.fabrica   = fabrica;
        this.gestor    = gestor;
        this.outputDir = new File("output");
        this.outputDir.mkdirs();
        initUI();
    }

    public void mostrar() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    // ══════════════════════════════════════════════════════════════
    // Construcción de la interfaz
    // ══════════════════════════════════════════════════════════════

    private void initUI() {
        setTitle("Tuberías y Filtros — v1.0 (En Memoria)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 680);
        setMinimumSize(new Dimension(820, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG);

        add(buildHeader(), BorderLayout.NORTH);

        cardLayout  = new CardLayout();
        panelCards  = new JPanel(cardLayout);
        panelCards.setBackground(COLOR_BG);

        // Pantalla 1: wrapper vacío — se llena al llamar mostrarSeleccion()
        wrapperSeleccion = new JPanel(new BorderLayout());
        wrapperSeleccion.setBackground(COLOR_BG);
        panelCards.add(wrapperSeleccion,      "seleccion");
        panelCards.add(buildPantallaEntrada(), "entrada");
        panelCards.add(buildPantallaResultado(),"resultado");

        add(panelCards, BorderLayout.CENTER);
        add(buildBarraEstado(), BorderLayout.SOUTH);

        // Mostrar pantalla inicial
        mostrarSeleccion();
    }

    // ── Header ────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(COLOR_AZUL);
        h.setBorder(new EmptyBorder(12, 24, 12, 24));

        JLabel titulo = new JLabel("⚙  Tuberías y Filtros");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(Color.WHITE);

        lblHeader = new JLabel("Selecciona un filtro para comenzar");
        lblHeader.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblHeader.setForeground(new Color(180, 210, 255));

        JPanel col = new JPanel(new GridLayout(2, 1, 0, 2));
        col.setOpaque(false);
        col.add(titulo);
        col.add(lblHeader);
        h.add(col, BorderLayout.WEST);
        return h;
    }

    private JLabel buildBarraEstado() {
        lblEstado = new JLabel("  Listo.");
        lblEstado.setBorder(new EmptyBorder(4, 12, 4, 12));
        lblEstado.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return lblEstado;
    }

    // ══════════════════════════════════════════════════════════════
    // PANTALLA 1 — Selección de filtro
    // ══════════════════════════════════════════════════════════════

    /**
     * Reconstruye y muestra la pantalla de selección.
     * Si hay resultado previo, desactiva las cards incompatibles.
     */
    private void mostrarSeleccion() {
        wrapperSeleccion.removeAll();
        wrapperSeleccion.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel contenido = new JPanel(new BorderLayout(0, 14));
        contenido.setOpaque(false);

        // ── Banner de datos previos ───────────────────────────────
        if (datosPrevios != null) {
            JPanel banner = new JPanel(new BorderLayout(10, 0));
            banner.setBackground(new Color(232, 245, 233));
            banner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(46, 204, 113), 1),
                    new EmptyBorder(10, 14, 10, 14)));

            JLabel lbInfo = new JLabel(
                    "<html><b>✔ Resultado anterior disponible</b> — Tipo: <b>"
                    + datosPrevios.getTipo()
                    + "</b> (" + datosPrevios.getTamanioBytes() + " bytes)"
                    + "<br><small>Solo los filtros compatibles están activos.</small></html>");
            lbInfo.setForeground(new Color(27, 94, 32));

            JButton btnNuevo = botonChico("✕  Empezar de nuevo", new Color(192, 57, 43));
            btnNuevo.addActionListener(e -> {
                datosPrevios = null;
                ultimoResultado = null;
                mostrarSeleccion();
            });

            banner.add(lbInfo, BorderLayout.CENTER);
            banner.add(btnNuevo, BorderLayout.EAST);
            contenido.add(banner, BorderLayout.NORTH);
        }

        // ── Grid de cards (4 filas × 2 columnas) ─────────────────
        JPanel grid = new JPanel(new GridLayout(4, 2, 14, 14));
        grid.setOpaque(false);

        for (IFiltro f : gestor.getFiltrosDisponibles()) {
            boolean compatible = datosPrevios == null
                    || Tuberia.sonCompatibles(datosPrevios.getTipo(), f.getTipoEntrada());
            grid.add(buildCardFiltro(f, compatible));
        }

        contenido.add(grid, BorderLayout.CENTER);
        wrapperSeleccion.add(contenido, BorderLayout.CENTER);
        wrapperSeleccion.revalidate();
        wrapperSeleccion.repaint();

        cardLayout.show(panelCards, "seleccion");
        lblHeader.setText(datosPrevios == null
                ? "Selecciona un filtro para comenzar"
                : "Selecciona el siguiente filtro compatible");
        setEstado("Listo.", true);
    }

    /** Construye la tarjeta visual de un filtro. */
    private JPanel buildCardFiltro(IFiltro filtro, boolean activo) {
        Color colorBase = activo
                ? COLOR_ID[Math.min(filtro.getId() - 1, COLOR_ID.length - 1)]
                : COLOR_GRIS;
        Color colorOsc = colorBase.darker();

        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(colorBase);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorOsc, 2),
                new EmptyBorder(10, 14, 10, 14)));
        card.setCursor(activo
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());

        // Header: número ID + badge de tipos
        JPanel headerCard = new JPanel(new BorderLayout());
        headerCard.setOpaque(false);
        JLabel lblId = new JLabel("ID" + filtro.getId());
        lblId.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblId.setForeground(Color.WHITE);
        JLabel lblTipos = new JLabel(filtro.getTipoEntrada() + " → " + filtro.getTipoSalida());
        lblTipos.setFont(new Font("Monospaced", Font.PLAIN, 10));
        lblTipos.setForeground(new Color(220, 220, 220));
        lblTipos.setHorizontalAlignment(SwingConstants.RIGHT);
        headerCard.add(lblId, BorderLayout.WEST);
        headerCard.add(lblTipos, BorderLayout.EAST);

        // Nombre
        JLabel lblNombre = new JLabel(filtro.getNombre());
        lblNombre.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblNombre.setForeground(Color.WHITE);
        lblNombre.setBorder(new EmptyBorder(4, 0, 2, 0));

        // Descripción
        JLabel lblDesc = new JLabel(
                "<html><body style='width:190px'>" + filtro.getDescripcion() + "</body></html>");
        lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblDesc.setForeground(activo ? new Color(230, 230, 230) : new Color(120, 120, 120));

        JPanel centro = new JPanel(new BorderLayout(0, 2));
        centro.setOpaque(false);
        centro.add(lblNombre, BorderLayout.NORTH);
        centro.add(lblDesc,   BorderLayout.CENTER);

        card.add(headerCard, BorderLayout.NORTH);
        card.add(centro,     BorderLayout.CENTER);

        if (activo) {
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    card.setBackground(colorOsc);
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.WHITE, 2),
                            new EmptyBorder(10, 14, 10, 14)));
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    card.setBackground(colorBase);
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(colorOsc, 2),
                            new EmptyBorder(10, 14, 10, 14)));
                }
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    seleccionarFiltro(filtro);
                }
            });
        }

        return card;
    }

    // ══════════════════════════════════════════════════════════════
    // PANTALLA 2 — Configurar entrada
    // ══════════════════════════════════════════════════════════════

    private JPanel buildPantallaEntrada() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(20, 32, 20, 32));

        // ── Norte: Botón volver + info del filtro ─────────────────
        JPanel norte = new JPanel(new BorderLayout(16, 0));
        norte.setOpaque(false);
        norte.setBorder(new EmptyBorder(0, 0, 16, 0));

        JButton btnVolver = botonChico("← Volver", new Color(127, 140, 141));
        btnVolver.addActionListener(e -> mostrarSeleccion());
        norte.add(btnVolver, BorderLayout.WEST);

        JPanel infoFiltro = new JPanel(new GridLayout(3, 1, 0, 3));
        infoFiltro.setOpaque(false);
        lblFiltroTitulo = new JLabel("Filtro");
        lblFiltroTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblFiltroTitulo.setForeground(COLOR_AZUL);
        lblFiltroDesc = new JLabel("Descripción");
        lblFiltroDesc.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblFiltroTipos = new JLabel("IN → OUT");
        lblFiltroTipos.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblFiltroTipos.setForeground(Color.GRAY);
        infoFiltro.add(lblFiltroTitulo);
        infoFiltro.add(lblFiltroDesc);
        infoFiltro.add(lblFiltroTipos);
        norte.add(infoFiltro, BorderLayout.CENTER);
        panel.add(norte, BorderLayout.NORTH);

        // ── Centro: Inputs ────────────────────────────────────────
        JPanel centro = new JPanel();
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.setOpaque(false);

        // Banner "usando resultado anterior"
        panelPrevio = new JPanel(new BorderLayout());
        panelPrevio.setBackground(new Color(232, 245, 233));
        panelPrevio.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(46, 204, 113), 1),
                new EmptyBorder(12, 16, 12, 16)));
        panelPrevio.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        lblPrevioTipo = new JLabel("✔ Usando resultado anterior como entrada.");
        lblPrevioTipo.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblPrevioTipo.setForeground(new Color(27, 94, 32));
        panelPrevio.add(lblPrevioTipo);
        centro.add(panelPrevio);
        centro.add(Box.createVerticalStrut(14));

        // Bloque de archivo
        panelArchivo = new JPanel(new BorderLayout(8, 6));
        panelArchivo.setOpaque(false);
        panelArchivo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel lbRuta = new JLabel("Archivo de entrada:");
        lbRuta.setFont(new Font("SansSerif", Font.BOLD, 13));
        campoPath = new JTextField();
        campoPath.setFont(new Font("Monospaced", Font.PLAIN, 13));
        campoPath.setToolTipText("Escribe la ruta del archivo o usa el botón Examinar");
        JButton btnExaminar = boton("📂  Examinar...", new Color(52, 152, 219));
        btnExaminar.addActionListener(e -> examinar());
        JPanel filaRuta = new JPanel(new BorderLayout(8, 0));
        filaRuta.setOpaque(false);
        filaRuta.add(campoPath,   BorderLayout.CENTER);
        filaRuta.add(btnExaminar, BorderLayout.EAST);
        panelArchivo.add(lbRuta,   BorderLayout.NORTH);
        panelArchivo.add(filaRuta, BorderLayout.CENTER);
        centro.add(panelArchivo);
        centro.add(Box.createVerticalStrut(18));

        // Bloque de palabra (ID7 y ID8 únicamente)
        panelPalabra = new JPanel(new BorderLayout(8, 6));
        panelPalabra.setOpaque(false);
        panelPalabra.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        JLabel lbPalabra = new JLabel("Palabra a buscar:");
        lbPalabra.setFont(new Font("SansSerif", Font.BOLD, 13));
        campoPalabra = new JTextField();
        campoPalabra.setFont(new Font("Monospaced", Font.PLAIN, 14));
        campoPalabra.setToolTipText("Escribe la palabra que deseas buscar o contar");
        panelPalabra.add(lbPalabra,   BorderLayout.NORTH);
        panelPalabra.add(campoPalabra, BorderLayout.CENTER);
        centro.add(panelPalabra);
        centro.add(Box.createVerticalStrut(28));

        // Botón PROCESAR
        JPanel filaProcesar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        filaProcesar.setOpaque(false);
        JButton btnProcesar = boton("▶   PROCESAR", new Color(39, 174, 96));
        btnProcesar.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnProcesar.setPreferredSize(new Dimension(250, 52));
        btnProcesar.addActionListener(e -> procesarFiltro());
        filaProcesar.add(btnProcesar);
        centro.add(filaProcesar);

        JScrollPane scroll = new JScrollPane(centro);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(COLOR_BG);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /** Llamado al hacer clic en una card de filtro. */
    private void seleccionarFiltro(IFiltro prototipo) {
        filtroActual = prototipo;

        lblFiltroTitulo.setText("ID" + prototipo.getId() + " — " + prototipo.getNombre());
        lblFiltroDesc.setText(prototipo.getDescripcion());

        boolean permiteDir = prototipo.getId() == 3 || prototipo.getId() == 4
                || prototipo.getId() == 7 || prototipo.getId() == 8;
        String tiposTexto = "Entrada:  " + prototipo.getTipoEntrada()
                + "          Salida:  " + prototipo.getTipoSalida();
        if (permiteDir) {
            tiposTexto += "     (acepta archivo o directorio)";
        }
        lblFiltroTipos.setText(tiposTexto);

        boolean usaPrevio = datosPrevios != null;
        panelPrevio.setVisible(usaPrevio);
        panelArchivo.setVisible(!usaPrevio);
        panelPalabra.setVisible(prototipo.getId() == 7 || prototipo.getId() == 8);

        if (usaPrevio) {
            lblPrevioTipo.setText("✔ Usando resultado anterior como entrada.  Tipo: "
                    + datosPrevios.getTipo()
                    + " (" + datosPrevios.getTamanioBytes() + " bytes)");
        }

        campoPalabra.setText("");
        if (!usaPrevio) {
            campoPath.setText("");
            campoPath.setToolTipText(permiteDir
                    ? "Escribe la ruta del archivo o directorio, o usa el botón Examinar"
                    : "Escribe la ruta del archivo o usa el botón Examinar");
        }

        lblHeader.setText("Paso 2 — Configurar: ID" + prototipo.getId()
                + " · " + prototipo.getNombre());
        cardLayout.show(panelCards, "entrada");
        setEstado("Completa los campos y pulsa PROCESAR.", true);
    }

    // ══════════════════════════════════════════════════════════════
    // PANTALLA 3 — Resultado
    // ══════════════════════════════════════════════════════════════

    private JPanel buildPantallaResultado() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(COLOR_BG);
        panel.setBorder(new EmptyBorder(18, 32, 18, 32));

        // ── Norte: botón ← Volver ─────────────────────────────────
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        norte.setOpaque(false);
        norte.setBorder(new EmptyBorder(0, 0, 8, 0));
        JButton btnVolverResultado = botonChico("← Volver a selección", new Color(127, 140, 141));
        btnVolverResultado.addActionListener(e -> mostrarSeleccion());
        norte.add(btnVolverResultado);
        panel.add(norte, BorderLayout.NORTH);

        // ── Área de resultado (texto o imágenes) ──────────────────
        cardTipoResultado        = new CardLayout();
        contenedorTipoResultado  = new JPanel(cardTipoResultado);

        areaTexto = new JTextArea();
        areaTexto.setEditable(false);
        areaTexto.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaTexto.setLineWrap(true);
        areaTexto.setBorder(new EmptyBorder(8, 10, 8, 10));
        contenedorTipoResultado.add(new JScrollPane(areaTexto), "texto");

        gridImagenes = new JPanel(new GridLayout(2, 2, 10, 10));
        gridImagenes.setBackground(COLOR_BG);
        contenedorTipoResultado.add(new JScrollPane(gridImagenes), "imagenes");

        panel.add(contenedorTipoResultado, BorderLayout.CENTER);

        // ── Panel sur: guardado + compatibles + nuevo ─────────────
        JPanel sur = new JPanel();
        sur.setLayout(new BoxLayout(sur, BoxLayout.Y_AXIS));
        sur.setOpaque(false);

        // Guardado automático
        lblGuardado = new JLabel("  ");
        lblGuardado.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblGuardado.setForeground(new Color(39, 174, 96));
        lblGuardado.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnOtraUbicacion = botonChico("Guardar en otra ubicación...", new Color(52, 152, 219));
        btnOtraUbicacion.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnOtraUbicacion.addActionListener(e -> guardarManual());

        // Separador
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));

        // Filtros compatibles para continuar
        JLabel lbContinuar = new JLabel("¿Deseas continuar procesando el resultado?");
        lbContinuar.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbContinuar.setForeground(COLOR_AZUL);
        lbContinuar.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbContinuar.setBorder(new EmptyBorder(10, 0, 6, 0));

        panelCompatibles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panelCompatibles.setOpaque(false);
        panelCompatibles.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnNuevo = boton("🔄  Nuevo proceso", new Color(127, 140, 141));
        btnNuevo.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnNuevo.addActionListener(e -> {
            datosPrevios    = null;
            ultimoResultado = null;
            mostrarSeleccion();
        });

        sur.add(lblGuardado);
        sur.add(Box.createVerticalStrut(4));
        sur.add(btnOtraUbicacion);
        sur.add(Box.createVerticalStrut(8));
        sur.add(sep);
        sur.add(lbContinuar);
        sur.add(panelCompatibles);
        sur.add(Box.createVerticalStrut(10));
        sur.add(btnNuevo);

        panel.add(sur, BorderLayout.SOUTH);
        return panel;
    }

    /** Actualiza y muestra la pantalla de resultado. */
    private void mostrarResultado(PaqueteDatos resultado, IFiltro filtro, File guardadoEn) {
        lblHeader.setText("✔ ID" + filtro.getId() + " completado — Resultado: "
                + resultado.getTipo());

        // ── Mostrar contenido del resultado ───────────────────────
        if (resultado.getTipo() == TipoDato.LISTA_IMAGEN) {
            mostrarImagenes(resultado.getLista(), resultado.getNombres());
            cardTipoResultado.show(contenedorTipoResultado, "imagenes");
        } else {
            String texto;
            if (resultado.getDatos() != null) {
                texto = new String(resultado.getDatos(), StandardCharsets.UTF_8);
            } else if (resultado.getLista() != null) {
                List<String> nombres = resultado.getNombres();
                StringBuilder sb = new StringBuilder();
                sb.append("Se procesaron ").append(resultado.getLista().size())
                  .append(" elemento(s):\n\n");
                for (int i = 0; i < resultado.getLista().size(); i++) {
                    String nombre = (nombres != null && i < nombres.size())
                            ? nombres.get(i) : "Elemento " + (i + 1);
                    int size = resultado.getLista().get(i) != null
                            ? resultado.getLista().get(i).length : 0;
                    sb.append("  • ").append(nombre)
                      .append(" (").append(size).append(" bytes)\n");
                }
                texto = sb.toString();
            } else {
                texto = "(vacío)";
            }
            areaTexto.setText(texto);
            areaTexto.setCaretPosition(0);
            cardTipoResultado.show(contenedorTipoResultado, "texto");
        }

        // ── Etiqueta de guardado ──────────────────────────────────
        if (guardadoEn != null) {
            lblGuardado.setText("💾 Guardado automáticamente en: output/" + guardadoEn.getName());
        } else {
            lblGuardado.setText("⚠ No se pudo guardar automáticamente.");
        }

        // ── Filtros compatibles para continuar ────────────────────
        panelCompatibles.removeAll();
        boolean hayCompatible = false;
        for (IFiltro f : gestor.getFiltrosDisponibles()) {
            if (Tuberia.sonCompatibles(resultado.getTipo(), f.getTipoEntrada())) {
                panelCompatibles.add(buildBotonCompatible(f));
                hayCompatible = true;
            }
        }
        if (!hayCompatible) {
            JLabel lbFin = new JLabel("No hay filtros compatibles — proceso terminado.");
            lbFin.setFont(new Font("SansSerif", Font.ITALIC, 12));
            lbFin.setForeground(Color.GRAY);
            panelCompatibles.add(lbFin);
        }
        panelCompatibles.revalidate();
        panelCompatibles.repaint();

        cardLayout.show(panelCards, "resultado");
        setEstado("✔ Filtro ID" + filtro.getId() + " completado correctamente.", true);
    }

    /** Construye un botón compacto para encadenar con un filtro compatible. */
    private JButton buildBotonCompatible(IFiltro f) {
        Color color = COLOR_ID[Math.min(f.getId() - 1, COLOR_ID.length - 1)];
        JButton btn = new JButton(
                "<html><center><b>ID" + f.getId() + "</b><br>"
                + "<small>" + f.getNombre() + "</small></center></html>");
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        btn.setPreferredSize(new Dimension(165, 52));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> seleccionarFiltro(f));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(color.darker()); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(color); }
        });
        return btn;
    }

    /** Muestra las imágenes resultado de FiltroImagenes en un grid dinámico. */
    private void mostrarImagenes(List<byte[]> imagenes, List<String> nombres) {
        gridImagenes.removeAll();
        String[] etqsDefault = {"Escala de grises", "Reducción 50%", "Brillo +50%", "Rotación 90°"};

        // Grid dinámico: 4 columnas, filas según necesidad
        int cols = Math.min(4, imagenes.size());
        int rows = (int) Math.ceil(imagenes.size() / (double) cols);
        gridImagenes.setLayout(new GridLayout(rows, cols, 10, 10));

        for (int i = 0; i < imagenes.size(); i++) {
            JPanel celda = new JPanel(new BorderLayout(0, 4));
            celda.setBackground(Color.WHITE);
            celda.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
            try {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(imagenes.get(i)));
                if (bi != null) {
                    // Escalar proporcionalmente para el grid
                    int maxW = 200, maxH = 180;
                    double scale = Math.min((double) maxW / bi.getWidth(),
                                            (double) maxH / bi.getHeight());
                    int sw = (int)(bi.getWidth() * scale);
                    int sh = (int)(bi.getHeight() * scale);
                    Image scaled = bi.getScaledInstance(sw, sh, Image.SCALE_SMOOTH);
                    JLabel img = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
                    celda.add(img, BorderLayout.CENTER);
                }
            } catch (IOException ignored) { }

            // Caption: usar nombre del resultado si existe, sino default
            String caption;
            if (nombres != null && i < nombres.size()) {
                caption = nombres.get(i);
            } else if (i < etqsDefault.length) {
                caption = etqsDefault[i];
            } else {
                caption = "Filtro " + i;
            }
            JLabel cap = new JLabel(caption, SwingConstants.CENTER);
            cap.setFont(new Font("SansSerif", Font.BOLD, 10));
            cap.setBorder(new EmptyBorder(2, 0, 5, 0));
            celda.add(cap, BorderLayout.SOUTH);
            gridImagenes.add(celda);
        }
        gridImagenes.revalidate();
        gridImagenes.repaint();
    }

    // ══════════════════════════════════════════════════════════════
    // Lógica de procesamiento
    // ══════════════════════════════════════════════════════════════

    private void procesarFiltro() {
        if (filtroActual == null) return;

        // ── Construir la entrada ──────────────────────────────────
        PaqueteDatos entrada;
        if (datosPrevios != null) {
            entrada = datosPrevios;
        } else {
            String path = campoPath.getText().trim();
            if (path.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Selecciona o escribe la ruta del archivo de entrada.",
                        "Falta el archivo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            entrada = leerArchivo(filtroActual.getTipoEntrada(), path);
            if (entrada == null) return;
        }

        // ── Crear la instancia del filtro (con palabra para ID7/ID8) ─
        int id = filtroActual.getId();
        final IFiltro filtroInstancia;
        if (id == 7 || id == 8) {
            String palabra = campoPalabra.getText().trim();
            if (palabra.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Escribe la palabra que deseas buscar.",
                        "Falta la palabra", JOptionPane.WARNING_MESSAGE);
                return;
            }
            filtroInstancia = (id == 7)
                    ? fabrica.crearFiltro7(palabra)
                    : fabrica.crearFiltro8(palabra);
        } else {
            filtroInstancia = fabrica.crearFiltro(id);
        }

        setEstado("⏳ Procesando con " + filtroActual.getNombre() + "...", true);

        final PaqueteDatos entradaFinal = entrada;

        // ── Ejecutar en hilo de fondo (SwingWorker) ───────────────
        new SwingWorker<PaqueteDatos, Void>() {
            @Override
            protected PaqueteDatos doInBackground() {
                return filtroInstancia.procesar(entradaFinal);
            }
            @Override
            protected void done() {
                try {
                    PaqueteDatos resultado = get();
                    ultimoResultado = resultado;
                    datosPrevios    = resultado;  // disponible para encadenamiento
                    File guardado   = autoGuardar(resultado, filtroInstancia.getId());
                    mostrarResultado(resultado, filtroInstancia, guardado);
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(VistaEscritorio.this,
                            "Error durante el procesamiento:\n" + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    setEstado("✘ Error: " + causa.getMessage(), false);
                }
            }
        }.execute();
    }

    // ══════════════════════════════════════════════════════════════
    // Utilidades de archivo
    // ══════════════════════════════════════════════════════════════

    /** Lee el archivo en la ruta dada y devuelve un PaqueteDatos con el tipo correcto. */
    private PaqueteDatos leerArchivo(TipoDato tipo, String ruta) {
        File file = new File(ruta);

        // ── Si es un directorio, leer todos los archivos relevantes ──
        if (file.isDirectory()) {
            return leerDirectorio(tipo, file);
        }

        try {
            switch (tipo) {
                case PATH:
                    // ID1 espera la ruta en sí; él lee el archivo internamente
                    return new PaqueteDatos(TipoDato.PATH, ruta.getBytes(StandardCharsets.UTF_8));
                case IMAGEN:
                    return new PaqueteDatos(TipoDato.IMAGEN, Files.readAllBytes(Paths.get(ruta)));
                case BINARIO:
                    return new PaqueteDatos(TipoDato.BINARIO, Files.readAllBytes(Paths.get(ruta)));
                case BASE64:
                    return new PaqueteDatos(TipoDato.BASE64, Files.readAllBytes(Paths.get(ruta)));
                default: // TEXTO, LISTA_TEXTO, etc.
                    return new PaqueteDatos(TipoDato.TEXTO, Files.readAllBytes(Paths.get(ruta)));
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo leer el archivo:\n" + ruta + "\n\n" + e.getMessage(),
                    "Error de lectura", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * Lee todos los archivos relevantes de un directorio y devuelve un PaqueteDatos tipo lista.
     * Filtra los archivos según el tipo esperado por el filtro.
     */
    private PaqueteDatos leerDirectorio(TipoDato tipo, File dir) {
        File[] archivos;
        switch (tipo) {
            case IMAGEN:
                archivos = dir.listFiles((d, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".png") || lower.endsWith(".jpg")
                            || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                            || lower.endsWith(".bmp");
                });
                break;
            case BINARIO:
            case LISTA_BINARIO:
                archivos = dir.listFiles((d, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".bin") || lower.endsWith(".dat")
                            || lower.endsWith(".txt");
                });
                break;
            case BASE64:
            case LISTA_BASE64:
                archivos = dir.listFiles((d, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".b64") || lower.endsWith(".txt");
                });
                break;
            default: // TEXTO, LISTA_TEXTO
                archivos = dir.listFiles((d, name) ->
                        !name.startsWith(".") && !new File(d, name).isDirectory());
                break;
        }

        if (archivos == null || archivos.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "No se encontraron archivos compatibles en el directorio:\n"
                            + dir.getAbsolutePath(),
                    "Directorio vacío", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        java.util.Arrays.sort(archivos);
        java.util.List<byte[]> lista = new java.util.ArrayList<>();
        java.util.List<String> nombres = new java.util.ArrayList<>();

        for (File f : archivos) {
            try {
                lista.add(Files.readAllBytes(f.toPath()));
                nombres.add(f.getName());
                System.out.println("  [Vista] Archivo cargado: " + f.getName()
                        + " (" + f.length() + " bytes)");
            } catch (IOException e) {
                System.err.println("  [Vista] Error leyendo: " + f.getName()
                        + " - " + e.getMessage());
            }
        }

        if (lista.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo leer ningún archivo del directorio.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        TipoDato tipoLista = tipo.aLista();
        System.out.println("  [Vista] Directorio cargado: " + lista.size()
                + " archivos → tipo " + tipoLista);
        return new PaqueteDatos(tipoLista, lista, nombres);
    }

    /**
     * Guarda el resultado automáticamente en output/ con nombre descriptivo.
     * Soporta guardado individual por archivo cuando hay nombres disponibles.
     * @return El archivo creado, o null si falló.
     */
    private File autoGuardar(PaqueteDatos resultado, int idFiltro) {
        String ts  = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String ext = extensionPara(resultado.getTipo());
        try {
            if (resultado.getTipo() == TipoDato.LISTA_IMAGEN) {
                // Imágenes — guardar cada una con su nombre
                List<byte[]> imgs = resultado.getLista();
                List<String> nombres = resultado.getNombres();
                String[] sufijosDefault = {"gris", "reduccion50pct", "brillo", "rotacion90"};

                for (int i = 0; i < imgs.size(); i++) {
                    String nombre;
                    if (nombres != null && i < nombres.size()) {
                        nombre = "id" + idFiltro + "_" + nombres.get(i) + "_" + ts + ".png";
                    } else {
                        String suf = i < sufijosDefault.length ? sufijosDefault[i] : "img" + i;
                        nombre = "id" + idFiltro + "_" + suf + "_" + ts + ".png";
                    }
                    File f = new File(outputDir, nombre);
                    Files.write(f.toPath(), imgs.get(i));
                }
                return new File(outputDir, "id" + idFiltro + "_" + imgs.size() + "imgs_" + ts);

            } else if (resultado.getDatos() != null) {
                File archivo = new File(outputDir, "id" + idFiltro + "_" + ts + ext);
                Files.write(archivo.toPath(), resultado.getDatos());
                return archivo;

            } else if (resultado.getLista() != null) {
                List<byte[]> items = resultado.getLista();
                List<String> nombres = resultado.getNombres();

                if (nombres != null && !nombres.isEmpty()) {
                    // Guardar cada archivo individualmente con su nombre
                    for (int i = 0; i < items.size(); i++) {
                        String nombre;
                        if (i < nombres.size()) {
                            nombre = "id" + idFiltro + "_" + nombres.get(i) + "_" + ts + ext;
                        } else {
                            nombre = "id" + idFiltro + "_item" + i + "_" + ts + ext;
                        }
                        File f = new File(outputDir, nombre);
                        Files.write(f.toPath(), items.get(i));
                    }
                    return new File(outputDir, "id" + idFiltro + "_" + items.size() + "archivos_" + ts);
                } else {
                    // Lista sin nombres: concatenar (comportamiento original)
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (byte[] b : items) baos.write(b);
                    File archivo = new File(outputDir, "id" + idFiltro + "_lista_" + ts + ext);
                    Files.write(archivo.toPath(), baos.toByteArray());
                    return archivo;
                }
            }
        } catch (IOException e) {
            System.err.println("[Vista] Error guardando resultado: " + e.getMessage());
        }
        return null;
    }

    /** Permite al usuario elegir dónde guardar el resultado manualmente. */
    private void guardarManual() {
        if (ultimoResultado == null) {
            setEstado("⚠ No hay resultado para guardar.", false);
            return;
        }
        JFileChooser fc = new JFileChooser(outputDir);

        if (ultimoResultado.getTipo() == TipoDato.LISTA_IMAGEN
                || (ultimoResultado.esLista() && ultimoResultado.getNombres() != null)) {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Elige la carpeta donde guardar los archivos");
        } else {
            fc.setDialogTitle("Guardar resultado");
            fc.setSelectedFile(new File(outputDir,
                    "resultado_id" + (filtroActual != null ? filtroActual.getId() : "x")
                    + extensionPara(ultimoResultado.getTipo())));
        }

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            if (ultimoResultado.getTipo() == TipoDato.LISTA_IMAGEN) {
                List<byte[]> imgs = ultimoResultado.getLista();
                List<String> nombres = ultimoResultado.getNombres();
                String[] sufijosDefault = {"gris", "reduccion50pct", "brillo", "rotacion90"};
                for (int i = 0; i < imgs.size(); i++) {
                    String nombre;
                    if (nombres != null && i < nombres.size()) {
                        nombre = nombres.get(i) + ".png";
                    } else {
                        String suf = i < sufijosDefault.length ? sufijosDefault[i] : "img" + i;
                        nombre = "imagen_" + suf + ".png";
                    }
                    Files.write(new File(fc.getSelectedFile(), nombre).toPath(), imgs.get(i));
                }
            } else if (ultimoResultado.esLista() && ultimoResultado.getNombres() != null) {
                List<byte[]> items = ultimoResultado.getLista();
                List<String> nombres = ultimoResultado.getNombres();
                String ext = extensionPara(ultimoResultado.getTipo());
                for (int i = 0; i < items.size(); i++) {
                    String nombre = (i < nombres.size()) ? nombres.get(i) + ext : "item" + i + ext;
                    Files.write(new File(fc.getSelectedFile(), nombre).toPath(), items.get(i));
                }
            } else if (ultimoResultado.getDatos() != null) {
                Files.write(fc.getSelectedFile().toPath(), ultimoResultado.getDatos());
            }
            setEstado("✔ Guardado en: " + fc.getSelectedFile().getAbsolutePath(), true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Abre un JFileChooser filtrando por el tipo de archivo esperado. */
    private void examinar() {
        JFileChooser fc = new JFileChooser();

        // Permitir seleccionar directorios para filtros que soportan múltiples archivos
        boolean permiteDirectorio = filtroActual != null
                && (filtroActual.getId() == 3 || filtroActual.getId() == 4
                    || filtroActual.getId() == 7 || filtroActual.getId() == 8);

        if (permiteDirectorio) {
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setDialogTitle("Seleccionar archivo o directorio de entrada");
        } else {
            fc.setDialogTitle("Seleccionar archivo de entrada");
        }

        if (filtroActual != null) {
            switch (filtroActual.getTipoEntrada()) {
                case IMAGEN:
                    fc.setFileFilter(new FileNameExtensionFilter(
                            "Imágenes (PNG, JPG, GIF, BMP)",
                            "png", "jpg", "jpeg", "gif", "bmp"));
                    break;
                case BINARIO:
                    fc.setFileFilter(new FileNameExtensionFilter(
                            "Archivos binarios (BIN, DAT)", "bin", "dat"));
                    break;
                case BASE64:
                    fc.setFileFilter(new FileNameExtensionFilter(
                            "Archivos Base64 (B64, TXT)", "b64", "txt"));
                    break;
                default:
                    fc.addChoosableFileFilter(new FileNameExtensionFilter(
                            "Archivos de texto (TXT, CSV, LOG, MD)",
                            "txt", "csv", "log", "md"));
                    break;
            }
        }

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            campoPath.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private String extensionPara(TipoDato tipo) {
        switch (tipo) {
            case BINARIO:
            case LISTA_BINARIO: return ".bin";
            case BASE64:
            case LISTA_BASE64:  return ".b64";
            case IMAGEN:
            case LISTA_IMAGEN:  return ".png";
            default:            return ".txt";
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers UI
    // ══════════════════════════════════════════════════════════════

    /**
     * Crea un botón con color de fondo personalizado.
     * setOpaque(true) + setBorderPainted(false) es necesario en macOS
     * para que setBackground() se aplique correctamente.
     */
    private JButton boton(String texto, Color fondo) {
        JButton b = new JButton(texto);
        b.setBackground(fondo);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Hover: oscurecer ligeramente
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(fondo.darker());
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(fondo);
            }
        });
        return b;
    }

    private JButton botonChico(String texto, Color fondo) {
        JButton b = boton(texto, fondo);
        b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        b.setBorder(new EmptyBorder(5, 12, 5, 12));
        return b;
    }

    private void setEstado(String msg, boolean ok) {
        lblEstado.setText("  " + msg);
        lblEstado.setForeground(ok ? new Color(39, 174, 96) : new Color(192, 57, 43));
    }
}
