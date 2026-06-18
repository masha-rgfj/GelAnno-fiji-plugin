# WBTool.py
# Drop into:  Fiji.app/plugins/WBTool.py
# Run via:    Plugins > WBTool
#
# Requires:   Fiji (https://fiji.sc) — no extra dependencies
# Formats:    TIFF, PNG, JPEG
#
# If you use WBTool in published research, please cite:
# Maria A. Pirozhkova (Masha) (RGFJ).
# *WBTool: A GUI tool for annotating and assembling Western blot figures* (2026).
# GitHub: https://github.com/masha-rgfj/western-blot-tool-package

from ij import IJ, ImagePlus, Prefs
from ij.gui import GenericDialog, Overlay, Line, TextRoi
from ij.process import ImageConverter

import java.awt as awt
from java.awt import (Color, Font, BasicStroke, RenderingHints,
                      BorderLayout, Dimension)
from java.awt.event import ActionListener, MouseAdapter, MouseEvent, KeyEvent
from javax.swing import (JFrame, JPanel, JButton, JLabel, JSeparator,
                         JScrollPane, JList, DefaultListModel,
                         ListSelectionModel, BorderFactory,
                         JOptionPane, BoxLayout, Box, JFileChooser,
                         KeyStroke, AbstractAction)
from javax.swing.filechooser import FileNameExtensionFilter

from com.itextpdf.text import Document as PdfDocument, Image as PdfImage
from com.itextpdf.text.pdf import PdfWriter

import math
from java.io import FileOutputStream
from java.awt.image import BufferedImage
from javax.imageio import ImageIO
import java.io.File as JFile
import java.io.ByteArrayOutputStream as ByteArrayOutputStream

# ── Constants ────────────────────────────────────────────────────────────────
TICK_LEN     = 20
TICK_GAP     = 4
LEFT_MARGIN  = 90
BAND_GAP     = 30
TOP_MARGIN   = 60
LABEL_PAD    = 8
FIG_INIT_W   = 800
FONT_KDA     = Font("Arial", Font.PLAIN, 11)
FONT_NAME    = Font("Arial", Font.BOLD,  12)
FONT_SAMPLE  = Font("Arial", Font.PLAIN, 11)
FONT_ANNOT   = Font("Arial", Font.PLAIN, 11)
FONT_BANDANN = Font("Arial", Font.PLAIN, 11)

COLOR_ACTIVE   = Color(255, 180, 0)
COLOR_INACTIVE = None

HIT_RADIUS   = 8
HANDLE_SIZE  = 7
PASTE_OFFSET = 12
SL_HIT_R     = 12    # hit radius for sample label anchor point
BA_HIT_R     = 8     # hit radius for right-side band annotation markers


# ── Helpers ──────────────────────────────────────────────────────────────────
def sized_font(base_font, size):
    return Font(base_font.getName(), base_font.getStyle(),
                int(max(5, min(72, round(size)))))

def ask_string(title, prompt, default=""):
    gd = GenericDialog(title)
    gd.addStringField(prompt, default, 20)
    gd.showDialog()
    if gd.wasCanceled():
        return None
    return gd.getNextString().strip()

def ask_float(title, prompt, default=0.0):
    gd = GenericDialog(title)
    gd.addNumericField(prompt, default, 1)
    gd.setAlwaysOnTop(True)
    gd.toFront()
    gd.showDialog()
    if gd.wasCanceled():
        return None
    return gd.getNextNumber()

def ask_int(title, prompt, default=300):
    gd = GenericDialog(title)
    gd.addNumericField(prompt, default, 0)
    gd.showDialog()
    if gd.wasCanceled():
        return None
    return int(gd.getNextNumber())

def crop_imp(imp, x, y, w, h):
    imp.setRoi(x, y, w, h)
    cropped = imp.crop()
    imp.killRoi()
    return cropped

def dist2(ax, ay, bx, by):
    return math.sqrt((ax - bx) ** 2 + (ay - by) ** 2)

def sl_anchor(sl, ix, iy, dw):
    """Return the scene (x, y) anchor point of a sample label."""
    cx = ix + int(round(sl["x_frac"] * dw))
    cy = iy - LABEL_PAD
    return float(cx), float(cy)


# ── Data classes ─────────────────────────────────────────────────────────────
class Band(object):
    def __init__(self, crop_imp, kda_markers, protein_name, width=None):
        self.orig_imp     = crop_imp
        self.orig_w       = crop_imp.getWidth()
        self.orig_h       = crop_imp.getHeight()
        self.kda_markers  = kda_markers
        self.protein_name = protein_name
        self.display_w    = width if width else self.orig_w
        self.sample_labels = []
        self.band_annots   = []
        self.protein_size  = FONT_NAME.getSize()
        self.protein_dx_frac = 0.0
        self.protein_dy_frac = 0.0

    def scale(self):
        return float(self.display_w) / float(self.orig_w)

    def display_h(self):
        return int(round(self.orig_h * self.scale()))


class HLine(object):
    def __init__(self, x0, y, x1, band_ref=None,
                 x0_frac=None, x1_frac=None, y_frac=None):
        self.x0 = float(x0)
        self.y  = float(y)
        self.x1 = float(x1)
        self.band_ref = band_ref
        self.x0_frac  = x0_frac
        self.x1_frac  = x1_frac
        self.y_frac   = y_frac

    def shallow_copy(self):
        return HLine(self.x0, self.y, self.x1, self.band_ref,
                     self.x0_frac, self.x1_frac, self.y_frac)


class FreeText(object):
    def __init__(self, x, y, text):
        self.x    = float(x)
        self.y    = float(y)
        self.text = text
        self.font_size = FONT_ANNOT.getSize()

    def shallow_copy(self):
        ft = FreeText(self.x, self.y, self.text)
        ft.font_size = self.font_size
        return ft


