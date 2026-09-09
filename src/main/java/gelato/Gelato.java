package gelato;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.process.ImageConverter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(
        type = Command.class,
        menuPath = "Plugins>Gelato>Gelato 1.0.1")
public class Gelato implements Command {
    @Override
    public void run() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Controller().showFrame();
            }
        });
    }

    public static final class Controller implements ActionListener {
        private static final int TICK_LEN = 20;
        private static final int TICK_GAP = 4;
        private static final int LEFT_MARGIN = 70;
        private static final int TOP_MARGIN = 50;
        private static final int BAND_GAP = 38;
        private static final int FIG_INIT_W = 900;
        private static final float A4_PAGE_WIDTH_PT = 595.2756f;
        private static final float A4_PAGE_HEIGHT_PT = 841.8898f;
        private static final int TOOL_BUTTON_W = 220;
        private static final int TOOL_BUTTON_H = 30;
        private static final String VERSION = "1.0.1";
        private static final int LOG_FORMAT_VERSION = 1;
        private static final double CROP_GEOMETRY_AGREEMENT_TOLERANCE = 1.0;
        private static final double MARKER_COORDINATE_AGREEMENT_TOLERANCE = 1.0;
        private static final int RECONSTRUCTION_WARNING_FONT_SIZE = 16;
        private static final int RECONSTRUCTION_ACCESSORY_FONT_SIZE = 14;
        private static final String TITLE = "Gelato " + VERSION;
        private static final String COORDINATE_LOG_HEADER = "Gelato Coordinate Log";
        private static final String GELANNO_COORDINATE_LOG_HEADER = "GelAnno Coordinate Log";
        private static final String WB_TOOL_COORDINATE_LOG_HEADER = "WB Tool Coordinate Log";
        private static final String LAST_DIR_PREFERENCE = "gelato.last_dir";
        private static final String GELANNO_LAST_DIR_PREFERENCE = "gelanno.last_dir";
        private static final String WB_TOOL_LAST_DIR_PREFERENCE = "wbtool.last_dir";
        private static final Color CROP_COLOR = Color.CYAN;
        private static final float CROP_STROKE_WIDTH = 3.0f;
        private static final float SOURCE_MARKER_STROKE_WIDTH = 4.0f;
        private static final double SOURCE_MARKER_R = 10.0;
        private static final float RECONSTRUCTION_CONNECTOR_STROKE_WIDTH = 2.0f;
        private static final Font FONT_KDA = new Font("Arial", Font.PLAIN, 11);
        private static final double SOURCE_OVERLAY_REFERENCE_WIDTH = 2000.0;
        private static final Font FONT_SOURCE_KDA = new Font("Arial", Font.BOLD, 36);
        private static final Font FONT_RECONSTRUCTION_CROP = new Font("Arial", Font.BOLD, 28);
        private static final Font FONT_RECONSTRUCTION_MARKER = new Font("Arial", Font.BOLD, 36);
        private static final Font FONT_NAME = new Font("Arial", Font.BOLD, 12);
        private static final Font FONT_SAMPLE = new Font("Arial", Font.PLAIN, 11);
        private static final Color[] RECONSTRUCTION_CROP_COLORS = {
            new Color(0, 174, 239),
            new Color(236, 0, 140),
            new Color(0, 166, 81),
            new Color(255, 127, 0),
            new Color(95, 70, 200),
            new Color(170, 150, 0)
        };
        private static final Color ANNOTATION_ACTIVE_COLOR = new Color(255, 180, 0);
        private static final Color ANNOTATION_SELECTION_COLOR = new Color(0, 100, 220);
        private static final float MIN_ANNOTATION_FONT_SIZE = 5.0f;
        private static final float MAX_ANNOTATION_FONT_SIZE = 72.0f;
        private static final float ANNOTATION_FONT_STEP = 1.0f;
        private static final double SAMPLE_LABEL_GAP = 8.0;
        private static final float FREE_LINE_STROKE_WIDTH = 1.5f;
        private static final double MIN_FREE_LINE_LENGTH = 3.0;
        private static final double LINE_HIT_RADIUS_PX = 7.0;
        private static final double LINE_HANDLE_SIZE_PX = 7.0;
        private static final double LINE_PASTE_GAP = 12.0;

        private JFrame frame;
        private JLabel statusLabel;
        private JButton markButton;
        private JButton cropButton;
        private JButton sourceKdaLabelsButton;
        private JButton narrowerButton;
        private JButton widerButton;
        private JToggleButton addSampleLabelsButton;
        private JToggleButton addBandTickButton;
        private JToggleButton drawHLineButton;
        private JToggleButton drawVLineButton;
        private JToggleButton addFreeTextButton;
        private JToggleButton editAnnotationsButton;
        private JLabel annotationFontSizeLabel;
        private JLabel activeMarkerSetLabel;
        private FigureCanvas figureCanvas;
        private JScrollPane figureScrollPane;
        private JSlider figureZoomSlider;
        private JLabel figureZoomLabel;
        private boolean updatingFigureZoomUi;
        private boolean figureFitMode = true;
        private JCheckBox swapTickSidesCheckBox;
        private boolean tickSidesSwapped;

        private ImagePlus gelImp;
        private ImagePlus markerImp;
        private String gelPath;
        private String markerPath;
        private File lastDir;
        private final List<KdaMarkerSet> markerSets = new ArrayList<KdaMarkerSet>();
        private final List<BandCrop> bands = new ArrayList<BandCrop>();
        private final List<LineAnnotation> lineAnnotations =
                new ArrayList<LineAnnotation>();
        private final List<TextAnnotation> freeTextAnnotations =
                new ArrayList<TextAnnotation>();
        private final List<AnnotatedMarkerImage> annotatedMarkerImages =
                new ArrayList<AnnotatedMarkerImage>();
        private final List<AnnotatedCropSourceImage> annotatedCropSourceImages =
                new ArrayList<AnnotatedCropSourceImage>();
        private final Set<String> shownMappingWarnings = new HashSet<String>();
        private final String windowTitle;

        private KdaMarkerSet activeMarkerSet;
        private MarkerSourceType markingSourceType;
        private boolean startFreshMarkerSetOnNextMark;
        private int nextMarkerSetNumber = 1;

        private boolean kdaModeActive;
        private boolean showSourceKdaLabels = true;
        private boolean waitingForCrop;
        private boolean cropWasMarking;
        private MouseListener gelMouseListener;
        private ImageCanvas kdaCanvas;
        private BandCrop selectedBand;
        private AnnotationMode annotationMode = AnnotationMode.NORMAL;
        private double defaultSampleAngleDeg;
        private float defaultKdaFontSize = FONT_KDA.getSize2D();
        private float defaultBandNameFontSize = FONT_NAME.getSize2D();
        private float defaultSampleFontSize = FONT_SAMPLE.getSize2D();
        private float defaultBandTickFontSize = FONT_KDA.getSize2D();
        private double defaultFreeTextAngleDeg;
        private float defaultFreeTextFontSize = FONT_SAMPLE.getSize2D();
        private final List<LineAnnotation> lineClipboard =
                new ArrayList<LineAnnotation>();
        private int linePasteGeneration;

        public Controller() {
            this(TITLE);
        }

        private Controller(String windowTitle) {
            this.windowTitle = windowTitle;
        }

        public void showFrame() {
            if (frame == null) {
                buildUi();
            }
            frame.setVisible(true);
            frame.toFront();
        }

        private void buildUi() {
            frame = new JFrame(windowTitle);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    if (kdaCanvas != null) {
                        deactivateKdaMode();
                    }
                }
            });
            frame.setLayout(new BorderLayout(8, 8));

            JPanel tools = new JPanel();
            tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
            tools.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 6));
            tools.setPreferredSize(new Dimension(TOOL_BUTTON_W + 20, 10));

            addSection(tools, "kDa Markers");
            tools.add(button("Open kDa Marker Image...", "open_marker_image"));
            markButton = button("Mark kDa Bands", "toggle_mark_kda");
            tools.add(markButton);
            sourceKdaLabelsButton = button("Hide kDa Labels", "toggle_source_kda_labels");
            tools.add(sourceKdaLabelsButton);
            tools.add(button("Undo Last kDa", "undo_kda"));
            tools.add(button("Clear All kDa", "clear_kda"));
            activeMarkerSetLabel = new JLabel("Active markers: none");
            Dimension markerLabelSize = new Dimension(TOOL_BUTTON_W, 24);
            activeMarkerSetLabel.setMinimumSize(markerLabelSize);
            activeMarkerSetLabel.setPreferredSize(markerLabelSize);
            activeMarkerSetLabel.setMaximumSize(markerLabelSize);
            tools.add(activeMarkerSetLabel);
            tools.add(Box.createVerticalStrut(8));

            addSection(tools, "Image");
            tools.add(button("Open Gel Image...", "open_image"));
            tools.add(Box.createVerticalStrut(8));

            addSection(tools, "Crop");
            cropButton = button("Crop Region -> Figure", "crop");
            tools.add(cropButton);
            tools.add(Box.createVerticalStrut(8));

            addSection(tools, "Annotations");
            addSampleLabelsButton = toggleButton(
                    "Add Sample Labels", "toggle_add_sample_labels");
            tools.add(addSampleLabelsButton);
            addBandTickButton = toggleButton(
                    "Add Band Tick", "toggle_add_band_tick");
            tools.add(addBandTickButton);
            JPanel lineControls = new JPanel(new GridLayout(1, 2, 4, 0));
            lineControls.setOpaque(false);
            lineControls.setAlignmentX(JPanel.LEFT_ALIGNMENT);
            Dimension lineControlsSize = new Dimension(TOOL_BUTTON_W, TOOL_BUTTON_H);
            lineControls.setMinimumSize(lineControlsSize);
            lineControls.setPreferredSize(lineControlsSize);
            lineControls.setMaximumSize(lineControlsSize);
            drawHLineButton = sidebarPairToggleButton(
                    "Draw H-line", "toggle_draw_h_line");
            drawVLineButton = sidebarPairToggleButton(
                    "Draw V-line", "toggle_draw_v_line");
            lineControls.add(drawHLineButton);
            lineControls.add(drawVLineButton);
            tools.add(lineControls);
            addFreeTextButton = toggleButton(
                    "Add Free Text", "toggle_add_free_text");
            tools.add(addFreeTextButton);
            editAnnotationsButton = toggleButton(
                    "Edit Annotations", "toggle_edit_annotations");
            tools.add(editAnnotationsButton);

            JPanel fontControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            fontControls.setOpaque(false);
            fontControls.setAlignmentX(JPanel.LEFT_ALIGNMENT);
            Dimension fontControlsSize = new Dimension(TOOL_BUTTON_W, TOOL_BUTTON_H);
            fontControls.setMinimumSize(fontControlsSize);
            fontControls.setPreferredSize(fontControlsSize);
            fontControls.setMaximumSize(fontControlsSize);
            fontControls.add(compactButton("A-", "annotation_font_smaller"));
            annotationFontSizeLabel = new JLabel("All: 11-12 pt", JLabel.CENTER);
            Dimension fontLabelSize = new Dimension(112, TOOL_BUTTON_H);
            annotationFontSizeLabel.setMinimumSize(fontLabelSize);
            annotationFontSizeLabel.setPreferredSize(fontLabelSize);
            annotationFontSizeLabel.setMaximumSize(fontLabelSize);
            fontControls.add(annotationFontSizeLabel);
            fontControls.add(compactButton("A+", "annotation_font_larger"));
            tools.add(fontControls);
            tools.add(Box.createVerticalStrut(8));

            addSection(tools, "Figure");
            JLabel cropSizeLabel = new JLabel("Selected crop size");
            cropSizeLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            tools.add(cropSizeLabel);
            JPanel cropSizeControls = new JPanel(new GridLayout(1, 2, 4, 0));
            cropSizeControls.setOpaque(false);
            cropSizeControls.setAlignmentX(JPanel.LEFT_ALIGNMENT);
            Dimension cropSizeControlsSize = new Dimension(TOOL_BUTTON_W, TOOL_BUTTON_H);
            cropSizeControls.setMinimumSize(cropSizeControlsSize);
            cropSizeControls.setPreferredSize(cropSizeControlsSize);
            cropSizeControls.setMaximumSize(cropSizeControlsSize);
            narrowerButton = sidebarPairButton("Narrower", "narrower");
            widerButton = sidebarPairButton("Wider", "wider");
            cropSizeControls.add(narrowerButton);
            cropSizeControls.add(widerButton);
            tools.add(cropSizeControls);
            tools.add(button("Show Coordinate Log", "show_coordinate_log"));
            tools.add(button("Reconstruct from Log...", "reconstruct_from_log"));
            tools.add(button("Clear Figure", "clear_figure"));
            tools.add(Box.createVerticalStrut(8));

            addSection(tools, "Export");
            tools.add(button("Export as PDF...", "export_pdf"));
            tools.add(Box.createVerticalGlue());

            figureCanvas = new FigureCanvas(this);
            figureScrollPane = new JScrollPane(figureCanvas);
            figureScrollPane.setPreferredSize(new Dimension(FIG_INIT_W, 620));
            figureScrollPane.getViewport().setBackground(FigureCanvas.WORKSPACE_COLOR);
            figureScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent event) {
                    if (figureFitMode && !updatingFigureZoomUi) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                fitFigureToViewport();
                            }
                        });
                    }
                }
            });

            statusLabel = new JLabel(" ");
            statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            JPanel statusBar = new JPanel(new BorderLayout(6, 0));
            statusBar.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 6));
            statusBar.add(statusLabel, BorderLayout.CENTER);
            statusBar.add(createZoomControls(), BorderLayout.EAST);

            frame.add(tools, BorderLayout.WEST);
            JPanel figurePanel = new JPanel(new BorderLayout(0, 4));
            figurePanel.add(createFigureControls(), BorderLayout.NORTH);
            figurePanel.add(figureScrollPane, BorderLayout.CENTER);
            frame.add(figurePanel, BorderLayout.CENTER);
            frame.add(statusBar, BorderLayout.SOUTH);
            frame.pack();
            placeFrameLeftHalf();
            syncAnnotationModeButtons();
            updateCropSizeButtons();
            updateAnnotationFontReadout();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    fitFigureToViewport();
                }
            });
        }

        private JPanel createFigureControls() {
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            swapTickSidesCheckBox = new JCheckBox("Swap tick sides", tickSidesSwapped);
            swapTickSidesCheckBox.setToolTipText(
                    "All crops: kDa ticks and values on the right, band ticks and labels on the left.");
            swapTickSidesCheckBox.setActionCommand("swap_tick_sides");
            swapTickSidesCheckBox.addActionListener(this);
            controls.add(swapTickSidesCheckBox);
            return controls;
        }

        private void setTickSidesSwapped(boolean swapped) {
            tickSidesSwapped = swapped;
            if (swapTickSidesCheckBox != null) {
                swapTickSidesCheckBox.setSelected(swapped);
            }
            if (figureCanvas != null) {
                figureCanvas.refreshLayout();
            }
            setStatusWithArtboardWarning(swapped
                    ? "All crops: kDa ticks on the right; band ticks on the left."
                    : "All crops: kDa ticks on the left; band ticks on the right.");
        }

        private void placeFrameLeftHalf() {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setLocation(0, 0);
            frame.setSize(screen.width / 2, screen.height);
        }

        private void addSection(JPanel parent, String title) {
            JLabel label = new JLabel(title);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setBorder(BorderFactory.createEmptyBorder(6, 0, 3, 0));
            parent.add(label);
        }

        private JButton button(String label, String command) {
            JButton b = new JButton(label);
            b.setActionCommand(command);
            b.addActionListener(this);
            b.setAlignmentX(JButton.LEFT_ALIGNMENT);
            Dimension size = new Dimension(TOOL_BUTTON_W, TOOL_BUTTON_H);
            b.setMinimumSize(size);
            b.setPreferredSize(size);
            b.setMaximumSize(size);
            return b;
        }

        private JToggleButton toggleButton(String label, String command) {
            JToggleButton button = new JToggleButton(label);
            button.setActionCommand(command);
            button.addActionListener(this);
            button.setAlignmentX(JToggleButton.LEFT_ALIGNMENT);
            Dimension size = new Dimension(TOOL_BUTTON_W, TOOL_BUTTON_H);
            button.setMinimumSize(size);
            button.setPreferredSize(size);
            button.setMaximumSize(size);
            button.setOpaque(true);
            return button;
        }

        private JButton compactButton(String label, String command) {
            JButton compact = new JButton(label);
            compact.setActionCommand(command);
            compact.addActionListener(this);
            Dimension size = new Dimension(46, TOOL_BUTTON_H - 2);
            compact.setMinimumSize(size);
            compact.setPreferredSize(size);
            compact.setMaximumSize(size);
            compact.setMargin(new java.awt.Insets(1, 4, 1, 4));
            compact.setFocusable(false);
            return compact;
        }

        private JButton sidebarPairButton(String label, String command) {
            JButton paired = new JButton(label);
            paired.setActionCommand(command);
            paired.addActionListener(this);
            paired.setMargin(new java.awt.Insets(2, 4, 2, 4));
            paired.setFocusable(false);
            return paired;
        }

        private JToggleButton sidebarPairToggleButton(String label, String command) {
            JToggleButton paired = new JToggleButton(label);
            paired.setActionCommand(command);
            paired.addActionListener(this);
            paired.setMargin(new java.awt.Insets(2, 3, 2, 3));
            paired.setFocusable(false);
            paired.setOpaque(true);
            return paired;
        }

        private JButton zoomButton(String label, String command, int width) {
            JButton zoomButton = new JButton(label);
            zoomButton.setActionCommand(command);
            zoomButton.addActionListener(this);
            Dimension size = new Dimension(width, 24);
            zoomButton.setMinimumSize(size);
            zoomButton.setPreferredSize(size);
            zoomButton.setMaximumSize(size);
            zoomButton.setMargin(new java.awt.Insets(1, 3, 1, 3));
            zoomButton.setFocusable(false);
            return zoomButton;
        }

        private JPanel createZoomControls() {
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
            controls.add(zoomButton("Fit", "zoom_fit", 42));
            controls.add(zoomButton("-", "zoom_out", 28));

            figureZoomSlider = new JSlider(25, 400, 100);
            Dimension sliderSize = new Dimension(125, 24);
            figureZoomSlider.setMinimumSize(sliderSize);
            figureZoomSlider.setPreferredSize(sliderSize);
            figureZoomSlider.setMaximumSize(sliderSize);
            figureZoomSlider.setFocusable(false);
            figureZoomSlider.setToolTipText("Canvas zoom (PDF size is unchanged)");
            figureZoomSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent event) {
                    if (!updatingFigureZoomUi) {
                        setManualFigureZoom(figureZoomSlider.getValue(), null);
                    }
                }
            });
            controls.add(figureZoomSlider);
            controls.add(zoomButton("+", "zoom_in", 28));

            figureZoomLabel = new JLabel("100%", JLabel.RIGHT);
            Dimension labelSize = new Dimension(48, 24);
            figureZoomLabel.setMinimumSize(labelSize);
            figureZoomLabel.setPreferredSize(labelSize);
            figureZoomLabel.setMaximumSize(labelSize);
            figureZoomLabel.setToolTipText("Click to reset the canvas zoom to 100%");
            figureZoomLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    setManualFigureZoom(100, null);
                }
            });
            controls.add(figureZoomLabel);
            return controls;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if ("open_image".equals(command)) {
                openImage();
            } else if ("open_marker_image".equals(command)) {
                openMarkerImage();
            } else if ("toggle_mark_kda".equals(command)) {
                toggleMarkKda();
            } else if ("toggle_source_kda_labels".equals(command)) {
                toggleSourceKdaLabels();
            } else if ("undo_kda".equals(command)) {
                undoLastKda();
            } else if ("clear_kda".equals(command)) {
                clearAllKda();
            } else if ("crop".equals(command)) {
                startOrConfirmCrop();
            } else if ("toggle_add_sample_labels".equals(command)) {
                toggleAnnotationMode(AnnotationMode.ADD_SAMPLE_LABELS);
            } else if ("toggle_add_band_tick".equals(command)) {
                toggleAnnotationMode(AnnotationMode.ADD_BAND_TICKS);
            } else if ("toggle_draw_h_line".equals(command)) {
                toggleAnnotationMode(AnnotationMode.DRAW_H_LINE);
            } else if ("toggle_draw_v_line".equals(command)) {
                toggleAnnotationMode(AnnotationMode.DRAW_V_LINE);
            } else if ("toggle_add_free_text".equals(command)) {
                toggleAnnotationMode(AnnotationMode.ADD_FREE_TEXT);
            } else if ("toggle_edit_annotations".equals(command)) {
                toggleAnnotationMode(AnnotationMode.EDIT_ANNOTATIONS);
            } else if ("annotation_font_smaller".equals(command)) {
                adjustAnnotationFont(-ANNOTATION_FONT_STEP);
            } else if ("annotation_font_larger".equals(command)) {
                adjustAnnotationFont(ANNOTATION_FONT_STEP);
            } else if ("wider".equals(command)) {
                resizeSelectedBand(1.12);
            } else if ("narrower".equals(command)) {
                resizeSelectedBand(1.0 / 1.12);
            } else if ("swap_tick_sides".equals(command)) {
                setTickSidesSwapped(swapTickSidesCheckBox.isSelected());
            } else if ("zoom_out".equals(command)) {
                setManualFigureZoom(currentFigureZoomPercent() - 10, null);
            } else if ("zoom_in".equals(command)) {
                setManualFigureZoom(currentFigureZoomPercent() + 10, null);
            } else if ("zoom_fit".equals(command)) {
                fitFigureToViewport();
            } else if ("show_coordinate_log".equals(command)) {
                showCoordinateLog();
            } else if ("reconstruct_from_log".equals(command)) {
                showReconstructionLogDialog();
            } else if ("clear_figure".equals(command)) {
                clearFigure();
            } else if ("export_pdf".equals(command)) {
                exportPdf();
            }
        }

        private int currentFigureZoomPercent() {
            return figureCanvas == null
                    ? 100 : (int) Math.round(figureCanvas.viewZoom() * 100.0);
        }

        private void setManualFigureZoom(int percent, Point anchorInCanvas) {
            figureFitMode = false;
            applyFigureZoom(percent, anchorInCanvas);
        }

        private void applyFigureZoom(int percent, Point anchorInCanvas) {
            applyFigureZoom(percent / 100.0, anchorInCanvas);
        }

        private void applyFigureZoom(double requestedZoom, Point anchorInCanvas) {
            double clampedZoom = Math.max(0.25, Math.min(4.0, requestedZoom));
            int displayPercent = (int) Math.round(clampedZoom * 100.0);
            updatingFigureZoomUi = true;
            try {
                if (figureZoomSlider != null
                        && figureZoomSlider.getValue() != displayPercent) {
                    figureZoomSlider.setValue(displayPercent);
                }
                if (figureZoomLabel != null) {
                    figureZoomLabel.setText(displayPercent + "%");
                }
            } finally {
                updatingFigureZoomUi = false;
            }
            if (figureCanvas != null) {
                figureCanvas.setViewZoom(clampedZoom, anchorInCanvas);
            }
        }

        private void fitFigureToViewport() {
            if (figureScrollPane == null || figureCanvas == null) {
                return;
            }
            Dimension extent = figureScrollPane.getViewport().getExtentSize();
            if (extent.width <= 0 || extent.height <= 0) {
                return;
            }
            double widthZoom = (extent.width - FigureCanvas.ARTBOARD_MARGIN * 2.0)
                    / FigureCanvas.ARTBOARD_WIDTH;
            double heightZoom = (extent.height - FigureCanvas.ARTBOARD_MARGIN * 2.0)
                    / FigureCanvas.ARTBOARD_HEIGHT;
            int percent = (int) Math.floor(100.0 * Math.min(widthZoom, heightZoom));
            figureFitMode = true;
            applyFigureZoom(percent, null);
        }

        void zoomFigureAt(double factor, Point anchorInCanvas) {
            if (factor <= 0.0 || figureCanvas == null) {
                return;
            }
            figureFitMode = false;
            applyFigureZoom(figureCanvas.viewZoom() * factor, anchorInCanvas);
        }

        private void toggleAnnotationMode(AnnotationMode requestedMode) {
            if (modeRequiresCrop(requestedMode) && bands.isEmpty()) {
                setAnnotationMode(AnnotationMode.NORMAL);
                setStatus("Add a crop to the figure first.");
                return;
            }
            if (requestedMode == AnnotationMode.EDIT_ANNOTATIONS
                    && allTextAnnotations().isEmpty() && lineAnnotations.isEmpty()) {
                setAnnotationMode(AnnotationMode.NORMAL);
                setStatus("There are no annotations to edit yet.");
                return;
            }
            setAnnotationMode(annotationMode == requestedMode
                    ? AnnotationMode.NORMAL : requestedMode);
        }

        private boolean modeRequiresCrop(AnnotationMode mode) {
            return mode == AnnotationMode.ADD_SAMPLE_LABELS
                    || mode == AnnotationMode.ADD_BAND_TICKS;
        }

        private void setAnnotationMode(AnnotationMode nextMode) {
            annotationMode = nextMode == null ? AnnotationMode.NORMAL : nextMode;
            if (figureCanvas != null) {
                figureCanvas.cancelPendingLineDrawing();
            }
            if (figureCanvas != null && annotationMode != AnnotationMode.EDIT_ANNOTATIONS) {
                figureCanvas.clearAnnotationSelection();
            }
            syncAnnotationModeButtons();
            if (annotationMode == AnnotationMode.ADD_SAMPLE_LABELS) {
                setStatus("Add Sample Labels: click inside a crop.");
            } else if (annotationMode == AnnotationMode.ADD_BAND_TICKS) {
                setStatus("Add Band Tick: click inside a crop at the band height.");
            } else if (annotationMode == AnnotationMode.DRAW_H_LINE) {
                setStatus("Draw H-line: drag horizontally on the artboard.");
            } else if (annotationMode == AnnotationMode.DRAW_V_LINE) {
                setStatus("Draw V-line: drag vertically on the artboard.");
            } else if (annotationMode == AnnotationMode.ADD_FREE_TEXT) {
                setStatus("Add Free Text: click anywhere on the A4 artboard.");
            } else if (annotationMode == AnnotationMode.EDIT_ANNOTATIONS) {
                setStatus("Edit Annotations: click an object to select; Shift-click selects several.");
            } else {
                setStatus(null);
            }
            if (figureCanvas != null) {
                figureCanvas.repaint();
            }
            updateAnnotationFontReadout();
        }

        private void syncAnnotationModeButtons() {
            Color inactive = UIManager.getColor("Button.background");
            if (addSampleLabelsButton != null) {
                boolean active = annotationMode == AnnotationMode.ADD_SAMPLE_LABELS;
                addSampleLabelsButton.setSelected(active);
                addSampleLabelsButton.setText(active
                        ? "Stop Adding Labels" : "Add Sample Labels");
                addSampleLabelsButton.setBackground(active
                        ? ANNOTATION_ACTIVE_COLOR : inactive);
            }
            if (addBandTickButton != null) {
                boolean active = annotationMode == AnnotationMode.ADD_BAND_TICKS;
                addBandTickButton.setSelected(active);
                addBandTickButton.setText(active
                        ? "Stop Adding Band Ticks" : "Add Band Tick");
                addBandTickButton.setBackground(active
                        ? ANNOTATION_ACTIVE_COLOR : inactive);
            }
            if (drawHLineButton != null) {
                boolean active = annotationMode == AnnotationMode.DRAW_H_LINE;
                drawHLineButton.setSelected(active);
                drawHLineButton.setText(active
                        ? "Stop Drawing H-lines" : "Draw H-line");
                drawHLineButton.setBackground(active
                        ? ANNOTATION_ACTIVE_COLOR : inactive);
            }
            if (drawVLineButton != null) {
                boolean active = annotationMode == AnnotationMode.DRAW_V_LINE;
                drawVLineButton.setSelected(active);
                drawVLineButton.setText(active
                        ? "Stop Drawing V-lines" : "Draw V-line");
                drawVLineButton.setBackground(active
                        ? ANNOTATION_ACTIVE_COLOR : inactive);
            }
            if (addFreeTextButton != null) {
                boolean active = annotationMode == AnnotationMode.ADD_FREE_TEXT;
                addFreeTextButton.setSelected(active);
                addFreeTextButton.setText(active
                        ? "Stop Adding Text" : "Add Free Text");
                addFreeTextButton.setBackground(active
                        ? ANNOTATION_ACTIVE_COLOR : inactive);
            }
            if (editAnnotationsButton != null) {
                boolean active = annotationMode == AnnotationMode.EDIT_ANNOTATIONS;
                editAnnotationsButton.setSelected(active);
                editAnnotationsButton.setText(active
                        ? "Stop Editing Annotations" : "Edit Annotations");
                editAnnotationsButton.setBackground(active
                        ? ANNOTATION_ACTIVE_COLOR : inactive);
            }
        }

        private void applyCurrentAnnotationDefaults(BandCrop band) {
            for (TextAnnotation annotation : band.textAnnotations) {
                if (annotation.kind == AnnotationKind.MW_VALUE) {
                    annotation.fontSize = defaultKdaFontSize;
                } else if (annotation.kind == AnnotationKind.BAND_NAME) {
                    annotation.fontSize = defaultBandNameFontSize;
                }
            }
        }

        private List<TextAnnotation> allTextAnnotations() {
            List<TextAnnotation> annotations = new ArrayList<TextAnnotation>();
            for (BandCrop band : bands) {
                annotations.addAll(band.textAnnotations);
            }
            annotations.addAll(freeTextAnnotations);
            return annotations;
        }

        private void addSampleLabel(BandCrop band, double xFraction) {
            if (band == null) {
                return;
            }
            SampleLabelDialogResult result = showSampleLabelDialog(
                    "Add Sample Label", "Sample", defaultSampleAngleDeg,
                    defaultSampleFontSize);
            if (result == null) {
                return;
            }
            TextAnnotation annotation = TextAnnotation.sampleLabel(
                    band, result.text, Math.max(0.0, Math.min(1.0, xFraction)),
                    result.angleDeg, result.fontSize);
            band.textAnnotations.add(annotation);
            defaultSampleAngleDeg = result.angleDeg;
            defaultSampleFontSize = result.fontSize;
            figureCanvas.refreshLayout();
            updateAnnotationFontReadout();
            setStatusWithArtboardWarning(
                    "Add Sample Labels: label added; click inside a crop to add another.");
        }

        private void addBandTick(BandCrop band, double yFraction) {
            if (band == null) {
                return;
            }
            TextDialogResult result = showTextDialog(
                    "Add Band Tick", "", defaultBandTickFontSize);
            if (result == null) {
                return;
            }
            TextAnnotation annotation = TextAnnotation.bandTick(
                    band, result.text, yFraction, result.fontSize);
            band.textAnnotations.add(annotation);
            defaultBandTickFontSize = result.fontSize;
            figureCanvas.refreshLayout();
            updateAnnotationFontReadout();
            setStatusWithArtboardWarning(
                    "Add Band Tick: tick added; click inside a crop to add another.");
        }

        private void addFreeText(double anchorX, double anchorY) {
            SampleLabelDialogResult result = showAngleTextDialog(
                    "Add Free Text", "Text:", "", defaultFreeTextAngleDeg,
                    defaultFreeTextFontSize, true);
            if (result == null) {
                return;
            }
            TextAnnotation annotation = TextAnnotation.freeText(
                    result.text, anchorX, anchorY,
                    result.angleDeg, result.fontSize);
            freeTextAnnotations.add(annotation);
            defaultFreeTextAngleDeg = result.angleDeg;
            defaultFreeTextFontSize = result.fontSize;
            figureCanvas.refreshLayout();
            updateAnnotationFontReadout();
            setStatusWithArtboardWarning(
                    "Add Free Text: text added; click the artboard to add another.");
        }

        private void editTextAnnotation(TextAnnotation annotation) {
            if (annotation == null) {
                return;
            }
            if (annotation.kind == AnnotationKind.MW_VALUE) {
                setStatus("MW values can be selected for A-/A+ font resizing only.");
                return;
            }
            if (annotation.kind == AnnotationKind.SAMPLE_LABEL
                    || annotation.kind == AnnotationKind.FREE_TEXT) {
                SampleLabelDialogResult result = annotation.kind == AnnotationKind.SAMPLE_LABEL
                        ? showSampleLabelDialog(
                                "Edit Sample Label", annotation.text,
                                annotation.angleDeg, annotation.fontSize)
                        : showAngleTextDialog(
                                "Edit Free Text", "Text:", annotation.text,
                                annotation.angleDeg, annotation.fontSize, true);
                if (result == null) {
                    return;
                }
                annotation.text = result.text;
                annotation.angleDeg = result.angleDeg;
                annotation.fontSize = result.fontSize;
            } else {
                TextDialogResult result = showTextDialog(
                        annotation.kind == AnnotationKind.BAND_TICK
                                ? "Edit Band Tick" : "Edit Band Name",
                        annotation.text, annotation.fontSize);
                if (result == null) {
                    return;
                }
                annotation.text = result.text;
                annotation.fontSize = result.fontSize;
            }
            figureCanvas.refreshLayout();
            updateAnnotationFontReadout();
            setStatusWithArtboardWarning("Annotation updated.");
        }

        private SampleLabelDialogResult showSampleLabelDialog(String title,
                String initialText, double initialAngleDeg, float initialFontSize) {
            return showAngleTextDialog(title, "Sample name:", initialText,
                    initialAngleDeg, initialFontSize, true);
        }

        private SampleLabelDialogResult showAngleTextDialog(String title,
                String textLabel, String initialText, double initialAngleDeg,
                float initialFontSize, boolean multiline) {
            JSpinner angleSpinner = new JSpinner(new SpinnerNumberModel(
                    Double.valueOf(initialAngleDeg), Double.valueOf(-360.0),
                    Double.valueOf(360.0), Double.valueOf(1.0)));
            JSpinner fontSpinner = new JSpinner(new SpinnerNumberModel(
                    Integer.valueOf(Math.round(clampAnnotationFontSize(initialFontSize))),
                    Integer.valueOf((int) MIN_ANNOTATION_FONT_SIZE),
                    Integer.valueOf((int) MAX_ANNOTATION_FONT_SIZE), Integer.valueOf(1)));
            JPanel optionsPanel = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
            optionsPanel.add(new JLabel("Angle (degrees):"));
            optionsPanel.add(angleSpinner);
            optionsPanel.add(new JLabel("Font size (pt):"));
            optionsPanel.add(fontSpinner);
            String currentText = initialText == null ? "" : initialText;

            while (true) {
                String input = showFocusedTextEntryDialog(title, textLabel,
                        currentText, multiline, optionsPanel);
                if (input == null) {
                    return null;
                }
                currentText = input;
                try {
                    angleSpinner.commitEdit();
                    fontSpinner.commitEdit();
                } catch (java.text.ParseException ex) {
                    JOptionPane.showMessageDialog(frame, "Enter valid numeric values.",
                            title, JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                String text = normalizeAnnotationText(currentText).trim();
                double angle = ((Number) angleSpinner.getValue()).doubleValue();
                int fontSize = ((Number) fontSpinner.getValue()).intValue();
                if (text.length() == 0) {
                    JOptionPane.showMessageDialog(frame, "Enter text.",
                            title, JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                if (Double.isNaN(angle) || Double.isInfinite(angle)) {
                    JOptionPane.showMessageDialog(frame, "Enter a finite angle.",
                            title, JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                if (angle < -360.0 || angle > 360.0) {
                    JOptionPane.showMessageDialog(frame,
                            "Enter an angle from -360 to 360 degrees.",
                            title, JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                return new SampleLabelDialogResult(text, angle,
                        clampAnnotationFontSize(fontSize));
            }
        }

        private TextDialogResult showTextDialog(String title, String initialText,
                float initialFontSize) {
            JSpinner fontSpinner = new JSpinner(new SpinnerNumberModel(
                    Integer.valueOf(Math.round(clampAnnotationFontSize(initialFontSize))),
                    Integer.valueOf((int) MIN_ANNOTATION_FONT_SIZE),
                    Integer.valueOf((int) MAX_ANNOTATION_FONT_SIZE), Integer.valueOf(1)));
            JPanel optionsPanel = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
            optionsPanel.add(new JLabel("Font size (pt):"));
            optionsPanel.add(fontSpinner);
            String currentText = initialText == null ? "" : initialText;
            while (true) {
                String input = showFocusedTextEntryDialog(
                        title, "Text:", currentText, true, optionsPanel);
                if (input == null) {
                    return null;
                }
                currentText = input;
                try {
                    fontSpinner.commitEdit();
                } catch (java.text.ParseException ex) {
                    JOptionPane.showMessageDialog(frame, "Enter a valid font size.",
                            title, JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                String text = normalizeAnnotationText(currentText).trim();
                if (text.length() == 0) {
                    JOptionPane.showMessageDialog(frame, "Enter text.",
                            title, JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                return new TextDialogResult(text, clampAnnotationFontSize(
                        ((Number) fontSpinner.getValue()).floatValue()));
            }
        }

        private String showCropNameDialog(String initialText) {
            String currentText = initialText == null ? "" : initialText;
            while (true) {
                String input = showFocusedTextEntryDialog(
                        "Crop Label", "Crop label:", currentText, true, null);
                if (input == null) {
                    return null;
                }
                currentText = input;
                String text = normalizeAnnotationText(currentText).trim();
                if (text.length() == 0) {
                    JOptionPane.showMessageDialog(frame, "Enter a crop label.",
                            "Crop Label", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                return text;
            }
        }

        private String showFocusedTextEntryDialog(String title, String textLabel,
                String initialText, boolean multiline, JComponent optionsPanel) {
            final JTextArea textArea = new JTextArea(
                    initialText == null ? "" : initialText,
                    multiline ? 4 : 1, 30);
            // Use the same font as Fiji's existing standard input dialogs.
            textArea.setFont(new JTextField().getFont());
            textArea.setLineWrap(false);
            textArea.setWrapStyleWord(false);
            textArea.setFocusTraversalKeysEnabled(true);
            textArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                    Collections.singleton(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)));
            textArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                    Collections.singleton(KeyStroke.getKeyStroke(
                            KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)));

            JPanel textPanel = new JPanel(new BorderLayout(0, 4));
            textPanel.add(new JLabel(textLabel), BorderLayout.NORTH);
            textPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            if (multiline) {
                textPanel.add(new JLabel(
                        "Shift+Enter: new line     Enter: OK     Esc: cancel"),
                        BorderLayout.SOUTH);
            }

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            if (optionsPanel != null) {
                optionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                content.add(optionsPanel);
                content.add(Box.createVerticalStrut(8));
            }
            textPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(textPanel);

            final JOptionPane pane = new JOptionPane(content,
                    JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
                @Override
                public void selectInitialValue() {
                    // Choose the input in Swing's own initial-focus hook.
                    textArea.requestFocus();
                    textArea.selectAll();
                }
            };
            final JDialog dialog = pane.createDialog(frame, title);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            textArea.getInputMap(JComponent.WHEN_FOCUSED).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "accept-text");
            textArea.getActionMap().put("accept-text", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    pane.setValue(Integer.valueOf(JOptionPane.OK_OPTION));
                    dialog.dispose();
                }
            });
            if (multiline) {
                textArea.getInputMap(JComponent.WHEN_FOCUSED).put(
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                                InputEvent.SHIFT_DOWN_MASK), "insert-text-line");
                textArea.getActionMap().put("insert-text-line", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        textArea.replaceSelection("\n");
                    }
                });
            } else {
                textArea.getInputMap(JComponent.WHEN_FOCUSED).put(
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                                InputEvent.SHIFT_DOWN_MASK), "accept-text");
            }
            dialog.getRootPane().registerKeyboardAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    pane.setValue(Integer.valueOf(JOptionPane.CANCEL_OPTION));
                    dialog.dispose();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);

            dialog.setVisible(true);
            Object value = pane.getValue();
            dialog.dispose();
            return value instanceof Integer
                    && ((Integer) value).intValue() == JOptionPane.OK_OPTION
                            ? textArea.getText() : null;
        }

        private static String normalizeAnnotationText(String text) {
            return text == null ? ""
                    : text.replace("\r\n", "\n").replace('\r', '\n');
        }

        private void adjustAnnotationFont(float delta) {
            List<TextAnnotation> selected = figureCanvas == null
                    ? Collections.<TextAnnotation>emptyList()
                    : figureCanvas.selectedTextAnnotations();
            if (selected.isEmpty()) {
                defaultKdaFontSize = clampAnnotationFontSize(defaultKdaFontSize + delta);
                defaultBandNameFontSize = clampAnnotationFontSize(defaultBandNameFontSize + delta);
                defaultSampleFontSize = clampAnnotationFontSize(defaultSampleFontSize + delta);
                defaultBandTickFontSize = clampAnnotationFontSize(
                        defaultBandTickFontSize + delta);
                defaultFreeTextFontSize = clampAnnotationFontSize(
                        defaultFreeTextFontSize + delta);
                for (TextAnnotation annotation : allTextAnnotations()) {
                    annotation.fontSize = clampAnnotationFontSize(annotation.fontSize + delta);
                }
                setStatus("Resized all figure text.");
            } else {
                for (TextAnnotation annotation : selected) {
                    annotation.fontSize = clampAnnotationFontSize(annotation.fontSize + delta);
                }
                setStatus(selected.size() == 1
                        ? "Resized selected text."
                        : "Resized selected text objects.");
            }
            if (figureCanvas != null) {
                figureCanvas.refreshLayout();
            }
            updateAnnotationFontReadout();
            if (figureCanvas != null && figureCanvas.hasArtworkOutsideArtboard()) {
                setStatusWithArtboardWarning("Text resized.");
            }
        }

        private static float clampAnnotationFontSize(float size) {
            return Math.max(MIN_ANNOTATION_FONT_SIZE,
                    Math.min(MAX_ANNOTATION_FONT_SIZE, size));
        }

        private void updateAnnotationFontReadout() {
            if (annotationFontSizeLabel == null) {
                return;
            }
            List<TextAnnotation> selected = figureCanvas == null
                    ? Collections.<TextAnnotation>emptyList()
                    : figureCanvas.selectedTextAnnotations();
            boolean selectedScope = !selected.isEmpty();
            List<Float> sizes = new ArrayList<Float>();
            if (selectedScope) {
                for (TextAnnotation annotation : selected) {
                    sizes.add(Float.valueOf(annotation.fontSize));
                }
            } else {
                for (TextAnnotation annotation : allTextAnnotations()) {
                    sizes.add(Float.valueOf(annotation.fontSize));
                }
                if (sizes.isEmpty()) {
                    sizes.add(Float.valueOf(defaultKdaFontSize));
                    sizes.add(Float.valueOf(defaultBandNameFontSize));
                    sizes.add(Float.valueOf(defaultSampleFontSize));
                    sizes.add(Float.valueOf(defaultBandTickFontSize));
                    sizes.add(Float.valueOf(defaultFreeTextFontSize));
                }
            }
            float minimum = Float.MAX_VALUE;
            float maximum = -Float.MAX_VALUE;
            for (Float size : sizes) {
                minimum = Math.min(minimum, size.floatValue());
                maximum = Math.max(maximum, size.floatValue());
            }
            String range = Math.abs(maximum - minimum) < 0.01f
                    ? formatFontSize(minimum) + " pt"
                    : formatFontSize(minimum) + "-" + formatFontSize(maximum) + " pt";
            if (selectedScope) {
                annotationFontSizeLabel.setText(range
                        + (sizes.size() > 1 ? " (" + sizes.size() + ")" : ""));
            } else {
                annotationFontSizeLabel.setText("All: " + range);
            }
        }

        private static String formatFontSize(float size) {
            if (Math.abs(size - Math.round(size)) < 0.01f) {
                return Integer.toString(Math.round(size));
            }
            return String.format(Locale.US, "%.1f", size);
        }

        private void copySelectedLines() {
            if (figureCanvas == null) {
                return;
            }
            List<LineAnnotation> copied = new ArrayList<LineAnnotation>();
            for (FigureAnnotation annotation : figureCanvas.selectedFigureAnnotations()) {
                if (annotation instanceof LineAnnotation) {
                    copied.add(((LineAnnotation) annotation).copy());
                }
            }
            if (copied.isEmpty()) {
                setStatus("Select an H-line or V-line to copy.");
                return;
            }
            lineClipboard.clear();
            lineClipboard.addAll(copied);
            linePasteGeneration = 0;
            setStatus(copied.size() == 1
                    ? "Copied line; press Ctrl+V to paste."
                    : "Copied " + copied.size() + " lines; press Ctrl+V to paste.");
        }

        private void pasteCopiedLines() {
            if (figureCanvas == null || lineClipboard.isEmpty()) {
                setStatus("Copy an H-line or V-line first.");
                return;
            }
            int generation = linePasteGeneration + 1;
            double minimumX = Double.MAX_VALUE;
            double maximumX = -Double.MAX_VALUE;
            for (LineAnnotation source : lineClipboard) {
                minimumX = Math.min(minimumX, Math.min(source.x1, source.x2));
                maximumX = Math.max(maximumX, Math.max(source.x1, source.x2));
            }
            double groupWidth = Math.max(0.0, maximumX - minimumX);
            double dx = generation * (groupWidth + LINE_PASTE_GAP);
            List<LineAnnotation> pasted = new ArrayList<LineAnnotation>();
            for (LineAnnotation source : lineClipboard) {
                LineAnnotation copy = source.copy();
                copy.translate(dx, 0.0);
                if (!figureCanvas.lineFitsArtboard(copy)) {
                    setStatus("The pasted line would extend outside the A4 artboard.");
                    return;
                }
                pasted.add(copy);
            }
            lineAnnotations.addAll(pasted);
            linePasteGeneration = generation;
            figureCanvas.replaceAnnotationSelection(
                    new ArrayList<FigureAnnotation>(pasted));
            figureCanvas.refreshLayout();
            updateAnnotationFontReadout();
            setStatus(pasted.size() == 1
                    ? "Pasted line at the same height."
                    : "Pasted " + pasted.size() + " lines at their original heights.");
        }

        private void deleteSelectedAnnotations() {
            if (figureCanvas == null) {
                return;
            }
            List<FigureAnnotation> selected = new ArrayList<FigureAnnotation>(
                    figureCanvas.selectedFigureAnnotations());
            if (selected.isEmpty()) {
                return;
            }
            List<FigureAnnotation> protectedAnnotations =
                    new ArrayList<FigureAnnotation>();
            int removed = 0;
            for (FigureAnnotation annotation : selected) {
                if (!annotation.isDeletable()) {
                    protectedAnnotations.add(annotation);
                    continue;
                }
                if (annotation instanceof LineAnnotation) {
                    if (lineAnnotations.remove((LineAnnotation) annotation)) {
                        removed++;
                    }
                } else if (annotation instanceof TextAnnotation) {
                    TextAnnotation text = (TextAnnotation) annotation;
                    if (text.kind == AnnotationKind.FREE_TEXT
                            && freeTextAnnotations.remove(text)) {
                        removed++;
                    } else if (text.owner != null
                            && text.owner.textAnnotations.remove(text)) {
                        removed++;
                    }
                }
            }
            figureCanvas.replaceAnnotationSelection(protectedAnnotations);
            figureCanvas.refreshLayout();
            updateAnnotationFontReadout();
            if (removed > 0 && !protectedAnnotations.isEmpty()) {
                setStatus("Deleted user annotations; calibrated kDa values remain protected.");
            } else if (removed > 0) {
                setStatus(removed == 1 ? "Deleted annotation." : "Deleted annotations.");
            } else {
                setStatus("Calibrated kDa values and ticks cannot be deleted.");
            }
        }

        private void openImage() {
            LoadedImage loaded = openRgbImage();
            if (loaded == null) {
                return;
            }
            cancelCropMode();
            if (kdaModeActive) {
                deactivateKdaMode();
            }
            clearOverlay(gelImp);
            gelImp = loaded.imagePlus;
            gelPath = loaded.path;
            markingSourceType = MarkerSourceType.GEL_IMAGE;
            startFreshMarkerSetOnNextMark = true;
            if (activeMarkerSet != null
                    && activeMarkerSet.sourceType != MarkerSourceType.MARKER_IMAGE) {
                activeMarkerSet = null;
            }
            showImageRightHalf(gelImp);
            warnAboutMarkerMapping(activeMarkerSet);
            redrawKdaOverlays();
            activateGelImage();
            setCropSelectionTool();
            updateActiveMarkerSetLabel();
            setStatus(activeMarkerSet == null
                    ? "Gel loaded. Click Mark kDa Bands to start a new marker set on this Gel."
                    : "Gel loaded. " + activeMarkerSet.id
                            + " from the kDa marker image is applied. Click Mark to start "
                            + "a new set directly on this Gel.");
        }

        private void openMarkerImage() {
            LoadedImage loaded = openRgbImage();
            if (loaded == null) {
                return;
            }
            cancelCropMode();
            if (kdaModeActive) {
                deactivateKdaMode();
            }
            clearOverlay(markerImp);
            markerImp = loaded.imagePlus;
            markerPath = loaded.path;
            markingSourceType = MarkerSourceType.MARKER_IMAGE;
            startFreshMarkerSetOnNextMark = true;
            activeMarkerSet = null;
            showImageRightHalf(markerImp);
            redrawKdaOverlays();
            setCropSelectionTool();
            updateActiveMarkerSetLabel();
            activateKdaMode();
            if (!kdaModeActive) {
                setStatus("kDa marker image loaded. Click Mark kDa Bands, then click its marker bands.");
            }
        }

        private void cancelCropMode() {
            waitingForCrop = false;
            cropWasMarking = false;
            if (cropButton != null) {
                cropButton.setText("Crop Region -> Figure");
                cropButton.setBackground(null);
            }
        }

        private LoadedImage openRgbImage() {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "Image files (TIFF, PNG, JPEG)", "tif", "tiff", "png", "jpg", "jpeg"));
            if (lastDir != null) {
                chooser.setCurrentDirectory(lastDir);
            } else {
                String prefDir = Prefs.get(LAST_DIR_PREFERENCE, null);
                if (prefDir == null) {
                    prefDir = Prefs.get(GELANNO_LAST_DIR_PREFERENCE, null);
                }
                if (prefDir == null) {
                    prefDir = Prefs.get(WB_TOOL_LAST_DIR_PREFERENCE, null);
                }
                if (prefDir != null) {
                    chooser.setCurrentDirectory(new File(prefDir));
                }
            }
            if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            File chosen = chooser.getSelectedFile();
            lastDir = chosen.getParentFile();
            if (lastDir != null) {
                Prefs.set(LAST_DIR_PREFERENCE, lastDir.getAbsolutePath());
            }
            ImagePlus imp = IJ.openImage(chosen.getAbsolutePath());
            if (imp == null) {
                JOptionPane.showMessageDialog(frame, "Could not open: " + chosen.getAbsolutePath(),
                        "Open image", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            if (imp.getType() != ImagePlus.COLOR_RGB) {
                new ImageConverter(imp).convertToRGB();
            }
            String path;
            try {
                path = chosen.getCanonicalPath();
            } catch (Exception ignored) {
                path = chosen.getAbsolutePath();
            }
            return new LoadedImage(imp, path);
        }

        private void showImageRightHalf(ImagePlus imp) {
            imp.show();
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            if (imp.getWindow() != null) {
                imp.getWindow().setLocation(screen.width / 2, 0);
                imp.getWindow().setSize(screen.width / 2, screen.height);
            }
        }

        private void toggleMarkKda() {
            if (markingSourceImage() == null) {
                JOptionPane.showMessageDialog(frame, "Open a gel image or a kDa marker image first.",
                        "No image", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (kdaModeActive) {
                deactivateKdaMode();
            } else {
                activateKdaMode();
            }
        }

        private void activateKdaMode() {
            final ImagePlus source = markingSourceImage();
            if (source == null || source.getCanvas() == null) {
                return;
            }
            if (startFreshMarkerSetOnNextMark
                    || activeMarkerSet == null
                    || !activeMarkerSet.matchesSource(
                            markingSourceType, markingSourcePath(), source.getWidth(), source.getHeight())) {
                activeMarkerSet = createMarkerSetForCurrentSource();
                startFreshMarkerSetOnNextMark = false;
                warnAboutMarkerMapping(activeMarkerSet);
                redrawKdaOverlays();
                updateActiveMarkerSetLabel();
            }
            kdaModeActive = true;
            markButton.setText("Stop Marking kDa");
            markButton.setBackground(new Color(255, 180, 0));
            setStatus("kDa marking active on the " + markingSourceType.displayName
                    + ". Current set: " + activeMarkerSet.id + ".");
            IJ.setTool("point");

            final ImageCanvas canvas = source.getCanvas();
            canvas.disablePopupMenu(true);
            gelMouseListener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    if (!kdaModeActive || event.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                    event.consume();
                    double x = canvas.offScreenXD(event.getX());
                    double y = canvas.offScreenYD(event.getY());
                    onGelClick(x, y);
                }
            };
            kdaCanvas = canvas;
            canvas.addMouseListener(gelMouseListener);
        }

        private void deactivateKdaMode() {
            kdaModeActive = false;
            markButton.setText("Mark kDa Bands");
            markButton.setBackground(null);
            if (kdaCanvas != null) {
                if (gelMouseListener != null) {
                    kdaCanvas.removeMouseListener(gelMouseListener);
                }
                kdaCanvas.disablePopupMenu(false);
            }
            gelMouseListener = null;
            kdaCanvas = null;
            setCropSelectionTool();
            setStatus(" ");
        }

        private void onGelClick(double x, double y) {
            String value = JOptionPane.showInputDialog(frame,
                    "Enter kDa label for this band:", "0");
            if (value == null) {
                return;
            }
            value = value.trim();
            if (value.length() == 0) {
                JOptionPane.showMessageDialog(frame, "Please enter a kDa label.",
                        "Invalid kDa", JOptionPane.WARNING_MESSAGE);
                return;
            }
            KdaMarkerSet markerSet = editableMarkerSetForCurrentSource();
            startFreshMarkerSetOnNextMark = false;
            markerSet.markers.add(new KdaMarker(x, y, value));
            warnAboutMarkerMapping(markerSet);
            redrawKdaOverlays();
            updateActiveMarkerSetLabel();
            setStatus("kDa marking active. " + markerSet.markers.size()
                    + " marker(s) saved in " + markerSet.id + ".");
        }

        private void toggleSourceKdaLabels() {
            showSourceKdaLabels = !showSourceKdaLabels;
            sourceKdaLabelsButton.setText(showSourceKdaLabels ? "Hide kDa Labels" : "Show kDa Labels");
            redrawKdaOverlays();
            redrawAnnotatedMarkerImages();
            redrawAnnotatedCropSourceImages();
            setStatus(showSourceKdaLabels
                    ? "kDa labels are visible on the loaded images."
                    : "kDa labels are hidden on the loaded images; marker Xs remain visible.");
        }

        private void redrawKdaOverlays() {
            clearOverlay(markerImp);
            clearOverlay(gelImp);
            if (activeMarkerSet == null || activeMarkerSet.markers.isEmpty()) {
                return;
            }
            if (activeMarkerSet.sourceType == MarkerSourceType.MARKER_IMAGE) {
                if (markerImp != null && samePath(markerPath, activeMarkerSet.sourcePath)) {
                    drawKdaOverlay(markerImp, activeMarkerSet, false);
                }
                if (gelImp != null) {
                    drawKdaOverlay(gelImp, activeMarkerSet, true);
                }
            } else if (gelImp != null && samePath(gelPath, activeMarkerSet.sourcePath)) {
                drawKdaOverlay(gelImp, activeMarkerSet, false);
            }
        }

        private void drawKdaOverlay(ImagePlus image, KdaMarkerSet markerSet,
                boolean useGelCoordinates) {
            double scaleX = 1.0;
            double scaleY = 1.0;
            if (useGelCoordinates && gelImp != null) {
                scaleX = gelImp.getWidth() / (double) markerSet.sourceWidth;
                scaleY = gelImp.getHeight() / (double) markerSet.sourceHeight;
            }
            drawKdaOverlay(image, markerSet, scaleX, scaleY);
        }

        private void drawKdaOverlay(ImagePlus image, KdaMarkerSet markerSet,
                double scaleX, double scaleY) {
            if (image == null || markerSet == null || markerSet.markers.isEmpty()) {
                clearOverlay(image);
                return;
            }
            double overlayScale = sourceOverlayScale(image.getWidth());
            double markerRadius = SOURCE_MARKER_R * overlayScale;
            Font markerFont = sourceOverlayFont(FONT_SOURCE_KDA, overlayScale);
            Overlay overlay = new Overlay();
            for (KdaMarker marker : markerSet.markers) {
                double x = marker.xAbs * scaleX;
                double y = marker.yAbs * scaleY;
                Line diagA = new Line(x - markerRadius, y - markerRadius,
                        x + markerRadius, y + markerRadius);
                diagA.setStrokeColor(Color.RED);
                diagA.setStrokeWidth(SOURCE_MARKER_STROKE_WIDTH * overlayScale);
                overlay.add(diagA);
                Line diagB = new Line(x - markerRadius, y + markerRadius,
                        x + markerRadius, y - markerRadius);
                diagB.setStrokeColor(Color.RED);
                diagB.setStrokeWidth(SOURCE_MARKER_STROKE_WIDTH * overlayScale);
                overlay.add(diagB);
                if (showSourceKdaLabels) {
                    TextRoi label = new TextRoi(x + 14.0 * overlayScale,
                            y - 36.0 * overlayScale, marker.label, markerFont);
                    label.setStrokeColor(Color.RED);
                    label.setFillColor(new Color(255, 255, 255, 170));
                    overlay.add(label);
                }
            }
            image.setOverlay(overlay);
            image.updateAndDraw();
        }

        private void redrawAnnotatedMarkerImages() {
            for (AnnotatedMarkerImage annotated : annotatedMarkerImages) {
                drawAnnotatedMarkerOverlay(annotated);
            }
        }

        private void redrawAnnotatedCropSourceImages() {
            for (AnnotatedCropSourceImage annotated : annotatedCropSourceImages) {
                drawAnnotatedCropSourceOverlay(annotated);
            }
        }

        private ImagePlus markingSourceImage() {
            if (markingSourceType == MarkerSourceType.MARKER_IMAGE) {
                return markerImp;
            }
            if (markingSourceType == MarkerSourceType.GEL_IMAGE) {
                return gelImp;
            }
            return markerImp != null ? markerImp : gelImp;
        }

        private String markingSourcePath() {
            return markingSourceType == MarkerSourceType.MARKER_IMAGE ? markerPath : gelPath;
        }

        private KdaMarkerSet createMarkerSetForCurrentSource() {
            ImagePlus source = markingSourceImage();
            if (source == null || markingSourceType == null) {
                return null;
            }
            KdaMarkerSet markerSet = new KdaMarkerSet(nextMarkerSetId(), markingSourceType,
                    markingSourcePath(), source.getWidth(), source.getHeight());
            markerSets.add(markerSet);
            return markerSet;
        }

        private KdaMarkerSet editableMarkerSetForCurrentSource() {
            ImagePlus source = markingSourceImage();
            if (activeMarkerSet == null
                    || !activeMarkerSet.matchesSource(markingSourceType, markingSourcePath(),
                            source.getWidth(), source.getHeight())) {
                activeMarkerSet = createMarkerSetForCurrentSource();
                startFreshMarkerSetOnNextMark = false;
            } else if (activeMarkerSet.frozen) {
                KdaMarkerSet copy = activeMarkerSet.editableCopy(nextMarkerSetId());
                markerSets.add(copy);
                activeMarkerSet = copy;
            }
            return activeMarkerSet;
        }

        private String nextMarkerSetId() {
            return String.format(Locale.US, "KDA-%03d", Integer.valueOf(nextMarkerSetNumber++));
        }

        private void updateActiveMarkerSetLabel() {
            if (activeMarkerSet == null) {
                activeMarkerSetLabel.setText("Active markers: none");
                activeMarkerSetLabel.setToolTipText(null);
                return;
            }
            activeMarkerSetLabel.setText("Active: " + activeMarkerSet.id
                    + " (" + activeMarkerSet.sourceType.shortName + ")");
            activeMarkerSetLabel.setToolTipText(activeMarkerSet.sourcePath);
        }

        private void clearOverlay(ImagePlus imp) {
            if (imp != null) {
                imp.setOverlay(null);
                imp.updateAndDraw();
            }
        }

        private void undoLastKda() {
            if (activeMarkerSet == null || activeMarkerSet.markers.isEmpty()) {
                return;
            }
            if (activeMarkerSet.frozen) {
                KdaMarkerSet copy = activeMarkerSet.editableCopy(nextMarkerSetId());
                markerSets.add(copy);
                activeMarkerSet = copy;
            }
            activeMarkerSet.markers.remove(activeMarkerSet.markers.size() - 1);
            redrawKdaOverlays();
            updateActiveMarkerSetLabel();
            setStatus("Last marker removed from " + activeMarkerSet.id + ".");
        }

        private void clearAllKda() {
            activeMarkerSet = null;
            startFreshMarkerSetOnNextMark = true;
            redrawKdaOverlays();
            updateActiveMarkerSetLabel();
            if (kdaModeActive) {
                setStatus("Markers cleared. The next marker starts a new set on the "
                        + markingSourceType.displayName + ".");
            } else {
                setStatus("Markers cleared. Click Mark kDa Bands to start a new set.");
            }
        }

        private void startOrConfirmCrop() {
            if (gelImp == null) {
                JOptionPane.showMessageDialog(frame, "Open a gel image first.",
                        "No image", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!waitingForCrop) {
                cropWasMarking = kdaModeActive;
                if (kdaModeActive) {
                    deactivateKdaMode();
                }
                activateGelImage();
                if (!setCropSelectionTool()) {
                    cropButton.setText("Crop Region -> Figure");
                    cropButton.setBackground(null);
                    JOptionPane.showMessageDialog(frame,
                            "Fiji could not activate the Rotated Rectangle tool. "
                                    + "Please restart Fiji and try again.",
                            "Crop tool unavailable", JOptionPane.ERROR_MESSAGE);
                    restoreCropMarkMode();
                    return;
                }
                if (gelImp.getRoi() == null) {
                    waitingForCrop = true;
                    cropButton.setText("Confirm Crop");
                    cropButton.setBackground(new Color(255, 180, 0));
                    setStatus("Draw and rotate the crop rectangle, then click Confirm Crop.");
                    return;
                }
            }

            Roi roi = gelImp.getRoi();
            if (roi == null) {
                JOptionPane.showMessageDialog(frame, "No selection found. Click Crop again and draw first.",
                        "No selection", JOptionPane.WARNING_MESSAGE);
                activateGelImage();
                setCropSelectionTool();
                setStatus("Crop mode is still active. Draw a crop on the source image, then click Confirm Crop.");
                return;
            }
            styleCropRoi(roi);
            CropResult crop = rotatedCropFromRoi(gelImp, roi);
            if (crop == null) {
                JOptionPane.showMessageDialog(frame, "Selection is too small. Please try again.",
                        "Crop", JOptionPane.WARNING_MESSAGE);
                gelImp.killRoi();
                activateGelImage();
                setCropSelectionTool();
                setStatus("Crop mode is still active. Draw a larger crop, then click Confirm Crop.");
                return;
            }
            waitingForCrop = false;
            cropButton.setText("Crop Region -> Figure");
            cropButton.setBackground(null);

            KdaMarkerSet cropMarkerSet = markerSetApplicableToCurrentGel();
            MarkerMapping markerMapping = markerMappingForCurrentGel(cropMarkerSet);
            warnAboutMarkerMapping(cropMarkerSet);
            List<CropMarker> localMarkers = new ArrayList<CropMarker>();
            if (cropMarkerSet != null) {
                for (KdaMarker marker : cropMarkerSet.markers) {
                    Point2D markerOnGel = markerInGelCoordinates(marker, cropMarkerSet);
                    double yInCrop = markerYInCrop(
                            markerOnGel.x, markerOnGel.y, crop.x, crop.y, crop.angleDeg);
                    if (yInCrop >= -0.5 && yInCrop <= crop.height + 0.5) {
                        localMarkers.add(new CropMarker(marker.label, yInCrop,
                                marker.xAbs, marker.yAbs, markerOnGel.x, markerOnGel.y));
                    }
                }
            }
            Collections.sort(localMarkers, new Comparator<CropMarker>() {
                @Override
                public int compare(CropMarker a, CropMarker b) {
                    return Double.compare(a.yInCrop, b.yInCrop);
                }
            });

            String name = showCropNameDialog("Protein");
            if (name == null) {
                restoreCropMarkMode();
                return;
            }

            BufferedImage image = crop.imagePlus.getProcessor().convertToRGB().getBufferedImage();
            int displayWidth = chooseInitialDisplayWidth(image.getWidth());
            BandCrop band = new BandCrop(image, localMarkers, name, displayWidth,
                    gelPath, gelImp.getWidth(), gelImp.getHeight(), cropMarkerSet, markerMapping);
            applyCurrentAnnotationDefaults(band);
            band.cropX = crop.x;
            band.cropY = crop.y;
            band.cropWidth = crop.width;
            band.cropHeight = crop.height;
            band.cropAngleDeg = crop.angleDeg;
            if (cropMarkerSet != null) {
                cropMarkerSet.frozen = true;
            }
            bands.add(band);
            selectedBand = band;
            updateCropSizeButtons();
            figureCanvas.refreshLayout();
            gelImp.killRoi();
            activateGelImage();
            setCropSelectionTool();
            setStatusWithArtboardWarning(
                    "Crop added. kDa ticks are tied to the crop and scale with it.");
            restoreCropMarkMode();
        }

        private void restoreCropMarkMode() {
            if (cropWasMarking) {
                activateKdaMode();
            }
        }

        private int chooseInitialDisplayWidth(int imageWidth) {
            if (!bands.isEmpty()) {
                return bands.get(bands.size() - 1).displayWidth;
            }
            int available = Math.max(220, (int) Math.floor(
                    A4_PAGE_WIDTH_PT - LEFT_MARGIN - 120.0));
            return Math.max(80, Math.min(imageWidth, available));
        }

        private void activateGelImage() {
            if (gelImp == null) {
                return;
            }
            if (gelImp.getWindow() != null) {
                WindowManager.setCurrentWindow(gelImp.getWindow());
                gelImp.getWindow().toFront();
            }
            if (gelImp.getCanvas() != null) {
                gelImp.getCanvas().requestFocusInWindow();
            }
        }

        private boolean setCropSelectionTool() {
            Roi.setColor(CROP_COLOR);
            trySetDefaultRoiStrokeWidth(CROP_STROKE_WIDTH);
            Toolbar toolbar = Toolbar.getInstance();
            if (toolbar == null || !toolbar.setTool("rotated rectangle")) {
                return false;
            }
            return Toolbar.getToolId() == Toolbar.RECTANGLE
                    && Toolbar.getRectToolType() == Toolbar.ROTATED_RECT_ROI;
        }

        private void trySetDefaultRoiStrokeWidth(float width) {
            try {
                Method method = Roi.class.getMethod("setDefaultStrokeWidth", double.class);
                method.invoke(null, Double.valueOf(width));
            } catch (Throwable ignored) {
                // Older ImageJ builds do not expose a global default stroke width.
            }
        }

        private void styleCropRoi(Roi roi) {
            roi.setStrokeColor(CROP_COLOR);
            roi.setStrokeWidth(CROP_STROKE_WIDTH);
            gelImp.updateAndDraw();
        }

        private CropResult rotatedCropFromRoi(ImagePlus imp, Roi roi) {
            Rectangle bounds = roi.getBounds();
            double x = bounds.x;
            double y = bounds.y;
            int width = bounds.width;
            int height = bounds.height;
            double angle = 0.0;

            try {
                ij.process.FloatPolygon polygon = roi.getFloatPolygon();
                if (polygon != null && polygon.npoints >= 4) {
                    double x0 = polygon.xpoints[0];
                    double y0 = polygon.ypoints[0];
                    double x1 = polygon.xpoints[1];
                    double y1 = polygon.ypoints[1];
                    double x2 = polygon.xpoints[2];
                    double y2 = polygon.ypoints[2];
                    double sideW = distance(x0, y0, x1, y1);
                    double sideH = distance(x1, y1, x2, y2);
                    if (sideW >= 2.0 && sideH >= 2.0) {
                        x = x0;
                        y = y0;
                        width = (int) Math.round(sideW);
                        height = (int) Math.round(sideH);
                        angle = Math.atan2(y1 - y0, x1 - x0);
                    }
                }
            } catch (RuntimeException ignored) {
                // Fall back to the ordinary rectangular ROI bounds.
            }

            if (width < 2 || height < 2) {
                return null;
            }

            return cropFromGeometry(imp, x, y, width, height, Math.toDegrees(angle));
        }

        private CropResult cropFromGeometry(ImagePlus imp, double x, double y,
                int width, int height, double angleDeg) {
            if (imp == null || width < 2 || height < 2) {
                return null;
            }
            double angle = Math.toRadians(angleDeg);
            ImagePlus cropped;
            if (Math.abs(angle) < 0.0001) {
                Roi oldRoi = imp.getRoi();
                imp.setRoi((int) Math.round(x), (int) Math.round(y), width, height);
                cropped = imp.crop();
                imp.setRoi(oldRoi);
                return new CropResult(cropped, x, y, width, height, angleDeg);
            }

            BufferedImage src = imp.getProcessor().convertToRGB().getBufferedImage();
            BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());

            double c = Math.cos(angle);
            double s = Math.sin(angle);
            double tx = -(c * x + s * y);
            double ty = (s * x - c * y);
            AffineTransform transform = new AffineTransform(c, -s, s, c, tx, ty);
            g.drawImage(src, transform, null);
            g.dispose();
            cropped = new ImagePlus("Rotated crop", out);
            return new CropResult(cropped, x, y, width, height, angleDeg);
        }

        private KdaMarkerSet markerSetApplicableToCurrentGel() {
            if (activeMarkerSet == null || gelImp == null) {
                return null;
            }
            if (activeMarkerSet.sourceType == MarkerSourceType.MARKER_IMAGE
                    || samePath(activeMarkerSet.sourcePath, gelPath)) {
                return activeMarkerSet;
            }
            return null;
        }

        private Point2D markerInGelCoordinates(KdaMarker marker, KdaMarkerSet markerSet) {
            if (gelImp == null || markerSet == null
                    || markerSet.sourceType == MarkerSourceType.GEL_IMAGE) {
                return new Point2D(marker.xAbs, marker.yAbs);
            }
            double scaleX = gelImp.getWidth() / (double) markerSet.sourceWidth;
            double scaleY = gelImp.getHeight() / (double) markerSet.sourceHeight;
            return new Point2D(marker.xAbs * scaleX, marker.yAbs * scaleY);
        }

        private MarkerMapping markerMappingForCurrentGel(KdaMarkerSet markerSet) {
            if (markerSet == null || gelImp == null) {
                return null;
            }
            double scaleX = gelImp.getWidth() / (double) markerSet.sourceWidth;
            double scaleY = gelImp.getHeight() / (double) markerSet.sourceHeight;
            boolean dimensionsDiffer = markerSet.sourceWidth != gelImp.getWidth()
                    || markerSet.sourceHeight != gelImp.getHeight();
            boolean aspectRatioMismatch = ((long) markerSet.sourceWidth) * gelImp.getHeight()
                    != ((long) gelImp.getWidth()) * markerSet.sourceHeight;
            return new MarkerMapping(markerSet.sourceWidth, markerSet.sourceHeight,
                    gelImp.getWidth(), gelImp.getHeight(), scaleX, scaleY,
                    dimensionsDiffer, aspectRatioMismatch);
        }

        private void warnAboutMarkerMapping(KdaMarkerSet markerSet) {
            if (markerSet == null || markerSet.markers.isEmpty() || gelImp == null
                    || markerSet.sourceType != MarkerSourceType.MARKER_IMAGE) {
                return;
            }
            MarkerMapping mapping = markerMappingForCurrentGel(markerSet);
            if (mapping == null || !mapping.dimensionsDiffer) {
                return;
            }
            String warningKey = markerSet.id + "|" + gelPath + "|"
                    + mapping.gelWidth + "x" + mapping.gelHeight;
            if (!shownMappingWarnings.add(warningKey)) {
                return;
            }
            StringBuilder message = new StringBuilder();
            message.append("The kDa marker image is ")
                    .append(mapping.markerWidth).append(" x ").append(mapping.markerHeight)
                    .append(" pixels, while the Gel is ")
                    .append(mapping.gelWidth).append(" x ").append(mapping.gelHeight)
                    .append(" pixels.\n\n");
            if (mapping.aspectRatioMismatch) {
                message.append("Their width-to-height ratios are different. Marker coordinates ")
                        .append("will be scaled independently in X and Y, and affected crops ")
                        .append("will contain a warning in the coordinate log.");
            } else {
                message.append("Their aspect ratios match. Marker coordinates will be ")
                        .append("scaled proportionally to the Gel dimensions.");
            }
            JOptionPane.showMessageDialog(frame, message.toString(),
                    mapping.aspectRatioMismatch
                            ? "kDa Aspect Ratio Warning" : "kDa Image Size Warning",
                    JOptionPane.WARNING_MESSAGE);
        }

        private static boolean samePath(String first, String second) {
            return first != null && second != null && first.equalsIgnoreCase(second);
        }

        private static double markerYInCrop(double markerX, double markerY,
                double cropX, double cropY,
                double cropAngleDeg) {
            double angle = Math.toRadians(cropAngleDeg);
            double dx = markerX - cropX;
            double dy = markerY - cropY;
            return -Math.sin(angle) * dx + Math.cos(angle) * dy;
        }

        private static double distance(double ax, double ay, double bx, double by) {
            double dx = ax - bx;
            double dy = ay - by;
            return Math.sqrt(dx * dx + dy * dy);
        }

        private void resizeSelectedBand(double factor) {
            BandCrop band = selectedBand;
            if (band == null) {
                setStatus("Select a crop before changing its size.");
                return;
            }
            int next = (int) Math.round(band.displayWidth * factor);
            int maximumWidth = (int) Math.floor(
                    A4_PAGE_WIDTH_PT - LEFT_MARGIN - FigureCanvas.CONTENT_EDGE_GAP);
            band.displayWidth = Math.max(50, Math.min(next, maximumWidth));
            figureCanvas.refreshLayout();
            setStatusWithArtboardWarning(
                    "Crop resized. kDa ticks were recomputed from the crop scale.");
        }

        private void updateCropSizeButtons() {
            boolean enabled = selectedBand != null;
            if (narrowerButton != null) {
                narrowerButton.setEnabled(enabled);
            }
            if (widerButton != null) {
                widerButton.setEnabled(enabled);
            }
        }

        private void clearFigure() {
            setAnnotationMode(AnnotationMode.NORMAL);
            if (figureCanvas != null) {
                figureCanvas.clearAnnotationSelection();
            }
            bands.clear();
            lineAnnotations.clear();
            freeTextAnnotations.clear();
            selectedBand = null;
            updateCropSizeButtons();
            figureCanvas.refreshLayout();
            updateAnnotationFontReadout();
        }

        private void exportPdf() {
            if (bands.isEmpty() && lineAnnotations.isEmpty()
                    && freeTextAnnotations.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Nothing to export.",
                        "Export PDF", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JTextField dpiField = new JTextField("300", 6);
            JCheckBox useClipGroupsCheckBox = new JCheckBox(
                    "Use clip groups to group crop bands with their annotations", false);
            JPanel dpiRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            dpiRow.setAlignmentX(0.0f);
            dpiRow.add(new JLabel("Raster resolution for crop images (DPI): "));
            dpiRow.add(dpiField);
            useClipGroupsCheckBox.setAlignmentX(0.0f);

            JLabel pageInfo = new JLabel(
                    "A4 portrait; vector text remains at its selected point size.");
            pageInfo.setAlignmentX(0.0f);

            JPanel exportOptions = new JPanel();
            exportOptions.setLayout(new BoxLayout(exportOptions, BoxLayout.Y_AXIS));
            exportOptions.add(dpiRow);
            exportOptions.add(Box.createVerticalStrut(4));
            exportOptions.add(pageInfo);
            exportOptions.add(Box.createVerticalStrut(8));
            exportOptions.add(useClipGroupsCheckBox);

            int option = JOptionPane.showConfirmDialog(frame, exportOptions, "Export PDF",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return;
            }
            String dpiText = dpiField.getText();
            boolean useClipGroups = useClipGroupsCheckBox.isSelected();
            int dpi;
            try {
                dpi = Integer.parseInt(dpiText.trim());
            } catch (NumberFormatException ex) {
                dpi = 300;
            }
            dpi = Math.max(72, Math.min(600, dpi));
            if (figureCanvas.hasArtworkOutsideArtboard()) {
                int overflowOption = JOptionPane.showConfirmDialog(frame,
                        "Some figure content extends outside the A4 artboard and will be clipped.\n"
                                + "Continue with the export?",
                        "Content outside A4", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (overflowOption != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            File path = chooseSavePath("Export PDF", "PDF", "pdf");
            if (path == null) {
                return;
            }

            com.itextpdf.text.Rectangle pageSize = PageSize.A4;
            float pageHeight = pageSize.getHeight();

            FileOutputStream out = null;
            Document doc = null;
            try {
                out = new FileOutputStream(path);
                doc = new Document(pageSize, 0, 0, 0, 0);
                PdfWriter writer = PdfWriter.getInstance(doc, out);
                writer.setPdfVersion(PdfWriter.VERSION_1_5);
                doc.open();
                PdfContentByte cb = writer.getDirectContent();
                if (useClipGroups) {
                    new FormXObjectPdfRenderer(figureCanvas, cb,
                            pageHeight, dpi).render();
                } else {
                    new FlatPdfRenderer(figureCanvas, cb,
                            pageHeight, dpi).render();
                }
                doc.close();
                out.close();
                JOptionPane.showMessageDialog(frame, "PDF saved: " + path.getAbsolutePath(),
                        "Export PDF", JOptionPane.INFORMATION_MESSAGE);
            } catch (Throwable ex) {
                try {
                    if (doc != null && doc.isOpen()) {
                        doc.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (Exception ignored) {
                    // Ignore cleanup failures and show the original export problem.
                }
                JOptionPane.showMessageDialog(frame,
                        "PDF export failed. Make sure the iText module/JAR is available in Fiji.\n"
                                + ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                        "Export PDF", JOptionPane.ERROR_MESSAGE);
            }
        }

        private File chooseSavePath(String title, String description, String extension) {
            return chooseSavePath(frame, title, description, extension, null);
        }

        private File chooseSavePath(Component parent, String title, String description,
                String extension, String suggestedBaseName) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(title);
            chooser.setFileFilter(new FileNameExtensionFilter(description, extension));
            if (lastDir != null) {
                chooser.setCurrentDirectory(lastDir);
            }
            if (suggestedBaseName != null && suggestedBaseName.length() > 0) {
                File suggested = new File(chooser.getCurrentDirectory(),
                        suggestedBaseName + "." + extension);
                chooser.setSelectedFile(suggested);
            }
            if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase(Locale.US).endsWith("." + extension)) {
                file = new File(file.getParentFile(), file.getName() + "." + extension);
            }
            lastDir = file.getParentFile();
            if (lastDir != null) {
                Prefs.set(LAST_DIR_PREFERENCE, lastDir.getAbsolutePath());
            }
            return file;
        }

        private void showCoordinateLog() {
            final JDialog dialog = new JDialog(frame, "Gelato Coordinate Log", false);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setLayout(new BorderLayout(8, 8));

            final JTextArea textArea = new JTextArea(buildCoordinateLog(), 34, 92);
            textArea.setEditable(false);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textArea.setLineWrap(false);
            textArea.setCaretPosition(0);
            dialog.add(new JScrollPane(textArea), BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton refresh = new JButton("Refresh");
            refresh.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    textArea.setText(buildCoordinateLog());
                    textArea.setCaretPosition(0);
                }
            });
            buttons.add(refresh);

            JButton copy = new JButton("Copy");
            copy.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new StringSelection(textArea.getText()), null);
                    setStatus("Coordinate log copied to the clipboard.");
                }
            });
            buttons.add(copy);

            JButton save = new JButton("Save Log...");
            save.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    saveCoordinateLog(textArea.getText());
                }
            });
            buttons.add(save);

            JButton close = new JButton("Close");
            close.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    dialog.dispose();
                }
            });
            buttons.add(close);
            dialog.add(buttons, BorderLayout.SOUTH);

            dialog.setSize(860, 650);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        }

        private void showReconstructionLogDialog() {
            final JDialog dialog = new JDialog(frame, "Reconstruct from Coordinate Log", true);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setLayout(new BorderLayout(8, 8));

            final JTextArea textArea = new JTextArea(34, 92);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textArea.setLineWrap(false);
            dialog.add(new JScrollPane(textArea), BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton load = new JButton("Load Log...");
            load.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Load Coordinate Log");
                    chooser.setFileFilter(new FileNameExtensionFilter(
                            "Coordinate logs (TXT, LOG)", "txt", "log"));
                    if (lastDir != null) {
                        chooser.setCurrentDirectory(lastDir);
                    }
                    if (chooser.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION) {
                        return;
                    }
                    File file = chooser.getSelectedFile();
                    try {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        textArea.setText(new String(bytes, StandardCharsets.UTF_8));
                        textArea.setCaretPosition(0);
                        lastDir = file.getParentFile();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog,
                                "Could not load the coordinate log.\n"
                                        + ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                                "Load Coordinate Log", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            buttons.add(load);

            JButton reconstruct = new JButton("Reconstruct");
            reconstruct.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    ParsedCoordinateLog parsed;
                    try {
                        parsed = parseCoordinateLog(textArea.getText());
                    } catch (IllegalArgumentException ex) {
                        showReadableMessageDialog(dialog, ex.getMessage(),
                                "Invalid Coordinate Log", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    dialog.dispose();
                    reconstructFromLog(parsed);
                }
            });
            buttons.add(reconstruct);

            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    dialog.dispose();
                }
            });
            buttons.add(cancel);
            dialog.add(buttons, BorderLayout.SOUTH);

            dialog.setSize(860, 650);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        }

        private ParsedCoordinateLog parseCoordinateLog(String text) {
            if (text == null || text.trim().length() == 0) {
                throw new IllegalArgumentException("Paste a coordinate log or load one from a file.");
            }
            String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
            String[] lines = normalized.split("\n", -1);
            String header = lines.length == 0 ? "" : lines[0].trim();
            if (!COORDINATE_LOG_HEADER.equals(header)
                    && !GELANNO_COORDINATE_LOG_HEADER.equals(header)
                    && !WB_TOOL_COORDINATE_LOG_HEADER.equals(header)) {
                throw new IllegalArgumentException(
                        "This does not begin with a recognized Gelato, GelAnno, "
                                + "or WB Tool Coordinate Log header.");
            }

            ParsedCoordinateLog parsed = new ParsedCoordinateLog();
            int section = 0;
            LoggedMarkerSet currentSet = null;
            LoggedCrop currentCrop = null;
            boolean inUsedMarkers = false;

            for (int index = 1; index < lines.length; index++) {
                String raw = lines[index];
                String line = raw.trim();
                int lineNumber = index + 1;
                if (line.length() == 0) {
                    continue;
                }
                if (line.startsWith("Log format version:")) {
                    parsed.formatVersion = parseInteger(
                            valueAfter(line, "Log format version:"), lineNumber, "log format version");
                    continue;
                }
                if (line.startsWith("Plugin version:")) {
                    parsed.pluginVersion = valueAfter(line, "Plugin version:");
                    continue;
                }
                if ("Global kDa marker sets:".equals(line)) {
                    section = 1;
                    currentSet = null;
                    continue;
                }
                if ("Crops in figure:".equals(line)) {
                    section = 2;
                    currentSet = null;
                    continue;
                }

                if (section == 1) {
                    if (!startsWithWhitespace(raw) && line.endsWith(":")) {
                        String id = line.substring(0, line.length() - 1).trim();
                        if (id.length() == 0 || "none".equalsIgnoreCase(id)) {
                            continue;
                        }
                        currentSet = new LoggedMarkerSet(id);
                        parsed.markerSets.add(currentSet);
                        continue;
                    }
                    if (currentSet == null) {
                        continue;
                    }
                    if (line.startsWith("Source type:")) {
                        String type = stripOptionalQuotes(
                                valueAfter(line, "Source type:"));
                        if ("kDa marker image".equalsIgnoreCase(type)) {
                            currentSet.sourceType = MarkerSourceType.MARKER_IMAGE;
                        } else if ("Gel image".equalsIgnoreCase(type)) {
                            currentSet.sourceType = MarkerSourceType.GEL_IMAGE;
                        } else {
                            throw parseError(lineNumber, "Unknown marker source type: " + type);
                        }
                    } else if (line.startsWith("Source image:")) {
                        currentSet.sourcePath = unescapeLogValue(
                                valueAfter(line, "Source image:"), parsed.formatVersion);
                    } else if (line.startsWith("Source dimensions:")) {
                        int[] dimensions = parseDimensions(
                                valueAfter(line, "Source dimensions:"), lineNumber);
                        currentSet.sourceWidth = dimensions[0];
                        currentSet.sourceHeight = dimensions[1];
                        currentSet.hasSourceDimensions = true;
                    } else if (isNumberedEntry(line) && line.contains(", x_abs = ")
                            && line.contains(", y_abs = ")) {
                        currentSet.markers.add(parseLoggedMarker(
                                line, lineNumber, parsed.formatVersion));
                    }
                    continue;
                }

                if (section == 2) {
                    if (!startsWithWhitespace(raw) && line.startsWith("Band ")) {
                        int colon = line.indexOf(':');
                        if (colon < 0) {
                            throw parseError(lineNumber, "Band entry has no name separator.");
                        }
                        currentCrop = new LoggedCrop();
                        currentCrop.name = unescapeLogValue(
                                line.substring(colon + 1).trim(), parsed.formatVersion);
                        parsed.crops.add(currentCrop);
                        inUsedMarkers = false;
                        continue;
                    }
                    if (currentCrop == null) {
                        continue;
                    }
                    if ("Used kDa markers:".equals(line)) {
                        inUsedMarkers = true;
                    } else if (!inUsedMarkers && line.startsWith("Source image:")) {
                        currentCrop.sourcePath = unescapeLogValue(
                                valueAfter(line, "Source image:"), parsed.formatVersion);
                    } else if (!inUsedMarkers && line.startsWith("Source dimensions:")) {
                        int[] dimensions = parseDimensions(
                                valueAfter(line, "Source dimensions:"), lineNumber);
                        currentCrop.sourceWidth = dimensions[0];
                        currentCrop.sourceHeight = dimensions[1];
                        currentCrop.hasSourceDimensions = true;
                    } else if (line.startsWith("Crop origin:")) {
                        double[] pair = parseNamedPair(valueAfter(line, "Crop origin:"),
                                "x", "y", lineNumber);
                        currentCrop.cropX = pair[0];
                        currentCrop.cropY = pair[1];
                        currentCrop.hasOrigin = true;
                    } else if (line.startsWith("Crop size:")) {
                        double[] pair = parseNamedPair(valueAfter(line, "Crop size:"),
                                "width", "height", lineNumber);
                        currentCrop.cropWidth = (int) Math.round(pair[0]);
                        currentCrop.cropHeight = (int) Math.round(pair[1]);
                        currentCrop.hasSize = true;
                    } else if (line.startsWith("Crop angle:")) {
                        String value = valueAfter(line, "Crop angle:");
                        int degrees = value.indexOf(" degrees");
                        if (degrees >= 0) {
                            value = value.substring(0, degrees).trim();
                        }
                        currentCrop.cropAngleDeg = parseDouble(
                                value, lineNumber, "crop angle");
                        currentCrop.hasAngle = true;
                    } else if (!inUsedMarkers
                            && parseCropCornerLine(currentCrop, line, lineNumber)) {
                        // The labeled corner was stored by parseCropCornerLine.
                    } else if (inUsedMarkers && line.startsWith("Marker set:")) {
                        String markerSetId = valueAfter(line, "Marker set:");
                        currentCrop.markerSetId = "none".equalsIgnoreCase(markerSetId)
                                ? null : markerSetId;
                    } else if (inUsedMarkers && isNumberedEntry(line)
                            && line.contains("label = ")) {
                        currentCrop.markers.add(parseLoggedCropMarker(
                                line, lineNumber, parsed.formatVersion));
                    }
                }
            }

            validateParsedCoordinateLog(parsed);
            return parsed;
        }

        private void validateParsedCoordinateLog(ParsedCoordinateLog parsed) {
            if (parsed.formatVersion < 0) {
                throw new IllegalArgumentException("Log format version cannot be negative.");
            }
            if (parsed.formatVersion > LOG_FORMAT_VERSION) {
                throw new IllegalArgumentException("Log format version " + parsed.formatVersion
                        + " is newer than this plugin supports (version "
                        + LOG_FORMAT_VERSION + ").");
            }
            Map<String, LoggedMarkerSet> markerSetsById =
                    new LinkedHashMap<String, LoggedMarkerSet>();
            for (LoggedMarkerSet markerSet : parsed.markerSets) {
                if (markerSet.sourceType == null || markerSet.sourcePath == null) {
                    throw new IllegalArgumentException("Marker set " + markerSet.id
                            + " is missing its source type or source image.");
                }
                if (markerSet.hasSourceDimensions
                        && (markerSet.sourceWidth <= 0 || markerSet.sourceHeight <= 0)) {
                    throw new IllegalArgumentException("Marker set " + markerSet.id
                            + " has invalid source dimensions.");
                }
                if (markerSetsById.containsKey(markerSet.id)) {
                    throw new IllegalArgumentException(
                            "The coordinate log contains duplicate marker set "
                                    + markerSet.id + ".");
                }
                markerSetsById.put(markerSet.id, markerSet);
            }
            if (parsed.crops.isEmpty()) {
                throw new IllegalArgumentException("The coordinate log contains no crops.");
            }
            int cropNumber = 1;
            for (LoggedCrop crop : parsed.crops) {
                if (crop.sourcePath == null) {
                    throw new IllegalArgumentException("Band " + cropNumber
                            + " is missing its source image.");
                }
                if (crop.hasSourceDimensions
                        && (crop.sourceWidth <= 0 || crop.sourceHeight <= 0)) {
                    throw new IllegalArgumentException("Band " + cropNumber
                            + " has invalid source dimensions.");
                }
                if (crop.hasSize && (crop.cropWidth <= 0 || crop.cropHeight <= 0)) {
                    if (crop.cornerCount() >= 3) {
                        crop.hasSize = false;
                        crop.parameterIssue = "non-positive parameter width or height was ignored";
                    } else {
                        throw new IllegalArgumentException("Band " + cropNumber
                                + " has a non-positive crop width or height.");
                    }
                }
                prepareLoggedCropGeometry(crop, cropNumber);
                if (!crop.hasResolvedGeometry() && !crop.geometryConflict) {
                    throw new IllegalArgumentException("Band " + cropNumber
                            + " needs either crop origin and size, or at least three labeled corners.");
                }
                if (crop.markerSetId != null && !markerSetsById.containsKey(crop.markerSetId)) {
                    throw new IllegalArgumentException("Band " + cropNumber
                            + " refers to unknown marker set " + crop.markerSetId + ".");
                }
                if (crop.markerSetId == null && !crop.markers.isEmpty()) {
                    throw new IllegalArgumentException("Band " + cropNumber
                            + " contains marker coordinates but names no marker set.");
                }
                cropNumber++;
            }
            parsed.markerSetsById.putAll(markerSetsById);
        }

        private static boolean parseCropCornerLine(
                LoggedCrop crop, String line, int lineNumber) {
            String[] labels = {"top-left:", "top-right:", "bottom-right:", "bottom-left:"};
            for (int index = 0; index < labels.length; index++) {
                if (!line.startsWith(labels[index])) {
                    continue;
                }
                double[] pair = parseNamedPair(valueAfter(line, labels[index]),
                        "x", "y", lineNumber);
                crop.loggedCorners[index] = new Point2D(pair[0], pair[1]);
                return true;
            }
            return false;
        }

        private static void prepareLoggedCropGeometry(LoggedCrop crop, int bandNumber) {
            boolean hasParameterBase = crop.hasOrigin && crop.hasSize;
            boolean parameterHadAngle = crop.hasAngle;
            if (hasParameterBase) {
                crop.parameterGeometry = new ReconstructedGeometry(
                        crop.cropX, crop.cropY, crop.cropWidth, crop.cropHeight,
                        parameterHadAngle ? crop.cropAngleDeg : 0.0);
            }

            int cornerCount = crop.cornerCount();
            if (cornerCount >= 3) {
                try {
                    crop.cornerFit = fitRectangleToCorners(crop.loggedCorners);
                } catch (IllegalArgumentException exception) {
                    if (!hasParameterBase) {
                        throw new IllegalArgumentException("Band " + bandNumber
                                + " has invalid crop corners: " + exception.getMessage());
                    }
                    crop.cornerIssue = exception.getMessage();
                }
            }

            if (crop.parameterGeometry != null && parameterHadAngle && crop.cornerFit != null) {
                crop.geometryDiscrepancy = maximumProvidedCornerDifference(
                        crop.parameterGeometry, crop.loggedCorners);
                if (crop.geometryDiscrepancy > CROP_GEOMETRY_AGREEMENT_TOLERANCE) {
                    crop.geometryConflict = true;
                    return;
                }
                applyLoggedCropGeometry(crop, crop.parameterGeometry);
                crop.geometryDescription = "origin, size and angle; crop corners agreed"
                        + geometryDifferenceSuffix(crop.geometryDiscrepancy);
                return;
            }

            if (crop.cornerFit != null) {
                applyLoggedCropGeometry(crop, crop.cornerFit.geometry);
                String reason = crop.parameterIssue != null
                        ? "; " + crop.parameterIssue
                        : crop.parameterGeometry != null && !parameterHadAngle
                        ? "; the incomplete parameter geometry had no angle"
                        : crop.hasOrigin || crop.hasSize || crop.hasAngle
                                ? "; incomplete parameter geometry was ignored" : "";
                crop.geometryDescription = crop.cornerFit.description() + reason;
                return;
            }

            if (crop.parameterGeometry != null) {
                applyLoggedCropGeometry(crop, crop.parameterGeometry);
                if (parameterHadAngle) {
                    crop.geometryDescription = "origin, size and angle";
                } else {
                    crop.geometryDescription = "origin and size; missing angle assumed to be 0 degrees";
                }
                if (crop.cornerIssue != null) {
                    crop.geometryDescription += "; invalid crop corners were ignored ("
                            + crop.cornerIssue + ")";
                } else if (cornerCount > 0) {
                    crop.geometryDescription += "; " + cornerCount
                            + " corner coordinate(s) were insufficient and ignored";
                }
            }
        }

        private boolean resolveCropGeometryConflicts(ParsedCoordinateLog parsed) {
            int bandNumber = 1;
            for (LoggedCrop crop : parsed.crops) {
                if (!crop.geometryConflict) {
                    bandNumber++;
                    continue;
                }
                String cropName = crop.name == null || crop.name.length() == 0
                        ? "Band " + bandNumber : "Band " + bandNumber + " (" + crop.name + ")";
                String message = cropName + " contains two crop descriptions that differ by up to "
                        + formatDisplayCoordinate(crop.geometryDiscrepancy) + " pixels.\n\n"
                        + "Origin/size/angle:\n  "
                        + geometrySummary(crop.parameterGeometry) + "\n\n"
                        + "Closest rectangle fitted to corners:\n  "
                        + geometrySummary(crop.cornerFit.geometry) + "\n  "
                        + crop.cornerFit.description() + "\n\n"
                        + "Which geometry should Gelato use?";
                Object[] options = {
                    "Use Origin / Size / Angle",
                    "Fit Crop Corners",
                    "Cancel Reconstruction"
                };
                int choice = showReadableOptionDialog(frame, message,
                        "Crop Geometry Disagreement", JOptionPane.WARNING_MESSAGE,
                        options, options[0]);
                if (choice == 0) {
                    applyLoggedCropGeometry(crop, crop.parameterGeometry);
                    crop.geometryDescription = "origin, size and angle selected after a "
                            + formatDisplayCoordinate(crop.geometryDiscrepancy)
                            + "-pixel disagreement with crop corners";
                } else if (choice == 1) {
                    applyLoggedCropGeometry(crop, crop.cornerFit.geometry);
                    crop.geometryDescription = crop.cornerFit.description()
                            + "; selected after a "
                            + formatDisplayCoordinate(crop.geometryDiscrepancy)
                            + "-pixel disagreement with origin/size/angle";
                } else {
                    return false;
                }
                crop.geometryConflict = false;
                bandNumber++;
            }
            return true;
        }

        private static CornerFit fitRectangleToCorners(Point2D[] sourceCorners) {
            Point2D[] corners = new Point2D[4];
            int missingIndex = -1;
            int count = 0;
            for (int index = 0; index < corners.length; index++) {
                corners[index] = sourceCorners[index];
                if (corners[index] == null) {
                    missingIndex = index;
                } else {
                    count++;
                }
            }
            if (count < 3) {
                throw new IllegalArgumentException("at least three labeled corners are required.");
            }
            if (count == 3) {
                corners[missingIndex] = inferMissingRectangleCorner(corners, missingIndex);
            }

            Point2D topLeft = corners[0];
            Point2D topRight = corners[1];
            Point2D bottomRight = corners[2];
            Point2D bottomLeft = corners[3];
            double horizontalX = ((topRight.x - topLeft.x)
                    + (bottomRight.x - bottomLeft.x)) / 2.0;
            double horizontalY = ((topRight.y - topLeft.y)
                    + (bottomRight.y - bottomLeft.y)) / 2.0;
            double verticalX = ((bottomLeft.x - topLeft.x)
                    + (bottomRight.x - topRight.x)) / 2.0;
            double verticalY = ((bottomLeft.y - topLeft.y)
                    + (bottomRight.y - topRight.y)) / 2.0;
            if (Math.hypot(horizontalX, horizontalY) < 1.0
                    || Math.hypot(verticalX, verticalY) < 1.0) {
                throw new IllegalArgumentException("the fitted rectangle has a degenerate edge.");
            }

            // Principal-axis fit for h = width*u and v = height*perpendicular(u).
            double matrixXX = horizontalX * horizontalX + verticalY * verticalY;
            double matrixXY = horizontalX * horizontalY - verticalX * verticalY;
            double matrixYY = horizontalY * horizontalY + verticalX * verticalX;
            double angle = 0.5 * Math.atan2(
                    2.0 * matrixXY, matrixXX - matrixYY);
            double unitX = Math.cos(angle);
            double unitY = Math.sin(angle);
            if (horizontalX * unitX + horizontalY * unitY < 0.0) {
                unitX = -unitX;
                unitY = -unitY;
                angle = Math.atan2(unitY, unitX);
            }
            double normalX = -unitY;
            double normalY = unitX;
            double fittedWidth = horizontalX * unitX + horizontalY * unitY;
            double fittedHeight = verticalX * normalX + verticalY * normalY;
            if (fittedWidth <= 0.0 || fittedHeight <= 0.0) {
                throw new IllegalArgumentException(
                        "corner labels do not describe a consistently oriented rectangle.");
            }

            int width = Math.max(2, (int) Math.round(fittedWidth));
            int height = Math.max(2, (int) Math.round(fittedHeight));
            double centerX = 0.0;
            double centerY = 0.0;
            for (Point2D corner : corners) {
                centerX += corner.x;
                centerY += corner.y;
            }
            centerX /= 4.0;
            centerY /= 4.0;
            double fittedX = centerX - unitX * width / 2.0 - normalX * height / 2.0;
            double fittedY = centerY - unitY * width / 2.0 - normalY * height / 2.0;
            ReconstructedGeometry geometry = new ReconstructedGeometry(
                    fittedX, fittedY, width, height, Math.toDegrees(angle));
            double fitError = maximumProvidedCornerDifference(geometry, sourceCorners);
            return new CornerFit(geometry, count, missingIndex, fitError);
        }

        private static Point2D inferMissingRectangleCorner(Point2D[] corners, int missingIndex) {
            if (missingIndex == 0) {
                return addSubtract(corners[1], corners[3], corners[2]);
            }
            if (missingIndex == 1) {
                return addSubtract(corners[0], corners[2], corners[3]);
            }
            if (missingIndex == 2) {
                return addSubtract(corners[1], corners[3], corners[0]);
            }
            return addSubtract(corners[0], corners[2], corners[1]);
        }

        private static Point2D addSubtract(Point2D first, Point2D second, Point2D subtract) {
            return new Point2D(first.x + second.x - subtract.x,
                    first.y + second.y - subtract.y);
        }

        private static double maximumProvidedCornerDifference(
                ReconstructedGeometry geometry, Point2D[] providedCorners) {
            Point2D[] fittedCorners = reconstructedCropCorners(geometry);
            double maximum = 0.0;
            for (int index = 0; index < fittedCorners.length; index++) {
                if (providedCorners[index] != null) {
                    maximum = Math.max(maximum, distance(
                            fittedCorners[index].x, fittedCorners[index].y,
                            providedCorners[index].x, providedCorners[index].y));
                }
            }
            return maximum;
        }

        private static void applyLoggedCropGeometry(
                LoggedCrop crop, ReconstructedGeometry geometry) {
            crop.cropX = geometry.x;
            crop.cropY = geometry.y;
            crop.cropWidth = geometry.width;
            crop.cropHeight = geometry.height;
            crop.cropAngleDeg = geometry.angleDeg;
            crop.hasOrigin = true;
            crop.hasSize = true;
            crop.hasAngle = true;
        }

        private static String geometrySummary(ReconstructedGeometry geometry) {
            return "origin (" + formatDisplayCoordinate(geometry.x) + ", "
                    + formatDisplayCoordinate(geometry.y) + "), size "
                    + geometry.width + " x " + geometry.height + ", angle "
                    + formatDisplayCoordinate(geometry.angleDeg) + " degrees";
        }

        private static String geometryDifferenceSuffix(double difference) {
            return " (maximum difference " + formatDisplayCoordinate(difference) + " px)";
        }

        private static String formatDisplayCoordinate(double value) {
            return String.format(Locale.US, "%.3f", value);
        }

        private static LoggedMarker parseLoggedMarker(
                String line, int lineNumber, int formatVersion) {
            int labelStart = line.indexOf("label = ");
            int xStart = line.indexOf(", x_abs = ", labelStart);
            int yStart = line.indexOf(", y_abs = ", xStart);
            if (labelStart < 0 || xStart < 0 || yStart < 0) {
                throw parseError(lineNumber, "Invalid marker coordinate entry.");
            }
            String label = unescapeLogValue(
                    line.substring(labelStart + "label = ".length(), xStart).trim(),
                    formatVersion);
            double x = parseDouble(line.substring(xStart + ", x_abs = ".length(), yStart).trim(),
                    lineNumber, "marker x coordinate");
            double y = parseDouble(line.substring(yStart + ", y_abs = ".length()).trim(),
                    lineNumber, "marker y coordinate");
            return new LoggedMarker(label, x, y);
        }

        private static LoggedCropMarker parseLoggedCropMarker(
                String line, int lineNumber, int formatVersion) {
            int labelStart = line.indexOf("label = ");
            if (labelStart < 0) {
                throw parseError(lineNumber, "Crop marker entry has no label.");
            }
            int labelValueStart = labelStart + "label = ".length();
            int labelEnd = line.length();
            String[] coordinateFields = {
                ", source_x_abs = ",
                ", source_y_abs = ",
                ", gel_x_abs = ",
                ", gel_y_abs = ",
                ", y_in_crop = "
            };
            for (String field : coordinateFields) {
                int fieldStart = line.indexOf(field, labelValueStart);
                if (fieldStart >= 0) {
                    labelEnd = Math.min(labelEnd, fieldStart);
                }
            }
            String label = unescapeLogValue(
                    line.substring(labelValueStart, labelEnd).trim(),
                    formatVersion);
            if (label.length() == 0) {
                throw parseError(lineNumber, "Crop marker label is empty.");
            }
            Double sourceX = parseOptionalMarkerCoordinate(line,
                    ", source_x_abs = ", lineNumber, "source marker x coordinate");
            Double sourceY = parseOptionalMarkerCoordinate(line,
                    ", source_y_abs = ", lineNumber, "source marker y coordinate");
            Double gelX = parseOptionalMarkerCoordinate(line,
                    ", gel_x_abs = ", lineNumber, "Gel marker x coordinate");
            Double gelY = parseOptionalMarkerCoordinate(line,
                    ", gel_y_abs = ", lineNumber, "Gel marker y coordinate");
            Double yInCrop = parseOptionalMarkerCoordinate(line,
                    ", y_in_crop = ", lineNumber, "marker y_in_crop coordinate");
            return new LoggedCropMarker(label, sourceX, sourceY, gelX, gelY,
                    yInCrop, lineNumber);
        }

        private static Double parseOptionalMarkerCoordinate(String line, String prefix,
                int lineNumber, String description) {
            int start = line.indexOf(prefix);
            if (start < 0) {
                return null;
            }
            int valueStart = start + prefix.length();
            int valueEnd = line.indexOf(',', valueStart);
            if (valueEnd < 0) {
                valueEnd = line.length();
            }
            String value = line.substring(valueStart, valueEnd).trim();
            return Double.valueOf(parseDouble(value, lineNumber, description));
        }

        private static int[] parseDimensions(String value, int lineNumber) {
            String cleaned = value.replace(" pixels", "").trim();
            int separator = cleaned.indexOf(" x ");
            if (separator < 0) {
                throw parseError(lineNumber, "Invalid image dimensions.");
            }
            int width = parseInteger(cleaned.substring(0, separator).trim(),
                    lineNumber, "image width");
            int height = parseInteger(cleaned.substring(separator + 3).trim(),
                    lineNumber, "image height");
            return new int[] {width, height};
        }

        private static double[] parseNamedPair(String value, String firstName,
                String secondName, int lineNumber) {
            String firstPrefix = firstName + " = ";
            String secondPrefix = ", " + secondName + " = ";
            int firstStart = value.indexOf(firstPrefix);
            int secondStart = value.indexOf(secondPrefix, firstStart + firstPrefix.length());
            if (firstStart < 0 || secondStart < 0) {
                throw parseError(lineNumber, "Invalid " + firstName + "/" + secondName + " pair.");
            }
            String firstValue = value.substring(firstStart + firstPrefix.length(), secondStart).trim();
            String secondValue = value.substring(secondStart + secondPrefix.length())
                    .replace(" pixels", "").trim();
            return new double[] {
                parseDouble(firstValue, lineNumber, firstName),
                parseDouble(secondValue, lineNumber, secondName)
            };
        }

        private static String valueAfter(String line, String prefix) {
            return line.substring(prefix.length()).trim();
        }

        private static int parseInteger(String value, int lineNumber, String field) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw parseError(lineNumber, "Invalid " + field + ": " + value);
            }
        }

        private static double parseDouble(String value, int lineNumber, String field) {
            try {
                double parsed = Double.parseDouble(value);
                if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                    throw parseError(lineNumber, "Invalid " + field + ": " + value);
                }
                return parsed;
            } catch (NumberFormatException ex) {
                throw parseError(lineNumber, "Invalid " + field + ": " + value);
            }
        }

        private static boolean startsWithWhitespace(String value) {
            return value.length() > 0 && Character.isWhitespace(value.charAt(0));
        }

        private static boolean isNumberedEntry(String value) {
            int dot = value.indexOf('.');
            if (dot <= 0) {
                return false;
            }
            for (int i = 0; i < dot; i++) {
                if (!Character.isDigit(value.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        private static IllegalArgumentException parseError(int lineNumber, String message) {
            return new IllegalArgumentException("Coordinate log line " + lineNumber + ": " + message);
        }

        private static String stripOptionalQuotes(String value) {
            String trimmed = value == null ? "" : value.trim();
            if (trimmed.length() >= 2) {
                char first = trimmed.charAt(0);
                char last = trimmed.charAt(trimmed.length() - 1);
                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    return trimmed.substring(1, trimmed.length() - 1).trim();
                }
            }
            return trimmed;
        }

        private static String unescapeLogValue(String value, int formatVersion) {
            return value;
        }

        private void reconstructFromLog(ParsedCoordinateLog parsed) {
            if (!resolveCropGeometryConflicts(parsed)) {
                setStatus("Reconstruction cancelled.");
                return;
            }
            Set<String> referencedMarkerSetIds = referencedMarkerSetIds(parsed);
            LinkedHashMap<String, ReconstructionImageRequest> requests =
                    buildReconstructionImageRequests(parsed, referencedMarkerSetIds);
            List<String> reconstructionWarnings = new ArrayList<String>();
            for (ReconstructionImageRequest request : requests.values()) {
                if (!selectReconstructionImage(request)) {
                    setStatus("Reconstruction cancelled.");
                    return;
                }
            }
            resolveMissingCropSourceDimensions(parsed, requests, reconstructionWarnings);
            resolveMissingMarkerSourceDimensions(parsed, requests,
                    referencedMarkerSetIds, reconstructionWarnings);
            if (!confirmMarkerDimensionCompatibility(parsed, reconstructionWarnings)) {
                setStatus("Reconstruction cancelled.");
                return;
            }
            try {
                resolveLoggedCropMarkers(parsed, reconstructionWarnings);
            } catch (IllegalArgumentException ex) {
                showReadableMessageDialog(frame, ex.getMessage(),
                        "Marker Reconstruction Failed", JOptionPane.ERROR_MESSAGE);
                setStatus("Reconstruction failed.");
                return;
            }

            Controller reconstructed = new Controller(
                    TITLE + " - Reconstructed from coordinate log");
            reconstructed.showFrame();

            Map<String, KdaMarkerSet> reconstructedSets =
                    new LinkedHashMap<String, KdaMarkerSet>();
            int highestMarkerSetNumber = 0;
            for (LoggedMarkerSet loggedSet : parsed.markerSets) {
                if (!referencedMarkerSetIds.contains(loggedSet.id)) {
                    continue;
                }
                KdaMarkerSet markerSet = new KdaMarkerSet(loggedSet.id, loggedSet.sourceType,
                        loggedSet.sourcePath, loggedSet.sourceWidth, loggedSet.sourceHeight);
                for (LoggedMarker marker : loggedSet.markers) {
                    markerSet.markers.add(new KdaMarker(marker.xAbs, marker.yAbs, marker.label));
                }
                markerSet.frozen = true;
                reconstructed.markerSets.add(markerSet);
                reconstructedSets.put(markerSet.id, markerSet);
                highestMarkerSetNumber = Math.max(
                        highestMarkerSetNumber, markerSetNumber(markerSet.id));
            }
            reconstructed.nextMarkerSetNumber = highestMarkerSetNumber + 1;

            for (ReconstructionImageRequest request : requests.values()) {
                if (request.dimensionMismatch && request.loadedImage != null) {
                    reconstructionWarnings.add("Selected dimensions differ for "
                            + request.originalPath
                            + (request.aspectRatioMismatch
                                    ? "; the aspect ratios also differ." : "."));
                }
                if (request.conflictingLoggedDimensions) {
                    reconstructionWarnings.add("The log contains conflicting dimensions for "
                            + request.originalPath + ".");
                }
            }

            int bandNumber = 1;
            for (LoggedCrop loggedCrop : parsed.crops) {
                ReconstructionImageRequest request = requests.get(
                        reconstructionRequestKey(loggedCrop.sourcePath));
                if (request == null || request.loadedImage == null) {
                    JOptionPane.showMessageDialog(frame,
                            "Required Gel image is missing for Band " + bandNumber + ".",
                            "Reconstruction Failed", JOptionPane.ERROR_MESSAGE);
                    reconstructed.frame.dispose();
                    return;
                }
                ReconstructedGeometry geometry = scaleLoggedCropGeometry(
                        loggedCrop, request.loadedImage.imagePlus);
                CropResult crop = reconstructed.cropFromGeometry(
                        request.loadedImage.imagePlus, geometry.x, geometry.y,
                        geometry.width, geometry.height, geometry.angleDeg);
                if (crop == null) {
                    JOptionPane.showMessageDialog(frame,
                            "Could not reconstruct Band " + bandNumber
                                    + " from the selected Gel image.",
                            "Reconstruction Failed", JOptionPane.ERROR_MESSAGE);
                    reconstructed.frame.dispose();
                    return;
                }

                double localScaleY = crop.height / (double) loggedCrop.cropHeight;
                List<CropMarker> cropMarkers = new ArrayList<CropMarker>();
                for (LoggedCropMarker marker : loggedCrop.resolvedMarkers) {
                    cropMarkers.add(new CropMarker(marker.label,
                            marker.yInCrop.doubleValue() * localScaleY,
                            marker.sourceXAbs.doubleValue(), marker.sourceYAbs.doubleValue(),
                            marker.gelXAbs.doubleValue(), marker.gelYAbs.doubleValue()));
                }
                Collections.sort(cropMarkers, new Comparator<CropMarker>() {
                    @Override
                    public int compare(CropMarker first, CropMarker second) {
                        return Double.compare(first.yInCrop, second.yInCrop);
                    }
                });

                BufferedImage image = crop.imagePlus.getProcessor()
                        .convertToRGB().getBufferedImage();
                int displayWidth = reconstructed.chooseInitialDisplayWidth(image.getWidth());
                KdaMarkerSet markerSet = loggedCrop.markerSetId == null
                        ? null : reconstructedSets.get(loggedCrop.markerSetId);
                MarkerMapping markerMapping = markerMappingForLoggedCrop(markerSet, loggedCrop);
                BandCrop band = new BandCrop(image, cropMarkers, loggedCrop.name, displayWidth,
                        loggedCrop.sourcePath, loggedCrop.sourceWidth, loggedCrop.sourceHeight,
                        markerSet, markerMapping);
                band.cropX = loggedCrop.cropX;
                band.cropY = loggedCrop.cropY;
                band.cropWidth = loggedCrop.cropWidth;
                band.cropHeight = loggedCrop.cropHeight;
                band.cropAngleDeg = loggedCrop.cropAngleDeg;
                reconstructed.bands.add(band);
                reconstructed.selectedBand = band;
                bandNumber++;
            }
            reconstructed.updateCropSizeButtons();
            reconstructed.figureCanvas.refreshLayout();

            List<String> skippedMarkerSets = new ArrayList<String>();
            int annotatedIndex = reconstructed.openAnnotatedCropSourceImages(
                    parsed, requests, 0);
            for (LoggedMarkerSet loggedSet : parsed.markerSets) {
                if (!referencedMarkerSetIds.contains(loggedSet.id)) {
                    continue;
                }
                ReconstructionImageRequest request = requests.get(
                        reconstructionRequestKey(loggedSet.sourcePath));
                if (request == null || request.loadedImage == null) {
                    skippedMarkerSets.add(loggedSet.id);
                    continue;
                }
                KdaMarkerSet markerSet = reconstructedSets.get(loggedSet.id);
                reconstructed.openAnnotatedMarkerImage(
                        markerSet, request.loadedImage, annotatedIndex++, parsed.crops);
            }

            String status = "Reconstructed " + reconstructed.bands.size()
                    + " crop(s) from coordinate log.";
            if (!skippedMarkerSets.isEmpty()) {
                status += " Marker sources not supplied: " + joinValues(skippedMarkerSets) + ".";
            }
            reconstructed.setStatus(status);
            reconstructed.frame.toFront();
            showReconstructionReport(reconstructed, parsed,
                    skippedMarkerSets, reconstructionWarnings);
        }

        private static void showReconstructionReport(Controller reconstructed,
                ParsedCoordinateLog parsed, List<String> skippedMarkerSets,
                List<String> reconstructionWarnings) {
            StringBuilder report = new StringBuilder();
            report.append("Reconstructed ").append(parsed.crops.size())
                    .append(" crop(s).\n\nGeometry used:\n");
            int bandNumber = 1;
            for (LoggedCrop crop : parsed.crops) {
                report.append("Band ").append(bandNumber++);
                if (crop.name != null && crop.name.length() > 0) {
                    report.append(" (").append(crop.name).append(")");
                }
                report.append(": ").append(crop.geometryDescription).append(".\n");
                if (crop.dimensionDescription != null) {
                    report.append("  ").append(crop.dimensionDescription).append(".\n");
                }
                if (crop.markerDescription != null) {
                    report.append("  Molecular-weight markers: ")
                            .append(crop.markerDescription).append(".\n");
                }
            }
            boolean describedMarkerDimensions = false;
            for (LoggedMarkerSet markerSet : parsed.markerSets) {
                if (markerSet.dimensionDescription == null) {
                    continue;
                }
                if (!describedMarkerDimensions) {
                    report.append("\nMarker source dimensions:\n");
                    describedMarkerDimensions = true;
                }
                report.append(markerSet.id).append(": ")
                        .append(markerSet.dimensionDescription).append(".\n");
            }
            if (!skippedMarkerSets.isEmpty() || !reconstructionWarnings.isEmpty()) {
                report.append("\nWarnings:\n");
                if (!skippedMarkerSets.isEmpty()) {
                    report.append("Marker source images were not supplied for: ")
                            .append(joinValues(skippedMarkerSets)).append(".\n");
                }
                for (String item : reconstructionWarnings) {
                    report.append(item).append("\n");
                }
            }

            JTextArea reportArea = new JTextArea(report.toString(),
                    Math.min(22, Math.max(8, parsed.crops.size() * 2 + 5)), 72);
            reportArea.setEditable(false);
            reportArea.setLineWrap(true);
            reportArea.setWrapStyleWord(true);
            reportArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN,
                    RECONSTRUCTION_WARNING_FONT_SIZE));
            reportArea.setCaretPosition(0);
            JScrollPane reportPane = new JScrollPane(reportArea);
            reportPane.setPreferredSize(new Dimension(780,
                    Math.min(560, Math.max(280, reportArea.getRows() * 25))));
            boolean hasWarnings = !skippedMarkerSets.isEmpty()
                    || !reconstructionWarnings.isEmpty();
            JOptionPane.showMessageDialog(reconstructed.frame, reportPane,
                    hasWarnings ? "Reconstruction Details and Warnings"
                            : "Reconstruction Details",
                    hasWarnings ? JOptionPane.WARNING_MESSAGE
                            : JOptionPane.INFORMATION_MESSAGE);
        }

        private static void showReadableMessageDialog(Component parent, String message,
                String title, int messageType) {
            JOptionPane.showMessageDialog(parent, readableMessagePane(message),
                    title, messageType);
        }

        private static int showReadableOptionDialog(Component parent, String message,
                String title, int messageType, Object[] options, Object initialValue) {
            return JOptionPane.showOptionDialog(parent, readableMessagePane(message), title,
                    JOptionPane.DEFAULT_OPTION, messageType, null, options, initialValue);
        }

        private static JScrollPane readableMessagePane(String message) {
            String text = message == null ? "" : message;
            int estimatedLines = 1;
            int lineLength = 0;
            for (int index = 0; index < text.length(); index++) {
                if (text.charAt(index) == '\n') {
                    estimatedLines += Math.max(1, (lineLength + 69) / 70);
                    lineLength = 0;
                } else {
                    lineLength++;
                }
            }
            estimatedLines += Math.max(1, (lineLength + 69) / 70);
            int rows = Math.min(18, Math.max(4, estimatedLines));
            JTextArea area = new JTextArea(text, rows, 70);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN,
                    RECONSTRUCTION_WARNING_FONT_SIZE));
            area.setCaretPosition(0);
            JScrollPane pane = new JScrollPane(area);
            pane.setPreferredSize(new Dimension(780,
                    Math.min(520, Math.max(170, rows * 27))));
            return pane;
        }

        private LinkedHashMap<String, ReconstructionImageRequest>
                buildReconstructionImageRequests(ParsedCoordinateLog parsed,
                        Set<String> referencedMarkerSetIds) {
            LinkedHashMap<String, ReconstructionImageRequest> requests =
                    new LinkedHashMap<String, ReconstructionImageRequest>();
            int bandNumber = 1;
            for (LoggedCrop crop : parsed.crops) {
                ReconstructionImageRequest request = getOrCreateReconstructionRequest(
                        requests, crop.sourcePath, crop.sourceWidth, crop.sourceHeight);
                request.required = true;
                request.addRole("Gel for Band " + bandNumber + " (" + crop.name + ")");
                bandNumber++;
            }
            for (LoggedMarkerSet markerSet : parsed.markerSets) {
                if (!referencedMarkerSetIds.contains(markerSet.id)) {
                    continue;
                }
                ReconstructionImageRequest request = getOrCreateReconstructionRequest(
                        requests, markerSet.sourcePath,
                        markerSet.sourceWidth, markerSet.sourceHeight);
                request.addRole("Marker source for " + markerSet.id);
            }
            return requests;
        }

        private static Set<String> referencedMarkerSetIds(ParsedCoordinateLog parsed) {
            Set<String> referenced = new LinkedHashSet<String>();
            for (LoggedCrop crop : parsed.crops) {
                if (crop.markerSetId != null) {
                    referenced.add(crop.markerSetId);
                }
            }
            return referenced;
        }

        private static ReconstructionImageRequest getOrCreateReconstructionRequest(
                LinkedHashMap<String, ReconstructionImageRequest> requests,
                String path, int width, int height) {
            String key = reconstructionRequestKey(path);
            ReconstructionImageRequest request = requests.get(key);
            if (request == null) {
                request = new ReconstructionImageRequest(path);
                requests.put(key, request);
            }
            request.addExpectedDimensions(width, height);
            return request;
        }

        private static void resolveMissingCropSourceDimensions(
                ParsedCoordinateLog parsed,
                Map<String, ReconstructionImageRequest> requests,
                List<String> reconstructionWarnings) {
            int bandNumber = 1;
            for (LoggedCrop crop : parsed.crops) {
                if (crop.sourceWidth > 0 && crop.sourceHeight > 0) {
                    bandNumber++;
                    continue;
                }
                ReconstructionImageRequest request = requests.get(
                        reconstructionRequestKey(crop.sourcePath));
                if (request == null || request.loadedImage == null) {
                    throw new IllegalStateException(
                            "No selected source image is available for Band " + bandNumber + ".");
                }
                if (request.hasExpectedDimensions() && !request.conflictingLoggedDimensions) {
                    crop.sourceWidth = request.expectedWidth;
                    crop.sourceHeight = request.expectedHeight;
                    crop.hasSourceDimensions = true;
                    crop.dimensionDescription = "source dimensions inferred from another log entry "
                            + "for the same image (" + crop.sourceWidth + " x "
                            + crop.sourceHeight + ")";
                } else {
                    crop.sourceWidth = request.loadedImage.imagePlus.getWidth();
                    crop.sourceHeight = request.loadedImage.imagePlus.getHeight();
                    crop.hasSourceDimensions = true;
                    crop.dimensionDescription = "source dimensions were absent; coordinates were "
                            + "assumed to refer directly to the selected " + crop.sourceWidth + " x "
                            + crop.sourceHeight + " image";
                    reconstructionWarnings.add("Band " + bandNumber
                            + " had no source dimensions; selected-image coordinates were used directly.");
                }
                bandNumber++;
            }
        }

        private static void resolveMissingMarkerSourceDimensions(
                ParsedCoordinateLog parsed,
                Map<String, ReconstructionImageRequest> requests,
                Set<String> referencedMarkerSetIds,
                List<String> reconstructionWarnings) {
            for (LoggedMarkerSet markerSet : parsed.markerSets) {
                if (!referencedMarkerSetIds.contains(markerSet.id)
                        || (markerSet.sourceWidth > 0 && markerSet.sourceHeight > 0)) {
                    continue;
                }
                ReconstructionImageRequest request = requests.get(
                        reconstructionRequestKey(markerSet.sourcePath));
                if (markerSet.sourceType == MarkerSourceType.GEL_IMAGE
                        && request != null && request.hasExpectedDimensions()
                        && !request.conflictingLoggedDimensions) {
                    markerSet.sourceWidth = request.expectedWidth;
                    markerSet.sourceHeight = request.expectedHeight;
                    markerSet.hasSourceDimensions = true;
                    markerSet.dimensionDescription = "source dimensions were absent; inferred "
                            + "from the linked Gel log entry ("
                            + markerSet.sourceWidth + " x " + markerSet.sourceHeight + ")";
                } else if (request != null && request.loadedImage != null) {
                    markerSet.sourceWidth = request.loadedImage.imagePlus.getWidth();
                    markerSet.sourceHeight = request.loadedImage.imagePlus.getHeight();
                    markerSet.hasSourceDimensions = true;
                    markerSet.dimensionDescription = "source dimensions were absent; inferred "
                            + "from the selected marker source image ("
                            + markerSet.sourceWidth + " x " + markerSet.sourceHeight + ")";
                } else if (request != null && request.hasExpectedDimensions()
                        && !request.conflictingLoggedDimensions) {
                    markerSet.sourceWidth = request.expectedWidth;
                    markerSet.sourceHeight = request.expectedHeight;
                    markerSet.hasSourceDimensions = true;
                    markerSet.dimensionDescription = "source dimensions were absent; inferred "
                            + "from another log entry for the same image ("
                            + markerSet.sourceWidth + " x " + markerSet.sourceHeight + ")";
                } else {
                    markerSet.dimensionDescription = "source dimensions remain unknown; marker "
                            + "coordinates were applied directly without scaling";
                }
            }
        }

        private boolean confirmMarkerDimensionCompatibility(ParsedCoordinateLog parsed,
                List<String> reconstructionWarnings) {
            List<String> issues = markerDimensionCompatibilityIssues(parsed);
            if (issues.isEmpty()) {
                return true;
            }
            StringBuilder message = new StringBuilder();
            message.append("IMPORTANT: molecular-weight coordinate compatibility needs review.\n\n");
            for (String issue : issues) {
                message.append("- ").append(issue).append("\n\n");
                if (!reconstructionWarnings.contains(issue)) {
                    reconstructionWarnings.add(issue);
                }
            }
            message.append("Continue reconstruction using the mapping described above?");
            Object[] options = {"Continue Reconstruction", "Cancel"};
            int choice = showReadableOptionDialog(frame, message.toString(),
                    "Important Marker Mapping Warning", JOptionPane.WARNING_MESSAGE,
                    options, options[1]);
            return choice == 0;
        }

        private static List<String> markerDimensionCompatibilityIssues(
                ParsedCoordinateLog parsed) {
            List<String> issues = new ArrayList<String>();
            Set<String> seen = new LinkedHashSet<String>();
            int bandNumber = 1;
            for (LoggedCrop crop : parsed.crops) {
                if (crop.markerSetId == null) {
                    bandNumber++;
                    continue;
                }
                LoggedMarkerSet markerSet = parsed.markerSetsById.get(crop.markerSetId);
                if (markerSet == null) {
                    bandNumber++;
                    continue;
                }
                String bandName = crop.name == null || crop.name.length() == 0
                        ? "Band " + bandNumber
                        : "Band " + bandNumber + " (" + crop.name + ")";
                String issue;
                String key;
                if (markerSet.sourceWidth <= 0 || markerSet.sourceHeight <= 0) {
                    key = markerSet.id + "|unknown";
                    issue = markerSet.id + " has no source dimensions and its source image was "
                            + "not supplied. Its coordinates will be used directly for linked "
                            + "Gel images; size and aspect-ratio compatibility cannot be verified.";
                } else if (markerSet.sourceWidth != crop.sourceWidth
                        || markerSet.sourceHeight != crop.sourceHeight) {
                    boolean ratioMismatch = ((long) markerSet.sourceWidth) * crop.sourceHeight
                            != ((long) crop.sourceWidth) * markerSet.sourceHeight;
                    key = markerSet.id + "|" + crop.sourcePath + "|"
                            + crop.sourceWidth + "x" + crop.sourceHeight;
                    issue = bandName + " uses " + markerSet.id + ", whose source is "
                            + markerSet.sourceWidth + " x " + markerSet.sourceHeight
                            + " pixels, while the Gel is " + crop.sourceWidth + " x "
                            + crop.sourceHeight + " pixels. "
                            + (ratioMismatch
                                    ? "The aspect ratios differ, so X and Y will be scaled "
                                            + "independently and the reconstruction may not be exact."
                                    : "The aspect ratios match; coordinates will be scaled proportionally.");
                } else {
                    bandNumber++;
                    continue;
                }
                if (seen.add(key)) {
                    issues.add(issue);
                }
                bandNumber++;
            }
            return issues;
        }

        private static void resolveLoggedCropMarkers(ParsedCoordinateLog parsed,
                List<String> reconstructionWarnings) {
            int bandNumber = 1;
            for (LoggedCrop crop : parsed.crops) {
                crop.resolvedMarkers.clear();
                if (crop.markerSetId == null) {
                    crop.markerDescription = "no marker set was used";
                    bandNumber++;
                    continue;
                }
                LoggedMarkerSet markerSet = parsed.markerSetsById.get(crop.markerSetId);
                if (markerSet == null) {
                    throw new IllegalArgumentException("Band " + bandNumber
                            + " refers to unknown marker set " + crop.markerSetId + ".");
                }
                if (crop.markers.isEmpty()) {
                    for (LoggedMarker marker : markerSet.markers) {
                        LoggedCropMarker resolved = resolveMarkerCoordinates(
                                markerSet, crop, marker);
                        if (markerFallsWithinCrop(resolved, crop)) {
                            crop.resolvedMarkers.add(resolved);
                        }
                    }
                    crop.markerDescription = "no per-crop marker list was supplied; all "
                            + markerSet.markers.size() + " marker(s) from " + markerSet.id
                            + " were evaluated and " + crop.resolvedMarkers.size()
                            + " fell within the crop";
                } else {
                    Set<LoggedMarker> selectedMarkers = new LinkedHashSet<LoggedMarker>();
                    for (LoggedCropMarker entry : crop.markers) {
                        LoggedMarker selected = selectLoggedMarker(
                                markerSet, crop, entry, bandNumber);
                        if (!selectedMarkers.add(selected)) {
                            continue;
                        }
                        LoggedCropMarker resolved = resolveMarkerCoordinates(
                                markerSet, crop, selected);
                        addMarkerCoordinateWarning(entry, resolved, bandNumber,
                                reconstructionWarnings);
                        if (markerFallsWithinCrop(resolved, crop)) {
                            crop.resolvedMarkers.add(resolved);
                        } else {
                            reconstructionWarnings.add("Band " + bandNumber + " marker "
                                    + entry.label + " was requested but falls outside the crop; "
                                    + "it was not plotted.");
                        }
                    }
                    crop.markerDescription = crop.markers.size()
                            + " per-crop marker label(s) selected from " + markerSet.id
                            + "; " + crop.resolvedMarkers.size() + " fell within the crop";
                }
                Collections.sort(crop.resolvedMarkers,
                        new Comparator<LoggedCropMarker>() {
                            @Override
                            public int compare(LoggedCropMarker first,
                                    LoggedCropMarker second) {
                                return Double.compare(first.yInCrop.doubleValue(),
                                        second.yInCrop.doubleValue());
                            }
                        });
                bandNumber++;
            }
        }

        private static LoggedMarker selectLoggedMarker(LoggedMarkerSet markerSet,
                LoggedCrop crop, LoggedCropMarker entry, int bandNumber) {
            List<LoggedMarker> candidates = new ArrayList<LoggedMarker>();
            for (LoggedMarker marker : markerSet.markers) {
                if (marker.label.equals(entry.label)) {
                    candidates.add(marker);
                }
            }
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("Band " + bandNumber + " requests marker label "
                        + entry.label + ", but " + markerSet.id + " has no marker with that label.");
            }
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
            if (!entry.hasAnyCoordinates()) {
                throw new IllegalArgumentException("Band " + bandNumber + " requests marker label "
                        + entry.label + ", but " + markerSet.id + " contains that label "
                        + candidates.size() + " times. Add coordinates to identify the intended marker.");
            }

            LoggedMarker best = null;
            double bestScore = Double.POSITIVE_INFINITY;
            boolean tied = false;
            for (LoggedMarker candidate : candidates) {
                LoggedCropMarker resolved = resolveMarkerCoordinates(markerSet, crop, candidate);
                double score = markerCoordinateDiscrepancy(entry, resolved);
                if (score + 1.0e-9 < bestScore) {
                    best = candidate;
                    bestScore = score;
                    tied = false;
                } else if (Math.abs(score - bestScore) <= 1.0e-9) {
                    tied = true;
                }
            }
            if (best == null || tied) {
                throw new IllegalArgumentException("Band " + bandNumber + " marker label "
                        + entry.label + " remains ambiguous in " + markerSet.id
                        + "; the supplied coordinates do not identify one marker uniquely.");
            }
            return best;
        }

        private static LoggedCropMarker resolveMarkerCoordinates(LoggedMarkerSet markerSet,
                LoggedCrop crop, LoggedMarker marker) {
            double scaleX = markerSet.sourceWidth > 0
                    ? crop.sourceWidth / (double) markerSet.sourceWidth : 1.0;
            double scaleY = markerSet.sourceHeight > 0
                    ? crop.sourceHeight / (double) markerSet.sourceHeight : 1.0;
            double gelX = marker.xAbs * scaleX;
            double gelY = marker.yAbs * scaleY;
            double yInCrop = markerYInCrop(
                    gelX, gelY, crop.cropX, crop.cropY, crop.cropAngleDeg);
            return LoggedCropMarker.resolved(marker.label, marker.xAbs, marker.yAbs,
                    gelX, gelY, yInCrop);
        }

        private static boolean markerFallsWithinCrop(LoggedCropMarker marker,
                LoggedCrop crop) {
            double localY = marker.yInCrop.doubleValue();
            return localY >= -0.5 && localY <= crop.cropHeight + 0.5;
        }

        private static double markerCoordinateDiscrepancy(LoggedCropMarker supplied,
                LoggedCropMarker resolved) {
            double discrepancy = 0.0;
            discrepancy = maximumSuppliedDifference(
                    discrepancy, supplied.sourceXAbs, resolved.sourceXAbs);
            discrepancy = maximumSuppliedDifference(
                    discrepancy, supplied.sourceYAbs, resolved.sourceYAbs);
            discrepancy = maximumSuppliedDifference(
                    discrepancy, supplied.gelXAbs, resolved.gelXAbs);
            discrepancy = maximumSuppliedDifference(
                    discrepancy, supplied.gelYAbs, resolved.gelYAbs);
            return maximumSuppliedDifference(
                    discrepancy, supplied.yInCrop, resolved.yInCrop);
        }

        private static double maximumSuppliedDifference(double current,
                Double supplied, Double resolved) {
            if (supplied == null || resolved == null) {
                return current;
            }
            return Math.max(current,
                    Math.abs(supplied.doubleValue() - resolved.doubleValue()));
        }

        private static void addMarkerCoordinateWarning(LoggedCropMarker supplied,
                LoggedCropMarker resolved, int bandNumber,
                List<String> reconstructionWarnings) {
            double discrepancy = markerCoordinateDiscrepancy(supplied, resolved);
            if (discrepancy <= MARKER_COORDINATE_AGREEMENT_TOLERANCE) {
                return;
            }
            reconstructionWarnings.add("Band " + bandNumber + " marker " + supplied.label
                    + " has detailed coordinates that differ from values recalculated from its "
                    + "global marker set by up to " + formatDisplayCoordinate(discrepancy)
                    + " pixels. Recalculated coordinates were used.");
        }

        private boolean selectReconstructionImage(ReconstructionImageRequest request) {
            while (true) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(request.required
                        ? "Select Required Gel Image" : "Select Marker Source Image");
                chooser.setApproveButtonText("Use Image");
                chooser.setFileFilter(new FileNameExtensionFilter(
                        "Image files (TIFF, PNG, JPEG)", "tif", "tiff", "png", "jpg", "jpeg"));
                if (lastDir != null) {
                    chooser.setCurrentDirectory(lastDir);
                }

                JTextArea details = new JTextArea(reconstructionImageDetails(request), 9, 34);
                details.setEditable(false);
                details.setOpaque(false);
                details.setLineWrap(true);
                details.setWrapStyleWord(true);
                details.setFont(new Font(Font.SANS_SERIF, Font.PLAIN,
                        RECONSTRUCTION_ACCESSORY_FONT_SIZE));
                chooser.setAccessory(details);

                if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
                    if (request.required) {
                        return false;
                    }
                    Object[] options = {"Skip Marker Source", "Choose Again", "Cancel Reconstruction"};
                    int choice = JOptionPane.showOptionDialog(frame,
                            "No image was selected for this optional marker source.",
                            "Marker Source Image", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    if (choice == 0) {
                        return true;
                    }
                    if (choice == 1) {
                        continue;
                    }
                    return false;
                }

                File selected = chooser.getSelectedFile();
                LoadedImage loaded = loadRgbImage(selected);
                if (loaded == null) {
                    continue;
                }
                lastDir = selected.getParentFile();
                if (lastDir != null) {
                    Prefs.set(LAST_DIR_PREFERENCE, lastDir.getAbsolutePath());
                }

                if (request.hasExpectedDimensions()
                        && (loaded.imagePlus.getWidth() != request.expectedWidth
                        || loaded.imagePlus.getHeight() != request.expectedHeight)) {
                    boolean ratioMismatch = ((long) loaded.imagePlus.getWidth())
                            * request.expectedHeight
                            != ((long) request.expectedWidth) * loaded.imagePlus.getHeight();
                    StringBuilder warning = new StringBuilder();
                    warning.append("The log expects ")
                            .append(request.expectedWidth).append(" x ")
                            .append(request.expectedHeight).append(" pixels, but the selected image is ")
                            .append(loaded.imagePlus.getWidth()).append(" x ")
                            .append(loaded.imagePlus.getHeight()).append(" pixels.\n\n");
                    warning.append(ratioMismatch
                            ? "The aspect ratios differ, so this will not be an exact reconstruction."
                            : "The aspect ratios match and coordinates can be scaled proportionally.");
                    Object[] options = {"Use Image Anyway", "Choose Another Image"};
                    int choice = showReadableOptionDialog(frame, warning.toString()
                                    + "\n\nUse this image anyway?",
                            "Reconstruction Image Size Warning",
                            JOptionPane.WARNING_MESSAGE, options, options[1]);
                    if (choice != 0) {
                        continue;
                    }
                    request.dimensionMismatch = true;
                    request.aspectRatioMismatch = ratioMismatch;
                }
                request.loadedImage = loaded;
                return true;
            }
        }

        private LoadedImage loadRgbImage(File file) {
            ImagePlus imp = IJ.openImage(file.getAbsolutePath());
            if (imp == null) {
                JOptionPane.showMessageDialog(frame, "Could not open: " + file.getAbsolutePath(),
                        "Open Reconstruction Image", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            if (imp.getType() != ImagePlus.COLOR_RGB) {
                new ImageConverter(imp).convertToRGB();
            }
            String path;
            try {
                path = file.getCanonicalPath();
            } catch (Exception ignored) {
                path = file.getAbsolutePath();
            }
            return new LoadedImage(imp, path);
        }

        private static String reconstructionImageDetails(ReconstructionImageRequest request) {
            StringBuilder details = new StringBuilder();
            details.append(request.required ? "Required Gel image" : "Optional marker source")
                    .append("\n\nExpected path:\n").append(request.originalPath)
                    .append("\n\nLogged dimensions: ");
            if (request.hasExpectedDimensions()) {
                details.append(request.expectedWidth).append(" x ")
                        .append(request.expectedHeight).append(" pixels");
            } else {
                details.append("not supplied; coordinates will be used directly");
            }
            details.append("\n\nUsed as:\n");
            for (String role : request.roles) {
                details.append("- ").append(role).append("\n");
            }
            if (request.conflictingLoggedDimensions) {
                details.append("\nWarning: this path has conflicting dimensions in the log.");
            }
            return details.toString();
        }

        private static ReconstructedGeometry scaleLoggedCropGeometry(
                LoggedCrop crop, ImagePlus selectedImage) {
            double scaleX = selectedImage.getWidth() / (double) crop.sourceWidth;
            double scaleY = selectedImage.getHeight() / (double) crop.sourceHeight;
            double angle = Math.toRadians(crop.cropAngleDeg);

            Point2D topLeft = new Point2D(crop.cropX * scaleX, crop.cropY * scaleY);
            Point2D topRight = new Point2D(
                    (crop.cropX + crop.cropWidth * Math.cos(angle)) * scaleX,
                    (crop.cropY + crop.cropWidth * Math.sin(angle)) * scaleY);
            Point2D bottomLeft = new Point2D(
                    (crop.cropX - crop.cropHeight * Math.sin(angle)) * scaleX,
                    (crop.cropY + crop.cropHeight * Math.cos(angle)) * scaleY);

            int width = Math.max(2, (int) Math.round(distance(
                    topLeft.x, topLeft.y, topRight.x, topRight.y)));
            int height = Math.max(2, (int) Math.round(distance(
                    topLeft.x, topLeft.y, bottomLeft.x, bottomLeft.y)));
            double scaledAngle = Math.toDegrees(Math.atan2(
                    topRight.y - topLeft.y, topRight.x - topLeft.x));
            return new ReconstructedGeometry(
                    topLeft.x, topLeft.y, width, height, scaledAngle);
        }

        private static MarkerMapping markerMappingForLoggedCrop(
                KdaMarkerSet markerSet, LoggedCrop crop) {
            if (markerSet == null || markerSet.sourceWidth <= 0
                    || markerSet.sourceHeight <= 0) {
                return null;
            }
            double scaleX = crop.sourceWidth / (double) markerSet.sourceWidth;
            double scaleY = crop.sourceHeight / (double) markerSet.sourceHeight;
            boolean dimensionsDiffer = markerSet.sourceWidth != crop.sourceWidth
                    || markerSet.sourceHeight != crop.sourceHeight;
            boolean ratioMismatch = ((long) markerSet.sourceWidth) * crop.sourceHeight
                    != ((long) crop.sourceWidth) * markerSet.sourceHeight;
            return new MarkerMapping(markerSet.sourceWidth, markerSet.sourceHeight,
                    crop.sourceWidth, crop.sourceHeight, scaleX, scaleY,
                    dimensionsDiffer, ratioMismatch);
        }

        private int openAnnotatedCropSourceImages(ParsedCoordinateLog parsed,
                Map<String, ReconstructionImageRequest> requests, int startingIndex) {
            LinkedHashMap<String, LoadedImage> sourceImages =
                    new LinkedHashMap<String, LoadedImage>();
            LinkedHashMap<String, List<ReconstructionCropPreview>> cropsBySource =
                    new LinkedHashMap<String, List<ReconstructionCropPreview>>();

            int bandNumber = 1;
            for (LoggedCrop crop : parsed.crops) {
                ReconstructionImageRequest request = requests.get(
                        reconstructionRequestKey(crop.sourcePath));
                if (request == null || request.loadedImage == null) {
                    bandNumber++;
                    continue;
                }
                String selectedPathKey = reconstructionRequestKey(request.loadedImage.path);
                if (!sourceImages.containsKey(selectedPathKey)) {
                    sourceImages.put(selectedPathKey, request.loadedImage);
                    cropsBySource.put(selectedPathKey,
                            new ArrayList<ReconstructionCropPreview>());
                }
                cropsBySource.get(selectedPathKey).add(
                        new ReconstructionCropPreview(crop, bandNumber));
                bandNumber++;
            }

            int windowIndex = startingIndex;
            for (Map.Entry<String, List<ReconstructionCropPreview>> entry
                    : cropsBySource.entrySet()) {
                LoadedImage source = sourceImages.get(entry.getKey());
                ImagePlus annotatedImage = new ImagePlus(
                        "Crops from coordinate log - " + new File(source.path).getName(),
                        source.imagePlus.getProcessor().duplicate());
                AnnotatedCropSourceImage annotated = new AnnotatedCropSourceImage(
                        annotatedImage, entry.getValue());
                annotatedCropSourceImages.add(annotated);
                annotatedImage.show();
                placeAnnotatedSourceWindow(annotatedImage, windowIndex++);
                drawAnnotatedCropSourceOverlay(annotated);
                installAnnotatedExportMenu(annotatedImage);
            }
            return windowIndex;
        }

        private void drawAnnotatedCropSourceOverlay(AnnotatedCropSourceImage annotated) {
            if (annotated == null || annotated.imagePlus == null) {
                return;
            }
            double overlayScale = sourceOverlayScale(annotated.imagePlus.getWidth());
            double markerRadius = SOURCE_MARKER_R * overlayScale;
            Overlay overlay = new Overlay();
            LinkedHashMap<String, ReconstructionMarkerPreview> markers =
                    new LinkedHashMap<String, ReconstructionMarkerPreview>();

            for (ReconstructionCropPreview preview : annotated.crops) {
                LoggedCrop crop = preview.crop;
                ReconstructedGeometry geometry = scaleLoggedCropGeometry(
                        crop, annotated.imagePlus);
                Color cropColor = reconstructionCropColor(preview.bandNumber);
                double angle = Math.toRadians(geometry.angleDeg);
                double sin = Math.sin(angle);
                double cos = Math.cos(angle);
                double localScaleY = geometry.height / (double) crop.cropHeight;
                double markerScaleX = annotated.imagePlus.getWidth()
                        / (double) crop.sourceWidth;
                double markerScaleY = annotated.imagePlus.getHeight()
                        / (double) crop.sourceHeight;

                for (LoggedCropMarker marker : crop.resolvedMarkers) {
                    double markerX = marker.gelXAbs.doubleValue() * markerScaleX;
                    double markerY = marker.gelYAbs.doubleValue() * markerScaleY;
                    double localY = marker.yInCrop.doubleValue() * localScaleY;
                    double tickX = geometry.x - sin * localY;
                    double tickY = geometry.y + cos * localY;
                    Line connector = new Line(markerX, markerY, tickX, tickY);
                    connector.setStrokeColor(cropColor);
                    connector.setStrokeWidth(RECONSTRUCTION_CONNECTOR_STROKE_WIDTH * overlayScale);
                    overlay.add(connector);

                    String markerKey = crop.markerSetId + "|" + marker.label + "|"
                            + formatCoordinate(markerX) + "|" + formatCoordinate(markerY);
                    ReconstructionMarkerPreview markerPreview = markers.get(markerKey);
                    if (markerPreview == null) {
                        markerPreview = new ReconstructionMarkerPreview(
                                marker.label, markerX, markerY);
                        markers.put(markerKey, markerPreview);
                    }
                    markerPreview.recordConnectorEndpoint(tickX);
                }
            }

            LinkedHashMap<String, TextRoi> markerLabels =
                    new LinkedHashMap<String, TextRoi>();
            List<Rectangle> occupiedLabelBounds = new ArrayList<Rectangle>();
            if (showSourceKdaLabels) {
                for (Map.Entry<String, ReconstructionMarkerPreview> entry
                        : markers.entrySet()) {
                    ReconstructionMarkerPreview marker = entry.getValue();
                    TextRoi label = createReconstructionMarkerLabel(marker,
                            annotated.imagePlus.getWidth(), annotated.imagePlus.getHeight());
                    label.setStrokeColor(Color.RED);
                    label.setFillColor(new Color(255, 255, 255, 170));
                    markerLabels.put(entry.getKey(), label);
                    occupiedLabelBounds.add(paddedBounds(label.getBounds(),
                            Math.max(1, (int) Math.round(4 * overlayScale))));
                }
            }

            addReconstructionCropOutlines(annotated.imagePlus, annotated.crops,
                    overlay, occupiedLabelBounds);

            for (Map.Entry<String, ReconstructionMarkerPreview> entry : markers.entrySet()) {
                ReconstructionMarkerPreview marker = entry.getValue();
                Line diagA = new Line(marker.x - markerRadius, marker.y - markerRadius,
                        marker.x + markerRadius, marker.y + markerRadius);
                diagA.setStrokeColor(Color.RED);
                diagA.setStrokeWidth(SOURCE_MARKER_STROKE_WIDTH * overlayScale);
                overlay.add(diagA);
                Line diagB = new Line(marker.x - markerRadius, marker.y + markerRadius,
                        marker.x + markerRadius, marker.y - markerRadius);
                diagB.setStrokeColor(Color.RED);
                diagB.setStrokeWidth(SOURCE_MARKER_STROKE_WIDTH * overlayScale);
                overlay.add(diagB);
                if (showSourceKdaLabels) {
                    overlay.add(markerLabels.get(entry.getKey()));
                }
            }

            annotated.imagePlus.setOverlay(overlay);
            annotated.imagePlus.updateAndDraw();
        }

        private static void addReconstructionCropOutlines(ImagePlus image,
                List<ReconstructionCropPreview> crops, Overlay overlay,
                List<Rectangle> occupiedLabelBounds) {
            double overlayScale = sourceOverlayScale(image.getWidth());
            for (ReconstructionCropPreview preview : crops) {
                ReconstructedGeometry geometry = scaleLoggedCropGeometry(
                        preview.crop, image);
                Point2D[] corners = reconstructedCropCorners(geometry);
                Color cropColor = reconstructionCropColor(preview.bandNumber);
                for (int corner = 0; corner < corners.length; corner++) {
                    Point2D start = corners[corner];
                    Point2D end = corners[(corner + 1) % corners.length];
                    Line edge = new Line(start.x, start.y, end.x, end.y);
                    edge.setStrokeColor(cropColor);
                    edge.setStrokeWidth(CROP_STROKE_WIDTH * overlayScale);
                    overlay.add(edge);
                }

                double minimumX = corners[0].x;
                double minimumY = corners[0].y;
                double maximumX = corners[0].x;
                double maximumY = corners[0].y;
                for (int corner = 1; corner < corners.length; corner++) {
                    minimumX = Math.min(minimumX, corners[corner].x);
                    minimumY = Math.min(minimumY, corners[corner].y);
                    maximumX = Math.max(maximumX, corners[corner].x);
                    maximumY = Math.max(maximumY, corners[corner].y);
                }
                String cropName = preview.crop.name == null ? "" : preview.crop.name;
                String labelText = "Band " + preview.bandNumber
                        + (cropName.length() == 0 ? "" : ": " + cropName);
                TextRoi label = createReconstructionCropLabel(labelText, cropColor,
                        minimumX, minimumY, maximumX, maximumY,
                        image.getWidth(), image.getHeight(),
                        occupiedLabelBounds);
                overlay.add(label);
            }

        }

        private void drawAnnotatedMarkerOverlay(AnnotatedMarkerImage annotated) {
            drawKdaOverlay(annotated.imagePlus, annotated.markerSet,
                    annotated.scaleX, annotated.scaleY);
            Overlay overlay = annotated.imagePlus.getOverlay();
            if (overlay == null) {
                overlay = new Overlay();
            }
            List<Rectangle> occupiedLabelBounds = new ArrayList<Rectangle>();
            double overlayScale = sourceOverlayScale(annotated.imagePlus.getWidth());
            for (Roi roi : overlay.toArray()) {
                if (roi instanceof TextRoi) {
                    occupiedLabelBounds.add(paddedBounds(roi.getBounds(),
                            Math.max(1, (int) Math.round(4 * overlayScale))));
                }
            }
            addReconstructionCropOutlines(annotated.imagePlus, annotated.crops,
                    overlay, occupiedLabelBounds);
            annotated.imagePlus.setOverlay(overlay);
            annotated.imagePlus.updateAndDraw();
        }

        private static TextRoi createReconstructionCropLabel(String text, Color color,
                double minimumX, double minimumY, double maximumX, double maximumY,
                int imageWidth, int imageHeight, List<Rectangle> occupiedBounds) {
            double overlayScale = sourceOverlayScale(imageWidth);
            Font cropFont = sourceOverlayFont(FONT_RECONSTRUCTION_CROP, overlayScale);
            double step = cropFont.getSize2D() + 8.0 * overlayScale;
            for (int distance = 0; distance <= 12; distance++) {
                int[] directions = distance == 0 ? new int[] {0} : new int[] {-1, 1};
                for (int direction : directions) {
                    TextRoi candidate = acceptableCropLabel(text,
                            maximumX + 8.0 * overlayScale, minimumY + direction * distance * step,
                            imageWidth, imageHeight, occupiedBounds);
                    if (candidate != null) {
                        styleReconstructionCropLabel(candidate, color, occupiedBounds, overlayScale);
                        return candidate;
                    }
                }
            }
            for (int distance = 0; distance <= 12; distance++) {
                TextRoi candidate = acceptableCropLabel(text, minimumX + 8.0 * overlayScale,
                        minimumY - cropFont.getSize2D() - 6.0 * overlayScale
                                - distance * step,
                        imageWidth, imageHeight, occupiedBounds);
                if (candidate != null) {
                    styleReconstructionCropLabel(candidate, color, occupiedBounds, overlayScale);
                    return candidate;
                }
            }
            for (int distance = 0; distance <= 12; distance++) {
                TextRoi candidate = acceptableCropLabel(text, minimumX + 8.0 * overlayScale,
                        maximumY + 6.0 * overlayScale + distance * step,
                        imageWidth, imageHeight, occupiedBounds);
                if (candidate != null) {
                    styleReconstructionCropLabel(candidate, color, occupiedBounds, overlayScale);
                    return candidate;
                }
            }

            TextRoi fallback = new TextRoi(
                    Math.max(0.0, Math.min(minimumX + 8.0 * overlayScale, imageWidth - 1.0)),
                    Math.max(0.0, Math.min(minimumY, imageHeight - 1.0)),
                    text, cropFont);
            styleReconstructionCropLabel(fallback, color, occupiedBounds, overlayScale);
            return fallback;
        }

        private static TextRoi createReconstructionMarkerLabel(
                ReconstructionMarkerPreview marker, int imageWidth, int imageHeight) {
            double overlayScale = sourceOverlayScale(imageWidth);
            Font markerFont = sourceOverlayFont(FONT_RECONSTRUCTION_MARKER, overlayScale);
            final double gap = 14.0 * overlayScale;
            TextRoi sizeProbe = new TextRoi(0.0, 0.0,
                    marker.label, markerFont);
            Rectangle probeBounds = sizeProbe.getBounds();
            double leftX = marker.x - gap - probeBounds.width;
            double rightX = marker.x + gap;
            boolean placeLeft = marker.prefersLabelOnLeft();
            double labelX = placeLeft ? leftX : rightX;
            double alternateX = placeLeft ? rightX : leftX;
            if (!fitsHorizontally(labelX, probeBounds.width, imageWidth)
                    && fitsHorizontally(alternateX, probeBounds.width, imageWidth)) {
                labelX = alternateX;
            } else {
                labelX = Math.max(0.0,
                        Math.min(labelX, Math.max(0.0, imageWidth - probeBounds.width)));
            }
            double labelY = Math.max(0.0, Math.min(marker.y - 36.0 * overlayScale,
                    Math.max(0.0, imageHeight - probeBounds.height)));
            return new TextRoi(labelX, labelY,
                    marker.label, markerFont);
        }

        private static boolean fitsHorizontally(double x, int width, int imageWidth) {
            return x >= 0.0 && x + width <= imageWidth;
        }

        private static TextRoi acceptableCropLabel(String text, double x, double y,
                int imageWidth, int imageHeight, List<Rectangle> occupiedBounds) {
            if (x < 0.0 || y < 0.0) {
                return null;
            }
            double overlayScale = sourceOverlayScale(imageWidth);
            TextRoi candidate = new TextRoi(x, y, text,
                    sourceOverlayFont(FONT_RECONSTRUCTION_CROP, overlayScale));
            Rectangle bounds = candidate.getBounds();
            if (bounds.x < 0 || bounds.y < 0
                    || bounds.x + bounds.width > imageWidth
                    || bounds.y + bounds.height > imageHeight) {
                return null;
            }
            Rectangle padded = paddedBounds(bounds, Math.max(1, (int) Math.round(4 * overlayScale)));
            for (Rectangle occupied : occupiedBounds) {
                if (padded.intersects(occupied)) {
                    return null;
                }
            }
            return candidate;
        }

        private static void styleReconstructionCropLabel(TextRoi label, Color color,
                List<Rectangle> occupiedBounds, double overlayScale) {
            label.setStrokeColor(color);
            label.setFillColor(new Color(255, 255, 255, 190));
            occupiedBounds.add(paddedBounds(label.getBounds(),
                    Math.max(1, (int) Math.round(4 * overlayScale))));
        }

        private static Rectangle paddedBounds(Rectangle bounds, int padding) {
            return new Rectangle(bounds.x - padding, bounds.y - padding,
                    bounds.width + padding * 2, bounds.height + padding * 2);
        }

        private static Color reconstructionCropColor(int bandNumber) {
            int index = Math.max(0, bandNumber - 1) % RECONSTRUCTION_CROP_COLORS.length;
            return RECONSTRUCTION_CROP_COLORS[index];
        }

        private static Point2D[] reconstructedCropCorners(ReconstructedGeometry geometry) {
            double angle = Math.toRadians(geometry.angleDeg);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Point2D topLeft = new Point2D(geometry.x, geometry.y);
            Point2D topRight = new Point2D(
                    geometry.x + geometry.width * cos,
                    geometry.y + geometry.width * sin);
            Point2D bottomLeft = new Point2D(
                    geometry.x - geometry.height * sin,
                    geometry.y + geometry.height * cos);
            Point2D bottomRight = new Point2D(
                    topRight.x + bottomLeft.x - topLeft.x,
                    topRight.y + bottomLeft.y - topLeft.y);
            return new Point2D[] {topLeft, topRight, bottomRight, bottomLeft};
        }

        private static double sourceOverlayScale(int imageWidth) {
            return imageWidth / SOURCE_OVERLAY_REFERENCE_WIDTH;
        }

        private static Font sourceOverlayFont(Font base, double scale) {
            return base.deriveFont((float) Math.max(1.0, base.getSize2D() * scale));
        }

        private static void placeAnnotatedSourceWindow(ImagePlus image, int index) {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            if (image.getWindow() != null) {
                int offset = (index % 8) * 24;
                image.getWindow().setLocation(screen.width / 2 + offset, offset);
                image.getWindow().setSize(
                        Math.max(320, screen.width / 2 - offset),
                        Math.max(280, screen.height - offset));
            }
        }

        private void installAnnotatedExportMenu(final ImagePlus image) {
            if (image == null || image.getWindow() == null) {
                return;
            }
            MenuBar menuBar = image.getWindow().getMenuBar();
            if (menuBar == null) {
                menuBar = new MenuBar();
            }
            Menu exportMenu = new Menu("Export");
            MenuItem pngItem = new MenuItem("Annotated image as PNG...");
            pngItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    exportAnnotatedPng(image);
                }
            });
            exportMenu.add(pngItem);

            MenuItem pdfItem = new MenuItem("Annotated image as PDF...");
            pdfItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    exportAnnotatedPdf(image);
                }
            });
            exportMenu.add(pdfItem);
            menuBar.add(exportMenu);
            image.getWindow().setMenuBar(menuBar);
            image.getWindow().validate();
        }

        private void exportAnnotatedPng(ImagePlus image) {
            File path = chooseSavePath(image.getWindow(), "Export Annotated PNG",
                    "PNG image", "png", annotatedExportBaseName(image));
            if (path == null) {
                return;
            }
            try {
                writeAnnotatedPng(image, path);
                setStatus("Annotated PNG saved: " + path.getAbsolutePath());
                JOptionPane.showMessageDialog(image.getWindow(),
                        "PNG saved: " + path.getAbsolutePath(),
                        "Export Annotated PNG", JOptionPane.INFORMATION_MESSAGE);
            } catch (Throwable ex) {
                JOptionPane.showMessageDialog(image.getWindow(),
                        "PNG export failed.\n" + ex.getClass().getSimpleName()
                                + ": " + ex.getMessage(),
                        "Export Annotated PNG", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void exportAnnotatedPdf(ImagePlus image) {
            JTextField dpiField = new JTextField("300", 6);
            JPanel dpiRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            dpiRow.setAlignmentX(0.0f);
            dpiRow.add(new JLabel("Source image resolution (DPI): "));
            dpiRow.add(dpiField);

            JLabel exportInfo = new JLabel(
                    "The source pixels remain native; overlay lines and text remain vector.");
            exportInfo.setAlignmentX(0.0f);
            JPanel options = new JPanel();
            options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
            options.add(dpiRow);
            options.add(Box.createVerticalStrut(5));
            options.add(exportInfo);

            int option = JOptionPane.showConfirmDialog(image.getWindow(), options,
                    "Export Annotated PDF", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return;
            }
            int dpi;
            try {
                dpi = Integer.parseInt(dpiField.getText().trim());
            } catch (NumberFormatException ex) {
                dpi = 300;
            }
            dpi = Math.max(72, Math.min(1200, dpi));

            File path = chooseSavePath(image.getWindow(), "Export Annotated PDF",
                    "PDF", "pdf", annotatedExportBaseName(image));
            if (path == null) {
                return;
            }
            try {
                writeAnnotatedPdf(image, path, dpi);
                setStatus("Annotated PDF saved: " + path.getAbsolutePath());
                JOptionPane.showMessageDialog(image.getWindow(),
                        "PDF saved: " + path.getAbsolutePath(),
                        "Export Annotated PDF", JOptionPane.INFORMATION_MESSAGE);
            } catch (Throwable ex) {
                JOptionPane.showMessageDialog(image.getWindow(),
                        "PDF export failed. Make sure the iText module/JAR is available in Fiji.\n"
                                + ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                        "Export Annotated PDF", JOptionPane.ERROR_MESSAGE);
            }
        }

        private static void writeAnnotatedPng(ImagePlus image, File path) throws Exception {
            ImagePlus flattened = image.flatten();
            if (flattened == null || flattened.getBufferedImage() == null
                    || !ImageIO.write(flattened.getBufferedImage(), "png", path)) {
                throw new IllegalStateException("No PNG writer is available.");
            }
        }

        private static void writeAnnotatedPdf(ImagePlus image, File path, int dpi)
                throws Exception {
            float pointsPerPixel = 72.0f / dpi;
            float pageWidth = image.getWidth() * pointsPerPixel;
            float pageHeight = image.getHeight() * pointsPerPixel;
            com.itextpdf.text.Rectangle pageSize =
                    new com.itextpdf.text.Rectangle(pageWidth, pageHeight);

            FileOutputStream output = new FileOutputStream(path);
            Document document = new Document(pageSize, 0, 0, 0, 0);
            try {
                PdfWriter writer = PdfWriter.getInstance(document, output);
                writer.setPdfVersion(PdfWriter.VERSION_1_5);
                document.open();
                PdfContentByte page = writer.getDirectContent();

                Image raster = Image.getInstance(image.getBufferedImage(), null);
                raster.setInterpolation(true);
                raster.scaleAbsolute(pageWidth, pageHeight);
                raster.setAbsolutePosition(0.0f, 0.0f);
                page.addImage(raster);

                new AnnotatedImagePdfRenderer(image.getOverlay(), page,
                        pageHeight, pointsPerPixel).render();
            } finally {
                try {
                    if (document.isOpen()) {
                        document.close();
                    }
                } finally {
                    output.close();
                }
            }
        }

        private static String annotatedExportBaseName(ImagePlus image) {
            String title = image == null || image.getTitle() == null
                    ? "annotated-source-image" : image.getTitle().trim();
            String lower = title.toLowerCase(Locale.US);
            String[] imageExtensions = {".tiff", ".jpeg", ".tif", ".png", ".jpg"};
            for (String extension : imageExtensions) {
                if (lower.endsWith(extension)) {
                    title = title.substring(0, title.length() - extension.length());
                    break;
                }
            }
            title = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            return (title.length() == 0 ? "annotated-source-image" : title) + " - annotated";
        }

        private void openAnnotatedMarkerImage(KdaMarkerSet markerSet,
                LoadedImage loadedImage, int index, List<LoggedCrop> loggedCrops) {
            if (markerSet == null || loadedImage == null) {
                return;
            }
            ImagePlus annotatedImage = new ImagePlus(
                    markerSet.id + " - marker source - " + new File(loadedImage.path).getName(),
                    loadedImage.imagePlus.getProcessor().duplicate());
            double scaleX = annotatedImage.getWidth() / (double) markerSet.sourceWidth;
            double scaleY = annotatedImage.getHeight() / (double) markerSet.sourceHeight;
            AnnotatedMarkerImage annotated = new AnnotatedMarkerImage(
                    annotatedImage, markerSet, scaleX, scaleY, loggedCrops);
            annotatedMarkerImages.add(annotated);
            annotatedImage.show();
            placeAnnotatedSourceWindow(annotatedImage, index);
            drawAnnotatedMarkerOverlay(annotated);
            installAnnotatedExportMenu(annotatedImage);
        }

        private static String reconstructionRequestKey(String path) {
            if (path == null) {
                return "";
            }
            return File.separatorChar == '\\' ? path.toLowerCase(Locale.US) : path;
        }

        private static String joinValues(List<String> values) {
            StringBuilder joined = new StringBuilder();
            for (String value : values) {
                if (joined.length() > 0) {
                    joined.append(", ");
                }
                joined.append(value);
            }
            return joined.toString();
        }

        private static int markerSetNumber(String id) {
            if (id == null) {
                return 0;
            }
            int dash = id.lastIndexOf('-');
            if (dash < 0 || dash == id.length() - 1) {
                return 0;
            }
            try {
                return Integer.parseInt(id.substring(dash + 1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        private void saveCoordinateLog(String text) {
            File path = chooseSavePath("Save Coordinate Log", "Text files", "txt");
            if (path == null) {
                return;
            }
            Writer writer = null;
            try {
                writer = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
                writer.write(text);
                writer.close();
                setStatus("Coordinate log saved: " + path.getAbsolutePath());
            } catch (Exception ex) {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (Exception ignored) {
                    // Preserve the original save error.
                }
                JOptionPane.showMessageDialog(frame,
                        "Could not save the coordinate log.\n"
                                + ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                        "Save Coordinate Log", JOptionPane.ERROR_MESSAGE);
            }
        }

        private String buildCoordinateLog() {
            StringBuilder log = new StringBuilder();
            log.append(COORDINATE_LOG_HEADER).append("\n");
            log.append("Log format version: ").append(LOG_FORMAT_VERSION).append("\n");
            log.append("Plugin version: ").append(VERSION).append("\n\n");
            log.append("Coordinate convention:\n");
            log.append("  Origin: top-left image pixel\n");
            log.append("  X direction: right\n");
            log.append("  Y direction: down\n");
            log.append("  Crop corners: top-left, top-right, bottom-right, bottom-left\n");
            log.append("  Angles: degrees clockwise in image coordinates\n\n");

            log.append("Global kDa marker sets:\n");
            boolean wroteMarkerSet = false;
            for (KdaMarkerSet markerSet : markerSets) {
                if (!isMarkerSetUsed(markerSet)) {
                    continue;
                }
                wroteMarkerSet = true;
                log.append(markerSet.id).append(":\n");
                log.append("  Source type: ").append(markerSet.sourceType.displayName).append("\n");
                log.append("  Source image: ").append(logValue(markerSet.sourcePath)).append("\n");
                if (markerSet.sourceWidth > 0 && markerSet.sourceHeight > 0) {
                    log.append("  Source dimensions: ").append(markerSet.sourceWidth).append(" x ")
                            .append(markerSet.sourceHeight).append(" pixels\n");
                }
                log.append("  Markers:\n");
                if (markerSet.markers.isEmpty()) {
                    log.append("    none\n");
                } else {
                    int markerNumber = 1;
                    for (KdaMarker marker : markerSet.markers) {
                        log.append("    ").append(markerNumber++).append(". label = ")
                                .append(logValue(marker.label))
                                .append(", x_abs = ").append(formatCoordinate(marker.xAbs))
                                .append(", y_abs = ").append(formatCoordinate(marker.yAbs))
                                .append("\n");
                    }
                }
                log.append("\n");
            }
            if (!wroteMarkerSet) {
                log.append("  none\n\n");
            }

            log.append("Crops in figure:\n");
            if (bands.isEmpty()) {
                log.append("  none\n");
                return log.toString();
            }

            int bandNumber = 1;
            for (BandCrop band : bands) {
                log.append("Band ").append(bandNumber++).append(": ")
                        .append(logValue(band.label)).append("\n");
                log.append("  Source image: ").append(logValue(band.sourcePath)).append("\n");
                log.append("  Source dimensions: ").append(band.sourceWidth).append(" x ")
                        .append(band.sourceHeight).append(" pixels\n");
                log.append("  Crop origin: x = ").append(formatCoordinate(band.cropX))
                        .append(", y = ").append(formatCoordinate(band.cropY)).append("\n");
                log.append("  Crop size: width = ").append(band.cropWidth)
                        .append(", height = ").append(band.cropHeight).append(" pixels\n");
                log.append("  Crop angle: ").append(formatCoordinate(band.cropAngleDeg))
                        .append(" degrees\n");
                Point2D[] corners = cropCorners(band);
                log.append("  Crop corners:\n");
                appendCorner(log, "top-left", corners[0]);
                appendCorner(log, "top-right", corners[1]);
                appendCorner(log, "bottom-right", corners[2]);
                appendCorner(log, "bottom-left", corners[3]);

                log.append("  Used kDa markers:\n");
                if (band.markerSet == null) {
                    log.append("    Marker set: none\n");
                } else {
                    log.append("    Marker set: ").append(band.markerSet.id).append("\n");
                    log.append("    Marker source image: ")
                            .append(logValue(band.markerSet.sourcePath)).append("\n");
                    if (band.markerMapping != null) {
                        MarkerMapping mapping = band.markerMapping;
                        log.append("    Marker source dimensions: ")
                                .append(mapping.markerWidth).append(" x ")
                                .append(mapping.markerHeight).append(" pixels\n");
                        log.append("    Gel dimensions: ").append(mapping.gelWidth).append(" x ")
                                .append(mapping.gelHeight).append(" pixels\n");
                        log.append("    Coordinate scale: x = ")
                                .append(formatCoordinate(mapping.scaleX))
                                .append(", y = ").append(formatCoordinate(mapping.scaleY)).append("\n");
                        if (mapping.aspectRatioMismatch) {
                            log.append("    WARNING: Marker source and Gel have different ")
                                    .append("width-to-height ratios.\n");
                        } else if (mapping.dimensionsDiffer) {
                            log.append("    Note: Image dimensions differ, but their aspect ratios match.\n");
                        }
                    }
                    if (band.markers.isEmpty()) {
                        log.append("    No markers from this set fell within the crop.\n");
                    } else {
                        int usedNumber = 1;
                        for (CropMarker marker : band.markers) {
                            log.append("    ").append(usedNumber++).append(". label = ")
                                    .append(logValue(marker.label))
                                    .append(", source_x_abs = ")
                                    .append(formatCoordinate(marker.sourceXAbs))
                                    .append(", source_y_abs = ")
                                    .append(formatCoordinate(marker.sourceYAbs))
                                    .append(", gel_x_abs = ")
                                    .append(formatCoordinate(marker.gelXAbs))
                                    .append(", gel_y_abs = ")
                                    .append(formatCoordinate(marker.gelYAbs))
                                    .append(", y_in_crop = ")
                                    .append(formatCoordinate(marker.yInCrop)).append("\n");
                        }
                    }
                }
                log.append("\n");
            }
            return log.toString();
        }

        private boolean isMarkerSetUsed(KdaMarkerSet markerSet) {
            for (BandCrop band : bands) {
                if (band.markerSet == markerSet) {
                    return true;
                }
            }
            return false;
        }

        private static Point2D[] cropCorners(BandCrop band) {
            double angle = Math.toRadians(band.cropAngleDeg);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Point2D topLeft = new Point2D(band.cropX, band.cropY);
            Point2D topRight = new Point2D(
                    band.cropX + band.cropWidth * cos,
                    band.cropY + band.cropWidth * sin);
            Point2D bottomLeft = new Point2D(
                    band.cropX - band.cropHeight * sin,
                    band.cropY + band.cropHeight * cos);
            Point2D bottomRight = new Point2D(
                    topRight.x + bottomLeft.x - topLeft.x,
                    topRight.y + bottomLeft.y - topLeft.y);
            return new Point2D[] {topLeft, topRight, bottomRight, bottomLeft};
        }

        private static void appendCorner(StringBuilder log, String name, Point2D point) {
            log.append("    ").append(name).append(": x = ")
                    .append(formatCoordinate(point.x)).append(", y = ")
                    .append(formatCoordinate(point.y)).append("\n");
        }

        private static String formatCoordinate(double value) {
            return String.format(Locale.US, "%.6f", Double.valueOf(value));
        }

        private static String logValue(String value) {
            if (value == null) {
                return "unknown";
            }
            return value.replace("\r", "\\r").replace("\n", "\\n");
        }

        private void setStatus(String text) {
            if (statusLabel != null) {
                statusLabel.setText(text == null ? " " : text);
            }
        }

        private void setStatusWithArtboardWarning(String text) {
            if (figureCanvas != null && figureCanvas.hasArtworkOutsideArtboard()) {
                setStatus(text + " Warning: some content is outside the A4 artboard.");
            } else {
                setStatus(text);
            }
        }

        private List<BandCrop> bands() {
            return bands;
        }

        private List<LineAnnotation> lineAnnotations() {
            return lineAnnotations;
        }

        private List<TextAnnotation> freeTextAnnotations() {
            return freeTextAnnotations;
        }

        private BandCrop selectedBand() {
            return selectedBand;
        }

        private void selectBand(BandCrop band) {
            selectedBand = band;
            updateCropSizeButtons();
            figureCanvas.repaint();
        }
    }

    private enum AnnotationMode {
        NORMAL,
        ADD_SAMPLE_LABELS,
        ADD_BAND_TICKS,
        DRAW_H_LINE,
        DRAW_V_LINE,
        ADD_FREE_TEXT,
        EDIT_ANNOTATIONS
    }

    private enum AnnotationKind {
        MW_VALUE,
        BAND_NAME,
        SAMPLE_LABEL,
        BAND_TICK,
        FREE_TEXT
    }

    private interface FigureAnnotation {
        boolean canMoveX();
        boolean canMoveY();
        boolean isDeletable();
    }

    private enum LineOrientation {
        HORIZONTAL,
        VERTICAL
    }

    private enum AnnotationHitPart {
        BODY,
        START,
        END
    }

    private static final class AnnotationHit {
        final FigureAnnotation annotation;
        final AnnotationHitPart part;

        AnnotationHit(FigureAnnotation annotation, AnnotationHitPart part) {
            this.annotation = annotation;
            this.part = part;
        }
    }

    private static final class LineAnnotation implements FigureAnnotation {
        final LineOrientation orientation;
        double x1;
        double y1;
        double x2;
        double y2;

        private LineAnnotation(LineOrientation orientation,
                double x1, double y1, double x2, double y2) {
            this.orientation = orientation;
            if (orientation == LineOrientation.HORIZONTAL) {
                this.x1 = Math.min(x1, x2);
                this.x2 = Math.max(x1, x2);
                this.y1 = y1;
                this.y2 = y1;
            } else {
                this.x1 = x1;
                this.x2 = x1;
                this.y1 = Math.min(y1, y2);
                this.y2 = Math.max(y1, y2);
            }
        }

        static LineAnnotation horizontal(double x1, double y, double x2) {
            return new LineAnnotation(LineOrientation.HORIZONTAL, x1, y, x2, y);
        }

        static LineAnnotation vertical(double x, double y1, double y2) {
            return new LineAnnotation(LineOrientation.VERTICAL, x, y1, x, y2);
        }

        LineAnnotation copy() {
            return new LineAnnotation(orientation, x1, y1, x2, y2);
        }

        double length() {
            return orientation == LineOrientation.HORIZONTAL
                    ? x2 - x1 : y2 - y1;
        }

        void translate(double dx, double dy) {
            x1 += dx;
            x2 += dx;
            y1 += dy;
            y2 += dy;
        }

        @Override
        public boolean canMoveX() {
            return true;
        }

        @Override
        public boolean canMoveY() {
            return true;
        }

        @Override
        public boolean isDeletable() {
            return true;
        }
    }

    private static final class TextAnnotation implements FigureAnnotation {
        final AnnotationKind kind;
        final BandCrop owner;
        final CropMarker marker;
        String text;
        float fontSize;
        double angleDeg;
        double sampleXFraction;
        double bandYFraction;
        double anchorX;
        double anchorY;
        double offsetX;
        double offsetY;

        private TextAnnotation(AnnotationKind kind, BandCrop owner,
                CropMarker marker, String text, float fontSize) {
            this.kind = kind;
            this.owner = owner;
            this.marker = marker;
            this.text = text;
            this.fontSize = fontSize;
        }

        static TextAnnotation mwValue(BandCrop owner, CropMarker marker) {
            return new TextAnnotation(AnnotationKind.MW_VALUE, owner, marker,
                    marker.label, Controller.FONT_KDA.getSize2D());
        }

        static TextAnnotation bandName(BandCrop owner, String text) {
            return new TextAnnotation(AnnotationKind.BAND_NAME, owner, null,
                    text, Controller.FONT_NAME.getSize2D());
        }

        static TextAnnotation sampleLabel(BandCrop owner, String text,
                double xFraction, double angleDeg, float fontSize) {
            TextAnnotation annotation = new TextAnnotation(
                    AnnotationKind.SAMPLE_LABEL, owner, null, text, fontSize);
            annotation.sampleXFraction = xFraction;
            annotation.angleDeg = angleDeg;
            return annotation;
        }

        static TextAnnotation bandTick(BandCrop owner, String text,
                double yFraction, float fontSize) {
            TextAnnotation annotation = new TextAnnotation(
                    AnnotationKind.BAND_TICK, owner, null, text, fontSize);
            annotation.bandYFraction = Math.max(0.0, Math.min(1.0, yFraction));
            return annotation;
        }

        static TextAnnotation freeText(String text, double anchorX,
                double anchorY, double angleDeg, float fontSize) {
            TextAnnotation annotation = new TextAnnotation(
                    AnnotationKind.FREE_TEXT, null, null, text, fontSize);
            annotation.anchorX = anchorX;
            annotation.anchorY = anchorY;
            annotation.angleDeg = angleDeg;
            return annotation;
        }

        @Override
        public boolean canMoveX() {
            return kind != AnnotationKind.MW_VALUE
                    && kind != AnnotationKind.BAND_TICK;
        }

        @Override
        public boolean canMoveY() {
            return kind != AnnotationKind.MW_VALUE;
        }

        @Override
        public boolean isDeletable() {
            return kind != AnnotationKind.MW_VALUE;
        }
    }

    private static final class AnnotationTextLine {
        final String text;
        final double drawX;
        final double baselineY;
        final double baselineStartX;
        final double baselineStartY;

        AnnotationTextLine(String text, double drawX, double baselineY,
                AffineTransform transform) {
            this.text = text;
            this.drawX = drawX;
            this.baselineY = baselineY;
            java.awt.geom.Point2D baselineStart = transform.transform(
                    new java.awt.geom.Point2D.Double(drawX, baselineY), null);
            this.baselineStartX = baselineStart.getX();
            this.baselineStartY = baselineStart.getY();
        }
    }

    private static final class AnnotationLayout {
        final TextAnnotation annotation;
        final Font font;
        final List<AnnotationTextLine> lines;
        final double pivotX;
        final double pivotY;
        final double screenRotationRadians;
        final Shape textHitShape;
        final Line2D.Double leaderLine;
        final Shape hitShape;

        AnnotationLayout(TextAnnotation annotation, Font font,
                String[] textLines, double[] drawXs, double[] baselineYs,
                double pivotX, double pivotY,
                double screenRotationRadians, Shape textHitShape,
                Line2D.Double leaderLine) {
            this.annotation = annotation;
            this.font = font;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.screenRotationRadians = screenRotationRadians;
            this.textHitShape = textHitShape;
            this.leaderLine = leaderLine;
            if (leaderLine == null) {
                this.hitShape = textHitShape;
            } else {
                Area compound = new Area(textHitShape);
                compound.add(new Area(new BasicStroke(4.0f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        .createStrokedShape(leaderLine)));
                this.hitShape = compound;
            }
            AffineTransform transform = AffineTransform.getRotateInstance(
                    screenRotationRadians, pivotX, pivotY);
            List<AnnotationTextLine> lineLayouts =
                    new ArrayList<AnnotationTextLine>(textLines.length);
            for (int i = 0; i < textLines.length; i++) {
                lineLayouts.add(new AnnotationTextLine(
                        textLines[i], drawXs[i], baselineYs[i], transform));
            }
            this.lines = Collections.unmodifiableList(lineLayouts);
        }
    }

    private static final class SampleLabelDialogResult {
        final String text;
        final double angleDeg;
        final float fontSize;

        SampleLabelDialogResult(String text, double angleDeg, float fontSize) {
            this.text = text;
            this.angleDeg = angleDeg;
            this.fontSize = fontSize;
        }
    }

    private static final class TextDialogResult {
        final String text;
        final float fontSize;

        TextDialogResult(String text, float fontSize) {
            this.text = text;
            this.fontSize = fontSize;
        }
    }

    private static final class FigureCanvas extends JPanel implements Scrollable {
        static final int CONTENT_EDGE_GAP = 12;
        static final int ARTBOARD_WIDTH = Math.round(
                Controller.A4_PAGE_WIDTH_PT);
        static final int ARTBOARD_HEIGHT = Math.round(
                Controller.A4_PAGE_HEIGHT_PT);
        static final int ARTBOARD_MARGIN = 24;
        static final Color WORKSPACE_COLOR = new Color(214, 216, 220);
        private static final float SELECTION_PAD = 3.0f;

        private final Controller controller;
        private final LinkedHashSet<FigureAnnotation> selectedAnnotations =
                new LinkedHashSet<FigureAnnotation>();
        private java.awt.geom.Point2D.Double dragLast;
        private BandCrop dragBand;
        private boolean draggingAnnotations;
        private LineAnnotation resizingLine;
        private AnnotationHitPart resizingLinePart;
        private java.awt.geom.Point2D.Double lineDrawStart;
        private LineAnnotation linePreview;
        private double viewZoom = 1.0;
        private int zoomChangeGeneration;
        private boolean zoomAnchorPending;
        private boolean pendingZoomUsesPointerAnchor;
        private JViewport pendingZoomViewport;
        private java.awt.geom.Point2D.Double pendingZoomDocumentAnchor;
        private Point pendingZoomAnchorInViewport;

        FigureCanvas(final Controller controller) {
            this.controller = controller;
            setBackground(WORKSPACE_COLOR);
            setFocusable(true);
            installMovementBindings();

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    requestFocusInWindow();
                    if (event.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                    java.awt.geom.Point2D.Double documentPoint = viewToDocument(event.getPoint());
                    if (controller.annotationMode == AnnotationMode.ADD_SAMPLE_LABELS) {
                        BandCrop band = bandAt(documentPoint.x, documentPoint.y);
                        if (band == null) {
                            controller.setStatus("Click inside a crop to add a sample label.");
                            return;
                        }
                        Rectangle rect = rectForBand(band);
                        double xFraction = (documentPoint.x - rect.x) / rect.getWidth();
                        controller.addSampleLabel(band, xFraction);
                        return;
                    }
                    if (controller.annotationMode == AnnotationMode.ADD_BAND_TICKS) {
                        BandCrop band = bandAt(documentPoint.x, documentPoint.y);
                        if (band == null) {
                            controller.setStatus("Click inside a crop to add a band tick.");
                            return;
                        }
                        Rectangle rect = rectForBand(band);
                        double yFraction = (documentPoint.y - rect.y) / rect.getHeight();
                        controller.addBandTick(band,
                                Math.max(0.0, Math.min(1.0, yFraction)));
                        return;
                    }
                    if (controller.annotationMode == AnnotationMode.ADD_FREE_TEXT) {
                        if (!isInsideArtboard(documentPoint.x, documentPoint.y)) {
                            controller.setStatus(
                                    "Click inside the white A4 artboard to add free text.");
                            return;
                        }
                        controller.addFreeText(documentPoint.x, documentPoint.y);
                        return;
                    }
                    if (controller.annotationMode == AnnotationMode.DRAW_H_LINE
                            || controller.annotationMode == AnnotationMode.DRAW_V_LINE) {
                        beginLineDrawing(documentPoint);
                        return;
                    }
                    if (controller.annotationMode == AnnotationMode.EDIT_ANNOTATIONS) {
                        beginAnnotationPress(event, documentPoint);
                        return;
                    }
                    BandCrop band = bandAt(documentPoint.x, documentPoint.y);
                    controller.selectBand(band);
                    if (band != null) {
                        dragBand = band;
                        dragLast = documentPoint;
                    }
                }

                @Override
                public void mouseDragged(MouseEvent event) {
                    java.awt.geom.Point2D.Double documentPoint = viewToDocument(event.getPoint());
                    if (controller.annotationMode == AnnotationMode.DRAW_H_LINE
                            || controller.annotationMode == AnnotationMode.DRAW_V_LINE) {
                        updateLineDrawing(documentPoint);
                        return;
                    }
                    if (controller.annotationMode == AnnotationMode.EDIT_ANNOTATIONS) {
                        if (resizingLine != null && resizingLinePart != null) {
                            resizeLineEndpoint(resizingLine, resizingLinePart, documentPoint);
                        } else if (draggingAnnotations && dragLast != null) {
                            double dx = documentPoint.x - dragLast.x;
                            double dy = documentPoint.y - dragLast.y;
                            if (dx != 0 || dy != 0) {
                                java.awt.geom.Point2D.Double applied =
                                        moveSelectedAnnotations(dx, dy);
                                dragLast = documentPoint;
                            }
                        }
                        return;
                    }
                    if (dragBand == null || dragLast == null) {
                        return;
                    }
                    double dy = documentPoint.y - dragLast.y;
                    Rectangle currentRect = rectForBand(dragBand);
                    Rectangle2D currentBounds = bandArtworkBounds(dragBand, currentRect);
                    if (dy < 0.0) {
                        dy = Math.max(dy, Math.min(0.0,
                                CONTENT_EDGE_GAP - currentBounds.getMinY()));
                    } else if (dy > 0.0) {
                        dy = Math.min(dy, Math.max(0.0,
                                ARTBOARD_HEIGHT - CONTENT_EDGE_GAP
                                        - currentBounds.getMaxY()));
                    }
                    dragBand.yOffset += dy;
                    dragLast = documentPoint;
                    refreshLayout();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    if (event.getButton() == MouseEvent.BUTTON1
                            && (controller.annotationMode == AnnotationMode.DRAW_H_LINE
                            || controller.annotationMode == AnnotationMode.DRAW_V_LINE)) {
                        finishLineDrawing(viewToDocument(event.getPoint()));
                    }
                    dragBand = null;
                    dragLast = null;
                    draggingAnnotations = false;
                    resizingLine = null;
                    resizingLinePart = null;
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (controller.annotationMode != AnnotationMode.EDIT_ANNOTATIONS
                            || event.getButton() != MouseEvent.BUTTON1
                            || event.getClickCount() != 2) {
                        return;
                    }
                    java.awt.geom.Point2D.Double documentPoint = viewToDocument(event.getPoint());
                    AnnotationHit hit = annotationHitAt(documentPoint.x, documentPoint.y);
                    if (hit != null && hit.annotation instanceof TextAnnotation) {
                        TextAnnotation annotation = (TextAnnotation) hit.annotation;
                        selectedAnnotations.add(annotation);
                        controller.selectBand(annotation.owner);
                        controller.updateAnnotationFontReadout();
                        repaint();
                        controller.editTextAnnotation(annotation);
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(new MouseAdapter() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent event) {
                    if (!event.isControlDown()) {
                        scrollContainingPane(event);
                        return;
                    }
                    if (dragLast != null) {
                        return;
                    }
                    event.consume();
                    double factor = Math.pow(1.10, -event.getPreciseWheelRotation());
                    controller.zoomFigureAt(factor, event.getPoint());
                }
            });
        }

        private void scrollContainingPane(MouseWheelEvent event) {
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
                    JScrollPane.class, this);
            if (scrollPane == null) {
                return;
            }
            javax.swing.JScrollBar scrollBar = event.isShiftDown()
                    ? scrollPane.getHorizontalScrollBar()
                    : scrollPane.getVerticalScrollBar();
            if (!scrollBar.isVisible() && !event.isShiftDown()) {
                scrollBar = scrollPane.getHorizontalScrollBar();
            }
            if (!scrollBar.isVisible()) {
                return;
            }
            int direction = event.getPreciseWheelRotation() < 0.0 ? -1 : 1;
            int increment = event.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL
                    ? scrollBar.getBlockIncrement(direction)
                    : scrollBar.getUnitIncrement(direction)
                            * Math.max(1, event.getScrollAmount());
            int delta = (int) Math.round(
                    increment * event.getPreciseWheelRotation());
            if (delta == 0) {
                delta = direction;
            }
            scrollBar.setValue(scrollBar.getValue() + delta);
            event.consume();
        }

        private void installMovementBindings() {
            bindMovement("annotation_left", KeyEvent.VK_LEFT, 0, -1, 0);
            bindMovement("annotation_right", KeyEvent.VK_RIGHT, 0, 1, 0);
            bindMovement("annotation_up", KeyEvent.VK_UP, 0, 0, -1);
            bindMovement("annotation_down", KeyEvent.VK_DOWN, 0, 0, 1);
            bindMovement("annotation_left_fast", KeyEvent.VK_LEFT,
                    InputEvent.SHIFT_DOWN_MASK, -10, 0);
            bindMovement("annotation_right_fast", KeyEvent.VK_RIGHT,
                    InputEvent.SHIFT_DOWN_MASK, 10, 0);
            bindMovement("annotation_up_fast", KeyEvent.VK_UP,
                    InputEvent.SHIFT_DOWN_MASK, 0, -10);
            bindMovement("annotation_down_fast", KeyEvent.VK_DOWN,
                    InputEvent.SHIFT_DOWN_MASK, 0, 10);
            bindAnnotationCommand("annotation_delete", KeyEvent.VK_DELETE, 0,
                    new Runnable() {
                        @Override
                        public void run() {
                            controller.deleteSelectedAnnotations();
                        }
                    });
            bindAnnotationCommand("annotation_backspace", KeyEvent.VK_BACK_SPACE, 0,
                    new Runnable() {
                        @Override
                        public void run() {
                            controller.deleteSelectedAnnotations();
                        }
                    });
            bindAnnotationCommand("annotation_copy", KeyEvent.VK_C,
                    InputEvent.CTRL_DOWN_MASK, new Runnable() {
                        @Override
                        public void run() {
                            controller.copySelectedLines();
                        }
                    });
            bindAnnotationCommand("annotation_paste", KeyEvent.VK_V,
                    InputEvent.CTRL_DOWN_MASK, new Runnable() {
                        @Override
                        public void run() {
                            controller.pasteCopiedLines();
                        }
                    });
        }

        private void bindAnnotationCommand(final String key, int keyCode,
                int modifiers, final Runnable command) {
            getInputMap(JComponent.WHEN_FOCUSED).put(
                    KeyStroke.getKeyStroke(keyCode, modifiers), key);
            getActionMap().put(key, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (controller.annotationMode == AnnotationMode.EDIT_ANNOTATIONS) {
                        command.run();
                    }
                }
            });
        }

        private void bindMovement(String key, int keyCode, int modifiers,
                final int dx, final int dy) {
            getInputMap(JComponent.WHEN_FOCUSED).put(
                    KeyStroke.getKeyStroke(keyCode, modifiers), key);
            getActionMap().put(key, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (controller.annotationMode == AnnotationMode.EDIT_ANNOTATIONS) {
                        moveSelectedAnnotations(dx, dy);
                    }
                }
            });
        }

        private void beginLineDrawing(java.awt.geom.Point2D.Double documentPoint) {
            if (!isInsideArtboard(documentPoint.x, documentPoint.y)) {
                controller.setStatus("Start the line inside the A4 artboard.");
                return;
            }
            lineDrawStart = clampToArtboard(documentPoint);
            updateLineDrawing(lineDrawStart);
        }

        private void updateLineDrawing(java.awt.geom.Point2D.Double documentPoint) {
            if (lineDrawStart == null) {
                return;
            }
            java.awt.geom.Point2D.Double end = clampToArtboard(documentPoint);
            if (controller.annotationMode == AnnotationMode.DRAW_H_LINE) {
                linePreview = LineAnnotation.horizontal(
                        lineDrawStart.x, lineDrawStart.y, end.x);
            } else if (controller.annotationMode == AnnotationMode.DRAW_V_LINE) {
                linePreview = LineAnnotation.vertical(
                        lineDrawStart.x, lineDrawStart.y, end.y);
            }
            repaint();
        }

        private void finishLineDrawing(java.awt.geom.Point2D.Double documentPoint) {
            if (lineDrawStart == null) {
                return;
            }
            updateLineDrawing(documentPoint);
            LineAnnotation finished = linePreview;
            lineDrawStart = null;
            linePreview = null;
            if (finished == null || finished.length() < Controller.MIN_FREE_LINE_LENGTH) {
                controller.setStatus(controller.annotationMode == AnnotationMode.DRAW_H_LINE
                        ? "Draw H-line: drag horizontally to create a line."
                        : "Draw V-line: drag vertically to create a line.");
                repaint();
                return;
            }
            controller.lineAnnotations().add(finished);
            refreshLayout();
            controller.setStatus(controller.annotationMode == AnnotationMode.DRAW_H_LINE
                    ? "H-line added; drag to draw another."
                    : "V-line added; drag to draw another.");
        }

        void cancelPendingLineDrawing() {
            lineDrawStart = null;
            linePreview = null;
            repaint();
        }

        private java.awt.geom.Point2D.Double clampToArtboard(
                java.awt.geom.Point2D.Double point) {
            double inset = Controller.FREE_LINE_STROKE_WIDTH / 2.0;
            return new java.awt.geom.Point2D.Double(
                    Math.max(inset, Math.min(ARTBOARD_WIDTH - inset, point.x)),
                    Math.max(inset, Math.min(ARTBOARD_HEIGHT - inset, point.y)));
        }

        private void beginAnnotationPress(MouseEvent event,
                java.awt.geom.Point2D.Double documentPoint) {
            AnnotationHit hit = annotationHitAt(documentPoint.x, documentPoint.y);
            boolean shift = event.isShiftDown();
            draggingAnnotations = false;
            resizingLine = null;
            resizingLinePart = null;
            dragLast = null;
            if (hit == null) {
                if (!shift) {
                    selectedAnnotations.clear();
                    controller.updateAnnotationFontReadout();
                    repaint();
                }
                controller.setStatus(
                        "Edit Annotations: click an object to select; Shift-click selects several.");
                return;
            }

            FigureAnnotation target = hit.annotation;
            if (shift) {
                if (selectedAnnotations.contains(target)) {
                    selectedAnnotations.remove(target);
                } else {
                    selectedAnnotations.add(target);
                }
            } else if (!selectedAnnotations.contains(target)) {
                selectedAnnotations.clear();
                selectedAnnotations.add(target);
            }
            controller.selectBand(target instanceof TextAnnotation
                    ? ((TextAnnotation) target).owner : null);
            controller.updateAnnotationFontReadout();
            repaint();

            if (!selectedAnnotations.contains(target)) {
                return;
            }
            if (target instanceof LineAnnotation
                    && hit.part != AnnotationHitPart.BODY) {
                resizingLine = (LineAnnotation) target;
                resizingLinePart = hit.part;
                controller.setStatus("Selected line endpoint: drag to extend or shorten.");
            } else if (target.canMoveX() || target.canMoveY()) {
                draggingAnnotations = true;
                dragLast = documentPoint;
                if (target instanceof TextAnnotation
                        && ((TextAnnotation) target).kind == AnnotationKind.BAND_TICK) {
                    controller.setStatus(
                            "Selected band tick: drag vertically, use arrows, or double-click to edit.");
                } else if (target instanceof LineAnnotation) {
                    controller.setStatus(
                            "Selected line: drag the body, use arrows, or drag an endpoint.");
                } else {
                    controller.setStatus(selectedAnnotations.size() == 1
                            ? "Selected text: drag, use arrows, or double-click to edit."
                            : "Selected objects: drag or use arrows to move them together.");
                }
            } else {
                controller.setStatus("MW values are resize-only; use A-/A+.");
            }
        }

        Set<FigureAnnotation> selectedFigureAnnotations() {
            return new LinkedHashSet<FigureAnnotation>(selectedAnnotations);
        }

        List<TextAnnotation> selectedTextAnnotations() {
            List<TextAnnotation> texts = new ArrayList<TextAnnotation>();
            for (FigureAnnotation annotation : selectedAnnotations) {
                if (annotation instanceof TextAnnotation) {
                    texts.add((TextAnnotation) annotation);
                }
            }
            return texts;
        }

        boolean hasAnnotationSelection() {
            return !selectedAnnotations.isEmpty();
        }

        void replaceAnnotationSelection(List<? extends FigureAnnotation> annotations) {
            selectedAnnotations.clear();
            selectedAnnotations.addAll(annotations);
            repaint();
        }

        void clearAnnotationSelection() {
            if (!selectedAnnotations.isEmpty()) {
                selectedAnnotations.clear();
                repaint();
            }
        }

        private java.awt.geom.Point2D.Double moveSelectedAnnotations(
                double requestedDx, double requestedDy) {
            double minimumDx = -Double.MAX_VALUE;
            double maximumDx = Double.MAX_VALUE;
            double minimumDy = -Double.MAX_VALUE;
            double maximumDy = Double.MAX_VALUE;
            boolean canMoveX = false;
            boolean canMoveY = false;

            for (FigureAnnotation annotation : selectedAnnotations) {
                if (annotation.canMoveX()) {
                    Rectangle2D bounds = annotationBounds(annotation);
                    if (bounds != null) {
                        double gap = annotation instanceof LineAnnotation
                                ? 0.0 : CONTENT_EDGE_GAP;
                        minimumDx = Math.max(minimumDx, gap - bounds.getMinX());
                        maximumDx = Math.min(maximumDx,
                                ARTBOARD_WIDTH - gap - bounds.getMaxX());
                        canMoveX = true;
                    }
                }
                if (annotation.canMoveY()) {
                    if (annotation instanceof TextAnnotation
                            && ((TextAnnotation) annotation).kind == AnnotationKind.BAND_TICK) {
                        TextAnnotation tick = (TextAnnotation) annotation;
                        Rectangle rect = rectForBand(tick.owner);
                        if (rect != null) {
                            double tickY = rect.y + tick.bandYFraction * rect.height;
                            minimumDy = Math.max(minimumDy, rect.y - tickY);
                            maximumDy = Math.min(maximumDy, rect.getMaxY() - tickY);
                            canMoveY = true;
                        }
                    } else {
                        Rectangle2D bounds = annotationBounds(annotation);
                        if (bounds != null) {
                            double gap = annotation instanceof LineAnnotation
                                    ? 0.0 : CONTENT_EDGE_GAP;
                            minimumDy = Math.max(minimumDy, gap - bounds.getMinY());
                            maximumDy = Math.min(maximumDy,
                                    ARTBOARD_HEIGHT - gap - bounds.getMaxY());
                            canMoveY = true;
                        }
                    }
                }
            }

            double dx = canMoveX
                    ? Math.max(minimumDx, Math.min(maximumDx, requestedDx)) : 0.0;
            double dy = canMoveY
                    ? Math.max(minimumDy, Math.min(maximumDy, requestedDy)) : 0.0;
            boolean moved = false;
            for (FigureAnnotation annotation : selectedAnnotations) {
                if (annotation instanceof LineAnnotation) {
                    LineAnnotation line = (LineAnnotation) annotation;
                    line.translate(annotation.canMoveX() ? dx : 0.0,
                            annotation.canMoveY() ? dy : 0.0);
                    moved |= dx != 0.0 || dy != 0.0;
                } else if (annotation instanceof TextAnnotation) {
                    TextAnnotation text = (TextAnnotation) annotation;
                    if (text.kind == AnnotationKind.BAND_TICK) {
                        Rectangle rect = rectForBand(text.owner);
                        if (rect != null && rect.height > 0 && dy != 0.0) {
                            text.bandYFraction = Math.max(0.0, Math.min(1.0,
                                    text.bandYFraction + dy / rect.height));
                            moved = true;
                        }
                    } else {
                        if (text.canMoveX()) {
                            text.offsetX += dx;
                            moved |= dx != 0.0;
                        }
                        if (text.canMoveY()) {
                            text.offsetY += dy;
                            moved |= dy != 0.0;
                        }
                    }
                }
            }
            if (moved) {
                refreshLayout();
                controller.setStatus(selectedAnnotations.size() == 1
                        ? "Moved selected annotation."
                        : "Moved selected annotations.");
            } else if (!selectedAnnotations.isEmpty() && !canMoveX && !canMoveY) {
                controller.setStatus("MW values are resize-only; use A-/A+.");
            }
            return new java.awt.geom.Point2D.Double(dx, dy);
        }

        private Rectangle2D annotationBounds(FigureAnnotation annotation) {
            if (annotation instanceof LineAnnotation) {
                return lineBounds((LineAnnotation) annotation);
            }
            TextAnnotation text = (TextAnnotation) annotation;
            if (text.kind == AnnotationKind.FREE_TEXT) {
                return annotationLayout(text, null).hitShape.getBounds2D();
            }
            Rectangle rect = rectForBand(text.owner);
            return rect == null ? null
                    : annotationLayout(text, rect).hitShape.getBounds2D();
        }

        private Rectangle2D lineBounds(LineAnnotation line) {
            double halfStroke = Controller.FREE_LINE_STROKE_WIDTH / 2.0;
            return new Rectangle2D.Double(
                    Math.min(line.x1, line.x2) - halfStroke,
                    Math.min(line.y1, line.y2) - halfStroke,
                    Math.abs(line.x2 - line.x1) + halfStroke * 2.0,
                    Math.abs(line.y2 - line.y1) + halfStroke * 2.0);
        }

        boolean lineFitsArtboard(LineAnnotation line) {
            double inset = Controller.FREE_LINE_STROKE_WIDTH / 2.0;
            return line.x1 >= inset && line.x1 <= ARTBOARD_WIDTH - inset
                    && line.x2 >= inset && line.x2 <= ARTBOARD_WIDTH - inset
                    && line.y1 >= inset && line.y1 <= ARTBOARD_HEIGHT - inset
                    && line.y2 >= inset && line.y2 <= ARTBOARD_HEIGHT - inset;
        }

        private void resizeLineEndpoint(LineAnnotation line,
                AnnotationHitPart part, java.awt.geom.Point2D.Double documentPoint) {
            java.awt.geom.Point2D.Double point = clampToArtboard(documentPoint);
            if (line.orientation == LineOrientation.HORIZONTAL) {
                if (part == AnnotationHitPart.START) {
                    line.x1 = Math.min(point.x,
                            line.x2 - Controller.MIN_FREE_LINE_LENGTH);
                } else {
                    line.x2 = Math.max(point.x,
                            line.x1 + Controller.MIN_FREE_LINE_LENGTH);
                }
            } else if (part == AnnotationHitPart.START) {
                line.y1 = Math.min(point.y,
                        line.y2 - Controller.MIN_FREE_LINE_LENGTH);
            } else {
                line.y2 = Math.max(point.y,
                        line.y1 + Controller.MIN_FREE_LINE_LENGTH);
            }
            refreshLayout();
        }

        void refreshLayout() {
            revalidate();
            repaint();
        }

        double viewZoom() {
            return viewZoom;
        }

        void setViewZoom(double requestedZoom, Point anchorInCanvas) {
            final double nextZoom = Math.max(0.25, Math.min(4.0, requestedZoom));
            if (Math.abs(nextZoom - viewZoom) < 0.0001) {
                repaint();
                return;
            }

            final JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(
                    JViewport.class, this);
            final Point oldViewPosition = viewport == null
                    ? new Point(0, 0) : viewport.getViewPosition();
            final Dimension extent = viewport == null
                    ? new Dimension(0, 0) : viewport.getExtentSize();
            boolean pointerAnchor = anchorInCanvas != null;
            if (!zoomAnchorPending || pendingZoomViewport != viewport
                    || pendingZoomUsesPointerAnchor != pointerAnchor) {
                Point anchor = anchorInCanvas;
                if (anchor == null) {
                    anchor = new Point(oldViewPosition.x + extent.width / 2,
                            oldViewPosition.y + extent.height / 2);
                }
                pendingZoomDocumentAnchor = viewToDocument(anchor);
                pendingZoomAnchorInViewport = new Point(
                        anchor.x - oldViewPosition.x,
                        anchor.y - oldViewPosition.y);
                pendingZoomViewport = viewport;
                pendingZoomUsesPointerAnchor = pointerAnchor;
                zoomAnchorPending = viewport != null;
            }
            final java.awt.geom.Point2D.Double documentAnchor =
                    pendingZoomDocumentAnchor;
            final Point anchorInViewport = pendingZoomAnchorInViewport;

            viewZoom = nextZoom;
            final int generation = ++zoomChangeGeneration;
            revalidate();
            repaint();
            if (viewport == null) {
                zoomAnchorPending = false;
                return;
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (generation != zoomChangeGeneration) {
                        return;
                    }
                    java.awt.geom.Point2D.Double newAnchor = documentToView(documentAnchor);
                    int targetX = (int) Math.round(newAnchor.x - anchorInViewport.x);
                    int targetY = (int) Math.round(newAnchor.y - anchorInViewport.y);
                    Dimension viewSize = getSize();
                    Dimension currentExtent = viewport.getExtentSize();
                    targetX = Math.max(0, Math.min(targetX,
                            Math.max(0, viewSize.width - currentExtent.width)));
                    targetY = Math.max(0, Math.min(targetY,
                            Math.max(0, viewSize.height - currentExtent.height)));
                    viewport.setViewPosition(new Point(targetX, targetY));
                    zoomAnchorPending = false;
                    pendingZoomViewport = null;
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(
                    (int) Math.ceil(ARTBOARD_WIDTH * viewZoom) + ARTBOARD_MARGIN * 2,
                    (int) Math.ceil(ARTBOARD_HEIGHT * viewZoom) + ARTBOARD_MARGIN * 2);
        }

        Dimension contentSize() {
            return new Dimension(ARTBOARD_WIDTH, ARTBOARD_HEIGHT);
        }

        Dimension groupedPdfContentSize() {
            return contentSize();
        }

        boolean hasArtworkOutsideArtboard() {
            Rectangle2D bounds = artworkBounds();
            return bounds != null && (bounds.getMinX() < 0.0
                    || bounds.getMinY() < 0.0
                    || bounds.getMaxX() > ARTBOARD_WIDTH
                    || bounds.getMaxY() > ARTBOARD_HEIGHT);
        }

        private Rectangle2D artworkBounds() {
            Rectangle2D bounds = null;
            int baseY = Controller.TOP_MARGIN;
            for (BandCrop band : controller.bands()) {
                Rectangle rect = layoutRectFor(band, baseY);
                bounds = union(bounds, bandArtworkBounds(band, rect));
                baseY += bandLayoutAdvance(band, rect);
            }
            for (LineAnnotation line : controller.lineAnnotations()) {
                bounds = union(bounds, lineBounds(line));
            }
            for (TextAnnotation annotation : controller.freeTextAnnotations()) {
                bounds = union(bounds, annotationLayout(annotation, null)
                        .hitShape.getBounds2D());
            }
            return bounds;
        }

        private Rectangle2D bandArtworkBounds(BandCrop band, Rectangle rect) {
            Rectangle2D bounds = new Rectangle2D.Double(
                    rect.x - (controller.tickSidesSwapped ? 0.6 : Controller.TICK_LEN + 2.6),
                    rect.y - 0.6, rect.width + Controller.TICK_LEN + 3.2,
                    rect.height + 1.2);
            for (TextAnnotation annotation : band.textAnnotations) {
                bounds = union(bounds, annotationLayout(annotation, rect)
                        .hitShape.getBounds2D());
            }
            return bounds;
        }

        private static Rectangle2D union(Rectangle2D first, Rectangle2D second) {
            if (first == null) {
                return (Rectangle2D) second.clone();
            }
            Rectangle2D combined = new Rectangle2D.Double();
            Rectangle2D.union(first, second, combined);
            return combined;
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect,
                int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect,
                int orientation, int direction) {
            return Math.max(18, orientation == javax.swing.SwingConstants.HORIZONTAL
                    ? visibleRect.width - 36 : visibleRect.height - 36);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport
                    && getPreferredSize().width <= getParent().getWidth();
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport
                    && getPreferredSize().height <= getParent().getHeight();
        }

        private double pageViewX() {
            return Math.max(ARTBOARD_MARGIN,
                    (getWidth() - ARTBOARD_WIDTH * viewZoom) / 2.0);
        }

        private double pageViewY() {
            return Math.max(ARTBOARD_MARGIN,
                    (getHeight() - ARTBOARD_HEIGHT * viewZoom) / 2.0);
        }

        private java.awt.geom.Point2D.Double viewToDocument(Point viewPoint) {
            return new java.awt.geom.Point2D.Double(
                    (viewPoint.x - pageViewX()) / viewZoom,
                    (viewPoint.y - pageViewY()) / viewZoom);
        }

        private java.awt.geom.Point2D.Double documentToView(
                java.awt.geom.Point2D documentPoint) {
            return new java.awt.geom.Point2D.Double(
                    pageViewX() + documentPoint.getX() * viewZoom,
                    pageViewY() + documentPoint.getY() * viewZoom);
        }

        private Rectangle layoutRectFor(BandCrop band, int baseY) {
            return rawLayoutRectFor(band, baseY);
        }

        private Rectangle rawLayoutRectFor(BandCrop band, int baseY) {
            int h = band.displayHeight();
            int x = Controller.LEFT_MARGIN + (int) Math.round(band.xOffset);
            int y = baseY + (int) Math.round(band.yOffset);
            return new Rectangle(x, y, band.displayWidth, h);
        }

        private Rectangle rectForBand(BandCrop target) {
            int baseY = Controller.TOP_MARGIN;
            for (BandCrop band : controller.bands()) {
                Rectangle rect = layoutRectFor(band, baseY);
                if (band == target) {
                    return rect;
                }
                baseY += bandLayoutAdvance(band, rect);
            }
            return null;
        }

        private BandCrop bandAt(double x, double y) {
            if (!isInsideArtboard(x, y)) {
                return null;
            }
            List<BandCrop> paintOrder = new ArrayList<BandCrop>();
            List<Rectangle> rectangles = new ArrayList<Rectangle>();
            int baseY = Controller.TOP_MARGIN;
            for (BandCrop band : controller.bands()) {
                Rectangle rect = layoutRectFor(band, baseY);
                paintOrder.add(band);
                rectangles.add(rect);
                baseY += bandLayoutAdvance(band, rect);
            }
            for (int i = paintOrder.size() - 1; i >= 0; i--) {
                if (rectangles.get(i).contains(x, y)) {
                    return paintOrder.get(i);
                }
            }
            return null;
        }

        private AnnotationHit annotationHitAt(double x, double y) {
            if (!isInsideArtboard(x, y)) {
                return null;
            }
            double handleRadius = Controller.LINE_HANDLE_SIZE_PX / viewZoom;
            List<LineAnnotation> lines = controller.lineAnnotations();
            for (int i = lines.size() - 1; i >= 0; i--) {
                LineAnnotation line = lines.get(i);
                if (!selectedAnnotations.contains(line)) {
                    continue;
                }
                if (java.awt.geom.Point2D.distance(x, y, line.x1, line.y1)
                        <= handleRadius) {
                    return new AnnotationHit(line, AnnotationHitPart.START);
                }
                if (java.awt.geom.Point2D.distance(x, y, line.x2, line.y2)
                        <= handleRadius) {
                    return new AnnotationHit(line, AnnotationHitPart.END);
                }
            }
            double lineTolerance = Controller.LINE_HIT_RADIUS_PX / viewZoom;
            for (int i = lines.size() - 1; i >= 0; i--) {
                LineAnnotation line = lines.get(i);
                if (Line2D.ptSegDist(line.x1, line.y1, line.x2, line.y2, x, y)
                        <= lineTolerance) {
                    return new AnnotationHit(line, AnnotationHitPart.BODY);
                }
            }
            List<AnnotationLayout> layouts = annotationLayoutsInPaintOrder();
            for (int i = layouts.size() - 1; i >= 0; i--) {
                AnnotationLayout layout = layouts.get(i);
                boolean hitText = layout.textHitShape.contains(x, y);
                boolean hitLeader = layout.leaderLine != null
                        && layout.leaderLine.ptSegDist(x, y) <= lineTolerance;
                if (hitText || hitLeader) {
                    return new AnnotationHit(layout.annotation, AnnotationHitPart.BODY);
                }
            }
            return null;
        }

        private boolean isInsideArtboard(double x, double y) {
            return x >= 0.0 && y >= 0.0
                    && x <= ARTBOARD_WIDTH && y <= ARTBOARD_HEIGHT;
        }

        private List<AnnotationLayout> annotationLayoutsInPaintOrder() {
            List<AnnotationLayout> layouts = new ArrayList<AnnotationLayout>();
            int baseY = Controller.TOP_MARGIN;
            for (BandCrop band : controller.bands()) {
                Rectangle rect = layoutRectFor(band, baseY);
                for (TextAnnotation annotation : band.textAnnotations) {
                    layouts.add(annotationLayout(annotation, rect));
                }
                baseY += bandLayoutAdvance(band, rect);
            }
            for (TextAnnotation annotation : controller.freeTextAnnotations()) {
                layouts.add(annotationLayout(annotation, null));
            }
            return layouts;
        }

        private Font annotationFont(TextAnnotation annotation) {
            Font base = annotation.kind == AnnotationKind.BAND_NAME
                    ? Controller.FONT_NAME
                    : annotation.kind == AnnotationKind.SAMPLE_LABEL
                            || annotation.kind == AnnotationKind.FREE_TEXT
                            ? Controller.FONT_SAMPLE : Controller.FONT_KDA;
            return base.deriveFont(Controller.clampAnnotationFontSize(annotation.fontSize));
        }

        private int bandLayoutAdvance(BandCrop band, Rectangle rect) {
            FontMetrics defaultMetrics = getFontMetrics(Controller.FONT_NAME);
            double defaultBlockHeight = defaultMetrics.getAscent()
                    + defaultMetrics.getDescent();
            double labelReserve = 20.0;
            for (TextAnnotation annotation : band.textAnnotations) {
                if (annotation.kind != AnnotationKind.BAND_NAME) {
                    continue;
                }
                FontMetrics metrics = getFontMetrics(annotationFont(annotation));
                int lineCount = Controller.normalizeAnnotationText(annotation.text)
                        .split("\n", -1).length;
                double blockHeight = metrics.getAscent() + metrics.getDescent()
                        + Math.max(0, lineCount - 1) * metrics.getHeight();
                labelReserve = Math.max(labelReserve,
                        20.0 + Math.max(0.0, blockHeight - defaultBlockHeight));
            }
            return rect.height + Controller.BAND_GAP
                    + (int) Math.ceil(labelReserve);
        }

        private Line2D.Double tickLine(Rectangle rect, double y, boolean onRight) {
            double innerX = onRight ? rect.getMaxX() + 2.0 : rect.x - 2.0;
            double outerX = innerX + (onRight ? Controller.TICK_LEN : -Controller.TICK_LEN);
            return new Line2D.Double(innerX, y, outerX, y);
        }

        private Line2D.Double kdaTickLine(BandCrop band, CropMarker marker, Rectangle rect) {
            double y = rect.y + Math.round(marker.yInCrop * band.scale());
            return tickLine(rect, y, controller.tickSidesSwapped);
        }

        private AnnotationLayout annotationLayout(TextAnnotation annotation, Rectangle rect) {
            Font font = annotationFont(annotation);
            FontMetrics metrics = getFontMetrics(font);
            String[] textLines = Controller.normalizeAnnotationText(annotation.text)
                    .split("\n", -1);
            if (textLines.length == 0) {
                textLines = new String[] { "" };
            }
            double[] widths = new double[textLines.length];
            for (int i = 0; i < textLines.length; i++) {
                widths[i] = metrics.stringWidth(textLines[i]);
            }
            double ascent = metrics.getAscent();
            double descent = metrics.getDescent();
            double lineAdvance = metrics.getHeight();
            double blockHeight = ascent + descent
                    + (textLines.length - 1) * lineAdvance;
            double[] drawXs = new double[textLines.length];
            double[] baselineYs = new double[textLines.length];
            double pivotX;
            double pivotY;
            double firstBaselineY;
            double rotation = 0.0;
            Line2D.Double leaderLine = null;

            if (annotation.kind == AnnotationKind.MW_VALUE) {
                boolean onRight = controller.tickSidesSwapped;
                Line2D.Double tick = kdaTickLine(annotation.owner, annotation.marker, rect);
                pivotX = tick.x2 + (onRight ? Controller.TICK_GAP : -Controller.TICK_GAP)
                        + annotation.offsetX;
                firstBaselineY = tick.y1 + (ascent - descent) / 2.0
                        + annotation.offsetY;
                pivotY = firstBaselineY + descent;
                for (int i = 0; i < textLines.length; i++) {
                    drawXs[i] = onRight ? pivotX : pivotX - widths[i];
                    baselineYs[i] = firstBaselineY + i * lineAdvance;
                }
            } else if (annotation.kind == AnnotationKind.BAND_NAME) {
                pivotX = rect.x + rect.width / 2.0 + annotation.offsetX;
                firstBaselineY = rect.y + rect.height + ascent + 5.0
                        + annotation.offsetY;
                pivotY = firstBaselineY + descent;
                for (int i = 0; i < textLines.length; i++) {
                    drawXs[i] = pivotX - widths[i] / 2.0;
                    baselineYs[i] = firstBaselineY + i * lineAdvance;
                }
            } else if (annotation.kind == AnnotationKind.BAND_TICK) {
                boolean onRight = !controller.tickSidesSwapped;
                double tickY = rect.y + annotation.bandYFraction * rect.height;
                leaderLine = tickLine(rect, tickY, onRight);
                pivotX = leaderLine.x2
                        + (onRight ? Controller.TICK_GAP : -Controller.TICK_GAP);
                pivotY = tickY;
                firstBaselineY = tickY - blockHeight / 2.0 + ascent;
                for (int i = 0; i < textLines.length; i++) {
                    drawXs[i] = onRight ? pivotX : pivotX - widths[i];
                    baselineYs[i] = firstBaselineY + i * lineAdvance;
                }
            } else if (annotation.kind == AnnotationKind.FREE_TEXT) {
                pivotX = annotation.anchorX + annotation.offsetX;
                pivotY = annotation.anchorY + annotation.offsetY;
                firstBaselineY = pivotY;
                rotation = Math.toRadians(-annotation.angleDeg);
                for (int i = 0; i < textLines.length; i++) {
                    drawXs[i] = pivotX;
                    baselineYs[i] = firstBaselineY + i * lineAdvance;
                }
            } else {
                pivotX = rect.x + annotation.sampleXFraction * rect.width
                        + annotation.offsetX;
                pivotY = rect.y - Controller.SAMPLE_LABEL_GAP + annotation.offsetY;
                firstBaselineY = pivotY - descent
                        - (textLines.length - 1) * lineAdvance;
                boolean zeroAngle = annotation.angleDeg == 0.0;
                rotation = Math.toRadians(-annotation.angleDeg);
                for (int i = 0; i < textLines.length; i++) {
                    drawXs[i] = zeroAngle ? pivotX - widths[i] / 2.0 : pivotX;
                    baselineYs[i] = firstBaselineY + i * lineAdvance;
                }
            }

            double blockLeft = Double.MAX_VALUE;
            double blockRight = -Double.MAX_VALUE;
            for (int i = 0; i < textLines.length; i++) {
                blockLeft = Math.min(blockLeft, drawXs[i]);
                blockRight = Math.max(blockRight,
                        drawXs[i] + Math.max(1.0, widths[i]));
            }
            double blockTop = baselineYs[0] - ascent;
            double blockBottom = baselineYs[baselineYs.length - 1] + descent;
            Rectangle2D padded = new Rectangle2D.Double(
                    blockLeft - SELECTION_PAD,
                    blockTop - SELECTION_PAD,
                    blockRight - blockLeft + SELECTION_PAD * 2.0,
                    blockBottom - blockTop + SELECTION_PAD * 2.0);
            AffineTransform transform = AffineTransform.getRotateInstance(
                    rotation, pivotX, pivotY);
            Shape textHitShape = transform.createTransformedShape(padded);
            return new AnnotationLayout(annotation, font,
                    textLines, drawXs, baselineYs,
                    pivotX, pivotY, rotation, textHitShape, leaderLine);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D screen = (Graphics2D) graphics.create();
            screen.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int pageX = (int) Math.round(pageViewX());
            int pageY = (int) Math.round(pageViewY());
            int pageWidth = (int) Math.round(ARTBOARD_WIDTH * viewZoom);
            int pageHeight = (int) Math.round(ARTBOARD_HEIGHT * viewZoom);

            screen.setColor(new Color(150, 150, 150, 90));
            screen.fillRect(pageX + 4, pageY + 4, pageWidth, pageHeight);
            screen.setColor(Color.WHITE);
            screen.fillRect(pageX, pageY, pageWidth, pageHeight);

            Graphics2D document = (Graphics2D) screen.create();
            document.translate(pageViewX(), pageViewY());
            document.scale(viewZoom, viewZoom);
            document.clip(new Rectangle2D.Double(
                    0.0, 0.0, ARTBOARD_WIDTH, ARTBOARD_HEIGHT));
            renderFigure(document, true);
            document.dispose();

            screen.setColor(new Color(120, 120, 120));
            screen.setStroke(new BasicStroke(1.0f));
            screen.drawRect(pageX, pageY, pageWidth, pageHeight);
            screen.setColor(new Color(85, 85, 85));
            screen.setFont(new Font("Arial", Font.PLAIN, 11));
            screen.drawString("A4 portrait", pageX, Math.max(12, pageY - 7));
            screen.dispose();
        }

        void renderFigure(Graphics2D g, boolean paintBackground) {
            Dimension size = contentSize();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (paintBackground) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, size.width, size.height);
            }
            if (controller.bands().isEmpty()
                    && controller.freeTextAnnotations().isEmpty()
                    && controller.lineAnnotations().isEmpty()
                    && linePreview == null) {
                if (paintBackground) {
                    g.setColor(Color.GRAY);
                    g.setFont(new Font("Arial", Font.ITALIC, 14));
                    g.drawString("No crops or annotations yet", 40, 150);
                }
                return;
            }

            int baseY = Controller.TOP_MARGIN;
            for (BandCrop band : controller.bands()) {
                Rectangle rect = layoutRectFor(band, baseY);
                boolean showSelection = paintBackground
                        && controller.annotationMode == AnnotationMode.NORMAL
                        && band == controller.selectedBand();
                drawBand(g, band, rect, showSelection);
                for (TextAnnotation annotation : band.textAnnotations) {
                    boolean annotationSelected = paintBackground
                            && controller.annotationMode == AnnotationMode.EDIT_ANNOTATIONS
                            && selectedAnnotations.contains(annotation);
                    drawAnnotation(g, annotationLayout(annotation, rect), annotationSelected);
                }
                baseY += bandLayoutAdvance(band, rect);
            }
            for (TextAnnotation annotation : controller.freeTextAnnotations()) {
                boolean annotationSelected = paintBackground
                        && controller.annotationMode == AnnotationMode.EDIT_ANNOTATIONS
                        && selectedAnnotations.contains(annotation);
                drawAnnotation(g, annotationLayout(annotation, null), annotationSelected);
            }
            for (LineAnnotation line : controller.lineAnnotations()) {
                boolean selected = paintBackground
                        && controller.annotationMode == AnnotationMode.EDIT_ANNOTATIONS
                        && selectedAnnotations.contains(line);
                drawLineAnnotation(g, line, selected);
            }
            if (paintBackground && linePreview != null) {
                Color oldColor = g.getColor();
                java.awt.Stroke oldStroke = g.getStroke();
                float viewStroke = (float) (1.0 / viewZoom);
                float viewDash = (float) (5.0 / viewZoom);
                g.setColor(new Color(80, 130, 190));
                g.setStroke(new BasicStroke(viewStroke, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10.0f,
                        new float[] { viewDash, viewDash }, 0.0f));
                g.draw(new Line2D.Double(linePreview.x1, linePreview.y1,
                        linePreview.x2, linePreview.y2));
                g.setStroke(oldStroke);
                g.setColor(oldColor);
            }
        }

        private void drawBand(Graphics2D g, BandCrop band, Rectangle rect, boolean selected) {
            g.drawImage(band.image, rect.x, rect.y, rect.width, rect.height, null);

            g.setColor(selected ? Controller.CROP_COLOR : Color.BLACK);
            g.setStroke(new BasicStroke(selected ? 2.0f : 1.2f));
            g.drawRect(rect.x, rect.y, rect.width, rect.height);

            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.2f));
            for (CropMarker marker : band.markers) {
                g.draw(kdaTickLine(band, marker, rect));
            }
        }

        private void drawAnnotation(Graphics2D g, AnnotationLayout layout,
                boolean selected) {
            if (layout.leaderLine != null) {
                Color oldColor = g.getColor();
                java.awt.Stroke oldStroke = g.getStroke();
                g.setColor(selected
                        ? Controller.ANNOTATION_SELECTION_COLOR : Color.BLACK);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(layout.leaderLine);
                g.setStroke(oldStroke);
                g.setColor(oldColor);
            }
            Graphics2D textGraphics = (Graphics2D) g.create();
            try {
                textGraphics.setFont(layout.font);
                textGraphics.setColor(selected
                        ? Controller.ANNOTATION_SELECTION_COLOR : Color.BLACK);
                textGraphics.rotate(layout.screenRotationRadians,
                        layout.pivotX, layout.pivotY);
                for (AnnotationTextLine line : layout.lines) {
                    textGraphics.drawString(line.text,
                            (float) line.drawX, (float) line.baselineY);
                }
            } finally {
                textGraphics.dispose();
            }
            if (selected) {
                Color oldColor = g.getColor();
                java.awt.Stroke oldStroke = g.getStroke();
                g.setColor(Controller.ANNOTATION_SELECTION_COLOR);
                float viewStroke = (float) (1.0 / viewZoom);
                float viewDash = (float) (4.0 / viewZoom);
                g.setStroke(new BasicStroke(viewStroke, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_MITER, 10.0f,
                        new float[] { viewDash, viewDash }, 0.0f));
                g.draw(layout.textHitShape);
                g.setStroke(oldStroke);
                g.setColor(oldColor);
            }
        }

        private void drawLineAnnotation(Graphics2D g, LineAnnotation line,
                boolean selected) {
            Color oldColor = g.getColor();
            java.awt.Stroke oldStroke = g.getStroke();
            g.setColor(selected
                    ? Controller.ANNOTATION_SELECTION_COLOR : Color.BLACK);
            g.setStroke(new BasicStroke(Controller.FREE_LINE_STROKE_WIDTH,
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g.draw(new Line2D.Double(line.x1, line.y1, line.x2, line.y2));
            if (selected) {
                double handleSize = Controller.LINE_HANDLE_SIZE_PX / viewZoom;
                double half = handleSize / 2.0;
                g.fill(new Rectangle2D.Double(
                        line.x1 - half, line.y1 - half, handleSize, handleSize));
                g.fill(new Rectangle2D.Double(
                        line.x2 - half, line.y2 - half, handleSize, handleSize));
            }
            g.setStroke(oldStroke);
            g.setColor(oldColor);
        }
    }

    private static BaseFont createPdfFont(boolean bold) throws Exception {
        List<File> candidates = new ArrayList<File>();
        String windowsDirectory = System.getenv("WINDIR");
        if (windowsDirectory != null) {
            candidates.add(new File(windowsDirectory, bold
                    ? "Fonts\\arialbd.ttf" : "Fonts\\arial.ttf"));
        }
        candidates.add(new File(bold
                ? "/Library/Fonts/Arial Bold.ttf" : "/Library/Fonts/Arial.ttf"));
        candidates.add(new File(bold
                ? "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
                : "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"));
        candidates.add(new File(bold
                ? "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf"
                : "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf"));

        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            candidates.add(new File(javaHome, bold
                    ? "lib/fonts/LucidaSansDemiBold.ttf" : "lib/fonts/LucidaSansRegular.ttf"));
        }

        for (File candidate : candidates) {
            if (!candidate.isFile()) {
                continue;
            }
            try {
                return BaseFont.createFont(candidate.getAbsolutePath(),
                        BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {
                // Try the next installed font before falling back to a PDF base font.
            }
        }
        return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private static Image createPdfRasterImage(BufferedImage source,
            float widthPoints, float heightPoints, int rasterDpi) throws Exception {
        long targetWidthLong = Math.max(1L, Math.round(
                widthPoints * rasterDpi / 72.0));
        long targetHeightLong = Math.max(1L, Math.round(
                heightPoints * rasterDpi / 72.0));
        if (targetWidthLong > 20000L || targetHeightLong > 20000L) {
            throw new IllegalArgumentException(
                    "The requested crop raster is too large at " + rasterDpi
                            + " DPI. Reduce the crop size or export DPI.");
        }
        long targetPixels = targetWidthLong * targetHeightLong;
        if (targetPixels > 80000000L) {
            throw new IllegalArgumentException(
                    "The requested crop raster is too large at " + rasterDpi
                            + " DPI. Reduce the crop size or export DPI.");
        }
        int targetWidth = (int) targetWidthLong;
        int targetHeight = (int) targetHeightLong;
        BufferedImage raster = source;
        if (source.getWidth() != targetWidth || source.getHeight() != targetHeight) {
            raster = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = raster.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            } finally {
                graphics.dispose();
            }
        }
        Image image = Image.getInstance(raster, null);
        image.setInterpolation(true);
        image.scaleAbsolute(widthPoints, heightPoints);
        return image;
    }

    private static final class AnnotatedImagePdfRenderer {
        private final Overlay overlay;
        private final PdfContentByte page;
        private final float pageHeight;
        private final float pointsPerPixel;
        private final BaseFont regularFont;
        private final BaseFont boldFont;

        AnnotatedImagePdfRenderer(Overlay overlay, PdfContentByte page,
                float pageHeight, float pointsPerPixel) throws Exception {
            this.overlay = overlay;
            this.page = page;
            this.pageHeight = pageHeight;
            this.pointsPerPixel = pointsPerPixel;
            this.regularFont = createPdfFont(false);
            this.boldFont = createPdfFont(true);
        }

        void render() {
            if (overlay == null) {
                return;
            }
            for (int index = 0; index < overlay.size(); index++) {
                Roi roi = overlay.get(index);
                if (roi instanceof Line) {
                    drawLine((Line) roi);
                } else if (roi instanceof TextRoi) {
                    drawText((TextRoi) roi);
                }
            }
        }

        private void drawLine(Line line) {
            Color color = line.getStrokeColor();
            float sourceWidth = line.getStrokeWidth();
            float lineWidth = Math.max(0.2f,
                    (sourceWidth > 0.0f ? sourceWidth : 1.0f) * pointsPerPixel);
            page.saveState();
            try {
                page.setColorStroke(pdfColor(color == null ? Color.RED : color));
                page.setLineWidth(lineWidth);
                page.setLineCap(PdfContentByte.LINE_CAP_ROUND);
                page.moveTo((float) line.x1d * pointsPerPixel,
                        pageHeight - (float) line.y1d * pointsPerPixel);
                page.lineTo((float) line.x2d * pointsPerPixel,
                        pageHeight - (float) line.y2d * pointsPerPixel);
                page.stroke();
            } finally {
                page.restoreState();
            }
        }

        private void drawText(TextRoi textRoi) {
            Rectangle bounds = textRoi.getBounds();
            Color background = textRoi.getFillColor();
            if (background != null && background.getAlpha() > 0) {
                page.saveState();
                try {
                    page.setColorFill(pdfColor(background));
                    if (background.getAlpha() < 255) {
                        PdfGState transparency = new PdfGState();
                        transparency.setFillOpacity(background.getAlpha() / 255.0f);
                        page.setGState(transparency);
                    }
                    page.rectangle(bounds.x * pointsPerPixel,
                            pageHeight - (bounds.y + bounds.height) * pointsPerPixel,
                            bounds.width * pointsPerPixel,
                            bounds.height * pointsPerPixel);
                    page.fill();
                } finally {
                    page.restoreState();
                }
            }

            Font font = textRoi.getCurrentFont();
            if (font == null) {
                font = Controller.FONT_RECONSTRUCTION_MARKER;
            }
            FontMetrics metrics = fontMetrics(font);
            BaseFont pdfFont = font.isBold() ? boldFont : regularFont;
            Color textColor = textRoi.getStrokeColor();
            if (textColor == null) {
                textColor = Color.BLACK;
            }
            int alignment = PdfContentByte.ALIGN_LEFT;
            float xPixels = bounds.x;
            if (textRoi.getJustification() == 1) {
                alignment = PdfContentByte.ALIGN_CENTER;
                xPixels = (float) bounds.getCenterX();
            } else if (textRoi.getJustification() == 2) {
                alignment = PdfContentByte.ALIGN_RIGHT;
                xPixels = bounds.x + bounds.width;
            }

            String[] lines = textRoi.getText().split("\\r?\\n", -1);
            page.saveState();
            page.beginText();
            try {
                page.setColorFill(pdfColor(textColor));
                if (textColor.getAlpha() < 255) {
                    PdfGState transparency = new PdfGState();
                    transparency.setFillOpacity(textColor.getAlpha() / 255.0f);
                    page.setGState(transparency);
                }
                page.setFontAndSize(pdfFont, font.getSize2D() * pointsPerPixel);
                for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                    float baselinePixels = bounds.y + metrics.getAscent()
                            + lineIndex * metrics.getHeight();
                    page.showTextAligned(alignment, lines[lineIndex],
                            xPixels * pointsPerPixel,
                            pageHeight - baselinePixels * pointsPerPixel,
                            (float) textRoi.getAngle());
                }
            } finally {
                page.endText();
                page.restoreState();
            }
        }

        private static FontMetrics fontMetrics(Font font) {
            BufferedImage metricsImage = new BufferedImage(
                    1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = metricsImage.createGraphics();
            try {
                return graphics.getFontMetrics(font);
            } finally {
                graphics.dispose();
            }
        }

        private static BaseColor pdfColor(Color color) {
            return new BaseColor(color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    private static final class FlatPdfRenderer {
        private static final float PDF_BORDER_WIDTH = 1.2f;
        private static final float PDF_TICK_WIDTH = 1.2f;

        private final FigureCanvas canvas;
        private final PdfContentByte page;
        private final float pageHeight;
        private final int rasterDpi;
        private final BaseFont kdaFont;
        private final BaseFont bandNameFont;

        FlatPdfRenderer(FigureCanvas canvas, PdfContentByte page,
                float pageHeight, int rasterDpi) throws Exception {
            this.canvas = canvas;
            this.page = page;
            this.pageHeight = pageHeight;
            this.rasterDpi = rasterDpi;
            this.kdaFont = createPdfFont(false);
            this.bandNameFont = createPdfFont(true);
        }

        void render() throws Exception {
            int baseY = Controller.TOP_MARGIN;
            for (BandCrop band : canvas.controller.bands()) {
                Rectangle rect = canvas.layoutRectFor(band, baseY);
                drawBand(band, rect);
                baseY += canvas.bandLayoutAdvance(band, rect);
            }
            for (TextAnnotation annotation : canvas.controller.freeTextAnnotations()) {
                drawTextAnnotation(annotation, null);
            }
            for (LineAnnotation line : canvas.controller.lineAnnotations()) {
                drawDocumentLine(line.x1, line.y1, line.x2, line.y2,
                        Controller.FREE_LINE_STROKE_WIDTH);
            }
        }

        private void drawBand(BandCrop band, Rectangle rect) throws Exception {
            float cropX = rect.x;
            float cropY = pageHeight - rect.y - rect.height;
            float cropWidth = rect.width;
            float cropHeight = rect.height;

            Image image = createPdfRasterImage(
                    band.image, cropWidth, cropHeight, rasterDpi);
            image.setAbsolutePosition(cropX, cropY);
            page.addImage(image);

            page.saveState();
            try {
                page.setColorStroke(BaseColor.BLACK);
                page.setLineWidth(PDF_BORDER_WIDTH);
                page.rectangle(cropX, cropY, cropWidth, cropHeight);
                page.stroke();
            } finally {
                page.restoreState();
            }

            drawMarkers(band, rect);
            drawAnnotations(band, rect);
        }

        private void drawMarkers(BandCrop band, Rectangle rect) {
            for (CropMarker marker : band.markers) {
                Line2D.Double tick = canvas.kdaTickLine(band, marker, rect);
                drawDocumentLine(tick.x1, tick.y1, tick.x2, tick.y2, PDF_TICK_WIDTH);
            }
        }

        private void drawAnnotations(BandCrop band, Rectangle rect) {
            for (TextAnnotation annotation : band.textAnnotations) {
                drawTextAnnotation(annotation, rect);
            }
        }

        private void drawTextAnnotation(TextAnnotation annotation, Rectangle rect) {
            AnnotationLayout layout = canvas.annotationLayout(annotation, rect);
            if (layout.leaderLine != null) {
                drawDocumentLine(layout.leaderLine.x1, layout.leaderLine.y1,
                        layout.leaderLine.x2, layout.leaderLine.y2,
                        PDF_TICK_WIDTH);
            }
            BaseFont font = annotation.kind == AnnotationKind.BAND_NAME
                    ? bandNameFont : kdaFont;
            for (AnnotationTextLine line : layout.lines) {
                int alignment = annotation.kind == AnnotationKind.BAND_NAME
                        ? PdfContentByte.ALIGN_CENTER : PdfContentByte.ALIGN_LEFT;
                float x = annotation.kind == AnnotationKind.BAND_NAME
                        ? (float) layout.pivotX : (float) line.baselineStartX;
                float y = (float) (pageHeight - line.baselineStartY);
                drawText(line.text, font, annotation.fontSize,
                        alignment, x, y, (float) annotation.angleDeg);
            }
        }

        private void drawDocumentLine(double x1, double y1,
                double x2, double y2, float width) {
            page.saveState();
            try {
                page.setColorStroke(BaseColor.BLACK);
                page.setLineWidth(width);
                page.moveTo((float) x1, pageHeight - (float) y1);
                page.lineTo((float) x2, pageHeight - (float) y2);
                page.stroke();
            } finally {
                page.restoreState();
            }
        }

        private void drawText(String text, BaseFont font, float fontSize,
                int alignment, float x, float y, float angleDeg) {
            page.saveState();
            page.beginText();
            try {
                page.setColorFill(BaseColor.BLACK);
                page.setFontAndSize(font, fontSize);
                page.showTextAligned(alignment, text, x, y, angleDeg);
            } finally {
                page.endText();
                page.restoreState();
            }
        }
    }

    private static final class FormXObjectPdfRenderer {
        private static final float PDF_BORDER_WIDTH = 1.2f;
        private static final float PDF_TICK_WIDTH = 1.2f;
        private static final float GROUP_MARGIN = 4.0f;

        private final FigureCanvas canvas;
        private final PdfContentByte page;
        private final float pageHeight;
        private final int rasterDpi;
        private final BaseFont kdaFont;
        private final BaseFont bandNameFont;

        FormXObjectPdfRenderer(FigureCanvas canvas, PdfContentByte page,
                float pageHeight, int rasterDpi)
                throws Exception {
            this.canvas = canvas;
            this.page = page;
            this.pageHeight = pageHeight;
            this.rasterDpi = rasterDpi;
            this.kdaFont = createPdfFont(false);
            this.bandNameFont = createPdfFont(true);
        }

        void render() throws Exception {
            int baseY = Controller.TOP_MARGIN;
            for (BandCrop band : canvas.controller.bands()) {
                Rectangle rect = canvas.layoutRectFor(band, baseY);
                drawBandForm(band, rect);
                baseY += canvas.bandLayoutAdvance(band, rect);
            }
            drawBandNamesForm();
            drawFreeText();
            drawFreeLines();
        }

        private void drawBandForm(BandCrop band, Rectangle rect) throws Exception {
            float markerEditingWidth = Controller.TICK_LEN + 2.0f + GROUP_MARGIN;
            for (TextAnnotation annotation : band.textAnnotations) {
                if (annotation.kind != AnnotationKind.MW_VALUE) {
                    continue;
                }
                Rectangle2D bounds = canvas.annotationLayout(annotation, rect)
                        .hitShape.getBounds2D();
                markerEditingWidth = Math.max(markerEditingWidth,
                        (float) Math.max(rect.x - bounds.getMinX(),
                                bounds.getMaxX() - rect.getMaxX()) + GROUP_MARGIN);
            }
            float groupLeft = rect.x - markerEditingWidth;
            float groupRight = rect.x + rect.width + markerEditingWidth;
            float groupTop = rect.y - GROUP_MARGIN;
            float groupBottom = rect.y + rect.height + GROUP_MARGIN;
            for (TextAnnotation annotation : band.textAnnotations) {
                if (annotation.kind == AnnotationKind.BAND_NAME) {
                    continue;
                }
                Rectangle2D bounds = canvas.annotationLayout(annotation, rect)
                        .hitShape.getBounds2D();
                groupLeft = Math.min(groupLeft, (float) bounds.getMinX() - GROUP_MARGIN);
                groupRight = Math.max(groupRight, (float) bounds.getMaxX() + GROUP_MARGIN);
                groupTop = Math.min(groupTop, (float) bounds.getMinY() - GROUP_MARGIN);
                groupBottom = Math.max(groupBottom, (float) bounds.getMaxY() + GROUP_MARGIN);
            }
            float groupWidth = Math.max(1.0f, groupRight - groupLeft);
            float groupHeight = Math.max(1.0f, groupBottom - groupTop);

            PdfTemplate bandForm = page.createTemplate(groupWidth, groupHeight);
            float cropX = rect.x - groupLeft;
            float cropY = groupBottom - (rect.y + rect.height);

            Image image = createPdfRasterImage(
                    band.image, rect.width, rect.height, rasterDpi);
            image.setAbsolutePosition(cropX, cropY);
            bandForm.addImage(image);

            bandForm.saveState();
            bandForm.setColorStroke(BaseColor.BLACK);
            bandForm.setLineWidth(PDF_BORDER_WIDTH);
            bandForm.rectangle(cropX, cropY, rect.width, rect.height);
            bandForm.stroke();
            bandForm.restoreState();

            if (!band.markers.isEmpty()) {
                // Keep every nested Form XObject wide enough for moving the MW
                // annotations from one side of the crop to the other in Illustrator.
                float markerWidth = groupWidth;
                PdfTemplate markerForm = page.createTemplate(markerWidth, groupHeight);
                PdfTemplate ticksForm = page.createTemplate(markerWidth, groupHeight);
                PdfTemplate valuesForm = page.createTemplate(markerWidth, groupHeight);
                drawTicksForm(ticksForm, band, rect, groupLeft, groupBottom);
                drawKdaValuesForm(valuesForm, band, rect, groupLeft, groupBottom);
                markerForm.addTemplate(ticksForm, 0.0f, 0.0f);
                markerForm.addTemplate(valuesForm, 0.0f, 0.0f);
                bandForm.addTemplate(markerForm, 0.0f, 0.0f);
            }
            if (hasAnnotationKind(band, AnnotationKind.SAMPLE_LABEL)) {
                PdfTemplate sampleLabelsForm = page.createTemplate(groupWidth, groupHeight);
                drawTextAnnotationsForm(sampleLabelsForm, band, rect,
                        groupLeft, groupBottom, AnnotationKind.SAMPLE_LABEL);
                bandForm.addTemplate(sampleLabelsForm, 0.0f, 0.0f);
            }
            if (hasAnnotationKind(band, AnnotationKind.BAND_TICK)) {
                PdfTemplate bandTicksForm = page.createTemplate(groupWidth, groupHeight);
                drawBandTicksForm(bandTicksForm, band, rect, groupLeft, groupBottom);
                bandForm.addTemplate(bandTicksForm, 0.0f, 0.0f);
            }

            page.addTemplate(bandForm, groupLeft, pageHeight - groupBottom);
        }

        private void drawTicksForm(PdfTemplate ticksForm, BandCrop band,
                Rectangle rect, float groupLeft, float groupBottom) {
            ticksForm.saveState();
            try {
                ticksForm.setColorStroke(BaseColor.BLACK);
                ticksForm.setLineWidth(PDF_TICK_WIDTH);
                for (CropMarker marker : band.markers) {
                    Line2D.Double tick = canvas.kdaTickLine(band, marker, rect);
                    ticksForm.moveTo((float) tick.x1 - groupLeft,
                            groupBottom - (float) tick.y1);
                    ticksForm.lineTo((float) tick.x2 - groupLeft,
                            groupBottom - (float) tick.y2);
                }
                ticksForm.stroke();
            } finally {
                ticksForm.restoreState();
            }
        }

        private void drawKdaValuesForm(PdfTemplate valuesForm, BandCrop band,
                Rectangle rect, float groupLeft, float groupBottom) {
            drawTextAnnotationsForm(valuesForm, band, rect,
                    groupLeft, groupBottom, AnnotationKind.MW_VALUE);
        }

        private void drawBandTicksForm(PdfTemplate form, BandCrop band,
                Rectangle rect, float groupLeft, float groupBottom) {
            form.saveState();
            try {
                form.setColorStroke(BaseColor.BLACK);
                form.setLineWidth(PDF_TICK_WIDTH);
                for (TextAnnotation annotation : band.textAnnotations) {
                    if (annotation.kind != AnnotationKind.BAND_TICK) {
                        continue;
                    }
                    AnnotationLayout layout = canvas.annotationLayout(annotation, rect);
                    Line2D.Double leader = layout.leaderLine;
                    form.moveTo((float) leader.x1 - groupLeft,
                            groupBottom - (float) leader.y1);
                    form.lineTo((float) leader.x2 - groupLeft,
                            groupBottom - (float) leader.y2);
                }
                form.stroke();
            } finally {
                form.restoreState();
            }
            drawTextAnnotationsForm(form, band, rect,
                    groupLeft, groupBottom, AnnotationKind.BAND_TICK);
        }

        private boolean hasAnnotationKind(BandCrop band, AnnotationKind kind) {
            for (TextAnnotation annotation : band.textAnnotations) {
                if (annotation.kind == kind) {
                    return true;
                }
            }
            return false;
        }

        private void drawTextAnnotationsForm(PdfTemplate form, BandCrop band,
                Rectangle rect, float groupLeft, float groupBottom,
                AnnotationKind kind) {
            form.saveState();
            form.beginText();
            try {
                form.setColorFill(BaseColor.BLACK);
                for (TextAnnotation annotation : band.textAnnotations) {
                    if (annotation.kind != kind) {
                        continue;
                    }
                    AnnotationLayout layout = canvas.annotationLayout(annotation, rect);
                    form.setFontAndSize(kdaFont, annotation.fontSize);
                    for (AnnotationTextLine line : layout.lines) {
                        form.showTextAligned(PdfContentByte.ALIGN_LEFT,
                                line.text,
                                (float) line.baselineStartX - groupLeft,
                                groupBottom - (float) line.baselineStartY,
                                (float) annotation.angleDeg);
                    }
                }
            } finally {
                form.endText();
                form.restoreState();
            }
        }

        private void drawBandNamesForm() {
            List<AnnotationLayout> layouts = new ArrayList<AnnotationLayout>();
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            int baseY = Controller.TOP_MARGIN;

            for (BandCrop band : canvas.controller.bands()) {
                Rectangle rect = canvas.layoutRectFor(band, baseY);
                for (TextAnnotation annotation : band.textAnnotations) {
                    if (annotation.kind != AnnotationKind.BAND_NAME) {
                        continue;
                    }
                    AnnotationLayout layout = canvas.annotationLayout(annotation, rect);
                    layouts.add(layout);
                    Rectangle2D bounds = layout.hitShape.getBounds2D();
                    minX = Math.min(minX, (float) bounds.getMinX());
                    maxX = Math.max(maxX, (float) bounds.getMaxX());
                    minY = Math.min(minY, (float) bounds.getMinY());
                    maxY = Math.max(maxY, (float) bounds.getMaxY());
                }
                baseY += canvas.bandLayoutAdvance(band, rect);
            }

            if (layouts.isEmpty()) {
                return;
            }
            minX -= GROUP_MARGIN;
            minY -= GROUP_MARGIN;
            maxX += GROUP_MARGIN;
            maxY += GROUP_MARGIN;
            PdfTemplate namesForm = page.createTemplate(maxX - minX, maxY - minY);
            namesForm.saveState();
            namesForm.beginText();
            try {
                namesForm.setColorFill(BaseColor.BLACK);
                for (AnnotationLayout layout : layouts) {
                    namesForm.setFontAndSize(bandNameFont,
                            layout.annotation.fontSize);
                    for (AnnotationTextLine line : layout.lines) {
                        namesForm.showTextAligned(PdfContentByte.ALIGN_CENTER,
                                line.text,
                                (float) layout.pivotX - minX,
                                maxY - (float) line.baselineStartY,
                                (float) layout.annotation.angleDeg);
                    }
                }
            } finally {
                namesForm.endText();
                namesForm.restoreState();
            }
            page.addTemplate(namesForm, minX, pageHeight - maxY);
        }

        private void drawFreeText() {
            for (TextAnnotation annotation : canvas.controller.freeTextAnnotations()) {
                AnnotationLayout layout = canvas.annotationLayout(annotation, null);
                page.saveState();
                page.beginText();
                try {
                    page.setColorFill(BaseColor.BLACK);
                    page.setFontAndSize(kdaFont, annotation.fontSize);
                    for (AnnotationTextLine line : layout.lines) {
                        page.showTextAligned(PdfContentByte.ALIGN_LEFT,
                                line.text,
                                (float) line.baselineStartX,
                                pageHeight - (float) line.baselineStartY,
                                (float) annotation.angleDeg);
                    }
                } finally {
                    page.endText();
                    page.restoreState();
                }
            }
        }

        private void drawFreeLines() {
            for (LineAnnotation line : canvas.controller.lineAnnotations()) {
                page.saveState();
                try {
                    page.setColorStroke(BaseColor.BLACK);
                    page.setLineWidth(Controller.FREE_LINE_STROKE_WIDTH);
                    page.moveTo((float) line.x1, pageHeight - (float) line.y1);
                    page.lineTo((float) line.x2, pageHeight - (float) line.y2);
                    page.stroke();
                } finally {
                    page.restoreState();
                }
            }
        }

    }

    private static final class ParsedCoordinateLog {
        int formatVersion;
        String pluginVersion;
        final List<LoggedMarkerSet> markerSets = new ArrayList<LoggedMarkerSet>();
        final Map<String, LoggedMarkerSet> markerSetsById =
                new LinkedHashMap<String, LoggedMarkerSet>();
        final List<LoggedCrop> crops = new ArrayList<LoggedCrop>();
    }

    private static final class LoggedMarkerSet {
        final String id;
        MarkerSourceType sourceType;
        String sourcePath;
        int sourceWidth;
        int sourceHeight;
        boolean hasSourceDimensions;
        String dimensionDescription;
        final List<LoggedMarker> markers = new ArrayList<LoggedMarker>();

        LoggedMarkerSet(String id) {
            this.id = id;
        }
    }

    private static final class LoggedMarker {
        final String label;
        final double xAbs;
        final double yAbs;

        LoggedMarker(String label, double xAbs, double yAbs) {
            this.label = label;
            this.xAbs = xAbs;
            this.yAbs = yAbs;
        }
    }

    private static final class LoggedCrop {
        String name;
        String sourcePath;
        int sourceWidth;
        int sourceHeight;
        boolean hasSourceDimensions;
        double cropX;
        double cropY;
        int cropWidth;
        int cropHeight;
        double cropAngleDeg;
        boolean hasOrigin;
        boolean hasSize;
        boolean hasAngle;
        final Point2D[] loggedCorners = new Point2D[4];
        ReconstructedGeometry parameterGeometry;
        CornerFit cornerFit;
        String parameterIssue;
        String cornerIssue;
        boolean geometryConflict;
        double geometryDiscrepancy;
        String geometryDescription;
        String dimensionDescription;
        String markerSetId;
        final List<LoggedCropMarker> markers = new ArrayList<LoggedCropMarker>();
        final List<LoggedCropMarker> resolvedMarkers = new ArrayList<LoggedCropMarker>();
        String markerDescription;

        int cornerCount() {
            int count = 0;
            for (Point2D corner : loggedCorners) {
                if (corner != null) {
                    count++;
                }
            }
            return count;
        }

        boolean hasResolvedGeometry() {
            return hasOrigin && hasSize && cropWidth > 0 && cropHeight > 0;
        }
    }

    private static final class LoggedCropMarker {
        final String label;
        final Double sourceXAbs;
        final Double sourceYAbs;
        final Double gelXAbs;
        final Double gelYAbs;
        final Double yInCrop;
        final int lineNumber;

        LoggedCropMarker(String label, Double sourceXAbs, Double sourceYAbs,
                Double gelXAbs, Double gelYAbs, Double yInCrop, int lineNumber) {
            this.label = label;
            this.sourceXAbs = sourceXAbs;
            this.sourceYAbs = sourceYAbs;
            this.gelXAbs = gelXAbs;
            this.gelYAbs = gelYAbs;
            this.yInCrop = yInCrop;
            this.lineNumber = lineNumber;
        }

        static LoggedCropMarker resolved(String label, double sourceXAbs,
                double sourceYAbs, double gelXAbs, double gelYAbs, double yInCrop) {
            return new LoggedCropMarker(label, Double.valueOf(sourceXAbs),
                    Double.valueOf(sourceYAbs), Double.valueOf(gelXAbs),
                    Double.valueOf(gelYAbs), Double.valueOf(yInCrop), -1);
        }

        boolean hasAnyCoordinates() {
            return sourceXAbs != null || sourceYAbs != null || gelXAbs != null
                    || gelYAbs != null || yInCrop != null;
        }
    }

    private static final class ReconstructionImageRequest {
        final String originalPath;
        int expectedWidth;
        int expectedHeight;
        final List<String> roles = new ArrayList<String>();
        boolean required;
        boolean dimensionMismatch;
        boolean aspectRatioMismatch;
        boolean conflictingLoggedDimensions;
        LoadedImage loadedImage;

        ReconstructionImageRequest(String originalPath) {
            this.originalPath = originalPath;
        }

        void addRole(String role) {
            if (!roles.contains(role)) {
                roles.add(role);
            }
        }

        void addExpectedDimensions(int width, int height) {
            if (width <= 0 || height <= 0) {
                return;
            }
            if (!hasExpectedDimensions()) {
                expectedWidth = width;
                expectedHeight = height;
            } else if (expectedWidth != width || expectedHeight != height) {
                conflictingLoggedDimensions = true;
            }
        }

        boolean hasExpectedDimensions() {
            return expectedWidth > 0 && expectedHeight > 0;
        }
    }

    private static final class ReconstructedGeometry {
        final double x;
        final double y;
        final int width;
        final int height;
        final double angleDeg;

        ReconstructedGeometry(double x, double y, int width, int height, double angleDeg) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.angleDeg = angleDeg;
        }
    }

    private static final class CornerFit {
        final ReconstructedGeometry geometry;
        final int suppliedCornerCount;
        final int inferredCornerIndex;
        final double maximumAdjustment;

        CornerFit(ReconstructedGeometry geometry, int suppliedCornerCount,
                int inferredCornerIndex, double maximumAdjustment) {
            this.geometry = geometry;
            this.suppliedCornerCount = suppliedCornerCount;
            this.inferredCornerIndex = inferredCornerIndex;
            this.maximumAdjustment = maximumAdjustment;
        }

        String description() {
            StringBuilder description = new StringBuilder();
            description.append("closest rectangle fitted from ")
                    .append(suppliedCornerCount).append(" crop corners");
            if (inferredCornerIndex >= 0) {
                String[] names = {"top-left", "top-right", "bottom-right", "bottom-left"};
                description.append("; missing ").append(names[inferredCornerIndex])
                        .append(" corner inferred");
            }
            description.append(" (maximum adjustment ")
                    .append(Controller.formatDisplayCoordinate(maximumAdjustment))
                    .append(" px)");
            return description.toString();
        }
    }

    private static final class AnnotatedMarkerImage {
        final ImagePlus imagePlus;
        final KdaMarkerSet markerSet;
        final double scaleX;
        final double scaleY;

        final List<ReconstructionCropPreview> crops =
                new ArrayList<ReconstructionCropPreview>();

        AnnotatedMarkerImage(ImagePlus imagePlus, KdaMarkerSet markerSet,
                double scaleX, double scaleY, List<LoggedCrop> loggedCrops) {
            this.imagePlus = imagePlus;
            this.markerSet = markerSet;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            for (int i = 0; i < loggedCrops.size(); i++) {
                LoggedCrop crop = loggedCrops.get(i);
                if (markerSet.id.equals(crop.markerSetId)) {
                    crops.add(new ReconstructionCropPreview(crop, i + 1));
                }
            }
        }
    }

    private static final class ReconstructionCropPreview {
        final LoggedCrop crop;
        final int bandNumber;

        ReconstructionCropPreview(LoggedCrop crop, int bandNumber) {
            this.crop = crop;
            this.bandNumber = bandNumber;
        }
    }

    private static final class ReconstructionMarkerPreview {
        final String label;
        final double x;
        final double y;
        double connectorDistanceToLeft;
        double connectorDistanceToRight;

        ReconstructionMarkerPreview(String label, double x, double y) {
            this.label = label;
            this.x = x;
            this.y = y;
        }

        void recordConnectorEndpoint(double endpointX) {
            double deltaX = endpointX - x;
            if (deltaX < 0.0) {
                connectorDistanceToLeft += -deltaX;
            } else {
                connectorDistanceToRight += deltaX;
            }
        }

        boolean prefersLabelOnLeft() {
            return connectorDistanceToRight > connectorDistanceToLeft;
        }
    }

    private static final class AnnotatedCropSourceImage {
        final ImagePlus imagePlus;
        final List<ReconstructionCropPreview> crops;

        AnnotatedCropSourceImage(ImagePlus imagePlus,
                List<ReconstructionCropPreview> crops) {
            this.imagePlus = imagePlus;
            this.crops = new ArrayList<ReconstructionCropPreview>(crops);
        }
    }

    private enum MarkerSourceType {
        GEL_IMAGE("Gel image", "Gel"),
        MARKER_IMAGE("kDa marker image", "Marker");

        final String displayName;
        final String shortName;

        MarkerSourceType(String displayName, String shortName) {
            this.displayName = displayName;
            this.shortName = shortName;
        }
    }

    private static final class LoadedImage {
        final ImagePlus imagePlus;
        final String path;

        LoadedImage(ImagePlus imagePlus, String path) {
            this.imagePlus = imagePlus;
            this.path = path;
        }
    }

    private static final class KdaMarkerSet {
        final String id;
        final MarkerSourceType sourceType;
        final String sourcePath;
        final int sourceWidth;
        final int sourceHeight;
        final List<KdaMarker> markers = new ArrayList<KdaMarker>();
        boolean frozen;

        KdaMarkerSet(String id, MarkerSourceType sourceType, String sourcePath,
                int sourceWidth, int sourceHeight) {
            this.id = id;
            this.sourceType = sourceType;
            this.sourcePath = sourcePath;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
        }

        boolean matchesSource(MarkerSourceType type, String path, int width, int height) {
            return sourceType == type && Controller.samePath(sourcePath, path)
                    && sourceWidth == width && sourceHeight == height;
        }

        KdaMarkerSet editableCopy(String newId) {
            KdaMarkerSet copy = new KdaMarkerSet(newId, sourceType, sourcePath,
                    sourceWidth, sourceHeight);
            copy.markers.addAll(markers);
            return copy;
        }
    }

    private static final class KdaMarker {
        final double xAbs;
        final double yAbs;
        final String label;

        KdaMarker(double xAbs, double yAbs, String label) {
            this.xAbs = xAbs;
            this.yAbs = yAbs;
            this.label = label;
        }
    }

    private static final class Point2D {
        final double x;
        final double y;

        Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class CropMarker {
        final String label;
        final double yInCrop;
        final double sourceXAbs;
        final double sourceYAbs;
        final double gelXAbs;
        final double gelYAbs;

        CropMarker(String label, double yInCrop,
                double sourceXAbs, double sourceYAbs,
                double gelXAbs, double gelYAbs) {
            this.label = label;
            this.yInCrop = yInCrop;
            this.sourceXAbs = sourceXAbs;
            this.sourceYAbs = sourceYAbs;
            this.gelXAbs = gelXAbs;
            this.gelYAbs = gelYAbs;
        }
    }

    private static final class MarkerMapping {
        final int markerWidth;
        final int markerHeight;
        final int gelWidth;
        final int gelHeight;
        final double scaleX;
        final double scaleY;
        final boolean dimensionsDiffer;
        final boolean aspectRatioMismatch;

        MarkerMapping(int markerWidth, int markerHeight, int gelWidth, int gelHeight,
                double scaleX, double scaleY,
                boolean dimensionsDiffer, boolean aspectRatioMismatch) {
            this.markerWidth = markerWidth;
            this.markerHeight = markerHeight;
            this.gelWidth = gelWidth;
            this.gelHeight = gelHeight;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.dimensionsDiffer = dimensionsDiffer;
            this.aspectRatioMismatch = aspectRatioMismatch;
        }
    }

    private static final class BandCrop {
        final BufferedImage image;
        final List<CropMarker> markers;
        final List<TextAnnotation> textAnnotations = new ArrayList<TextAnnotation>();
        final String label;
        final String sourcePath;
        final int sourceWidth;
        final int sourceHeight;
        final KdaMarkerSet markerSet;
        final MarkerMapping markerMapping;
        int displayWidth;
        double xOffset;
        double yOffset;
        double cropX;
        double cropY;
        int cropWidth;
        int cropHeight;
        double cropAngleDeg;

        BandCrop(BufferedImage image, List<CropMarker> markers, String label, int displayWidth,
                String sourcePath, int sourceWidth, int sourceHeight,
                KdaMarkerSet markerSet, MarkerMapping markerMapping) {
            this.image = image;
            this.markers = markers;
            this.label = label;
            this.displayWidth = displayWidth;
            this.sourcePath = sourcePath;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.markerSet = markerSet;
            this.markerMapping = markerMapping;
            if (label != null && label.length() > 0) {
                textAnnotations.add(TextAnnotation.bandName(this, label));
            }
            for (CropMarker marker : markers) {
                textAnnotations.add(TextAnnotation.mwValue(this, marker));
            }
        }

        double scale() {
            return displayWidth / (double) image.getWidth();
        }

        int displayHeight() {
            return Math.max(1, (int) Math.round(image.getHeight() * scale()));
        }
    }

    private static final class CropResult {
        final ImagePlus imagePlus;
        final double x;
        final double y;
        final int width;
        final int height;
        final double angleDeg;

        CropResult(ImagePlus imagePlus, double x, double y, int width, int height, double angleDeg) {
            this.imagePlus = imagePlus;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.angleDeg = angleDeg;
        }
    }
}
