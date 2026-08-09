//
// TylerSwing -- a modern Swing front end for Tyler.
//
// It reuses the existing drawing engine unchanged: the heavyweight AWT canvas
// (TylerPanel) is embedded in a JFrame, and the surrounding controls are Swing
// widgets wired to the same public TylerPanel methods the AWT UI used.
//
// The look comes from FlatLaf (https://www.formdev.com/flatlaf/) if its jar is
// on the classpath -- a flat, modern, light/dark theme.  FlatLaf is loaded by
// name so it is an *optional* dependency: if the jar is absent the program
// still compiles and runs, falling back to Nimbus (bundled with the JDK).
//
// Build:  javac *.java
// Run:    java -cp .:flatlaf-<version>.jar TylerSwing     (Linux/macOS)
//         java -cp ".;flatlaf-<version>.jar" TylerSwing   (Windows)
//   (without the jar it still runs, using the Nimbus fallback)
//
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class TylerSwing implements TylerHost
{
    private static final int    MIN_P = 3, MAX_P = 12, INITIAL_P = 6;
    private static final double MIN_ZOOM = 1., MAX_ZOOM = 10000., INITIAL_ZOOM = 40.;

    private final JFrame     frame;
    private final TylerPanel  canvas;

    // controls we need to reach from callbacks / handlers
    private JCheckBox   hyperbolicBox;
    private JLabel      curvLabel;
    private JTextField  curvField;     // "curvature based on"
    private JTextField  polyField;
    private JToggleButton polyButtons[] = new JToggleButton[MAX_P+1];
    private ButtonGroup polyGroup = new ButtonGroup();
    private JCheckBox   rhombusBox;
    private JTextField  rhombusField;
    private JButton     colorButton;
    private JCheckBox   darkBox;
    private JFileChooser fileChooser;
    private boolean     syncing = false; // guards programmatic control updates

    // ---------- look & feel ----------

    private static boolean flatlafPresent = false;

    private static void installLaf(boolean dark)
    {
        String flat = dark ? "com.formdev.flatlaf.FlatDarkLaf"
                           : "com.formdev.flatlaf.FlatLightLaf";
        try {
            UIManager.setLookAndFeel(flat);   // FlatLaf, if on the classpath
            flatlafPresent = true;
            return;
        } catch (Throwable t) {
            flatlafPresent = false;
        }
        // Fallbacks that ship with the JDK...
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); return; }
        catch (Throwable t) {}
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Throwable t) {}
    }

    // ---------- construction ----------

    public TylerSwing()
    {
        frame  = new JFrame("Tyler");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // the existing engine, unchanged; this object is its host
        canvas = new TylerPanel(new Rational(INITIAL_P,1), INITIAL_ZOOM, 0., this);
        canvas.setPreferredSize(new Dimension(720, 600));

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(buildControls(), BorderLayout.NORTH);
        frame.getContentPane().add(canvas,          BorderLayout.CENTER);
        frame.getContentPane().add(buildFileBar(),  BorderLayout.SOUTH);

        setCurvatureControlsVisible(false);
        frame.pack();
        frame.setLocationByPlatform(true);
    }

    private JComponent buildControls()
    {
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

        // --- geometry ---
        hyperbolicBox = new JCheckBox("Hyperbolic");
        hyperbolicBox.addActionListener(e -> {
            if (syncing) return;
            if (hyperbolicBox.isSelected())
                canvas.setCurvatureBasedOn(canvas.getCurvatureBasedOn());
            else
                canvas.setCurvature(0.);
            focusCanvas();
        });
        row1.add(hyperbolicBox);

        curvLabel = new JLabel("Curvature based on:");
        curvField = new JTextField("5,5,5,3", 9);
        curvField.addActionListener(e -> {
            canvas.setCurvatureBasedOn(Rational.parseRationalList(curvField.getText()));
            focusCanvas();
        });
        row1.add(curvLabel);
        row1.add(curvField);

        row1.add(sep());

        // --- polygon selection ---
        row1.add(new JLabel("Poly:"));
        polyField = new JTextField(""+INITIAL_P, 3);
        polyField.addActionListener(e -> applyPolyField());
        row1.add(polyField);
        JButton apply = new JButton("Apply");
        apply.addActionListener(e -> applyPolyField());
        row1.add(apply);

        for (int i = MIN_P; i <= MAX_P; i++)
        {
            final Rational p = new Rational(i,1);
            JToggleButton b = new JToggleButton(""+i);
            b.setMargin(new Insets(2,7,2,7));
            b.setSelected(i == INITIAL_P);
            b.addActionListener(e -> {
                canvas.setCurrentP(p);           // this fires rhombusOff()
                polyField.setText(""+p.n);
                focusCanvas();
            });
            polyGroup.add(b);
            polyButtons[i] = b;
            row1.add(b);
        }

        row1.add(sep());

        // --- zoom ---
        row1.add(new JLabel("Zoom:"));
        final JSlider zoom = new JSlider(0, 1000, zoomToSlider(INITIAL_ZOOM));
        zoom.setPreferredSize(new Dimension(150, zoom.getPreferredSize().height));
        zoom.addChangeListener(e -> { canvas.setScale(sliderToZoom(zoom.getValue())); });
        row1.add(zoom);

        JButton clear = new JButton("Clear");
        clear.addActionListener(e -> { canvas.reset(); focusCanvas(); });
        row1.add(clear);

        // --- theme toggle ---
        darkBox = new JCheckBox("Dark");
        darkBox.addActionListener(e -> {
            installLaf(darkBox.isSelected());
            SwingUtilities.updateComponentTreeUI(frame);
            if (fileChooser != null) SwingUtilities.updateComponentTreeUI(fileChooser);
            focusCanvas();
        });
        row1.add(darkBox);

        // --- color ---
        colorButton = new JButton("Color\u2026");
        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(frame, "Choose tile color", canvas.getCurrentColor());
            if (c != null) { canvas.setCurrentColor(c); colorButton.setBackground(c); }
            focusCanvas();
        });
        row2.add(colorButton);

        JButton defColor = new JButton("Default color");
        defColor.addActionListener(e -> { canvas.setCurrentColor(null); colorButton.setBackground(null); focusCanvas(); });
        row2.add(defColor);

        JCheckBox recolor = new JCheckBox("Recolor");
        recolor.addActionListener(e -> { canvas.setRecolorMode(recolor.isSelected()); focusCanvas(); });
        row2.add(recolor);

        row2.add(sep());

        // --- rhombus tool ---
        row2.add(new JLabel("Rhombus \u00d7\u03c0:"));
        rhombusField = new JTextField("2/5", 3);
        rhombusField.addActionListener(e -> {
            Rational ang = Tyler.parseRhombusAngle(rhombusField.getText());
            if (ang != null) { setRhombusChecked(true); canvas.setCurrentRhombus(ang); }
            else beep();
            focusCanvas();
        });
        row2.add(rhombusField);

        rhombusBox = new JCheckBox("Rhombus");
        rhombusBox.addActionListener(e -> {
            if (syncing) return;
            if (rhombusBox.isSelected()) {
                Rational ang = Tyler.parseRhombusAngle(rhombusField.getText());
                if (ang != null) canvas.setCurrentRhombus(ang);
                else { beep(); rhombusBox.setSelected(false); }
            } else
                canvas.setCurrentRhombus(null);
            focusCanvas();
        });
        row2.add(rhombusBox);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(row1);
        north.add(row2);
        return north;
    }

    private JComponent buildFileBar()
    {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton save   = new JButton("Save As\u2026");
        JButton savePs = new JButton("Save PostScript\u2026");
        JButton open   = new JButton("Open\u2026");
        save.addActionListener(e -> doSave(false));
        savePs.addActionListener(e -> doSave(true));
        open.addActionListener(e -> doOpen());
        bar.add(save); bar.add(savePs); bar.add(open);
        return bar;
    }

    private JComponent sep()
    {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(8, 22));
        return s;
    }

    // ---------- handlers ----------

    private void applyPolyField()
    {
        Rational p = Tyler.parseRationalOrBeep(polyField.getText());
        if (p.n > 1) {
            canvas.setCurrentP(p);               // fires rhombusOff()
            selectPolyButton(p);
            if (curvField.isVisible())
                canvas.setCurvatureBasedOn(Rational.parseRationalList(curvField.getText()));
        }
        focusCanvas();
    }

    private void selectPolyButton(Rational p)
    {
        if (p.d == 1 && p.n >= MIN_P && p.n <= MAX_P)
            polyButtons[p.n].setSelected(true);
        else
            polyGroup.clearSelection();
    }

    private void doSave(boolean postscript)
    {
        JFileChooser fc = chooser();
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) { focusCanvas(); return; }
        String path = fc.getSelectedFile().getAbsolutePath();
        if (postscript) {
            String lower = path.toLowerCase();
            if (!lower.endsWith(".ps") && !lower.endsWith(".eps")) path += ".ps";
        }
        String actual = canvas.saveAs(path);
        if (actual != null) frame.setTitle("Tyler - [" + actual + "]");
        focusCanvas();
    }

    private void doOpen()
    {
        JFileChooser fc = chooser();
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) { focusCanvas(); return; }
        File f = fc.getSelectedFile();
        canvas.open(f.getAbsolutePath());
        frame.setTitle("Tyler - [" + f.getName() + "]");
        focusCanvas();
    }

    private JFileChooser chooser()
    {
        if (fileChooser == null) fileChooser = new JFileChooser();
        return fileChooser;
    }

    // ---------- TylerHost callbacks (from TylerPanel) ----------

    public void addHyperbolicControls()
    {
        syncing = true;
        setCurvatureControlsVisible(true);
        if (hyperbolicBox != null) hyperbolicBox.setSelected(true);
        if (curvField != null)
            curvField.setText(Rational.rationalListToString(canvas.getCurvatureBasedOn()));
        syncing = false;
    }
    public void removeHyperbolicControls()
    {
        syncing = true;
        setCurvatureControlsVisible(false);
        if (hyperbolicBox != null) hyperbolicBox.setSelected(false);
        syncing = false;
    }
    public void rhombusOff() { setRhombusChecked(false); }

    public java.net.URL getDocumentBase()
    {
        try { return new java.net.URL("http://localhost/"); } // dummy; server save/load unused on desktop
        catch (Exception e) { return null; }
    }

    // ---------- helpers ----------

    private void setRhombusChecked(boolean on)
    {
        if (rhombusBox == null) return;
        syncing = true;
        rhombusBox.setSelected(on);
        syncing = false;
    }

    private void setCurvatureControlsVisible(boolean v)
    {
        if (curvLabel != null) curvLabel.setVisible(v);
        if (curvField != null) curvField.setVisible(v);
        if (frame != null) frame.validate();
    }

    private void focusCanvas() { canvas.requestFocus(); }

    private static void beep() { Toolkit.getDefaultToolkit().beep(); }

    // logarithmic zoom mapping between the slider (0..1000) and the scale...
    private static int zoomToSlider(double zoom)
    {
        double t = Math.log(zoom/MIN_ZOOM) / Math.log(MAX_ZOOM/MIN_ZOOM);
        return (int)Math.round(t * 1000.);
    }
    private static double sliderToZoom(int v)
    {
        double t = v / 1000.;
        return MIN_ZOOM * Math.pow(MAX_ZOOM/MIN_ZOOM, t);
    }

    private void show() { frame.setVisible(true); canvas.requestFocus(); }

    // ---------- OS light/dark auto-detect ----------

    // Best-effort detection of the OS "dark mode" preference.  Toolkit-independent
    // (works with or without FlatLaf).  Returns true for dark, false for light or
    // unknown.  Each probe is short and failure-tolerant, defaulting to light.
    static boolean systemPrefersDark()
    {
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            if (os.contains("mac")) {
                // prints "Dark" in dark mode; errors (key absent) in light mode
                String out = runQuick("defaults", "read", "-g", "AppleInterfaceStyle");
                return out != null && out.toLowerCase().contains("dark");
            }
            else if (os.contains("win")) {
                String out = runQuick("reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme");
                if (out != null) {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("AppsUseLightTheme\\s+REG_DWORD\\s+0x(\\w+)").matcher(out);
                    if (m.find()) return "0".equals(m.group(1)); // 0 == dark, 1 == light
                }
                return false;
            }
            else {
                // Linux (GNOME and compatible): color-scheme, then gtk-theme name
                String cs = runQuick("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
                if (cs != null) {
                    String c = cs.toLowerCase();
                    if (c.contains("dark"))  return true;
                    if (c.contains("light")) return false;
                }
                String theme = runQuick("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
                if (theme != null) return theme.toLowerCase().contains("dark");
            }
        } catch (Throwable t) { /* fall through to light */ }
        return false;
    }

    // Run a command with a short timeout; return trimmed stdout, or null on any failure.
    private static String runQuick(String... cmd)
    {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            if (!p.waitFor(1500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return null;
            }
            return p.exitValue() == 0 ? sb.toString().trim() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    // ---------- entry point ----------

    public static void main(String[] args)
    {
        Boolean darkOverride = null;   // null => follow the OS
        String openPath = null;
        for (String a : args) {
            if ("-dark".equalsIgnoreCase(a))       darkOverride = Boolean.TRUE;
            else if ("-light".equalsIgnoreCase(a))  darkOverride = Boolean.FALSE;
            else if (!a.startsWith("-"))            openPath = a; // a tiling file to open on startup
        }
        final boolean dark = (darkOverride != null) ? darkOverride.booleanValue()
                                                    : systemPrefersDark();
        final String fopen = openPath;
        installLaf(dark);
        if (!flatlafPresent)
            System.out.println("(FlatLaf jar not on classpath; using the built-in Nimbus look. "
                             + "Add flatlaf.jar to the classpath for the flat modern theme.)");
        SwingUtilities.invokeLater(() -> {
            TylerSwing app = new TylerSwing();
            app.darkBox.setSelected(dark);
            app.show();
            if (fopen != null) app.canvas.open(fopen);
        });
    }
}