# ── Figure renderer ──────────────────────────────────────────────────────────
class FigureRenderer(object):

    def band_extra_bottom(self, b):
        dh = b.display_h()
        size = getattr(b, "protein_size", FONT_NAME.getSize())
        dy = getattr(b, "protein_dy_frac", 0.0) * dh
        if b.band_annots:
            baseline_y = dh + size + 5 + dy
        else:
            baseline_y = dh / 2.0 + size / 2.0 + dy
        text_bottom = baseline_y + size * 0.30
        return max(0, int(math.ceil(text_bottom - dh)) + 6)

    def band_step(self, b):
        return b.display_h() + BAND_GAP + self.band_extra_bottom(b)

    def band_img_rect(self, band_idx, bands):
        y_cursor = TOP_MARGIN
        for i, b in enumerate(bands):
            dh = b.display_h()
            if i == band_idx:
                return (LEFT_MARGIN, y_cursor, b.display_w, dh)
            y_cursor += self.band_step(b)
        return None

    def recompute_hline(self, hl, bands):
        if hl.band_ref is None or hl.band_ref not in bands:
            hl.band_ref = None
            return
        idx  = bands.index(hl.band_ref)
        rect = self.band_img_rect(idx, bands)
        if rect is None:
            return
        img_x, img_y, dw, dh = rect
        hl.x0 = img_x + hl.x0_frac * dw
        hl.x1 = img_x + hl.x1_frac * dw
        hl.y  = img_y + hl.y_frac  * dh

    def render(self, bands, hlines, freetexts, canvas_w,
               selected=None, edit_mode=False):
        for hl in hlines:
            self.recompute_hline(hl, bands)

        if not bands and not hlines and not freetexts:
            bi = BufferedImage(canvas_w, 300, BufferedImage.TYPE_INT_RGB)
            g  = bi.createGraphics()
            g.setColor(Color.WHITE);  g.fillRect(0, 0, canvas_w, 300)
            g.setColor(Color.LIGHT_GRAY)
            g.setFont(Font("Arial", Font.ITALIC, 14))
            g.drawString("No bands yet — crop from the gel image", 40, 150)
            g.dispose()
            return bi

        total_h = TOP_MARGIN
        for b in bands:
            total_h += self.band_step(b)
        total_h = max(total_h, 300)

        bi = BufferedImage(canvas_w, total_h, BufferedImage.TYPE_INT_RGB)
        g  = bi.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setColor(Color.WHITE);  g.fillRect(0, 0, canvas_w, total_h)

        y_cursor = TOP_MARGIN
        for b in bands:
            sc    = b.scale()
            dw    = b.display_w
            dh    = b.display_h()
            ix    = LEFT_MARGIN
            iy    = y_cursor

            src_bi = b.orig_imp.getProcessor().convertToRGB().getBufferedImage()
            g.drawImage(src_bi, ix, iy, dw, dh, None)

            g.setColor(Color.BLACK)
            g.setStroke(BasicStroke(1.5))
            g.drawRect(ix, iy, dw, dh)

            for m in b.kda_markers:
                ty = iy + int(round(m["y_orig"] * sc))
                x1 = ix - 2;  x0 = x1 - TICK_LEN
                kda_font = sized_font(FONT_KDA, m.get("font_size", FONT_KDA.getSize()))
                g.setFont(kda_font)
                fm = g.getFontMetrics()
                is_sel = (edit_mode and isinstance(selected, tuple)
                          and selected[0] == "kda"
                          and selected[1] is b
                          and selected[2] is m)
                g.setColor(Color.BLACK)
                g.setStroke(BasicStroke(1.2))
                g.drawLine(x0, ty, x1, ty)
                lbl = "%g" % m["kda"]
                lw  = fm.stringWidth(lbl)
                if is_sel:
                    g.setColor(Color(0, 100, 220))
                kda_base_y = ty + (fm.getAscent() - fm.getDescent()) // 2
                g.drawString(lbl, x0 - TICK_GAP - lw, kda_base_y)
                if is_sel:
                    th = fm.getHeight()
                    g.setStroke(BasicStroke(1.0,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, [3, 3], 0))
                    g.drawRect(x0 - TICK_GAP - lw - 2, ty - th // 2 - 2,
                               lw + 4, th + 4)
                    g.setStroke(BasicStroke(1.0))

            for ba in b.band_annots:
                ty = iy + int(round(ba["y_frac"] * dh))
                x0 = ix + dw + 2;  x1 = x0 + TICK_LEN
                ba_font = sized_font(FONT_BANDANN,
                                     ba.get("font_size", FONT_BANDANN.getSize()))
                g.setFont(ba_font)
                fm_ba = g.getFontMetrics()
                is_sel = (edit_mode and isinstance(selected, tuple)
                          and selected[0] == "ba"
                          and selected[1] is b
                          and selected[2] is ba)
                g.setColor(Color(0, 100, 220) if is_sel else Color.BLACK)
                g.setStroke(BasicStroke(1.2))
                g.drawLine(x0, ty, x1, ty)
                ba_base_y = ty + (fm_ba.getAscent() - fm_ba.getDescent()) // 2
                g.drawString(ba["text"], x1 + TICK_GAP,
                             ba_base_y)
                if is_sel:
                    tw = fm_ba.stringWidth(ba["text"])
                    th = fm_ba.getHeight()
                    g.setStroke(BasicStroke(1.0,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, [3, 3], 0))
                    g.drawRect(x1 + TICK_GAP - 2, ty - th // 2 - 2,
                               tw + 4, th + 4)
                    g.setStroke(BasicStroke(1.0))

            name_font = sized_font(FONT_NAME, getattr(b, "protein_size",
                                                      FONT_NAME.getSize()))
            g.setFont(name_font)
            fm2 = g.getFontMetrics()
            name_sel = (edit_mode and isinstance(selected, tuple)
                        and selected[0] == "protein"
                        and selected[1] is b)
            g.setColor(Color(0, 100, 220) if name_sel else Color.BLACK)
            if b.band_annots:
                name_w = fm2.stringWidth(b.protein_name)
                name_x = ix + dw // 2 - name_w // 2
                name_y = iy + dh + fm2.getAscent() + 5
            else:
                name_w = fm2.stringWidth(b.protein_name)
                name_x = ix + dw + 10
                name_y = iy + dh // 2 + fm2.getAscent() // 2
            name_x += int(round(getattr(b, "protein_dx_frac", 0.0) * dw))
            name_y += int(round(getattr(b, "protein_dy_frac", 0.0) * dh))
            g.drawString(b.protein_name, name_x, name_y)
            if name_sel:
                th = fm2.getHeight()
                g.setStroke(BasicStroke(1.0,
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, [3, 3], 0))
                g.drawRect(name_x - 2, name_y - th, name_w + 4, th + 4)
                g.setStroke(BasicStroke(1.0))

            # sample labels
            for sl in b.sample_labels:
                ax, ay = sl_anchor(sl, ix, iy, dw)
                sl_font = sized_font(FONT_SAMPLE,
                                     sl.get("font_size", FONT_SAMPLE.getSize()))
                # center-anchor: shift left by half text width
                text_w = g.getFontMetrics(sl_font).stringWidth(sl["text"])
                lx = int(ax) - int(text_w / 2.0)
                ly = int(ay)

                # is this label selected?
                is_sel = (edit_mode and isinstance(selected, tuple)
                          and selected[0] == "sl"
                          and selected[1] is b
                          and selected[2] is sl)

                old = g.getTransform()
                g.setFont(sl_font)
                g.setColor(Color(0, 100, 220) if is_sel else Color.BLACK)
                g.translate(lx, ly)
                g.rotate(math.radians(-sl["angle"]))
                g.drawString(sl["text"], 0, 0)
                g.setTransform(old)

                # draw anchor dot when selected
                if is_sel:
                    g.setColor(Color(0, 100, 220))
                    r = 4
                    g.fillOval(int(ax) - r, int(ay) - r, r*2, r*2)

            y_cursor += self.band_step(b)

        # H-lines
        hs = HANDLE_SIZE
        for hl in hlines:
            is_sel = edit_mode and (hl is selected)
            g.setColor(Color(0, 100, 220) if is_sel else Color.BLACK)
            g.setStroke(BasicStroke(1.5))
            g.drawLine(int(hl.x0), int(hl.y), int(hl.x1), int(hl.y))
            if is_sel:
                g.fillRect(int(hl.x0) - hs//2, int(hl.y) - hs//2, hs, hs)
                g.fillRect(int(hl.x1) - hs//2, int(hl.y) - hs//2, hs, hs)

        # Free texts
        for ft in freetexts:
            is_sel = edit_mode and (ft is selected)
            ft_font = sized_font(FONT_ANNOT, getattr(ft, "font_size",
                                                     FONT_ANNOT.getSize()))
            g.setFont(ft_font)
            fm3 = g.getFontMetrics()
            g.setColor(Color(0, 100, 220) if is_sel else Color.BLACK)
            g.drawString(ft.text, int(ft.x), int(ft.y))
            if is_sel:
                tw = fm3.stringWidth(ft.text)
                th = fm3.getHeight()
                g.setStroke(BasicStroke(1.0,
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, [3, 3], 0))
                g.drawRect(int(ft.x) - 2, int(ft.y) - th, tw + 4, th + 4)
                g.setStroke(BasicStroke(1.0))

        g.dispose()
        return bi


# ── Figure canvas ─────────────────────────────────────────────────────────────
class FigureCanvas(JPanel):
    def __init__(self, controller):
        JPanel.__init__(self)
        self.ctrl         = controller
        self.bi           = None
        self.mode         = None
        self._drag_start  = None
        self._drag_last   = None
        self._drag_target = None

        self.setFocusable(True)
        canvas = self

        # ── Key bindings ──────────────────────────────────────────────────
        copy_key  = KeyStroke.getKeyStroke(KeyEvent.VK_C, awt.event.InputEvent.CTRL_DOWN_MASK)
        paste_key = KeyStroke.getKeyStroke(KeyEvent.VK_V, awt.event.InputEvent.CTRL_DOWN_MASK)
        up_key    = KeyStroke.getKeyStroke(KeyEvent.VK_UP,    0)
        down_key  = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,  0)
        left_key  = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,  0)
        right_key = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)
        del_key   = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,     0)
        bsp_key   = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)

        class _CopyAction(AbstractAction):
            def actionPerformed(self, e):
                canvas.ctrl.copy_selected()

        class _PasteAction(AbstractAction):
            def actionPerformed(self, e):
                canvas.ctrl.paste_clipboard()

        class _NudgeAction(AbstractAction):
            def __init__(self, axis, delta):
                AbstractAction.__init__(self)
                self.axis  = axis
                self.delta = delta
            def actionPerformed(self, e):
                if canvas.ctrl.edit_mode_active:
                    canvas.ctrl.nudge_annot(self.axis, self.delta)

        class _DeleteAction(AbstractAction):
            def actionPerformed(self, e):
                if canvas.ctrl.edit_mode_active:
                    canvas.ctrl.delete_selected_annot()

        im = self.getInputMap(JPanel.WHEN_FOCUSED)
        am = self.getActionMap()
        im.put(copy_key,  "copy");   am.put("copy",  _CopyAction())
        im.put(paste_key, "paste");  am.put("paste", _PasteAction())
        im.put(up_key,    "up");     am.put("up",    _NudgeAction("y",  -2))
        im.put(down_key,  "down");   am.put("down",  _NudgeAction("y",  +2))
        im.put(left_key,  "left");   am.put("left",  _NudgeAction("x0", -2))
        im.put(right_key, "right");  am.put("right", _NudgeAction("x1", +2))
        im.put(del_key,   "delete"); am.put("delete", _DeleteAction())
        im.put(bsp_key,   "bspace"); am.put("bspace", _DeleteAction())

        # ── Mouse ─────────────────────────────────────────────────────────
        class _Mouse(MouseAdapter):
            def mousePressed(self, event):
                canvas.requestFocusInWindow()
                if event.getButton() != MouseEvent.BUTTON1:
                    return
                x, y = event.getX(), event.getY()

                if canvas.mode == "draw_line":
                    canvas._drag_start = (x, y)

                elif canvas.mode == "sample_label":
                    canvas.ctrl.place_sample_label(x, y)

                elif canvas.mode == "band_annot":
                    canvas.ctrl.place_band_annot(x, y)

                elif canvas.mode == "add_text":
                    canvas.ctrl.place_free_text(x, y)
                    canvas.mode = None

                elif canvas.mode == "edit":
                    target = canvas.ctrl.hit_test(x, y)
                    canvas._drag_target = target
                    canvas._drag_last   = (x, y)
                    if target is None:
                        canvas.ctrl.edit_select(x, y)

            def mouseClicked(self, event):
                if canvas.mode == "edit" and event.getClickCount() == 2:
                    a = canvas.ctrl.selected_annot
                    if isinstance(a, FreeText):
                        canvas.ctrl.rename_selected_text()
                    elif isinstance(a, tuple) and a[0] == "sl":
                        canvas.ctrl.rename_selected_sl()
                    elif isinstance(a, tuple) and a[0] == "ba":
                        canvas.ctrl.rename_selected_ba()
                    elif isinstance(a, tuple) and a[0] == "protein":
                        canvas.ctrl.rename_selected_protein()

            def mouseDragged(self, event):
                x, y = event.getX(), event.getY()
                if canvas.mode == "draw_line" and canvas._drag_start:
                    canvas.ctrl.preview_draw_line(
                        canvas._drag_start[0], canvas._drag_start[1], x)
                elif canvas.mode == "edit" and canvas._drag_target and canvas._drag_last:
                    dx = x - canvas._drag_last[0]
                    dy = y - canvas._drag_last[1]
                    canvas.ctrl.drag_annot(canvas._drag_target, dx, dy)
                    canvas._drag_last = (x, y)

            def mouseReleased(self, event):
                if canvas.mode == "draw_line" and canvas._drag_start:
                    x0, y0 = canvas._drag_start
                    canvas.ctrl.finish_draw_line(x0, y0, event.getX())
                    canvas._drag_start = None
                elif canvas.mode == "edit":
                    canvas.ctrl.sync_fractions_after_drag()
                    canvas._drag_target = None
                    canvas._drag_last   = None

        ml = _Mouse()
        self.addMouseListener(ml)
        self.addMouseMotionListener(ml)

    def set_image(self, bi):
        self.bi = bi
        self.setPreferredSize(Dimension(bi.getWidth(), bi.getHeight()))
        self.revalidate()
        self.repaint()

    def paintComponent(self, g):
        super(FigureCanvas, self).paintComponent(g)
        if self.bi:
            g.drawImage(self.bi, 0, 0, None)


# ── Main controller ───────────────────────────────────────────────────────────
class WBTool(ActionListener):
    def __init__(self):
        self._last_dir            = Prefs.get("wbtool.last_dir", None)
        self.bands                = []
        self.hlines               = []
        self.freetexts            = []
        self.sel_idx              = -1
        self.gel_imp              = None
        self.kda_markers          = []
        self.kda_mode_active      = False
        self._waiting_for_crop    = False
        self._crop_was_marking    = False
        self._default_label_angle = 45.0
        self.default_font_sizes    = {
            "kda": FONT_KDA.getSize(),
            "protein": FONT_NAME.getSize(),
            "sample": FONT_SAMPLE.getSize(),
            "free": FONT_ANNOT.getSize(),
            "band": FONT_BANDANN.getSize(),
        }
        self.edit_mode_active     = False
        self.selected_annot       = None   # HLine | FreeText | ("sl", band, sl_dict)
        self.clipboard            = None
        self.renderer             = FigureRenderer()
        self._gel_mouse_listener           = None
        self._saved_mouse_listeners        = []
        self._saved_mouse_motion_listeners = []

        self._build_ui()
        self._refresh_figure()

    # ── UI ────────────────────────────────────────────────────────────────
    def _build_ui(self):
        self.frame = JFrame("WBTool — Western Blot Figure Tool")
        self.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE)
        self.frame.setLayout(BorderLayout())

        ctrl = JPanel()
        ctrl.setLayout(BoxLayout(ctrl, BoxLayout.Y_AXIS))
        ctrl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8))
        ctrl.setPreferredSize(Dimension(200, 860))

        def section(t):
            lbl = JLabel(t)
            lbl.setFont(Font("Arial", Font.BOLD, 11))
            lbl.setAlignmentX(0.0)
            ctrl.add(lbl);  ctrl.add(JSeparator())
            ctrl.add(Box.createVerticalStrut(4))

        def btn(label, cmd):
            b = JButton(label)
            b.setActionCommand(cmd)
            b.addActionListener(self)
            b.setAlignmentX(0.0)
            b.setMaximumSize(Dimension(190, 28))
            ctrl.add(b);  ctrl.add(Box.createVerticalStrut(3))
            return b

        section("Image")
        btn("Open Gel Image...", "open_image")
        ctrl.add(Box.createVerticalStrut(6))

        section("kDa Markers")
        self.btn_mark_kda = btn("Mark kDa Bands", "toggle_mark_kda")
        self.btn_mark_kda.setOpaque(True)
        btn("Undo Last kDa", "undo_kda")
        btn("Clear All kDa", "clear_kda")
        ctrl.add(Box.createVerticalStrut(6))

        section("Crop -> Figure")
        self.btn_crop = btn("Crop Region -> Figure", "crop")
        self.btn_crop.setOpaque(True)
        ctrl.add(Box.createVerticalStrut(6))

        section("Bands (select to edit)")
        self.list_model = DefaultListModel()
        self.band_list  = JList(self.list_model)
        self.band_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        self.band_list.setFont(Font("Arial", Font.PLAIN, 11))
        self.band_list.addListSelectionListener(
            lambda e: self._on_list_select() if not e.getValueIsAdjusting() else None)
        lsp = JScrollPane(self.band_list)
        lsp.setPreferredSize(Dimension(185, 90))
        lsp.setMaximumSize(Dimension(190, 90))
        lsp.setAlignmentX(0.0)
        ctrl.add(lsp);  ctrl.add(Box.createVerticalStrut(4))
        btn("Move Up",    "band_up")
        btn("Move Down",  "band_down")
        btn("Set Width...", "set_width")
        btn("Width +10%", "width_inc")
        btn("Width -10%", "width_dec")
        btn("Remove Band","remove_band")
        ctrl.add(Box.createVerticalStrut(6))

        section("Annotations")
        btn("Draw H-Line (drag)",   "draw_line")
        self.btn_band_annot = btn("Add Band Tick", "toggle_band_annot")
        self.btn_band_annot.setOpaque(True)
        btn("Add Text (click)",     "add_text")
        self.btn_sample_label = btn("Add Sample Labels", "toggle_sample_label")
        self.btn_sample_label.setOpaque(True)
        self.btn_edit = btn("Edit Annotations", "toggle_edit")
        self.btn_edit.setOpaque(True)

        # D-pad
        from java.awt import GridLayout as GL
        dpad = JPanel(GL(3, 3, 2, 2))
        dpad.setMaximumSize(Dimension(90, 68))
        dpad.setAlignmentX(0.0)

        def _dpad_btn(label, cmd):
            b = JButton(label)
            b.setActionCommand(cmd)
            b.addActionListener(self)
            b.setFont(Font("Dialog", Font.PLAIN, 12))
            b.setMargin(awt.Insets(0, 0, 0, 0))
            return b

        empty = lambda: JLabel("")
        dpad.add(empty())
        dpad.add(_dpad_btn("^", "nudge_up"))
        dpad.add(empty())
        dpad.add(_dpad_btn("<", "nudge_x0_left"))
        dpad.add(empty())
        dpad.add(_dpad_btn(">", "nudge_x1_right"))
        dpad.add(empty())
        dpad.add(_dpad_btn("v", "nudge_down"))
        dpad.add(empty())

        ctrl.add(dpad)
        ctrl.add(Box.createVerticalStrut(4))
        size_pad = JPanel(GL(1, 2, 2, 2))
        size_pad.setMaximumSize(Dimension(90, 24))
        size_pad.setAlignmentX(0.0)
        size_pad.add(_dpad_btn("A-", "text_smaller"))
        size_pad.add(_dpad_btn("A+", "text_larger"))
        ctrl.add(size_pad)
        ctrl.add(Box.createVerticalStrut(4))
        btn("Rename",        "rename_text")
        btn("Delete Selected","delete_annot")
        ctrl.add(Box.createVerticalStrut(6))

        section("Export")
        btn("Export as PDF...", "export_pdf")
        btn("Export as PNG...", "export_img")
        btn("Clear Figure",     "clear_figure")

        ctrl.add(Box.createVerticalGlue())

        self.status_label = JLabel(" ")
        self.status_label.setFont(Font("Arial", Font.BOLD, 11))
        self.status_label.setForeground(Color(180, 100, 0))
        self.status_label.setAlignmentX(0.0)
        ctrl.add(JSeparator());  ctrl.add(Box.createVerticalStrut(4))
        ctrl.add(self.status_label)

        self.fig_canvas = FigureCanvas(self)
        self.fig_scroll = JScrollPane(self.fig_canvas)
        self.fig_scroll.setPreferredSize(Dimension(750, 860))

        self.frame.add(ctrl,            BorderLayout.WEST)
        self.frame.add(self.fig_scroll, BorderLayout.CENTER)
        self.frame.pack()
        screen = awt.Toolkit.getDefaultToolkit().getScreenSize()
        self.frame.setLocation(0, 0)
        self.frame.setSize(min(self.frame.getWidth(), screen.width // 2),
                           min(self.frame.getHeight(), screen.height))
        self.frame.setVisible(True)

    def _set_status(self, t): self.status_label.setText(t)
    def _clear_status(self):  self.status_label.setText(" ")

    def _clear_canvas_add_modes(self):
        if hasattr(self, "btn_sample_label"):
            self.btn_sample_label.setBackground(COLOR_INACTIVE)
            self.btn_sample_label.setText("Add Sample Labels")
        if hasattr(self, "btn_band_annot"):
            self.btn_band_annot.setBackground(COLOR_INACTIVE)
            self.btn_band_annot.setText("Add Band Tick")

    # ── Dispatcher ────────────────────────────────────────────────────────
    def actionPerformed(self, event):
        cmd = event.getActionCommand()
        {
            "open_image":          self.open_image,
            "toggle_mark_kda":     self.toggle_mark_kda,
            "undo_kda":            self.undo_last_kda,
            "clear_kda":           self.clear_all_kda,
            "crop":                self.start_crop,
            "band_up":             lambda: self.move_band(-1),
            "band_down":           lambda: self.move_band(+1),
            "set_width":           self.set_width_dialog,
            "width_inc":           lambda: self.bump_width(1.10),
            "width_dec":           lambda: self.bump_width(1/1.10),
            "remove_band":         self.remove_band,
            "draw_line":           self.enable_draw_line,
            "toggle_band_annot":   self.toggle_band_annot,
            "add_text":            self.enable_add_text,
            "toggle_sample_label": self.toggle_sample_label,
            "toggle_edit":         self.toggle_edit_mode,
            "nudge_x0_left":       lambda: self.nudge_annot("x0", -2),
            "nudge_x1_right":      lambda: self.nudge_annot("x1", +2),
            "nudge_up":            lambda: self.nudge_annot("y",  -2),
            "nudge_down":          lambda: self.nudge_annot("y",  +2),
            "text_smaller":        lambda: self.resize_text(-1),
            "text_larger":         lambda: self.resize_text(+1),
            "rename_text":         self.rename_selected,
            "delete_annot":        self.delete_selected_annot,
            "export_pdf":          self.export_pdf,
            "export_img":          self.export_image,
            "clear_figure":        self.clear_figure,
        }.get(cmd, lambda: None)()

    # ── Open image ────────────────────────────────────────────────────────
    def open_image(self):
        fc = JFileChooser()
        fc.setFileFilter(FileNameExtensionFilter(
            "Image files", ["tif","tiff","png","jpg","jpeg"]))
        if self._last_dir is not None:
            fc.setCurrentDirectory(JFile(self._last_dir))
        if fc.showOpenDialog(self.frame) != JFileChooser.APPROVE_OPTION:
            return
        chosen = fc.getSelectedFile()
        self._last_dir = chosen.getParent()
        Prefs.set("wbtool.last_dir", self._last_dir)
        path = chosen.getAbsolutePath()
        imp  = IJ.openImage(path)
        if imp is None:
            JOptionPane.showMessageDialog(self.frame,
                "Could not open: " + path, "Error",
                JOptionPane.ERROR_MESSAGE)
            return
        if imp.getType() != ImagePlus.COLOR_RGB:
            ImageConverter(imp).convertToRGB()
        if self.kda_mode_active:
            self._deactivate_kda_mode()
        self.gel_imp     = imp
        self.kda_markers = []
        imp.show()
        screen = awt.Toolkit.getDefaultToolkit().getScreenSize()
        win = imp.getWindow()
        if win is not None:
            win.setLocation(screen.width // 2, 0)
            win.setSize(screen.width // 2, screen.height)
        IJ.setTool("rectangle")

    # ── kDa mode ──────────────────────────────────────────────────────────
    def toggle_mark_kda(self):
        if self.gel_imp is None:
            JOptionPane.showMessageDialog(self.frame,
                "Open a gel image first.", "No image",
                JOptionPane.WARNING_MESSAGE)
            return
        if self.kda_mode_active: self._deactivate_kda_mode()
        else:                    self._activate_kda_mode()

    def _activate_kda_mode(self):
        self.kda_mode_active = True
        self.btn_mark_kda.setBackground(COLOR_ACTIVE)
        self.btn_mark_kda.setText("Stop Marking kDa")
        self._set_status("kDa marking active -- click gel")
        IJ.setTool("point")
        canvas = self.gel_imp.getCanvas()
        if canvas is None: return
        self._saved_mouse_listeners = canvas.getMouseListeners()
        for ml in self._saved_mouse_listeners:
            canvas.removeMouseListener(ml)
        self._saved_mouse_motion_listeners = canvas.getMouseMotionListeners()
        for ml in self._saved_mouse_motion_listeners:
            canvas.removeMouseMotionListener(ml)
        ctrl = self
        class _KdaMouse(MouseAdapter):
            def mousePressed(self, event):
                if not ctrl.kda_mode_active: return
                if event.getButton() != MouseEvent.BUTTON1: return
                event.consume()
                ic = ctrl.gel_imp.getCanvas()
                ctrl._on_gel_click(float(ic.offScreenY(event.getY())))
        self._gel_mouse_listener = _KdaMouse()
        canvas.addMouseListener(self._gel_mouse_listener)

    def _deactivate_kda_mode(self):
        self.kda_mode_active = False
        self.btn_mark_kda.setBackground(COLOR_INACTIVE)
        self.btn_mark_kda.setText("Mark kDa Bands")
        self._clear_status()
        if self.gel_imp is not None:
            canvas = self.gel_imp.getCanvas()
            if canvas is not None:
                if self._gel_mouse_listener is not None:
                    canvas.removeMouseListener(self._gel_mouse_listener)
                for ml in self._saved_mouse_listeners:
                    canvas.addMouseListener(ml)
                for ml in self._saved_mouse_motion_listeners:
                    canvas.addMouseMotionListener(ml)
        self._gel_mouse_listener           = None
        self._saved_mouse_listeners        = []
        self._saved_mouse_motion_listeners = []
        IJ.setTool("rectangle")

    def _on_gel_click(self, scene_y):
        gd = GenericDialog("kDa value")
        gd.addNumericField("Enter kDa for this band:", 0.0, 1)
        gd.setAlwaysOnTop(True);  gd.toFront();  gd.requestFocus()
        gd.showDialog()
        if gd.wasCanceled(): return
        val = gd.getNextNumber()
        self.kda_markers.append({"y_orig": scene_y, "kda": float(val),
                                 "font_size": self.default_font_sizes["kda"]})
        self.kda_markers.sort(key=lambda d: d["y_orig"])
        self._redraw_kda_overlay()
        n = len(self.kda_markers)
        self._set_status("kDa marking active -- %d mark%s" % (n, "s" if n!=1 else ""))

    def _redraw_kda_overlay(self):
        if self.gel_imp is None: return
        ov = Overlay()
        for m in self.kda_markers:
            y = m["y_orig"];  x1 = -2.0;  x0 = x1 - TICK_LEN
            tick = Line(x0, y, x1, y)
            tick.setStrokeColor(Color.RED);  tick.setStrokeWidth(1.5)
            ov.add(tick)
            lbl = TextRoi(x0 - 35, y - 7, "%g" % m["kda"], FONT_KDA)
            lbl.setStrokeColor(Color.RED);  ov.add(lbl)
        self.gel_imp.setOverlay(ov);  self.gel_imp.updateAndDraw()

    def undo_last_kda(self):
        if not self.kda_markers: return
        self.kda_markers.pop();  self._redraw_kda_overlay()
        if self.kda_mode_active:
            n = len(self.kda_markers)
            self._set_status("kDa marking active -- %d mark%s" % (n, "s" if n!=1 else ""))

    def clear_all_kda(self):
        self.kda_markers = [];  self._redraw_kda_overlay()
        if self.kda_mode_active:
            self._set_status("kDa marking active -- click gel")

    # ── Crop ──────────────────────────────────────────────────────────────
    def start_crop(self):
        if self.gel_imp is None:
            JOptionPane.showMessageDialog(self.frame, "Open a gel image first.",
                "No image", JOptionPane.WARNING_MESSAGE)
            return
        if not self._waiting_for_crop:
            self._waiting_for_crop = True
            self._crop_was_marking = self.kda_mode_active
            if self.kda_mode_active: self._deactivate_kda_mode()
            IJ.setTool("rectangle")
            self.btn_crop.setBackground(COLOR_ACTIVE)
            self.btn_crop.setText("Confirm Crop")
            self._set_status("Draw a rectangle on the gel, then click Confirm Crop")
            return
        self._waiting_for_crop = False
        self.btn_crop.setBackground(COLOR_INACTIVE)
        self.btn_crop.setText("Crop Region -> Figure")
        self._clear_status()
        roi = self.gel_imp.getRoi()
        if roi is None:
            JOptionPane.showMessageDialog(self.frame,
                "No selection found. Click Crop again and draw first.",
                "No selection", JOptionPane.WARNING_MESSAGE)
            if self._crop_was_marking: self._activate_kda_mode()
            return
        bounds = roi.getBounds()
        x, y, w, h = bounds.x, bounds.y, bounds.width, bounds.height
        if w < 2 or h < 2:
            JOptionPane.showMessageDialog(self.frame,
                "Selection too small -- please try again.",
                "Too small", JOptionPane.WARNING_MESSAGE)
            if self._crop_was_marking: self._activate_kda_mode()
            return
        cropped = crop_imp(self.gel_imp, x, y, w, h)
        inside  = [m for m in self.kda_markers if y <= m["y_orig"] <= y + h]
        local_m = [{"y_orig": m["y_orig"] - y, "kda": m["kda"],
                    "font_size": m.get("font_size",
                                       self.default_font_sizes["kda"])}
                   for m in inside]
        protein = ask_string("Protein name", "Enter protein name:", "Protein")
        if protein is None:
            if self._crop_was_marking: self._activate_kda_mode()
            return
        band = Band(cropped, local_m, protein or "Protein",
                    width=self.bands[-1].display_w if self.bands else None)
        band.protein_size = self.default_font_sizes["protein"]
        self.bands.append(band)
        self.list_model.addElement(protein or "Protein")
        self.band_list.setSelectedIndex(len(self.bands) - 1)
        self._refresh_figure()
        if self._crop_was_marking: self._activate_kda_mode()

    # ── Band list ─────────────────────────────────────────────────────────
    def _on_list_select(self):
        self.sel_idx = self.band_list.getSelectedIndex()

    def move_band(self, direction):
        i = self.sel_idx
        if i < 0: return
        j = i + direction
        if j < 0 or j >= len(self.bands): return
        self.bands[i], self.bands[j] = self.bands[j], self.bands[i]
        a, b = self.list_model.get(i), self.list_model.get(j)
        self.list_model.set(i, b);  self.list_model.set(j, a)
        self.band_list.setSelectedIndex(j);  self.sel_idx = j
        self._refresh_figure()

    def remove_band(self):
        i = self.sel_idx
        if i < 0 or i >= len(self.bands): return
        removed = self.bands.pop(i)
        for hl in self.hlines:
            if hl.band_ref is removed:
                hl.band_ref = None
        self.list_model.remove(i)
        self.sel_idx = min(i, len(self.bands) - 1)
        self.band_list.setSelectedIndex(self.sel_idx)
        self._refresh_figure()

    # ── Width ─────────────────────────────────────────────────────────────
    def _sel_band(self):
        return self.bands[self.sel_idx] if 0 <= self.sel_idx < len(self.bands) else None

    def bump_width(self, factor):
        b = self._sel_band()
        if b is None: return
        b.display_w = max(10, int(b.display_w * factor));  self._refresh_figure()

    def set_width_dialog(self):
        b = self._sel_band()
        if b is None: return
        val = ask_int("Set width", "Width (pixels):", b.display_w)
        if val and val > 0:
            b.display_w = val;  self._refresh_figure()

    # ── Sample labels ─────────────────────────────────────────────────────
    def toggle_sample_label(self):
        if not self.bands: return
        if self.fig_canvas.mode == "sample_label":
            self._deactivate_sample_label_mode()
        else:
            self._activate_sample_label_mode()

    def _activate_sample_label_mode(self):
        self._clear_canvas_add_modes()
        self.fig_canvas.mode = "sample_label"
        self.btn_sample_label.setBackground(COLOR_ACTIVE)
        self.btn_sample_label.setText("Stop Adding Labels")
        self._set_status("Label mode -- click a lane to add label")

    def _deactivate_sample_label_mode(self):
        self.fig_canvas.mode = None
        self.btn_sample_label.setBackground(COLOR_INACTIVE)
        self.btn_sample_label.setText("Add Sample Labels")
        self._clear_status()

    def place_sample_label(self, px, py):
        if not self.bands: return
        text = ask_string("Sample name", "Enter sample name:", "Sample")
        if not text:
            self._deactivate_sample_label_mode();  return
        angle = ask_float("Label angle", "Tilt angle (degrees):",
                          self._default_label_angle)
        if angle is None:
            self._deactivate_sample_label_mode();  return
        self._default_label_angle = angle
        y_cursor = TOP_MARGIN
        for b in self.bands:
            dh = b.display_h()
            if y_cursor - TOP_MARGIN <= py <= y_cursor + dh:
                x_frac = max(0.0, min(1.0,
                    float(px - LEFT_MARGIN) / float(b.display_w)))
                b.sample_labels.append({"x_frac": x_frac,
                                        "text":   text,
                                        "angle":  float(angle),
                                        "font_size": self.default_font_sizes["sample"]})
                break
            y_cursor += self.renderer.band_step(b)
        n = sum(len(b.sample_labels) for b in self.bands)
        self._set_status("Label mode -- %d label%s  (click to add more)" % (
            n, "s" if n != 1 else ""))
        self._refresh_figure()

    # ── Draw H-Line ───────────────────────────────────────────────────────
    def enable_draw_line(self):
        self._clear_canvas_add_modes()
        self.fig_canvas.mode = "draw_line"
        self._set_status("Drag on figure to draw a horizontal line")

    def _band_at_y(self, py):
        y_cursor = TOP_MARGIN
        for b in self.bands:
            dh = b.display_h()
            if y_cursor <= py <= y_cursor + dh:
                return b, LEFT_MARGIN, y_cursor, b.display_w, dh
            y_cursor += self.renderer.band_step(b)
        return None, 0, 0, 0, 0

    def preview_draw_line(self, x0, y0, x1):
        self._refresh_figure(preview_line=(min(x0,x1), y0, max(x0,x1)))

    def finish_draw_line(self, x0, y0, x1):
        if abs(x1 - x0) < 3:
            self._refresh_figure();  return
        lx0 = min(x0, x1);  lx1 = max(x0, x1)
        band, img_x, img_y, dw, dh = self._band_at_y(y0)
        if band is not None and dw > 0 and dh > 0:
            hl = HLine(lx0, y0, lx1, band_ref=band,
                       x0_frac=(lx0-img_x)/float(dw),
                       x1_frac=(lx1-img_x)/float(dw),
                       y_frac =(y0 -img_y)/float(dh))
        else:
            hl = HLine(lx0, y0, lx1)
        self.hlines.append(hl)
        self.fig_canvas.mode = None;  self._clear_status()
        self._refresh_figure()

    # ── Add free text ─────────────────────────────────────────────────────
    def toggle_band_annot(self):
        if not self.bands: return
        if self.fig_canvas.mode == "band_annot":
            self._deactivate_band_annot_mode()
        else:
            self._activate_band_annot_mode()

    def _activate_band_annot_mode(self):
        self._clear_canvas_add_modes()
        self.fig_canvas.mode = "band_annot"
        self.btn_band_annot.setBackground(COLOR_ACTIVE)
        self.btn_band_annot.setText("Stop Adding Ticks")
        self._set_status("Band tick mode -- click a crop at the band height")

    def _deactivate_band_annot_mode(self):
        self.fig_canvas.mode = None
        self.btn_band_annot.setBackground(COLOR_INACTIVE)
        self.btn_band_annot.setText("Add Band Tick")
        self._clear_status()

    def place_band_annot(self, px, py):
        band, img_x, img_y, dw, dh = self._band_at_y(py)
        if band is None or dh <= 0:
            self._set_status("Band tick mode -- click inside a crop")
            return
        text = ask_string("Band annotation", "Enter text:", "")
        if not text:
            self._deactivate_band_annot_mode();  return
        y_frac = max(0.0, min(1.0, float(py - img_y) / float(dh)))
        band.band_annots.append({"y_frac": y_frac, "text": text,
                                 "font_size": self.default_font_sizes["band"]})
        n = sum(len(b.band_annots) for b in self.bands)
        self._set_status("Band tick mode -- %d tick%s  (click to add more)" % (
            n, "s" if n != 1 else ""))
        self._refresh_figure()

    def enable_add_text(self):
        self._clear_canvas_add_modes()
        self.fig_canvas.mode = "add_text"
        self._set_status("Click on figure to place text")

    def place_free_text(self, px, py):
        text = ask_string("Annotation text", "Enter text:", "")
        if not text:
            self._clear_status();  return
        ft = FreeText(px, py, text)
        ft.font_size = self.default_font_sizes["free"]
        self.freetexts.append(ft)
        self.fig_canvas.mode = None;  self._clear_status()
        self._refresh_figure()

    # ── Edit mode ─────────────────────────────────────────────────────────
    def toggle_edit_mode(self):
        if self.edit_mode_active: self._deactivate_edit_mode()
        else:                     self._activate_edit_mode()

    def _activate_edit_mode(self):
        self._clear_canvas_add_modes()
        self.edit_mode_active = True
        self.btn_edit.setBackground(COLOR_ACTIVE)
        self.btn_edit.setText("Stop Editing")
        self.fig_canvas.mode = "edit"
        self._set_status("Edit mode -- click to select, drag to move")
        self._refresh_figure()

    def _deactivate_edit_mode(self):
        self.edit_mode_active = False
        self.selected_annot   = None
        self.btn_edit.setBackground(COLOR_INACTIVE)
        self.btn_edit.setText("Edit Annotations")
        self.fig_canvas.mode = None
        self._clear_status();  self._refresh_figure()

    def _iter_sl(self):
        """Iterate all (band, sl_dict, ix, iy, dw) for all sample labels."""
        y_cursor = TOP_MARGIN
        for b in self.bands:
            dh = b.display_h()
            for sl in b.sample_labels:
                yield b, sl, LEFT_MARGIN, y_cursor, b.display_w
            y_cursor += self.renderer.band_step(b)

    def _iter_ba(self):
        """Iterate all (band, ba_dict, ix, iy, dw, dh) for band annotations."""
        y_cursor = TOP_MARGIN
        for b in self.bands:
            dh = b.display_h()
            for ba in b.band_annots:
                yield b, ba, LEFT_MARGIN, y_cursor, b.display_w, dh
            y_cursor += self.renderer.band_step(b)

    def _band_annot_hit(self, ba, ix, iy, dw, dh, px, py):
        ty = iy + ba["y_frac"] * dh
        x0 = ix + dw + 2
        x1 = x0 + TICK_LEN
        text_w = HIT_RADIUS * 8
        if self.fig_canvas.bi is not None:
            g = self.fig_canvas.bi.createGraphics()
            g.setFont(sized_font(FONT_BANDANN,
                                  ba.get("font_size", FONT_BANDANN.getSize())))
            text_w = g.getFontMetrics().stringWidth(ba["text"])
            g.dispose()
        if x0 - BA_HIT_R <= px <= x1 + BA_HIT_R and abs(py - ty) <= BA_HIT_R:
            return True
        tx = x1 + TICK_GAP
        if tx - 2 <= px <= tx + text_w + 4 and ty - BA_HIT_R <= py <= ty + BA_HIT_R:
            return True
        return False

    def _kda_hit(self, m, ix, iy, sc, px, py):
        ty = iy + m["y_orig"] * sc
        x1 = ix - 2
        x0 = x1 - TICK_LEN
        lbl = "%g" % m["kda"]
        text_w = HIT_RADIUS * 4
        text_h = FONT_KDA.getSize()
        if self.fig_canvas.bi is not None:
            g = self.fig_canvas.bi.createGraphics()
            g.setFont(sized_font(FONT_KDA,
                                  m.get("font_size", FONT_KDA.getSize())))
            fm = g.getFontMetrics()
            text_w = fm.stringWidth(lbl)
            text_h = fm.getHeight()
            g.dispose()
        tx0 = x0 - TICK_GAP - text_w
        if x0 - BA_HIT_R <= px <= x1 + BA_HIT_R and abs(py - ty) <= BA_HIT_R:
            return True
        if tx0 - 2 <= px <= x0 - TICK_GAP + 2 and ty - text_h <= py <= ty + text_h:
            return True
        return False

    def _protein_hit(self, b, ix, iy, dw, dh, px, py):
        text_w = HIT_RADIUS * 8
        text_h = FONT_NAME.getSize()
        if self.fig_canvas.bi is not None:
            g = self.fig_canvas.bi.createGraphics()
            g.setFont(sized_font(FONT_NAME, getattr(b, "protein_size",
                                                     FONT_NAME.getSize())))
            fm = g.getFontMetrics()
            text_w = fm.stringWidth(b.protein_name)
            text_h = fm.getHeight()
            g.dispose()
        if b.band_annots:
            tx = ix + dw // 2 - text_w // 2
            ty = iy + dh + text_h + 5
        else:
            tx = ix + dw + 10
            ty = iy + dh // 2 + text_h // 2
        tx += int(round(getattr(b, "protein_dx_frac", 0.0) * dw))
        ty += int(round(getattr(b, "protein_dy_frac", 0.0) * dh))
        return tx - 2 <= px <= tx + text_w + 2 and ty - text_h <= py <= ty + 4

    def hit_test(self, px, py):
        a = self.selected_annot
        if a is None:
            return None
        hs = HANDLE_SIZE

        if isinstance(a, HLine):
            if abs(px - a.x0) <= hs and abs(py - a.y) <= hs:
                return "h_x0"
            if abs(px - a.x1) <= hs and abs(py - a.y) <= hs:
                return "h_x1"
            if a.x0 <= px <= a.x1 and abs(py - a.y) <= HIT_RADIUS:
                return "h_body"

        elif isinstance(a, FreeText):
            text_w = HIT_RADIUS * 6
            FONT_H = 14
            if self.fig_canvas.bi is not None:
                g = self.fig_canvas.bi.createGraphics()
                g.setFont(sized_font(FONT_ANNOT,
                                      getattr(a, "font_size", FONT_ANNOT.getSize())))
                fm = g.getFontMetrics()
                text_w = fm.stringWidth(a.text)
                FONT_H = fm.getHeight()
                g.dispose()
            if (a.x <= px <= a.x + text_w and
                    a.y - FONT_H - 4 <= py <= a.y + 4):
                return "ft_body"

        elif isinstance(a, tuple) and a[0] == "sl":
            _, b, sl = a
            # find band rect
            y_cursor = TOP_MARGIN
            for band in self.bands:
                if band is b:
                    ax, ay = sl_anchor(sl, LEFT_MARGIN, y_cursor, b.display_w)
                    if dist2(px, py, ax, ay) <= SL_HIT_R:
                        return "sl_body"
                    break
                y_cursor += self.renderer.band_step(band)

        elif isinstance(a, tuple) and a[0] == "ba":
            _, b, ba = a
            y_cursor = TOP_MARGIN
            for band in self.bands:
                dh = band.display_h()
                if band is b:
                    if self._band_annot_hit(ba, LEFT_MARGIN, y_cursor,
                                            b.display_w, dh, px, py):
                        return "ba_body"
                    break
                y_cursor += self.renderer.band_step(band)

        elif isinstance(a, tuple) and a[0] == "kda":
            _, b, m = a
            y_cursor = TOP_MARGIN
            for band in self.bands:
                dh = band.display_h()
                if band is b:
                    if self._kda_hit(m, LEFT_MARGIN, y_cursor,
                                     b.scale(), px, py):
                        return "kda_body"
                    break
                y_cursor += self.renderer.band_step(band)

        elif isinstance(a, tuple) and a[0] == "protein":
            _, b = a
            y_cursor = TOP_MARGIN
            for band in self.bands:
                dh = band.display_h()
                if band is b:
                    if self._protein_hit(b, LEFT_MARGIN, y_cursor,
                                         b.display_w, dh, px, py):
                        return "protein_body"
                    break
                y_cursor += self.renderer.band_step(band)

        return None

    def edit_select(self, px, py):
        best = None;  best_dist = HIT_RADIUS + 1

        for hl in self.hlines:
            cx   = max(hl.x0, min(float(px), hl.x1))
            dist = dist2(float(px), float(py), cx, hl.y)
            if dist < best_dist:
                best_dist = dist;  best = hl

        for ft in self.freetexts:
            dist = dist2(float(px), float(py), ft.x, ft.y)
            if dist < best_dist:
                best_dist = dist;  best = ft

        # sample labels — hit by anchor point
        for b, sl, ix, iy, dw in self._iter_sl():
            ax, ay = sl_anchor(sl, ix, iy, dw)
            dist = dist2(float(px), float(py), ax, ay)
            if dist < best_dist:
                best_dist = dist;  best = ("sl", b, sl)

        for b, ba, ix, iy, dw, dh in self._iter_ba():
            if self._band_annot_hit(ba, ix, iy, dw, dh, px, py):
                ty = iy + ba["y_frac"] * dh
                dist = abs(float(py) - ty)
                if dist < best_dist:
                    best_dist = dist;  best = ("ba", b, ba)

        y_cursor = TOP_MARGIN
        for b in self.bands:
            dh = b.display_h()
            for m in b.kda_markers:
                if self._kda_hit(m, LEFT_MARGIN, y_cursor, b.scale(), px, py):
                    ty = y_cursor + m["y_orig"] * b.scale()
                    dist = abs(float(py) - ty)
                    if dist < best_dist:
                        best_dist = dist;  best = ("kda", b, m)
            if self._protein_hit(b, LEFT_MARGIN, y_cursor, b.display_w, dh, px, py):
                if best is None:
                    best_dist = 0;  best = ("protein", b)
            y_cursor += self.renderer.band_step(b)

        self.selected_annot = best
        if best is None:
            self._set_status("Edit mode -- click to select")
        elif isinstance(best, HLine):
            self._set_status("Selected: H-Line  |  drag handles or body")
        elif isinstance(best, FreeText):
            self._set_status("Selected: \"%s\"  |  drag / dbl-click to rename" % best.text)
        elif isinstance(best, tuple) and best[0] == "sl":
            self._set_status("Selected: label \"%s\"  |  dbl-click to rename, Del to delete" % best[2]["text"])
        elif isinstance(best, tuple) and best[0] == "ba":
            self._set_status("Selected: band tick \"%s\"  |  drag up/down, dbl-click to rename" % best[2]["text"])
        elif isinstance(best, tuple) and best[0] == "kda":
            self._set_status("Selected: kDa label \"%g\"  |  A-/A+ to resize" % best[2]["kda"])
        elif isinstance(best, tuple) and best[0] == "protein":
            self._set_status("Selected: protein name \"%s\"  |  drag/nudge, A-/A+ to resize" % best[1].protein_name)
        self._refresh_figure()

    def drag_annot(self, target, dx, dy):
        a = self.selected_annot
        if a is None: return
        if target == "h_x0" and isinstance(a, HLine):
            a.x0 = min(a.x0 + dx, a.x1 - 2)
        elif target == "h_x1" and isinstance(a, HLine):
            a.x1 = max(a.x1 + dx, a.x0 + 2)
        elif target == "h_body" and isinstance(a, HLine):
            a.x0 += dx;  a.x1 += dx;  a.y += dy
        elif target == "ft_body" and isinstance(a, FreeText):
            a.x += dx;  a.y += dy
        elif target == "ba_body" and isinstance(a, tuple) and a[0] == "ba":
            _, b, ba = a
            idx = self.bands.index(b) if b in self.bands else -1
            rect = self.renderer.band_img_rect(idx, self.bands) if idx >= 0 else None
            if rect is not None:
                img_x, img_y, dw, dh = rect
                cur_y = img_y + ba["y_frac"] * dh
                new_y = max(img_y, min(img_y + dh, cur_y + dy))
                if dh > 0:
                    ba["y_frac"] = (new_y - img_y) / float(dh)
        elif target == "protein_body" and isinstance(a, tuple) and a[0] == "protein":
            _, b = a
            idx = self.bands.index(b) if b in self.bands else -1
            rect = self.renderer.band_img_rect(idx, self.bands) if idx >= 0 else None
            if rect is not None:
                img_x, img_y, dw, dh = rect
                if dw > 0:
                    b.protein_dx_frac = getattr(b, "protein_dx_frac", 0.0) + dx / float(dw)
                if dh > 0:
                    b.protein_dy_frac = getattr(b, "protein_dy_frac", 0.0) + dy / float(dh)
        self._refresh_figure()

    def sync_fractions_after_drag(self):
        a = self.selected_annot
        if a is None or not isinstance(a, HLine): return
        if a.band_ref is None or a.band_ref not in self.bands: return
        idx  = self.bands.index(a.band_ref)
        rect = self.renderer.band_img_rect(idx, self.bands)
        if rect is None: return
        img_x, img_y, dw, dh = rect
        if dw > 0:
            a.x0_frac = (a.x0 - img_x) / float(dw)
            a.x1_frac = (a.x1 - img_x) / float(dw)
        if dh > 0:
            a.y_frac  = (a.y  - img_y) / float(dh)

    def nudge_annot(self, axis, delta):
        a = self.selected_annot
        if a is None: return
        if isinstance(a, HLine):
            if axis == "x0":   a.x0 += delta
            elif axis == "x1": a.x1 += delta
            elif axis == "y":  a.y  += delta
            self.sync_fractions_after_drag()
        elif isinstance(a, FreeText):
            if axis in ("x0","x1"): a.x += delta
            elif axis == "y":       a.y += delta
        elif isinstance(a, tuple) and a[0] == "ba" and axis == "y":
            _, b, ba = a
            idx = self.bands.index(b) if b in self.bands else -1
            rect = self.renderer.band_img_rect(idx, self.bands) if idx >= 0 else None
            if rect is not None:
                img_x, img_y, dw, dh = rect
                cur_y = img_y + ba["y_frac"] * dh
                new_y = max(img_y, min(img_y + dh, cur_y + delta))
                if dh > 0:
                    ba["y_frac"] = (new_y - img_y) / float(dh)
        elif isinstance(a, tuple) and a[0] == "protein":
            _, b = a
            idx = self.bands.index(b) if b in self.bands else -1
            rect = self.renderer.band_img_rect(idx, self.bands) if idx >= 0 else None
            if rect is not None:
                img_x, img_y, dw, dh = rect
                if axis in ("x0", "x1") and dw > 0:
                    b.protein_dx_frac = getattr(b, "protein_dx_frac", 0.0) + delta / float(dw)
                elif axis == "y" and dh > 0:
                    b.protein_dy_frac = getattr(b, "protein_dy_frac", 0.0) + delta / float(dh)
        self._refresh_figure()

    # ── Copy / paste ──────────────────────────────────────────────────────
    def _clamp_font_size(self, size):
        return int(max(5, min(72, size)))

    def _resize_item(self, kind, obj, delta):
        if kind == "protein":
            obj.protein_size = self._clamp_font_size(
                getattr(obj, "protein_size", FONT_NAME.getSize()) + delta)
        else:
            base = {
                "kda": FONT_KDA.getSize(),
                "sample": FONT_SAMPLE.getSize(),
                "free": FONT_ANNOT.getSize(),
                "band": FONT_BANDANN.getSize(),
            }.get(kind, FONT_ANNOT.getSize())
            obj["font_size"] = self._clamp_font_size(obj.get("font_size", base) + delta)

    def _resize_all_text(self, delta):
        for k in self.default_font_sizes:
            self.default_font_sizes[k] = self._clamp_font_size(
                self.default_font_sizes[k] + delta)
        for b in self.bands:
            self._resize_item("protein", b, delta)
            for m in b.kda_markers:
                self._resize_item("kda", m, delta)
            for sl in b.sample_labels:
                self._resize_item("sample", sl, delta)
            for ba in b.band_annots:
                self._resize_item("band", ba, delta)
        for ft in self.freetexts:
            ft.font_size = self._clamp_font_size(
                getattr(ft, "font_size", FONT_ANNOT.getSize()) + delta)

    def resize_text(self, delta):
        a = self.selected_annot
        changed_one = False
        if isinstance(a, FreeText):
            a.font_size = self._clamp_font_size(
                getattr(a, "font_size", FONT_ANNOT.getSize()) + delta)
            changed_one = True
        elif isinstance(a, tuple):
            if a[0] == "sl":
                self._resize_item("sample", a[2], delta);  changed_one = True
            elif a[0] == "ba":
                self._resize_item("band", a[2], delta);  changed_one = True
            elif a[0] == "kda":
                self._resize_item("kda", a[2], delta);  changed_one = True
            elif a[0] == "protein":
                self._resize_item("protein", a[1], delta);  changed_one = True

        if changed_one:
            self._set_status("Resized selected text")
        else:
            self._resize_all_text(delta)
            self._set_status("Resized all text")
        self._refresh_figure()

    def copy_selected(self):
        a = self.selected_annot
        if a is None: return
        if isinstance(a, HLine):
            self.clipboard = a.shallow_copy()
            self._set_status("Copied H-Line -- Ctrl+V to paste")
        elif isinstance(a, FreeText):
            self.clipboard = a.shallow_copy()
            self._set_status("Copied \"%s\" -- Ctrl+V to paste" % a.text)
        # sample labels: copy not supported (they're band-relative)

    def paste_clipboard(self):
        if self.clipboard is None: return
        new = self.clipboard.shallow_copy()
        if isinstance(new, HLine):
            new.x0 += PASTE_OFFSET;  new.x1 += PASTE_OFFSET;  new.y += PASTE_OFFSET
            self.hlines.append(new)
        else:
            new.x += PASTE_OFFSET;  new.y += PASTE_OFFSET
            self.freetexts.append(new)
        self.selected_annot = new
        self._refresh_figure()

    # ── Rename ────────────────────────────────────────────────────────────
    def rename_selected(self):
        """Button-triggered rename — works for FreeText and sample labels."""
        a = self.selected_annot
        if isinstance(a, FreeText):
            self.rename_selected_text()
        elif isinstance(a, tuple) and a[0] == "sl":
            self.rename_selected_sl()
        elif isinstance(a, tuple) and a[0] == "ba":
            self.rename_selected_ba()
        elif isinstance(a, tuple) and a[0] == "protein":
            self.rename_selected_protein()

    def rename_selected_text(self):
        a = self.selected_annot
        if a is None or not isinstance(a, FreeText): return
        new_text = ask_string("Rename", "New text:", a.text)
        if new_text is not None:
            a.text = new_text;  self._refresh_figure()

    def rename_selected_sl(self):
        """Rename (and keep angle) for a selected sample label."""
        a = self.selected_annot
        if not (isinstance(a, tuple) and a[0] == "sl"): return
        _, b, sl = a
        new_text = ask_string("Rename label", "New text:", sl["text"])
        if new_text is not None:
            sl["text"] = new_text
            self._refresh_figure()

    def rename_selected_ba(self):
        """Rename a selected right-side band annotation."""
        a = self.selected_annot
        if not (isinstance(a, tuple) and a[0] == "ba"): return
        _, b, ba = a
        new_text = ask_string("Rename band annotation", "New text:", ba["text"])
        if new_text is not None:
            ba["text"] = new_text
            self._refresh_figure()

    def rename_selected_protein(self):
        a = self.selected_annot
        if not (isinstance(a, tuple) and a[0] == "protein"): return
        _, b = a
        new_text = ask_string("Rename protein", "New text:", b.protein_name)
        if new_text is not None:
            b.protein_name = new_text
            if b in self.bands:
                idx = self.bands.index(b)
                self.list_model.set(idx, new_text)
            self._refresh_figure()

    # ── Delete ────────────────────────────────────────────────────────────
    def delete_selected_annot(self):
        a = self.selected_annot
        if a is None: return
        if isinstance(a, HLine) and a in self.hlines:
            self.hlines.remove(a)
        elif isinstance(a, FreeText) and a in self.freetexts:
            self.freetexts.remove(a)
        elif isinstance(a, tuple) and a[0] == "sl":
            _, b, sl = a
            if sl in b.sample_labels:
                b.sample_labels.remove(sl)
        elif isinstance(a, tuple) and a[0] == "ba":
            _, b, ba = a
            if ba in b.band_annots:
                b.band_annots.remove(ba)
        self.selected_annot = None
        self._set_status("Edit mode -- click to select")
        self._refresh_figure()

    # ── Figure refresh ────────────────────────────────────────────────────
    def _canvas_width(self):
        return max(self.fig_scroll.getViewport().getWidth(), FIG_INIT_W)

    def _refresh_figure(self, preview_line=None):
        hlines = list(self.hlines)
        if preview_line:
            x0, y, x1 = preview_line
            hlines = hlines + [HLine(x0, y, x1)]
        bi = self.renderer.render(
            self.bands, hlines, self.freetexts, self._canvas_width(),
            selected=self.selected_annot, edit_mode=self.edit_mode_active)
        self.fig_canvas.set_image(bi)

    # ── Clear ─────────────────────────────────────────────────────────────
    def clear_figure(self):
        self.bands = [];  self.hlines = [];  self.freetexts = []
        self.sel_idx = -1;  self.selected_annot = None
        self.list_model.clear();  self._refresh_figure()

    # ── Export ────────────────────────────────────────────────────────────
    def _choose_save_path(self, title, ext_desc, ext):
        fc = JFileChooser()
        fc.setFileFilter(FileNameExtensionFilter(ext_desc, [ext]))
        fc.setSelectedFile(JFile("figure." + ext))
        if fc.showSaveDialog(self.frame) != JFileChooser.APPROVE_OPTION:
            return None
        p = fc.getSelectedFile().getAbsolutePath()
        if not p.lower().endswith("." + ext): p += "." + ext
        return p

    def export_image(self):
        if not self.bands and not self.hlines and not self.freetexts:
            JOptionPane.showMessageDialog(self.frame, "Nothing to export.",
                "Empty", JOptionPane.WARNING_MESSAGE); return
        path = self._choose_save_path("Export PNG", "PNG", "png")
        if path is None: return
        bi = self.renderer.render(self.bands, self.hlines, self.freetexts,
                                  self._canvas_width())
        ImageIO.write(bi, "PNG", JFile(path))
        JOptionPane.showMessageDialog(self.frame, "Saved: " + path,
            "Done", JOptionPane.INFORMATION_MESSAGE)

    def export_pdf(self):
        if not self.bands and not self.hlines and not self.freetexts:
            JOptionPane.showMessageDialog(self.frame, "Nothing to export.",
                "Empty", JOptionPane.WARNING_MESSAGE); return
        dpi = ask_int("PDF resolution", "Raster DPI (72-600):", 300)
        if dpi is None: return
        dpi = max(72, min(600, dpi));  spt = 72.0 / dpi

        path = self._choose_save_path("Export PDF", "PDF", "pdf")
        if path is None: return

        from com.itextpdf.text import Rectangle as PdfRect
        from com.itextpdf.text.pdf import PdfContentByte, BaseFont

        for hl in self.hlines:
            self.renderer.recompute_hline(hl, self.bands)

        cw = self._canvas_width()
        th = TOP_MARGIN
        for b in self.bands: th += self.renderer.band_step(b)
        th = max(th, 300)

        pw = cw * spt;  ph = th * spt
        doc    = PdfDocument(PdfRect(pw, ph), 0, 0, 0, 0)
        fos    = FileOutputStream(path)
        writer = PdfWriter.getInstance(doc, fos)
        doc.open()
        cb = writer.getDirectContent()
        bf = BaseFont.createFont(BaseFont.HELVETICA,
                                 BaseFont.WINANSI, BaseFont.NOT_EMBEDDED)

        def pt(px):   return float(px) * spt
        def fy(ypx):  return ph - float(ypx) * spt
        def font_pt(font, size=None):
            return float(size if size is not None else font.getSize()) * spt

        def pdf_line(x0, y0, x1, y1, w=1.0):
            cb.setLiteral("0 G\n%.3f w\n%f %f m %f %f l S\n" % (w*spt,x0,y0,x1,y1))

        def pdf_rect(x, y, w, h):
            cb.setLiteral("0 G\n%.3f w\n%f %f %f %f re S\n" % (1.5*spt,x,y,w,h))

        def pdf_text(text, size, x, y, cos_a=1.0, sin_a=0.0):
            cb.setLiteral("BT\n")
            cb.setFontAndSize(bf, size)
            cb.setLiteral("0 g\n%f %f %f %f %f %f Tm\n" % (
                cos_a, sin_a, -sin_a, cos_a, x, y))
            safe = text.replace("\\","\\\\").replace("(","\\(").replace(")","\\)")
            cb.setLiteral("(%s) Tj\nET\n" % safe)

        def pdf_center_y(text, size, y_center):
            asc = bf.getAscentPoint(text, size)
            desc = bf.getDescentPoint(text, size)
            return y_center - (asc + desc) / 2.0

        y_cur = TOP_MARGIN
        for b in self.bands:
            sc = b.scale();  dw = b.display_w;  dh = b.display_h()
            ix = LEFT_MARGIN;  iy = y_cur

            orig_bi   = b.orig_imp.getProcessor().convertToRGB().getBufferedImage()
            scaled_bi = BufferedImage(dw, dh, BufferedImage.TYPE_INT_RGB)
            sg = scaled_bi.createGraphics()
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            sg.drawImage(orig_bi, 0, 0, dw, dh, None);  sg.dispose()
            baos = ByteArrayOutputStream()
            ImageIO.write(scaled_bi, "PNG", baos)
            pi = PdfImage.getInstance(baos.toByteArray())
            pi.scaleAbsolute(pt(dw), pt(dh))
            pi.setAbsolutePosition(pt(ix), fy(iy + dh))
            doc.add(pi)

            pdf_rect(pt(ix), fy(iy+dh), pt(dw), pt(dh))

            for m in b.kda_markers:
                ty = iy + m["y_orig"] * sc
                x1p = ix - 2;  x0p = x1p - TICK_LEN
                pdf_line(pt(x0p), fy(ty), pt(x1p), fy(ty), w=1.2)
                lbl = "%g" % m["kda"]
                fs = font_pt(FONT_KDA, m.get("font_size", FONT_KDA.getSize()))
                lw  = bf.getWidthPoint(lbl, fs)
                pdf_text(lbl, fs, pt(x0p) - TICK_GAP*spt - lw,
                         pdf_center_y(lbl, fs, fy(ty)))

            for ba in b.band_annots:
                ty = iy + ba["y_frac"] * dh
                x0p = ix + dw + 2;  x1p = x0p + TICK_LEN
                pdf_line(pt(x0p), fy(ty), pt(x1p), fy(ty), w=1.2)
                fs = font_pt(FONT_BANDANN,
                             ba.get("font_size", FONT_BANDANN.getSize()))
                pdf_text(ba["text"], fs, pt(x1p + TICK_GAP),
                         pdf_center_y(ba["text"], fs, fy(ty)))

            if b.band_annots:
                name_px = getattr(b, "protein_size", FONT_NAME.getSize())
                fs_name = font_pt(FONT_NAME, name_px)
                lw_name = bf.getWidthPoint(b.protein_name, fs_name)
                name_dx = getattr(b, "protein_dx_frac", 0.0) * dw
                name_dy = getattr(b, "protein_dy_frac", 0.0) * dh
                pdf_text(b.protein_name, fs_name,
                         pt(ix + dw/2.0 + name_dx) - lw_name/2.0,
                         fy(iy + dh + name_px + 5 + name_dy))
            else:
                fs_name = font_pt(FONT_NAME, getattr(b, "protein_size",
                                                     FONT_NAME.getSize()))
                name_dx = getattr(b, "protein_dx_frac", 0.0) * dw
                name_dy = getattr(b, "protein_dy_frac", 0.0) * dh
                pdf_text(b.protein_name, fs_name, pt(ix+dw+10 + name_dx),
                         fy(iy+dh/2.0 + name_dy) - fs_name*0.35)

            for sl in b.sample_labels:
                ax, ay = sl_anchor(sl, ix, iy, dw)
                # center-anchor: shift left by half text width in points
                fs  = font_pt(FONT_SAMPLE,
                              sl.get("font_size", FONT_SAMPLE.getSize()))
                lw  = bf.getWidthPoint(sl["text"], fs)
                tx  = pt(ax) - lw / 2.0
                ty  = fy(ay)
                ar  = math.radians(sl["angle"])
                pdf_text(sl["text"], fs, tx, ty,
                         math.cos(ar), math.sin(ar))

            y_cur += self.renderer.band_step(b)

        for hl in self.hlines:
            pdf_line(pt(hl.x0), fy(hl.y), pt(hl.x1), fy(hl.y), w=1.5)

        for ft in self.freetexts:
            pdf_text(ft.text,
                     font_pt(FONT_ANNOT, getattr(ft, "font_size",
                                                 FONT_ANNOT.getSize())),
                     pt(ft.x), fy(ft.y))

        doc.close();  fos.close()
        JOptionPane.showMessageDialog(self.frame, "PDF saved: " + path,
            "Done", JOptionPane.INFORMATION_MESSAGE)


# ── Entry point ───────────────────────────────────────────────────────────────
tool = WBTool()
