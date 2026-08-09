
// Tyler.java
//
// use this code for anything you like.
// if you use it in a mission critical application and 
// a bug in this code causes a global nuclear war, we will
// take full responsibility and will fix the bug for free.

import java.util.*;
import java.awt.*;
import java.applet.*;
import java.io.*;

/**
 * Tyler Applet<br>
 * A planar geometry drawing tool allowing users to quickly place adjacient 
 * regular convex and star polygons to produce diagrams and tilings.<br>
 * 
 * Last Modified: Mon Sep  9 17:55:51 PDT 2002
 *
 * @author Melinda Green
 * @author Don Hatch
 */
public class Tyler extends Applet implements TylerHost {
    private static final int MIN_P = 3;
    private static final int MAX_P = 12;
    private static final int INITIAL_P = 5;
    private static final double INITIAL_ZOOM = 40.; // edge length in pixels
    private Panel control_panel;
    private static final double initial_curvature = 0.;
    private TylerPanel tyler_panel = new TylerPanel(new Rational(INITIAL_P,1), INITIAL_ZOOM, initial_curvature, this);

    // rhombus tool controls (see the constructor); kept as fields so the panel
    // can uncheck the box when a polygon is chosen.
    private TextField rhombField;
    private Checkbox  rhombBox;

    // Called by the panel when a polygon becomes the current tile, so the
    // Rhombus checkbox reflects reality (unchecked == placing polygons).
    public void rhombusOff() { if (rhombBox != null) rhombBox.setState(false); }

    // Parse a rhombus corner angle "a/b", meaning (a/b)*PI.  Valid iff
    // 0 < a/b < 1 (a strictly-between-0-and-PI interior angle).  Returns null on
    // a bad or out-of-range value.  This is deliberately NOT parseRationalOrBeep,
    // which is the polygon validator: it requires numerator >= 3 and so would
    // reject almost every rhombus angle (1/5, 2/5, 1/4, 1/3, ...).
    public static Rational parseRhombusAngle(String str) {
        Rational r;
        try { r = Rational.parseRational(str); }
        catch (NumberFormatException nfe) { return null; }
        if (r.n <= 0 || r.d <= 0 || r.n >= r.d) return null;
        return r;
    }
    private Checkbox hyperbolic_checkbox;
    private Label curvatureBasedOn_label = null;
    private TextComponent curvatureBasedOn_textfield = null; // TextComponent for Java 1.0 in which there is no TextField.setText()
    public String saveAs(String fname) { return tyler_panel.saveAs(fname); }
    public void open(String fname) { tyler_panel.open(fname); }
    
    public Tyler() {
        this(true);
    }
    public Tyler(boolean doFill) {
        tyler_panel.setFill(doFill);
        
        setLayout(new BorderLayout());
        add("Center", tyler_panel);
        control_panel = new Panel();
        final CheckboxGroup polygroup = new CheckboxGroup();
        final Checkbox poly_sizes[] = new Checkbox[MAX_P+1];
        hyperbolic_checkbox = new Checkbox_("Hyperbolic") {
            public boolean action(Event e, Object what)
            {
                if (((Boolean)what).booleanValue())
                {
                    // XXX names are confusing... this causes the curvature
                    // XXX to be set to a negative number, meaning hyperbolic
                    tyler_panel.setCurvatureBasedOn(tyler_panel.getCurvatureBasedOn());
                }
                else
                    tyler_panel.setCurvature(0.);
                return true;
            }
        };
        final TextComponent polyField = new TextField_(INITIAL_P+"", 3) { // TextComponent for Java 1.0 in which there is no TextField.setText()
            public boolean action(Event e, Object what)
            {
                Rational newP = parseRationalOrBeep((String)what);
                if (newP.n > 1) {
                    tyler_panel.setCurrentP(newP);
                    polygroup.setCurrent(newP.d == 1 && newP.n >= MIN_P && newP.n <= MAX_P ? poly_sizes[newP.n] : null);
                }
                return true;
            }
        };
        Panel poly_grid = new Panel(); // can't specify layout in ctor in 1.0
        poly_grid.setLayout(new GridLayout(MAX_P, 1));
        for(int i=MIN_P; i<=MAX_P; i++) {
            final Rational p = new Rational(i,1);
            Checkbox poly_button = new Checkbox_(""+i, polygroup, i == INITIAL_P) {
                public boolean action(Event e, Object what) {
                    tyler_panel.setCurrentP(p);
                    polyField.setText(""+p);
                    return true;
                }
            };
            poly_grid.add(poly_button);
            poly_sizes[p.n] = poly_button;
        }
        Panel poly_buttons = new Panel();
        poly_buttons.setLayout(new BorderLayout());
        poly_buttons.add("South", poly_grid);
        Button reset_button = new Button_("Clear") {
            public boolean action(Event e, Object what) {
                tyler_panel.reset();
                return true;
            }
        };
        
        final double minZoom = 1;
        final double maxZoom = 10000;

        final LogScrollbar zoomer = new LogScrollbar(
                  Scrollbar.HORIZONTAL,
                  40,  // scroller visible width
                  800, // incrs
                  INITIAL_ZOOM, // val
                  minZoom,
                  maxZoom) {
            public Dimension minimumSize()    { return new Dimension(200, polyField.minimumSize().height); }
            public Dimension preferredSize()  { return new Dimension(200, polyField.preferredSize().height); }
            public boolean handleEvent(Event e)
            {
                //System.out.println("zoomer handleEvent: "+e.id);
                // Just assume it's a relevant event...
                double newZoom = getValueD();
                //System.out.println("newZoom="+newZoom);
                tyler_panel.setScale(newZoom);
                return true;
            }
        };

        zoomer.resize(zoomer.size().width*2, zoomer.size().height*2);
        
        Button apply_button = new Button_("Apply") {
            public boolean action(Event e, Object what)
            {
                Rational poly = parseRationalOrBeep(polyField.getText());
                if (poly.n > 1) {
                    tyler_panel.setCurrentP(poly);
                    polygroup.setCurrent(poly.d == 1 && poly.n >= MIN_P && poly.n <= MAX_P ? poly_sizes[poly.n] : null);

                    if (curvatureBasedOn_textfield != null)
                        tyler_panel.setCurvatureBasedOn(Rational.parseRationalList(curvatureBasedOn_textfield.getText()));
                }
                return true;
            }
        };

        control_panel.add(hyperbolic_checkbox);
        control_panel.add(new Label("Poly:"));
        control_panel.add(polyField);
        control_panel.add(apply_button);
        control_panel.add(new Label("      Zoom:"));
        control_panel.add(zoomer);
        control_panel.add(new Label(""));
        control_panel.add(reset_button);

        // ---- tile color controls ----
        final Button colorButt = new Button_("Color...") {
            public boolean action(Event e, Object what)
            {
                Color c = javax.swing.JColorChooser.showDialog(
                              Tyler.this, "Choose tile color",
                              tyler_panel.getCurrentColor());
                if (c != null) // null => user cancelled, leave current color as is
                {
                    tyler_panel.setCurrentColor(c);
                    setBackground(c);
                }
                return true;
            }
        };
        final Button defColorButt = new Button_("Default color") {
            public boolean action(Event e, Object what)
            {
                tyler_panel.setCurrentColor(null); // new tiles use size-based color
                colorButt.setBackground(null);
                return true;
            }
        };
        final Checkbox recolorBox = new Checkbox_("Recolor") {
            public boolean action(Event e, Object what)
            {
                tyler_panel.setRecolorMode(getState()); // click recolors vs. adds
                return true;
            }
        };
        control_panel.add(new Label("   ")); // small gap
        control_panel.add(colorButt);
        control_panel.add(defColorButt);
        control_panel.add(recolorBox);

        // ---- rhombus tool (Euclidean): corner angle entered as a/b == (a/b)*PI ----
        rhombField = new TextField_("2/5", 3) {
            public boolean action(Event e, Object what)
            {
                Rational ang = parseRhombusAngle((String)what);
                if (ang != null) {
                    rhombBox.setState(true);              // pressing Enter turns rhombus mode on
                    tyler_panel.setCurrentRhombus(ang);
                } else
                    getToolkit().beep();
                return true;
            }
        };
        rhombBox = new Checkbox_("Rhombus") {
            public boolean action(Event e, Object what)
            {
                if (getState()) {
                    Rational ang = parseRhombusAngle(rhombField.getText());
                    if (ang != null)
                        tyler_panel.setCurrentRhombus(ang);   // place rhombi
                    else {
                        getToolkit().beep();
                        setState(false);                      // bad angle -> stay off
                    }
                } else
                    tyler_panel.setCurrentRhombus(null);       // back to placing polygons
                return true;
            }
        };
        control_panel.add(new Label("  Rhombus x pi:"));
        control_panel.add(rhombField);
        control_panel.add(rhombBox);

        add("South", control_panel);
        add("East", poly_buttons);

        if (tyler_panel.getCurvature() < 0.)
            addHyperbolicControls();
    }


    public void addHyperbolicControls()
    {
        if (curvatureBasedOn_textfield == null)
        {
            curvatureBasedOn_label = new Label("Curvature based on:");
            curvatureBasedOn_textfield = new TextField_(13) {
                public boolean action(Event e, Object what)
                {
                    tyler_panel.setCurvatureBasedOn(Rational.parseRationalList(curvatureBasedOn_textfield.getText()));
                    return true;
                }
            };
            // add after the Hyperbolic checkbox...
            control_panel.add(curvatureBasedOn_label, 1);
            control_panel.add(curvatureBasedOn_textfield, 2);
            validate();

            hyperbolic_checkbox.setState(true);
        }
        // XXXhatch this must be out here; this function really updates hyperbolic controls rather than adding them
        curvatureBasedOn_textfield.setText(
            Rational.rationalListToString(
                tyler_panel.getCurvatureBasedOn()));
    } // addHyperbolicControls
    public void removeHyperbolicControls()
    {
        if (curvatureBasedOn_textfield != null)
        {
            control_panel.remove(curvatureBasedOn_label);
            curvatureBasedOn_label = null;
            control_panel.remove(curvatureBasedOn_textfield);
            curvatureBasedOn_textfield = null;

            validate();

            hyperbolic_checkbox.setState(false);
        }
    }
    
    public static Rational parseRationalOrBeep(String str) {
        Rational ratval=null;
        try { ratval = Rational.parseRational(str); }
        catch(NumberFormatException nfe) { 
            TylerPanel.beep();
            return new Rational(-1,1);
        }
        if (ratval.n < 3 || ratval.d % ratval.n == 0)
        {
            TylerPanel.beep();
            return new Rational(-1,1);
        }
        return ratval;
    }

    //
    // Make a guess as to whether we are in the sandbox
    // (more specifically, whether we are unable to read or write files).
    //
    public static boolean inSandbox()
    {
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null)
                sm.checkRead("."); // hoping "." means current directory on all systems
        } catch(SecurityException e) { 
            //System.out.println("in sandbox: "+e);
            return true;
        }
        //System.out.println("not in sandbox");
        return false;
    }


    //
    // For debugging applet viewers and browsers...
    //
    /*
    public void init()
    {
        System.out.println("in init");
        super.init();
        System.out.println("out init");
    }
    public void start()
    {
        System.out.println("in start");
        super.start();
        System.out.println("out start");
    }
    public void stop()
    {
        System.out.println("in stop");
        super.stop();
        System.out.println("out stop");
    }
    public void destroy()
    {
        System.out.println("in destroy");
        super.destroy();
        System.out.println("out destroy");
    }
    */


    //
    // Trivial wrapper classes to get around strange Jikes error when
    // anonymously subclassing:
    // *** Error: A constructor associated with this anonymous type does not throw the exception "java/awt/HeadlessException" thrown by its super type, "java/awt/Checkbox".
    // XXX duplicated in two files-- can we put these in their own file somehow?
    //
    private static class Checkbox_ extends java.awt.Checkbox {
        Checkbox_(String s) { super(s); }
        Checkbox_(String s, java.awt.CheckboxGroup cbg, boolean state) { super(s, cbg, state); }
    }
    private static class TextField_ extends java.awt.TextField {
        TextField_(int cols) { super(cols); }
        TextField_(String s, int cols) { super(s, cols); }
    }
    private static class Button_ extends java.awt.Button {
        Button_(String s) { super(s); }
    }
    private static class List_ extends java.awt.List {
        List_(int rows, boolean multipleMode) { super(rows, multipleMode); }
    }
    private static class Frame_ extends java.awt.Frame {
        public Frame_() { super(); }
        public Frame_(String title) { super(title); }
    }
    
    private static int totalFrameCount=0, activeFrameCount=0;
    
    public static void launchTylerFrame(boolean fill) {
        final Tyler tyler = new Tyler(fill);
        tyler.resize(new Dimension(700, 700));
        final Frame frame = new Frame_("Tyler - [Untitled]") {
            public boolean handleEvent(java.awt.Event event)
            {
                switch(event.id)
                {
                    case java.awt.Event.WINDOW_DESTROY:
                        System.out.println("ciao!");
                        tyler.stop();
                        tyler.destroy();
                        dispose(); // hide() doesn't delete the windows
                        if(--activeFrameCount <= 0)
                            System.exit(0);
                        return true;
                }
                return super.handleEvent(event);
            }
        };
        
/*
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent we) {
                System.out.println("ciao!");
                tyler.stop();
                tyler.destroy();
                frame.dispose();
                if(--activeFrameCount <= 0)
                    System.exit(0);
            }
        });
*/

        Panel mainpanel = new Panel();
        mainpanel.setLayout(new BorderLayout());
        mainpanel.add("Center", tyler);

        if (!inSandbox())
        {
            final FileDialog fileDlg = new FileDialog(frame, "");
            Panel controls = new Panel();
            Button saveButt = new Button_("Save As...") {
                public boolean action(Event e, Object what)
                {
                    fileDlg.setTitle("Select file to save tyler drawing to");
                    fileDlg.setMode(FileDialog.SAVE);
                    fileDlg.show();
                    String fname = fileDlg.getFile();
                    if(fname == null)
                        return true; // user cancelled
                    String dir = fileDlg.getDirectory();
                    if(dir != null) fname = dir + fname; // else it lands in the launch directory
                    System.out.println("saving to " + fname);
                    String actual_name = tyler.saveAs(fname);
                    if(actual_name != null)
                        frame.setTitle("Tyler - [" + actual_name + "]");
                    return true;
                }
            };
            controls.add(saveButt);
            Button savePsButt = new Button_("Save PostScript...") {
                public boolean action(Event e, Object what)
                {
                    fileDlg.setTitle("Select file to save PostScript to");
                    fileDlg.setMode(FileDialog.SAVE);
                    fileDlg.show();
                    String fname = fileDlg.getFile();
                    if(fname == null)
                        return true; // user cancelled
                    String lower = fname.toLowerCase();
                    if(!lower.endsWith(".ps") && !lower.endsWith(".eps"))
                        fname += ".ps";
                    String dir = fileDlg.getDirectory();
                    if(dir != null) fname = dir + fname; // else it lands in the launch directory
                    System.out.println("saving PostScript to " + fname);
                    String actual_name = tyler.saveAs(fname);
                    if(actual_name != null)
                        frame.setTitle("Tyler - [" + actual_name + "]");
                    return true;
                }
            };
            controls.add(savePsButt);
            Button openButt = new Button_("Open...") {
                public boolean action(Event e, Object what)
                {
                    fileDlg.setTitle("Select file to read tyler drawing from");
                    fileDlg.setMode(FileDialog.LOAD);
                    fileDlg.show();
                    String fname = fileDlg.getFile();
                    if(fname == null)
                        return true; // user cancelled
                    System.out.println("opening " + fileDlg.getDirectory() + fname );
                    tyler.open(fileDlg.getDirectory() + fname);
                    frame.setTitle("Tyler - [" + fname + "]");
                    return true;
                }
            };
            controls.add(openButt);
            mainpanel.add("South", controls);
        }

        frame.add(mainpanel);
        tyler.init();
        tyler.start();

        //
        // Pack based on hyperbolic controls being on,
        // but then turn them off
        //
        tyler.addHyperbolicControls();
        frame.pack();
        tyler.removeHyperbolicControls();
        
        frame.reshape(totalFrameCount*28, totalFrameCount*28, frame.getSize().width, frame.getSize().height);
        totalFrameCount++;
        activeFrameCount++;
        frame.show();
        //System.out.println("out main");
    } // end launchTylerFrame
    
    
    public static void main(String[] args) {
        //System.out.println("in main");
        boolean fill = args.length == 0 || !"-nofill".equalsIgnoreCase(args[0]);
        launchTylerFrame(fill);
    } // end main
} // end class Tyler


class TylerPanel extends DoubleBufferedCanvas {
    private TylerHost applet; // parent host (AWT applet or Swing UI)
    private double scale = 1.;
    private static final double eps = 1e-6;
    private static final Color colors[] =  {
        Color.green,   // 3,  12, ...
        Color.red,     // 4,  13, ...
        Color.yellow,  // 5,  14, ...
        Color.blue,    // 6,  15, ...
        Color.orange,  // 7,  16, ...
        Color.magenta, // 8,  17, ...
        Color.pink,    // 9,  18, ...
        Color.orange,  // 10, 19, ...
        Color.cyan,    // 11, 20, ...
    };
    private Rational mostRecentP;
    private Edge mostRecentEdge, secondMostRecentEdge;
    private Vector polys = new Vector();          // Vector of Poly
    private Vector perimeterEdges = new Vector(); // Vector of Edge
    private double focusX = 0., focusY = 0.; // the point in the scene that's placed in the center of the screen
    private boolean doFill = true;
    private Rational hyperbolicEdgeLengthBasedOn[] = Rational.parseRationalList("5,5,5,3");
    private double hyperbolicEdgeLength; // function of hyperbolicEdgeLengthBasedOn
    private double curvature; // negative function of hyperbolicEdgeLength when hyperbolic, 0 when euclidean
    private double arrowToDraw[/*2*/][/*2*/];
    private boolean antiAlias = false;
    private boolean doArcs = false; // whether to draw arcs when hyperbolic
    private double circleThickness = 1.; // of bounding unit circle when hyperbolic


    public void setFill(boolean doFill) { this.doFill = doFill; }
    private static class Poly {
        public Rational p;
        public double X[/*p*/], Y[/*p*/];
        public double centerX, centerY;
        public Color color = null; // null => use the default color for this poly's size
        public Rational rhombusAngle = null; // null => regular polygon; else this is a
                                             // rhombus whose corner angle at vertex 0 is
                                             // rhombusAngle*PI (Euclidean only)

        // Explicit-vertex constructor, used for rhombi.  p is set to 4/1 so the
        // rest of the pipeline (fill, outline, perimeter, color) treats it as an
        // ordinary quadrilateral; rhombusAngle records the shape so it can be
        // saved and reloaded exactly rather than regenerated as a square.
        public Poly(Rational rhombusAngle, double Xs[/*4*/], double Ys[/*4*/])
        {
            this.p = new Rational(4,1);
            this.rhombusAngle = rhombusAngle;
            this.X = Xs;
            this.Y = Ys;
            double cx=0., cy=0.;
            for (int i = 0; i < 4; i++) { cx += Xs[i]; cy += Ys[i]; }
            this.centerX = cx/4.;
            this.centerY = cy/4.;
        }
        public Poly(Rational p, double centerX, double centerY, double x0, double y0, double curvature)
        {
            //System.out.println("in Poly("+p+", centerX="+centerX+", centerY="+centerY+", x0="+x0+", y0="+y0+", curvature="+curvature+")");
            if (curvature == 0.) // Euclidean
            {
                double ang0 = Math.atan2(y0-centerY, x0-centerX);
                double circumRadius = MyMath.hypot(x0-centerX, y0-centerY);
                X = new double[p.n];
                Y = new double[p.n];
                for (int i = 0; i < p.n; ++i)
                {
                    // store in p.n/1 order regardless of p.d
                    double ang = ang0 + 2*Math.PI/p.n * i;
                    X[i] = centerX + circumRadius * Math.cos(ang);
                    Y[i] = centerY + circumRadius * Math.sin(ang);
                    
                }
                this.centerX = centerX;
                this.centerY = centerY;
                this.p = p;
            }
            else if (curvature < 0.) // Hyperbolic
            {
                Isometry2 centerToOrigin = Isometry2.pureTranslation(-centerX,-centerY);
                Isometry2 originToCenter = Isometry2.pureTranslation(centerX,centerY);
                Complex p0_ = centerToOrigin.apply(x0,y0);

                double ang0 = Math.atan2(p0_.y,p0_.x);
                double circumRadius = MyMath.hypot(p0_.x, p0_.y);
                X = new double[p.n];
                Y = new double[p.n];
                double point[] = new double[2];
                for (int i = 0; i < p.n; ++i)
                {
                    // store in p.n/1 order regardless of p.d
                    double ang = ang0 + 2*Math.PI/p.n * i;
                    originToCenter.apply(circumRadius * Math.cos(ang),
                                         circumRadius * Math.sin(ang),
                                         point);
                    X[i] = point[0];
                    Y[i] = point[1];
                }
                this.centerX = centerX;
                this.centerY = centerY;
                this.p = p;
            }
            else // Spherical
            {
                SphericalIsometry2 centerToOrigin = SphericalIsometry2.pureTranslation(-centerX,-centerY);
                SphericalIsometry2 originToCenter = SphericalIsometry2.pureTranslation(centerX,centerY);
                Complex p0_ = centerToOrigin.apply(x0,y0);

                double ang0 = Math.atan2(p0_.y,p0_.x);
                double circumRadius = MyMath.hypot(p0_.x, p0_.y);
                X = new double[p.n];
                Y = new double[p.n];
                double point[] = new double[2];
                for (int i = 0; i < p.n; ++i)
                {
                    // store in p.n/1 order regardless of p.d
                    double ang = ang0 + 2*Math.PI/p.n * i;
                    originToCenter.apply(circumRadius * Math.cos(ang),
                                         circumRadius * Math.sin(ang),
                                         point);
                    X[i] = point[0];
                    Y[i] = point[1];
                }
                this.centerX = centerX;
                this.centerY = centerY;
                this.p = p;
            }
        } // Poly()
    } // private static class poly
    private static class Edge {
        public double x0, y0, x1, y1;
        public double centerX, centerY;
        public Edge(double x0, double y0, double x1, double y1, double curvature)
        {
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            if (curvature == 0.) // Euclidean
            {
                this.centerX = .5 * (x0 + x1);
                this.centerY = .5 * (y0 + y1);
            }
            else if (curvature < 0.) // Hyperbolic
            {
                Complex center_ = HyperbolicUtils.hlerp(x0,y0, x1,y1, .5);

                this.centerX = center_.x;
                this.centerY = center_.y;
            }
            else // Spherical
            {
                Complex center_ = SphericalUtils.slerp(x0,y0, x1,y1, .5);

                this.centerX = center_.x;
                this.centerY = center_.y;
            }
        }
    } // private static class Edge


    // return a new Edge edge2 such that edge2 is to edge1
    // as edge1 is to edge0.
    private static Edge extrapolateEdges(Edge edge0, Edge edge1,
                                         double curvature)
    {
        // build the matrix representing edge0 as a local coordinate system...
        double m0[][] = {
            {  edge0.x1-edge0.x0,  edge0.y1-edge0.y0, 0.},
            {-(edge0.y1-edge0.y0), edge0.x1-edge0.x0, 0.},
            {  edge0.x0,           edge0.y0,          1.},
        };
        // likewise for edge1...
        double m1[][] = {
            {  edge1.x1-edge1.x0,  edge1.y1-edge1.y0, 0.},
            {-(edge1.y1-edge1.y0), edge1.x1-edge1.x0, 0.},
            {  edge1.x0,           edge1.y0,          1.},
        };
        // extrapolate: m2 = m1 * inv(m0) * m1
        double m2[][] = MatrixMath.mxm(MatrixMath.mxm(m1, MatrixMath.invertOrtho(m0)), m1);
        // and convert back from local-coord-system matrix to edge...
        Edge edge2 = new Edge(m2[2][0],          m2[2][1],
                              m2[2][0]+m2[0][0], m2[2][1]+m2[0][1],
                              curvature);
        return edge2;
    }


    // Bump minor version when old program can't read new file
    // Bump major version when new program can't even read old file
    // Bump other version any time we feel like it
    static final int myVersion[] = {0,2,0}; // 0.1.1 added optional per-tile color; 0.2.0 adds rhombi

    public void write(PrintStream w) throws java.io.IOException {
        w.println("tyler data format " +
                  myVersion[0]+"."+myVersion[1]+"."+myVersion[2]);

        // added in 0.1
        {
            w.println(""+curvature);
            if (curvature != 0.)
                w.println(Rational.rationalListToString(getCurvatureBasedOn()));
        }

        w.println(""+polys.size());
        for(Enumeration e=polys.elements(); e.hasMoreElements(); ) {
            Poly poly = (Poly)e.nextElement();
            Rational p = poly.p;
            w.print(p + " ");
            for(int i=0; i<p.n; i++)
                w.print(" " + poly.X[i] + "," + poly.Y[i]);
            if (poly.rhombusAngle != null) // marks this quad as a rhombus, added in 0.2.0
                w.print(" @" + poly.rhombusAngle);
            if (poly.color != null) // optional, added in 0.1.1
                w.print(" #" + colorToHex(poly.color));
            w.println();    
        }
        w.println(""+perimeterEdges.size());
        for(Enumeration e=perimeterEdges.elements(); e.hasMoreElements(); ) {
            Edge edge = (Edge)e.nextElement();
            w.println(edge.x0 + "," + edge.y0 + " " + edge.x1 + "," + edge.y1);    
        }
        System.out.println("wrote " + polys.size() + " polys, " + perimeterEdges.size() + " perimeter edges");     
        w.println(scale + " " + focusX + "," + focusY);
    } // write


    // The color newly-created tiles get; null means "use the default for the
    // poly's size".  Also used as the color that Recolor paints onto a tile.
    private Color currentColor = null;
    // When true, clicking a tile recolors it (with currentColor) instead of
    // adding a new tile.
    private boolean recolorMode = false;

    // When non-null, the "add current tile" gesture places a rhombus with this
    // corner angle (angle*PI) instead of a regular polygon.  Euclidean only.
    private Rational currentRhombus = null;
    public void setCurrentRhombus(Rational angle) { currentRhombus = angle; requestFocus(); }
    public Rational getCurrentRhombus()           { return currentRhombus; }

    public void setCurrentColor(Color c) { currentColor = c; requestFocus(); }
    public Color getCurrentColor()       { return currentColor; }
    public void setRecolorMode(boolean b){ recolorMode = b; requestFocus(); }

    // The default color for a poly's size (green for triangles, red for
    // squares, ...), which is used when the tile has no explicit color.
    private static Color colorForPoly(Rational p)
    {
        int base_color = p.n - 3;
        Color c = colors[base_color % colors.length];
        for (int i = 0; i < (base_color/colors.length)%5; i++)
            c = c.darker();
        return c;
    }

    // The color a tile is actually drawn with: its own if set, else the
    // size-based default.  paint() and writePostScript() both go through this.
    private static Color effectiveColor(Poly poly)
    {
        return poly.color != null ? poly.color : colorForPoly(poly.p);
    }

    // Per-tile color <-> "rrggbb" hex, for the save file.
    private static String colorToHex(Color c)
    {
        int rgb = c.getRGB() & 0xffffff;
        String s = Integer.toHexString(rgb);
        while (s.length() < 6) s = "0" + s;
        return s;
    }
    private static Color parseColor(String s)
    {
        if (s.startsWith("#")) s = s.substring(1);
        return new Color(Integer.parseInt(s, 16));
    }

    // Tidy, locale-independent number formatting for PostScript coordinates.
    // (String.format would honor the locale's decimal separator, which PS won't parse.)
    private static String fmt(double v)
    {
        double r = Math.round(v * 10000.) / 10000.;
        if (r == Math.floor(r) && !Double.isInfinite(r))
            return "" + (long)r;
        return "" + r;
    }


    //
    // Write the current tiling as Encapsulated PostScript.
    // What's emitted mirrors paint():
    //   - fill pass: non-star tiles (p.d==1), filled with their paint() color,
    //     only when doFill is on;
    //   - stroke pass: every tile boundary in black, visiting vertices in
    //     MOD(i*p.d,p.n) order so star polygons {p/d} come out right.
    // The whole tiling is fit to the page by its own bounding box (not the
    // current pan/zoom), so the file captures the drawing rather than a
    // screenshot crop.
    //
    // Edges are straight segments for Euclidean and spherical tilings (Tyler
    // draws spherical edges as straight chords too, so this matches the screen).
    // For hyperbolic tilings the edges are the true Poincare-disk geodesics --
    // arcs of circles orthogonal to the unit disk -- emitted as PostScript arcs
    // for both fill and outline, so the exported tiles are properly arc-sided.
    //
    // Interior edges belong to two tiles and so are stroked twice; with an
    // opaque black hairline that overprint is invisible, so we don't bother
    // deduplicating the way paint() does for anti-aliasing.
    //
    public void writePostScript(PrintStream w) throws java.io.IOException
    {
        // bounding box over all tile vertices, in scene coords...
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Enumeration e = polys.elements(); e.hasMoreElements(); )
        {
            Poly poly = (Poly)e.nextElement();
            for (int i = 0; i < poly.p.n; i++)
            {
                if (poly.X[i] < minX) minX = poly.X[i];
                if (poly.X[i] > maxX) maxX = poly.X[i];
                if (poly.Y[i] < minY) minY = poly.Y[i];
                if (poly.Y[i] > maxY) maxY = poly.Y[i];
            }
        }
        if (minX > maxX) // no polys - still emit a valid, empty file
        {
            minX = minY = 0.;
            maxX = maxY = 1.;
        }

        // fit scene into a page box (in points), preserving aspect ratio...
        final double margin  = 36.;  // 1/2 inch
        final double maxSide = 540.; // 7.5 inch drawable side
        double sceneW = maxX - minX;
        double sceneH = maxY - minY;
        double s = maxSide / Math.max(sceneW, sceneH);
        double pageW = sceneW * s + 2*margin;
        double pageH = sceneH * s + 2*margin;
        boolean hyperbolic = (curvature < 0.); // Poincare-disk edges are circular arcs
        // scene (x,y) -> page point, y flipped so orientation matches the screen:
        //   px = margin + (x - minX)*s
        //   py = margin + (maxY - y)*s

        w.println("%!PS-Adobe-3.0 EPSF-3.0");
        w.println("%%Creator: Tyler (https://superliminal.com/geometry/tyler/)");
        w.println("%%Title: Tyler tiling");
        w.println("%%BoundingBox: 0 0 "
                  + (int)Math.ceil(pageW) + " " + (int)Math.ceil(pageH));
        w.println("%%EndComments");
        w.println("/m {moveto} bind def");
        w.println("/l {lineto} bind def");
        w.println("/cp {closepath} bind def");
        w.println("/rgb {setrgbcolor} bind def");
        w.println("0.4 setlinewidth 1 setlinejoin 1 setlinecap");

        // fill pass...
        if (doFill)
        {
            for (Enumeration e = polys.elements(); e.hasMoreElements(); )
            {
                Poly poly = (Poly)e.nextElement();
                Rational p = poly.p;
                if (p.d != 1) // don't fill stars, same as paint()
                    continue;
                Color c = effectiveColor(poly);
                w.println(fmt(c.getRed()/255.) + " "
                        + fmt(c.getGreen()/255.) + " "
                        + fmt(c.getBlue()/255.) + " rgb");
                emitPolyPath(w, poly, minX, maxY, s, margin, hyperbolic);
                w.println("fill");
            }
        }

        // stroke pass...
        w.println("0 0 0 rgb");
        for (Enumeration e = polys.elements(); e.hasMoreElements(); )
        {
            Poly poly = (Poly)e.nextElement();
            emitPolyPath(w, poly, minX, maxY, s, margin, hyperbolic);
            w.println("stroke");
        }

        w.println("showpage");
        w.println("%%EOF");
        System.out.println("wrote " + polys.size() + " polys as PostScript");
    } // writePostScript

    // Emit one tile's closed path (moveto + edges + closepath), visiting the
    // vertices in MOD(i*p.d,n) order so stars trace correctly.  Straight edges
    // become 'l'; in hyperbolic mode each edge becomes its Poincare geodesic arc.
    private void emitPolyPath(PrintStream w, Poly poly,
                              double minX, double maxY, double s, double margin,
                              boolean hyperbolic)
    {
        Rational p = poly.p;
        int n = p.n, d = p.d;
        double x0 = poly.X[0], y0 = poly.Y[0];
        w.println(fmt(margin + (x0-minX)*s) + " " + fmt(margin + (maxY-y0)*s) + " m");
        for (int i = 0; i < n; i++)
        {
            double ax = poly.X[MOD(i*d,n)],     ay = poly.Y[MOD(i*d,n)];
            double bx = poly.X[MOD((i+1)*d,n)], by = poly.Y[MOD((i+1)*d,n)];
            emitEdgeTo(w, ax,ay, bx,by, minX,maxY,s,margin, hyperbolic);
        }
        w.println("cp");
    }

    // Append the edge from A=(ax,ay) to B=(bx,by) to the current path, in page
    // coordinates.  Euclidean/spherical: a straight lineto.  Hyperbolic: the arc
    // of the circle through A and B orthogonal to the unit disk (the geodesic),
    // as a PostScript arc/arcn.
    private void emitEdgeTo(PrintStream w,
                            double ax, double ay, double bx, double by,
                            double minX, double maxY, double s, double margin,
                            boolean hyperbolic)
    {
        double axp = margin + (ax-minX)*s, ayp = margin + (maxY-ay)*s;
        double bxp = margin + (bx-minX)*s, byp = margin + (maxY-by)*s;
        if (!hyperbolic)
        {
            w.println(fmt(bxp) + " " + fmt(byp) + " l");
            return;
        }
        // geodesic circle center C (world coords): C.A = (|A|^2+1)/2, C.B = (|B|^2+1)/2
        double det = ax*by - ay*bx;
        if (Math.abs(det) < 1e-9) // A,B,origin collinear -> geodesic is a diameter
        {
            w.println(fmt(bxp) + " " + fmt(byp) + " l");
            return;
        }
        double ka = (ax*ax + ay*ay + 1.)/2.;
        double kb = (bx*bx + by*by + 1.)/2.;
        double cx = (ka*by - kb*ay)/det;
        double cy = (ax*kb - bx*ka)/det;
        if (cx*cx + cy*cy - 1. <= 0.) // shouldn't happen for interior points
        {
            w.println(fmt(bxp) + " " + fmt(byp) + " l");
            return;
        }
        // to page coords...
        double cxp = margin + (cx-minX)*s, cyp = margin + (maxY-cy)*s;
        double oxp = margin + (0.-minX)*s, oyp = margin + (maxY-0.)*s; // disk center
        double Rp  = MyMath.hypot(axp-cxp, ayp-cyp);
        double angA = Math.atan2(ayp-cyp, axp-cxp);
        double angB = Math.atan2(byp-cyp, bxp-cxp);
        // pick the arc (CCW vs CW from A to B) whose midpoint is nearer the disk
        // center -- that is the geodesic, which bulges toward the center...
        double dCCW = angB - angA;
        while (dCCW <= 0.) dCCW += 2*Math.PI; // (0, 2pi]
        double mid = angA + dCCW/2.;
        double mxCCW = cxp + Rp*Math.cos(mid),      myCCW = cyp + Rp*Math.sin(mid);
        double mxCW  = cxp - Rp*Math.cos(mid),      myCW  = cyp - Rp*Math.sin(mid);
        boolean ccw = hypotSqrd(mxCCW-oxp, myCCW-oyp) <= hypotSqrd(mxCW-oxp, myCW-oyp);
        w.println(fmt(cxp) + " " + fmt(cyp) + " " + fmt(Rp) + " "
                + fmt(Math.toDegrees(angA)) + " " + fmt(Math.toDegrees(angB))
                + (ccw ? " arc" : " arcn"));
    }


    public void read(DataInputStream r) throws IOException {
        initialize();
        perimeterEdges.removeAllElements();

        String line = r.readLine();
        int fileVersion[] = {0,0,0};
        if (line.startsWith("tyler data format "))
        {
            StringTokenizer st = new StringTokenizer(line, " ");
            st.nextToken(); // skip over "tyler"
            st.nextToken(); // skip over "data"
            st.nextToken(); // skip over "format"
            String versionString = st.nextToken();
            {
                StringTokenizer vt = new StringTokenizer(versionString, ".");
                fileVersion[0] = Integer.parseInt(vt.nextToken());
                if (vt.hasMoreTokens())
                    fileVersion[1] = Integer.parseInt(vt.nextToken());
                if (vt.hasMoreTokens())
                    fileVersion[2] = Integer.parseInt(vt.nextToken());
            }
            line = r.readLine();
        }

        if (fileVersion[0] > myVersion[0]
         || fileVersion[0] == myVersion[0] && fileVersion[1] > myVersion[1])
        {
            // XXXhatch- use message box!
            System.out.println("Oops, file version "
                  +fileVersion[0]+"."+fileVersion[1]+"."+fileVersion[2]
                  +" is from the future,");
            System.out.println("my version is "
                  +myVersion[0]+"."+myVersion[1]+"."+myVersion[2]
                  +".");
            System.out.println("Please use a newer version of Tyler.\n");
            throw new IOException(); // XXXhatch shouldn't really be an exception, should return failure
        }
        if (fileVersion[0] < myVersion[0])
        {
            // XXXhatch- use message box!
            System.out.println("Oops, can't read majorly old file version "
                  +fileVersion[0]+"."+fileVersion[1]+"."+fileVersion[2]
                  +",");
            System.out.println("my version is "
                  +myVersion[0]+"."+myVersion[1]+"."+myVersion[2]
                  +".");
            throw new IOException(); // XXXhatch shouldn't really be an exception, should return failure
        }

        if (fileVersion[0] > 0
         || fileVersion[0] == 0 && fileVersion[1] >= 1)
        {
            setCurvature(parseDouble(line));
            line = r.readLine();
            if (getCurvature() != 0.)
            {
                setCurvatureBasedOn(Rational.parseRationalList(line));
                line = r.readLine();
            }
        }
        else
            setCurvature(0.); // Euclidean

        int npolys = Integer.parseInt(line);
        Rational p=null;
        for(int i=0; i<npolys; i++) {
            StringTokenizer st = new StringTokenizer(r.readLine(), ", ");
            p = Rational.parseRational(st.nextToken());
            double Xs[] = new double[p.n];
            double Ys[] = new double[p.n];
            double xsum=0, ysum=0;
            for(int j=0; j<p.n; j++) {
                xsum += Xs[j] = parseDouble(st.nextToken());
                ysum += Ys[j] = parseDouble(st.nextToken());
            }
            // optional trailing tokens, order-independent:
            //   @a/b    -> this quad is a rhombus, corner angle (a/b)*PI (0.2.0)
            //   #rrggbb -> explicit tile color                          (0.1.1)
            Rational rhombusAngle = null;
            Color color = null;
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                if (tok.startsWith("@"))
                    rhombusAngle = Rational.parseRational(tok.substring(1));
                else if (tok.startsWith("#"))
                    color = parseColor(tok);
            }
            Poly poly;
            if (rhombusAngle != null)
                poly = new Poly(rhombusAngle, Xs, Ys); // keep the exact rhombus, don't regularize
            else
                poly = new Poly(p, xsum/p.n, ysum/p.n, Xs[0], Ys[0],
                                curvature);
            poly.color = color;
            polys.addElement(poly);
        }
        int nperimeterEdges = Integer.parseInt(r.readLine());
        for(int i=0; i<nperimeterEdges; i++) {
            StringTokenizer st = new StringTokenizer(r.readLine(), ", ");
            perimeterEdges.addElement(new Edge(
                    parseDouble(st.nextToken()),
                    parseDouble(st.nextToken()),
                    parseDouble(st.nextToken()),
                    parseDouble(st.nextToken()),
                    curvature));
        }
        StringTokenizer st = new StringTokenizer(r.readLine(), ", ");
        scale = parseDouble(st.nextToken());
        focusX = parseDouble(st.nextToken());
        focusY = parseDouble(st.nextToken());

        if(perimeterEdges.size() == 0)
            return;
        System.out.println("read " + npolys + " polys, " + perimeterEdges.size() + " perimeter edges");     
    } // read

    // Double.parseDouble is not in Java 1.1...
    private double parseDouble(String string)
    {
        return Double.valueOf(string).doubleValue();
    }
 
    // utility used by addPolyAtPerimeterEdge and deletePoly
    private static void addOrDeletePolyEdgesFromPerimeter(Poly poly,
                                                   Vector perimeterEdges,
                                                   double curvature,
                                                   boolean deleting)
    {     
        Rational p = poly.p;
        for (int i = 0; i < p.n; ++i)
        {
            double x0 = poly.X[i];
            double y0 = poly.Y[i];
            double x1 = poly.X[MOD(i+p.d,p.n)];
            double y1 = poly.Y[MOD(i+p.d,p.n)];
            boolean foundIt = false;
            //System.out.println("adding or deleting "+centerX+","+centerY+"");
            for (Enumeration e = perimeterEdges.elements(); e.hasMoreElements(); )
            {
                Edge edge = (Edge)e.nextElement();
                if ((EQ(x0, edge.x0, eps) && EQ(y0, edge.y0, eps)
                  && EQ(x1, edge.x1, eps) && EQ(y1, edge.y1, eps))
                 || (EQ(x0, edge.x1, eps) && EQ(y0, edge.y1, eps)
                  && EQ(x1, edge.x0, eps) && EQ(y1, edge.y0, eps)))
                {
                    //System.out.println("    "+edge.centerX+","+edge.centerY+" == "+centerX+","+centerY+", deleting");
                    perimeterEdges.removeElement(edge);
                    foundIt = true;
                    break;
                }
                else {
                    //System.out.println("    "+edge.centerX+","+edge.centerY+" != "+centerX+","+centerY+"");
                }  
            }
            if (!foundIt) {
                //System.out.println("    didn't find it, adding "+centerX+","+centerY+"");
                if (deleting)
                    perimeterEdges.addElement(new Edge(x1,y1,x0,y0,curvature));
                else // adding
                    perimeterEdges.addElement(new Edge(x0,y0,x1,y1,curvature));
            }
        }
    } // addOrDeletePolyEdgesFromPerimeter
    
    
    // note side effect: stores most recent and second-most-recent
    // edge used
    private void addPolyAtPerimeterEdge(Rational p,
                                        Edge edge,
                                        Vector polys,
                                        Vector perimeterEdges,
                                        double curvature)
    {
        //System.out.println("in addPolyAtPerimeterEdge(p="+p+",edge=(p0="+edge.x0+","+edge.y0+"  center="+edge.centerX+","+edge.centerY+" p1="+edge.x1+","+edge.y1+"),curvature="+curvature+")");
        if (curvature == 0.) // Euclidean
        {
            double towardsPolyCenterX = edge.y1-edge.y0;
            double towardsPolyCenterY = -(edge.x1-edge.x0);
            double length = MyMath.hypot(towardsPolyCenterX, towardsPolyCenterY);
            // normalize...
            towardsPolyCenterX *= 1./length;
            towardsPolyCenterY *= 1./length;
            double polyInRadius = .5 / Math.tan(Math.PI/p.toDouble());
            //double polyCircumRadius = .5 / Math.sin(Math.PI/p.toDouble());
            double polyCenterX = edge.centerX + polyInRadius * towardsPolyCenterX;
            double polyCenterY = edge.centerY + polyInRadius * towardsPolyCenterY;
            Poly poly = new Poly(p, polyCenterX, polyCenterY, edge.x0, edge.y0, curvature);
            poly.color = currentColor;
            polys.addElement(poly);
            addOrDeletePolyEdgesFromPerimeter(poly, perimeterEdges, curvature, false);
        }
        else if (curvature < 0.) // Hyperbolic
        {
            Isometry2 edgeCenterToOrigin = Isometry2.pureTranslation(-edge.centerX, -edge.centerY);
            Isometry2 originToEdgeCenter = Isometry2.pureTranslation(edge.centerX, edge.centerY);
            Complex p0_ = edgeCenterToOrigin.apply(edge.x0,edge.y0);

            double towardsPolyCenterX_ = -p0_.y;
            double towardsPolyCenterY_ = p0_.x;
            double length = MyMath.hypot(towardsPolyCenterX_,towardsPolyCenterY_);
            // normalize...
            towardsPolyCenterX_ *= 1./length;
            towardsPolyCenterY_ *= 1./length;
            double halfEdgeLength = HyperbolicUtils.e2hNorm(MyMath.hypot(p0_.x,p0_.y));
            double hInRadius = HyperbolicUtils.polygonInRadius(p.toDouble(), halfEdgeLength);
            double eInRadius = HyperbolicUtils.h2eNorm(hInRadius);
            double polyCenterX_ = eInRadius * towardsPolyCenterX_;
            double polyCenterY_ = eInRadius * towardsPolyCenterY_;
            Complex polyCenter = originToEdgeCenter.apply(polyCenterX_,
                                                          polyCenterY_);
            Poly poly = new Poly(p, polyCenter.x, polyCenter.y, edge.x0, edge.y0, curvature);
            poly.color = currentColor;
            polys.addElement(poly);
            addOrDeletePolyEdgesFromPerimeter(poly, perimeterEdges, curvature, false);
        }
        else // Spherical
        {
            SphericalIsometry2 edgeCenterToOrigin = SphericalIsometry2.pureTranslation(-edge.centerX, -edge.centerY);
            SphericalIsometry2 originToEdgeCenter = SphericalIsometry2.pureTranslation(edge.centerX, edge.centerY);
            Complex p0_ = edgeCenterToOrigin.apply(edge.x0,edge.y0);

            double towardsPolyCenterX_ = -p0_.y;
            double towardsPolyCenterY_ = p0_.x;
            double length = MyMath.hypot(towardsPolyCenterX_,towardsPolyCenterY_);
            // normalize...
            towardsPolyCenterX_ *= 1./length;
            towardsPolyCenterY_ *= 1./length;
            double halfEdgeLength = SphericalUtils.e2sNorm(MyMath.hypot(p0_.x,p0_.y));
            double hInRadius = SphericalUtils.polygonInRadius(p.toDouble(), halfEdgeLength);
            double eInRadius = SphericalUtils.s2eNorm(hInRadius);
            double polyCenterX_ = eInRadius * towardsPolyCenterX_;
            double polyCenterY_ = eInRadius * towardsPolyCenterY_;
            Complex polyCenter = originToEdgeCenter.apply(polyCenterX_,
                                                          polyCenterY_);
            Poly poly = new Poly(p, polyCenter.x, polyCenter.y, edge.x0, edge.y0, curvature);
            poly.color = currentColor;
            polys.addElement(poly);
            addOrDeletePolyEdgesFromPerimeter(poly, perimeterEdges, curvature, false);
        }

        secondMostRecentEdge = mostRecentEdge;
        mostRecentEdge = edge;
    } // addPolyAtPerimeterEdge

    //
    // Attach a rhombus to a perimeter edge (Euclidean only).
    //
    // The rhombus shares the edge as one side, sits on the same side as a
    // polygon would (towards the empty exterior), and has interior angle
    // angle*PI at the two endpoints of the shared edge.  Entering the acute
    // angle (e.g. 2/5 -> 72 degrees) or the obtuse one (3/5 -> 108 degrees)
    // selects the two possible orientations, so no separate flip control is
    // needed.  All four sides are unit length, matching the rest of Tyler.
    //
    private void addRhombusAtPerimeterEdge(Rational angle,
                                           Edge edge,
                                           Vector polys,
                                           Vector perimeterEdges)
    {
        double ux = edge.x1 - edge.x0;
        double uy = edge.y1 - edge.y0;
        double len = MyMath.hypot(ux, uy);
        ux *= 1./len;
        uy *= 1./len;
        double a = angle.toDouble() * Math.PI;
        double ca = Math.cos(a), sa = Math.sin(a);
        // second edge direction = base direction rotated by -a, which points
        // to the same side as (edge.y1-edge.y0, -(edge.x1-edge.x0)), i.e. the
        // side a polygon's center would be on...
        double wx = ux*ca + uy*sa;
        double wy = -ux*sa + uy*ca;
        double Xs[] = new double[4];
        double Ys[] = new double[4];
        // Wind CCW (interior on the left of each directed edge), matching Tyler's
        // regular polygons, so the free edges are oriented for the next tile to
        // attach on the exterior side.  Vertex 0 keeps corner angle angle*PI.
        Xs[0] = edge.x0;      Ys[0] = edge.y0;      // v0
        Xs[1] = edge.x0 + wx; Ys[1] = edge.y0 + wy; // v0 + w
        Xs[2] = edge.x1 + wx; Ys[2] = edge.y1 + wy; // v1 + w
        Xs[3] = edge.x1;      Ys[3] = edge.y1;      // v1

        Poly poly = new Poly(angle, Xs, Ys);
        poly.color = currentColor;
        polys.addElement(poly);
        addOrDeletePolyEdgesFromPerimeter(poly, perimeterEdges, 0., false);

        secondMostRecentEdge = mostRecentEdge;
        mostRecentEdge = edge;
    } // addRhombusAtPerimeterEdge

    // Add whatever tile is currently selected (a rhombus if rhombus mode is on
    // and we're Euclidean, otherwise the current regular polygon) at a given
    // perimeter edge...
    private void addCurrentAtPerimeterEdge(Edge edge,
                                           Vector polys,
                                           Vector perimeterEdges,
                                           double curvature)
    {
        if (currentRhombus != null && curvature == 0.)
            addRhombusAtPerimeterEdge(currentRhombus, edge, polys, perimeterEdges);
        else
            addPolyAtPerimeterEdge(mostRecentP, edge, polys, perimeterEdges, curvature);
    }
    // ...or at the perimeter edge closest to a point.
    private void addCurrentAtClosestPerimeterEdge(double x, double y,
                                                  Vector polys,
                                                  Vector perimeterEdges,
                                                  double curvature)
    {
        Edge edge = findClosestPerimeterEdge(x, y, perimeterEdges);
        addCurrentAtPerimeterEdge(edge, polys, perimeterEdges, curvature);
    }
                                      
    
    private static Edge findClosestPerimeterEdge(double x, double y, Vector perimeterEdges)
    {
        double minDistSqrd = Double.POSITIVE_INFINITY;
        Edge closest = null;
        for(Enumeration e=perimeterEdges.elements(); e.hasMoreElements(); ) {
            Edge edge = (Edge)e.nextElement();
            double distSqrd = hypotSqrd(edge.centerX - x, edge.centerY - y);
            // XXX Don't use LT with eps, since we can be way smaller than that!
            // XXX need to figure out a consistent tolerance methodology
            if (distSqrd < minDistSqrd) {
                minDistSqrd = distSqrd;
                closest = edge;
            }
        }
        // XXX hack! now that we are not using LT, it's anyone's guess
        // XXX which of the two seed edges we will get.
        // XXX treat this as a special case, and always get the first one.
        // XXX All hell will break loose otherwise.
        if (perimeterEdges.size() == 2)
            closest = (Edge)perimeterEdges.elementAt(0);

        return closest;
    } // findClosestPerimeterEdge
    
    private static Poly findClosestPoly(double x, double y, Vector polys)
    {
        double minDistSqrd = Double.POSITIVE_INFINITY;
        Poly closest = null;
        for(Enumeration e=polys.elements(); e.hasMoreElements(); ) {
            Poly poly = (Poly)e.nextElement();
            double distSqrd = hypotSqrd(poly.centerX - x, poly.centerY - y);
            // XXX Don't use LT with eps, since we can be way smaller than that!
            // XXX need to figure out a consistent tolerance methodology
            if (distSqrd < minDistSqrd) {
                minDistSqrd = distSqrd;
                closest = poly;
            }
        }
        return closest;
    } // findClosestPoly

    //
    // arrow[0] is the closest element center (tile center, edge center,
    //           or vertex).
    // arrow[1] is the center of the closest (in angle) adjacent element.
    //
    private static double[/*2*/][/*2*/] pickArrow(double x, double y,
                                          Vector polys,
                                          int mode,
                                          double curvature,
                                          boolean extraAnglesIfEdge)
    {
        ClosestArrowFinder closestArrowFinder = new ClosestArrowFinder(x, y, mode, 0); // XXX NOT eps!  need to rethink the whole eps thing, for tiny parts of poincare disk :-(
        {
            for (Enumeration e = polys.elements(); e.hasMoreElements(); )
            {
                Poly poly = (Poly)e.nextElement();
                closestArrowFinder.anotherPossibleTail(poly.centerX, poly.centerY, poly);
                int n = poly.p.n;
                int d = poly.p.d;
                {
                    for (int i = 0; i < n; ++i)
                    {
                        double vert[/*2*/] = {poly.X[i], poly.Y[i]};
                        closestArrowFinder.anotherPossibleTail(vert[0], vert[1], vert);

                        Edge edge = new Edge(poly.X[i], poly.Y[i],
                                             poly.X[MOD(i-d,n)], poly.Y[MOD(i-d,n)],
                                             curvature);
                        closestArrowFinder.anotherPossibleTail(
                                             edge.centerX, edge.centerY, edge);
                    }
                }
            }
        }
        Object tail = closestArrowFinder.getBestTailObj();
        if (tail == null)
            return null;
        else if (tail instanceof Poly)
        {
            // A Poly is closest.
            // Find the closest (in angle) neighbor vertex or edge.
            Poly poly = (Poly)tail;
            int n = poly.p.n;
            int d = poly.p.d;
            for (int i = 0; i < n; ++i)
            {
                closestArrowFinder.anotherPossibleHead(poly.X[i], poly.Y[i]);
                Edge edge = new Edge(poly.X[i], poly.Y[i],
                                     poly.X[MOD(i+d,n)], poly.Y[MOD(i+d,n)],
                                     curvature);
                closestArrowFinder.anotherPossibleHead(edge.centerX, edge.centerY);
            }
        }
        else if (tail instanceof Edge)
        {
            // An Edge is closest.
            // Find the closest (in angle) neighbor poly or vertex.
            Edge edge = (Edge)tail;
            closestArrowFinder.anotherPossibleHead(edge.x0, edge.y0);
            closestArrowFinder.anotherPossibleHead(edge.x1, edge.y1);
            for (Enumeration e = polys.elements(); e.hasMoreElements(); )
            {
                Poly poly = (Poly)e.nextElement();
                int n = poly.p.n;
                int d = poly.p.d;
                for (int i = 0; i < n; ++i)
                {
                    if ((EQ(poly.X[i], edge.x0, eps)
                      && EQ(poly.Y[i], edge.y0, eps)
                      && EQ(poly.X[MOD(i+d,n)], edge.x1, eps)
                      && EQ(poly.Y[MOD(i+d,n)], edge.y1, eps))
                     || (EQ(poly.X[i], edge.x1, eps)
                      && EQ(poly.Y[i], edge.y1, eps)
                      && EQ(poly.X[MOD(i+d,n)], edge.x0, eps)
                      && EQ(poly.Y[MOD(i+d,n)], edge.y0, eps)))
                    {
                        closestArrowFinder.anotherPossibleHead(poly.centerX, poly.centerY);
                        if (extraAnglesIfEdge)
                        {
                            // An edge only has four neighbors
                            // (two end points and two tiles)
                            // but sometimes that doesn't seem like
                            // enough, so we add four more directions
                            // between them.

                            // XXX wrong! need to bisect the angle
                            //closestArrowFinder.anotherPossibleHead(.5*(poly.centerX+edge.x0), .5*(poly.centerY+edge.y0));
                            //closestArrowFinder.anotherPossibleHead(.5*(poly.centerX+edge.x1), .5*(poly.centerY+edge.y1));

                            if (curvature == 0.) // Euclidean
                            {
                                double edgeCenterToVertex = MyMath.hypot(edge.x0-edge.centerX, edge.y0-edge.centerY);
                                double edgeCenterToTileCenter = MyMath.hypot(poly.centerX-edge.centerX, poly.centerY-edge.centerY);
                                double t = edgeCenterToVertex / (edgeCenterToVertex + edgeCenterToTileCenter);
                                closestArrowFinder.anotherPossibleHead(
                                        edge.x0 + t * (poly.centerX-edge.x0),
                                        edge.y0 + t * (poly.centerY-edge.y0));
                                closestArrowFinder.anotherPossibleHead(
                                        edge.x1 + t * (poly.centerX-edge.x1),
                                        edge.y1 + t * (poly.centerY-edge.y1));
                            }
                            else if (curvature < 0.) // Hyperbolic
                            {
                                double edgeCenterToVertex = HyperbolicUtils.hdist(edge.centerX,edge.centerY,edge.x0,edge.y0);
                                double edgeCenterToTileCenter = HyperbolicUtils.hdist(edge.centerX,edge.centerY,poly.centerX,poly.centerY);
                                double tileCenterToVertex = HyperbolicUtils.hdist(poly.centerX,poly.centerY,edge.x0,edge.y0);

                                //
                                // Let a,b,f be the edge center, v0, and
                                // tile center respectively.
                                // We want to find the point c which
                                // is the intersection of the line segment
                                // b->f with the bisector
                                // of the angle baf.
                                //
                                // We can use hyperbolic trig
                                // on the triangle with vertices a,b,c.
                                // Let alpha,beta,gamma be the respective
                                // angles and A,B,C be the opposite sides
                                // in this triangle.
                                //
                                // The known values are:
                                //      alpha = 45 degrees
                                //      beta = asin(sinh(|f-a|)/sinh(|f-b|))
                                //              (by law of hyperbolic sines
                                //               on the right triangle baf)
                                //      C = |b-a|
                                // (where | - | denotes distance, not literal
                                // subtraction which wouldn't make sense).
                                //
                                double alpha = Math.PI*.25;
                                double beta = Math.asin(MyMath.sinh(edgeCenterToTileCenter)/MyMath.sinh(tileCenterToVertex));
                                double C = edgeCenterToVertex;
                                double A = HyperbolicUtils.solveTriangleAFromAlphaCBeta(alpha, C, beta);

                                double t = A / tileCenterToVertex;

                                Complex p0 = HyperbolicUtils.hlerp(edge.x0,edge.y0,poly.centerX,poly.centerY,t);
                                Complex p1 = HyperbolicUtils.hlerp(edge.x1,edge.y1,poly.centerX,poly.centerY,t);
                                closestArrowFinder.anotherPossibleHead(p0.x,p0.y);
                                closestArrowFinder.anotherPossibleHead(p1.x,p1.y);
                            }
                            else // Spherical
                            {
                                // XXX implement me!
                            }
                        }
                        break;
                    }
                }
            }
        }
        else // it's a vertex
        {
            // A vertex is closest.
            // Find the closest (in angle) neighbor poly or edge.
            double vert[/*2*/] = (double[])tail;
            for (Enumeration e = polys.elements(); e.hasMoreElements(); )
            {
                Poly poly = (Poly)e.nextElement();
                int n = poly.p.n;
                int d = poly.p.d;
                for (int i = 0; i < n; ++i)
                {
                    if (EQ(poly.X[i], vert[0], eps)
                     && EQ(poly.Y[i], vert[1], eps))
                    {
                        Edge prevEdge = new Edge(poly.X[MOD(i-d,n)], poly.Y[MOD(i-d,n)],
                                                 poly.X[i], poly.Y[i],
                                                 curvature);
                        closestArrowFinder.anotherPossibleHead(prevEdge.centerX, prevEdge.centerY);
                        closestArrowFinder.anotherPossibleHead(poly.centerX, poly.centerY);
                        Edge nextEdge = new Edge(poly.X[i], poly.Y[i],
                                                 poly.X[MOD(i+d,n)], poly.Y[MOD(i+d,n)],
                                                 curvature);
                        closestArrowFinder.anotherPossibleHead(nextEdge.centerX, nextEdge.centerY);
                        break;
                    }
                }
            }
        }

        double[/*2*/][/*2*/] arrow = closestArrowFinder.getBestArrow();
        return arrow;
    } // pickArrow
    

    //
    // Snap arrow[0] to the origin
    // and rotate arrow[1] to targetAngle.
    //
    private void snap(double arrow[/*2*/][/*2*/], double targetAng)
    {
        AbstractIsometry2 transform;

        if (TylerPanel.this.curvature == 0.) // Euclidean
        {
            EuclideanIsometry2 transl = EuclideanIsometry2.pureTranslation(-arrow[0][0], -arrow[0][1]);
            Complex temp = transl.apply(arrow[1][0],arrow[1][1]);
            double tempAng = Math.atan2(temp.y, temp.x);
            double rotAng = targetAng - tempAng;
            EuclideanIsometry2 rot = EuclideanIsometry2.pureRotation(rotAng);
            transform = AbstractIsometry2.mul(rot, transl);
        }
        else if (TylerPanel.this.curvature < 0.) // Hyperbolic
        {
            Isometry2 transl = Isometry2.pureTranslation(-arrow[0][0], -arrow[0][1]);
            Complex temp = transl.apply(arrow[1][0],arrow[1][1]);
            double tempAng = Math.atan2(temp.y, temp.x);
            double rotAng = targetAng - tempAng;
            Isometry2 rot = Isometry2.pureRotation(rotAng);
            transform = Isometry2.mul(rot, transl);
        }
        else // Spherical
        {
            // XXXhatch implement me!
            transform = null;
        }

        // apply transform to everything...
        // XXXhatch - cumulative errors! should just
        // have a pervasive transform for rendering,
        // and never touch the coords themselves
        {
            Complex temp = new Complex();
            for (Enumeration e = polys.elements(); e.hasMoreElements(); )
            {
                Poly poly = (Poly)e.nextElement();
                transform.apply(poly.centerX, poly.centerY, temp);
                poly.centerX = temp.x;
                poly.centerY = temp.y;
                int n = poly.p.n;
                for (int i = 0; i < n; ++i)
                {
                    transform.apply(poly.X[i], poly.Y[i], temp);
                    poly.X[i] = temp.x;
                    poly.Y[i] = temp.y;
                }
            }
            for (Enumeration e = perimeterEdges.elements(); e.hasMoreElements(); )
            {
                Edge edge = (Edge)e.nextElement();

                transform.apply(edge.centerX, edge.centerY, temp);
                edge.centerX = temp.x;
                edge.centerY = temp.y;
                transform.apply(edge.x0, edge.y0, temp);

                edge.x0 = temp.x;
                edge.y0 = temp.y;
                transform.apply(edge.x1, edge.y1, temp);
                edge.x1 = temp.x;
                edge.y1 = temp.y;
            }
        }
        focusX = 0.;
        focusY = 0.;
    } // snap

    public void drift(double dir) // dir is an angle in radians
    {
        pushCursor(Cursor.WAIT_CURSOR);

        // caller thinks in terms of moving the tiling
        // with respect to the focus, but we want to think
        // in terms of moving the focus with respect to the tiling...
        dir += Math.PI;

        double xDir = Math.cos(dir);
        double yDir = Math.sin(dir);

        double arrow[][] = pickArrow(curvature==0. ? focusX : 0.,
                                     curvature==0. ? focusY : 0.,
                                     polys,
                                     ClosestArrowFinder.CLOSEST_ANGLE,
                                     curvature,
                                     false);
        // arrow head is irrelevant at this point

        arrow = pickArrow(arrow[0][0] + xDir * 1e-3,
                          arrow[0][1] + yDir * 1e-3,
                          polys,
                          ClosestArrowFinder.CLOSEST_ANGLE,
                          curvature,
                          false);

        // center arrow head, then spin to closest axis-aligned position
        spin(arrow[1][0], arrow[1][1], ClosestArrowFinder.CLOSEST_ANGLE);

        popCursor();
    } // drift

    // Snap to the next any-axis-aligned orientation
    // in the given direction (CCW or CW),
    // or snap to the closest axis-aligned orientation (CLOSEST_ANGLE).
    public void spin(double x, double y, int dir)
    {
        pushCursor(Cursor.WAIT_CURSOR);

        // caller thinks in terms of spinning the tiling
        // with respect to the screen,
        // but we want to think in terms of spinning the screen
        // with respect to the tiling...
        dir = ClosestArrowFinder.reverseMode(dir); // CCW -> CW,  CW -> CCW

        double arrow[][] = pickArrow(x, y, polys, ClosestArrowFinder.CLOSEST_ANGLE, curvature, false);
        // arrow head is irrelevant at this point

        double minAngleDiff = Double.POSITIVE_INFINITY;
        double bestAxisAngle = 0.; // shut up compiler
        double bestArrow[][] = null; // shut up compiler
        //System.out.println();
        for (int i = 0; i < 4; ++i)
        {
            double axisAngle = .5*Math.PI * i;
            double xDir = Math.cos(axisAngle);
            double yDir = Math.sin(axisAngle);
            axisAngle = Math.atan2(yDir, xDir); // make sure in canonical range
            //System.out.println("axisAngle = "+axisAngle*180/Math.PI);
            double thisArrow[][] = pickArrow(arrow[0][0] + xDir * 1e-3,
                                             arrow[0][1] + yDir * 1e-3,
                                             polys,
                                             dir,
                                             curvature,
                                             true);
            double thisAngle = Math.atan2(thisArrow[1][1]-thisArrow[0][1],
                                          thisArrow[1][0]-thisArrow[0][0]);
            //System.out.println("thisAngle = "+thisAngle*180/Math.PI);
            double thisAngleDiff = thisAngle - axisAngle;

            //System.out.println("    "+i+": thisAngleDiff = "+thisAngleDiff*180/Math.PI);
            switch (dir)
            {
                case ClosestArrowFinder.CLOSEST_ANGLE:
                    while (thisAngleDiff <= -Math.PI)
                        thisAngleDiff += 2*Math.PI;
                    while (thisAngleDiff > Math.PI)
                        thisAngleDiff -= 2*Math.PI;
                    break;
                case ClosestArrowFinder.CCW:
                    while (LEQ(thisAngleDiff, 0., eps))
                        thisAngleDiff += 2*Math.PI;
                    break;
                case ClosestArrowFinder.CW:
                    while (GEQ(thisAngleDiff, 0., eps))
                        thisAngleDiff -= 2*Math.PI;
                    break;
            }
            //System.out.println("    "+i+": thisAngleDiff = "+thisAngleDiff*180/Math.PI);
            thisAngleDiff = Math.abs(thisAngleDiff);
            if (thisAngleDiff < minAngleDiff)
            {
                minAngleDiff = thisAngleDiff;
                bestAxisAngle = axisAngle;
                bestArrow = thisArrow;
            }
        }
        snap(bestArrow, bestAxisAngle);

        popCursor();
    } // spin

    
    //
    // Do this when user hits the delete key
    // (not static since it calls deletePoly which is not static)
    //
    private void deleteClosestPoly(double x, double y, Vector polys, Vector perimeterEdges, double curvature) {
        deletePoly(findClosestPoly(x, y, polys), polys, perimeterEdges, curvature);
    }

    // (not static since it calls initialize which is not static)
    private void deletePoly(Poly poly, Vector polys, Vector perimeterEdges, double curvature) {
        if (poly != null) {
            polys.removeElement(poly);
            addOrDeletePolyEdgesFromPerimeter(poly, perimeterEdges, curvature, true);
        }
        if (perimeterEdges.size() == 0) {
            initialize(); // add back the seed edge
        }
    }

    //
    // Do this when user hits a number key
    // (not static since calls addPolyAtPerimeterEdge which is not static)
    //   
    private void
    addPolyAtClosestPerimeterEdge(Rational p, double x, double y, Vector polys, Vector perimeterEdges, double curvature) {
        Edge edge = findClosestPerimeterEdge(x, y, perimeterEdges);
        addPolyAtPerimeterEdge(p, edge, polys, perimeterEdges, curvature);
    }
    
    //
    // Do this when user hits 'extrapolate' key.
    // (not static since calls addPolyAtClosestPerimeterEdge which is not static)
    //
    private void
    addPolyExtrapolated(Rational p,
                        Edge edge0, Edge edge1,
                        Vector polys, Vector perimeterEdges,
                        double curvature)
    {
        if (edge0 == null || edge1 == null)
            return;
        Edge tempEdge = extrapolateEdges(edge0, edge1, curvature);
        addPolyAtClosestPerimeterEdge(p,
                                      tempEdge.centerX, 
                                      tempEdge.centerY,
                                      polys, perimeterEdges,
                                      curvature);
    } // addPolyExtrapolated
    

    public void paint(Graphics g) {
        //super.paint(g);
        g = startPaint(g);
        g.setColor(Color.white);
        g.fillRect(0, 0, size().width, size().height);
        g.setColor(Color.black);

        double windowCenterX = size().width*.5;
        double windowCenterY = size().height*.5;

        double Scale = (TylerPanel.this.curvature == 0. ? scale : scale / Math.abs(TylerPanel.this.curvature));
        for(Enumeration e=polys.elements(); e.hasMoreElements(); ) {
            Poly poly = (Poly)e.nextElement();
            Rational p = poly.p;
            // don't fill stars...
            if (p.d != 1)
                continue;
            int xs[] = new int[p.n];
            int ys[] = new int[p.n];
            // assumes p.n,p.d are relatively prime... (so if we decide to fill stars, this still won't work if they aren't relatively prime)
            for(int i = 0; i < p.n; i++) {
                xs[i] = (int)((poly.X[MOD(i*p.d,p.n)]-focusX)*Scale+windowCenterX);
                ys[i] = (int)((poly.Y[MOD(i*p.d,p.n)]-focusY)*Scale+windowCenterY);
            }
            Color newColor = effectiveColor(poly);
            if (doFill) {
                g.setColor(newColor);
                g.fillPolygon(xs, ys, p.n);
            }
        }

        MyGraphics mg = new MyGraphics(g, size());
        mg.fitToPixel(focusX, focusY, 1./Scale, 1./Scale);


        if (curvature < 0.
         && circleThickness > 0)
        {
            g.setColor(Color.black);
            for (double e = 0; e <= circleThickness-1.; e += .5)
            {
                mg.smartDrawArc(1.-e/Scale, 0., 0., 1/(1.-e/Scale), 0., 2*Math.PI*(1.-e/Scale), antiAlias && press == null);
            }
        }

        //g.setColor(Color.gray.brighter());
        g.setColor(Color.black);

        for(Enumeration e=polys.elements(); e.hasMoreElements(); ) {
            Poly poly = (Poly)e.nextElement();
            Rational p = poly.p;
            for(int i = 0; i < p.n; i++) {
                double x0 = poly.X[i];
                double y0 = poly.Y[i];
                double x1 = poly.X[MOD(i+p.d,p.n)];
                double y1 = poly.Y[MOD(i+p.d,p.n)];
                if (doublePairCmp(x0,y0,x1,y1,eps) <= 0) // avoid drawing segment twice by only drawing them if endpoints are canonically ordered
                {
                    if (curvature < 0. // hyperbolic
                     && doArcs
                     && press == null) // don't do it while dragging
                        drawPoincareArc(mg, x0, y0, x1, y1, antiAlias && press == null);
                    else
                        mg.drawLine(x0, y0, x1, y1, antiAlias && press == null);
                }
            }
        }
        // draw the perimeter edges we missed due to not being in canonical order
        if (perimeterEdges.size() > 2) // don't draw seed edges
            for (Enumeration e = perimeterEdges.elements(); e.hasMoreElements(); )
            {
                Edge edge = (Edge)e.nextElement();
                if (doublePairCmp(edge.x0,edge.y0,edge.x1,edge.y1,eps) > 0) // i.e. if we didn't draw it during the above loop over polys
                {
                    if (curvature < 0. // hyperbolic
                     && doArcs
                     && press == null) // don't do it while dragging
                        drawPoincareArc(mg, edge.x0, edge.y0, edge.x1, edge.y1,
                                        antiAlias && press == null);
                    else
                        mg.drawLine(edge.x0, edge.y0, edge.x1, edge.y1,
                                    antiAlias && press == null);
                }
            }

        if (arrowToDraw != null)
        {
            g.setColor(Color.lightGray);

            double x0 = (arrowToDraw[0][0]-focusX)*Scale+windowCenterX;
            double y0 = (arrowToDraw[0][1]-focusY)*Scale+windowCenterY;
            double x1 = (arrowToDraw[1][0]-focusX)*Scale+windowCenterX;
            double y1 = (arrowToDraw[1][1]-focusY)*Scale+windowCenterY;

            // only go halfway to original head...
            x1 = (x0+x1)*.5;
            y1 = (y0+y1)*.5;

            double xDir = x1-x0;
            double yDir = y1-y0;
            double xPerp = -yDir;
            double yPerp = xDir;

            double x2 = x1 + .25 * (-xDir + Math.sqrt(1./3.)*xPerp);
            double y2 = y1 + .25 * (-yDir + Math.sqrt(1./3.)*yPerp);
            double x3 = x1 + .25 * (-xDir - Math.sqrt(1./3.)*xPerp);
            double y3 = y1 + .25 * (-yDir - Math.sqrt(1./3.)*yPerp);

            g.drawLine((int)x0, (int)y0, (int)x1, (int)y1);
            g.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
            g.drawLine((int)x1, (int)y1, (int)x3, (int)y3);
        }

        endPaint(); // swaps double buffers
    } // end paint

    // Return -1, 0, or 1
    // depending on whether x0,y0 is less than, equal to, or greater than
    // x1,y1 lexicographically, with tolerance eps.
    private static int
    doublePairCmp(double x0, double y0, double x1, double y1, double eps)
    {
        if (LT(x0, x1, eps)) return -1;
        if (GT(x0, x1, eps)) return 1;
        if (LT(y0, y1, eps)) return -1;
        if (GT(y0, y1, eps)) return 1;
        return 0;
    } // doublePairCmp

    //
    // Draw the geodesic segment from x0,y0 to x1,y1 in the Poincare disk.
    // This is an arc of a circle that crosses the unit circle at right angles.
    //
    private static void
    drawPoincareArc(MyGraphics mg, double x0, double y0, double x1, double y1,
                    boolean antiAlias)
    {

        Isometry2 take_x0y0_to_origin = Isometry2.pureTranslation(-x0, -y0);
        Complex p1_ = take_x0y0_to_origin.apply(x1, y1);
        double tangentAngle = Math.atan2(p1_.y, p1_.x);
        double normalAngle = tangentAngle + Math.PI/2;
        double tangentX = Math.cos(tangentAngle);
        double tangentY = Math.sin(tangentAngle);
        double normalX = -tangentY;
        double normalY = tangentX;
        double tangentCoeff = tangentX * (x1-x0) + tangentY * (y1-y0);
        double normalCoeff = normalX * (x1-x0) + normalY * (y1-y0);
        // c = 1 / (arc radius)
        // c and arcLength were derived on paper.
        double c = 2 * normalCoeff / (normalCoeff*normalCoeff + tangentCoeff*tangentCoeff);
        double arcLength = tangentCoeff * MyMath.asin_over_x(tangentCoeff * c);

        // XXX I think this is necessary because y is still reversed
        normalAngle = normalAngle - Math.PI;

        mg.smartDrawArc(x0, y0,
                        normalAngle,
                        c,
                        0., arcLength,
                        antiAlias);
    } // drawPoincareArc
    
    
    private void initialize() {
        polys.removeAllElements();
        perimeterEdges.removeAllElements();

        // reset pan params
        focusX = 0.;
        focusY = 0.;

        // Add the seed edge, in both directions

        if (curvature == 0.) // Euclidean
        {
            double x = 0.;
            double y = 0.;
            // The first of the two edges is always matched first.
            // The order of the two edges is right even though it seems wrong,
            // because the screen is drawn upside down.
            perimeterEdges.addElement(new Edge(x-.5, y,
                                               x+.5, y, curvature));
            perimeterEdges.addElement(new Edge(x+.5, y,
                                               x-.5, y, curvature));
        }
        else // Hyperbolic or Spherical
        {
            // The first of the two edges is always matched first.
            // The order of the two edges is right even though it seems wrong,
            // because the screen is drawn upside down.
            perimeterEdges.addElement(new Edge(
                                          0.,0.,
                                          hyperbolicEdgeLength,0., curvature));
            perimeterEdges.addElement(new Edge(
                                          hyperbolicEdgeLength,0.,
                                          0.,0., curvature));
        }

        repaint();
    }
    
    public void setCurrentP(Rational p) {
        mostRecentP = p;
        currentRhombus = null; // choosing a polygon leaves rhombus mode
        if (applet != null) applet.rhombusOff(); // keep the Rhombus checkbox in sync
        requestFocus();
    }
    
    public void setScale(double length) {
        //System.out.println("scale "+scale+" -> "+length+"");
        scale = length;
        requestFocus();
        repaint();
    }

    public double getScale() {
        return scale;
    }

    public Rational[] getCurvatureBasedOn() {
        return hyperbolicEdgeLengthBasedOn;
    }
    public void setCurvatureBasedOn(Rational vertexConfig[])
    {
        double dVertexConfig[] = null;

        //
        // Hack for Jim McNeill...
        // If there are exactly two items with negative numerators,
        // treat the positive-numerator items
        // as a Schwarz polygon specification,
        // and the two negative-numerator items
        // as (negations of) polygons to place at two of the vertices
        // of the schwarz polygon, sized so that they
        // meet with a common edge length.
        //
        int nNegativeNumerators = 0;
        {
            for (int i = 0; i < vertexConfig.length; ++i)
                if (vertexConfig[i].n < 0)
                    ++nNegativeNumerators;
        }
        if (nNegativeNumerators == 2)
        {
            double schwarzPolygon[] = new double[vertexConfig.length-2];
            int substitute_poly_inds[] = new int[2];
            double substitute_polys[] = new double[2];
            int iNegativeNumerator = 0;
            int iSchwarz = 0;
            for (int i = 0; i < vertexConfig.length; ++i)
                if (vertexConfig[i].n < 0)
                {
                    substitute_poly_inds[iNegativeNumerator] = MOD(iSchwarz-1, schwarzPolygon.length); // previous schwarz polygon entry
                    substitute_polys[iNegativeNumerator] = -vertexConfig[i].toDouble();
                    iNegativeNumerator++;
                }
                else
                    schwarzPolygon[iSchwarz++] = vertexConfig[i].toDouble();

            hyperbolicEdgeLength = HyperbolicUtils.h2eNorm(
                2*HyperbolicUtils.calcJimsTilingHalfEdgeLength(
                    schwarzPolygon,
                    substitute_poly_inds[0], substitute_polys[0],
                    substitute_poly_inds[1], substitute_polys[1]));

            System.out.println("Jims edge length = "+hyperbolicEdgeLength);
        }
        else
        {
            {
                System.out.println("calculating hyperbolic edge length and curvature based on:");
                for (int i = 0; i < vertexConfig.length; ++i)
                    System.out.println("    " + vertexConfig[i]);
            }
            dVertexConfig = new double[vertexConfig.length];
            for (int i = 0; i < vertexConfig.length; ++i)
                dVertexConfig[i] = vertexConfig[i].toDouble();

            double coveringDensity = 1;
            hyperbolicEdgeLength = HyperbolicUtils.h2eNorm(
                  2*HyperbolicUtils.calcUniformTilingHalfEdgeLength(
                        dVertexConfig, 1, coveringDensity));
        }

        if (true) // XXX for now, reject euclidean or spherical
        {
            if (EQ(hyperbolicEdgeLength, 0., eps))
            {
                beep();
                hyperbolicEdgeLength = -getCurvature(); // revert
                System.out.println(Rational.rationalListToString(vertexConfig)+" rejected!\n");
                this.applet.addHyperbolicControls(); // reverts the text
                return;
            }
        }

        if (EQ(hyperbolicEdgeLength, 0., eps)) // hyperbolic failed, try spherical
            hyperbolicEdgeLength = -SphericalUtils.s2eNorm(
                  2*SphericalUtils.calcUniformTilingHalfEdgeLength(
                        dVertexConfig, 1));

        if (EQ(hyperbolicEdgeLength, 0., eps)) // both failed
            hyperbolicEdgeLength = 0; // so subsequent tests are easier

        hyperbolicEdgeLengthBasedOn = vertexConfig; // must be set before setCurvature, but after the premature return above

        //
        // Now set the curvature so that the edge length comes out to 1
        // in the center of the poincare disk...
        // (NOTE: This is probably not the mathematical definition
        //  of curvature, but the way we use it works-- we
        //  always divide the scale by -curvature when rendering).
        //
        setCurvature(-hyperbolicEdgeLength); // negative if hyperbolic, positive if spherical
        hyperbolicEdgeLength = Math.abs(hyperbolicEdgeLength);


        System.out.println("hyperbolicEdgeLength = "+hyperbolicEdgeLength+", curvature = "+curvature+"");
    } // setCurvatureBasedOn

    public double getCurvature()
    {
        return curvature;
    }
    public void setCurvature(double curvature)
    {
        if (curvature != 0.)
            this.applet.addHyperbolicControls();
        else
            this.applet.removeHyperbolicControls();
        System.out.println("curvature "+this.curvature+"-> "+curvature);
        if (curvature != this.curvature)
        {
            this.curvature = curvature;
            initialize(); // repaints
        }
    }
    
    public void deleteClosest() {
        double windowCenterX = size().width*.5;
        double windowCenterY = size().height*.5;
        double Scale = (curvature == 0. ? scale : scale / Math.abs(curvature));
        deleteClosestPoly(
            (currentPosition.x-windowCenterX)/Scale+focusX, 
            (currentPosition.y-windowCenterY)/Scale+focusY, 
            polys, perimeterEdges,
            curvature);
        repaint();  
    }

    public void reset() {
        requestFocus();
        initialize();
    }   
    
    private static double hypotSqrd(double x, double y)
    {
        return x*x + y*y;
    }
    
    // fuzzy less-than
    private static boolean LT(double a, double b, double eps)
    {
        return b-a > eps;
    }
    // fuzzy greater-than
    private static boolean GT(double a, double b, double eps)
    {
        return a-b > eps;
    }
    // fuzzy equals
    private static boolean EQ(double a, double b, double eps)
    {
        return a-b <= eps
            && b-a <= eps;
    }
    // fuzzy less-or-equal
    private static boolean LEQ(double a, double b, double eps)
    {
        return a-b <= eps;
    }
    // fuzzy greater-or-equal
    private static boolean GEQ(double a, double b, double eps)
    {
        return b-a <= eps;
    }

    // non-stupid % operator, always returns a number in the range 0..b-1
    // as long as b > 0
    public static int MOD(int a, int b)
    {
        return (a%b+b)%b;
    }
    
    Point currentPosition = new Point(0,0); // params required in Java 1.0
    Point press=null; // non null while mouse is dragging
    
    public TylerPanel(Rational initP, double _scale, double curvature, TylerHost applet)
    {
        this.curvature = curvature;
        this.applet = applet;
        mostRecentP = initP;
        mostRecentEdge = null;
        secondMostRecentEdge = null;
        scale = _scale;
        initialize();
    }

    //
    // Overriding Component's...
    //
    // uncomment this to see all events, to see what is available
    //public boolean handleEvent(Event e) { System.out.println("handleEvent event="+e+", modifiers="+e.modifiers); return super.handleEvent(e); }

    public boolean mouseMove(Event e, int x, int y)
    {
        //System.out.println("mouse move to "+x+","+y+" event="+e+", modifiers="+e.modifiers);
        if (false) // XXXhatch - argh- unfriendly to other netscape windows on Windows. Maybe can do this on some machines if we can detect the machine type?
        {
            requestFocus(); // ha! makes it so user doesn't have to click in window to make keys work!
        }
        currentPosition = new Point(x,y);
        return true;
    }
    
    public boolean mouseDrag(Event e, int x, int y)
    {
        //System.out.println("mouse drag to "+x+","+y+" event="+e+", modifiers="+e.modifiers);
        Point movedTo = new Point(x,y);
        if (e.controlDown())
        {
            // XXX duplicated below
            double windowCenterX = size().width*.5;
            double windowCenterY = size().height*.5;
            double Scale = (TylerPanel.this.curvature == 0. ? scale : scale / Math.abs(TylerPanel.this.curvature));
            double arrow[/*2*/][/*2*/] = pickArrow(
                (x-windowCenterX)/Scale+focusX, 
                (y-windowCenterY)/Scale+focusY,
                polys,
                ClosestArrowFinder.CLOSEST_ANGLE,
                TylerPanel.this.curvature,
                true);
            arrowToDraw = arrow;
            repaint();      
        }
        else
        {
            if (press != null) {
                // the focus (window center) in the scene
                // moves in the opposite direction
                // from the mouse motion in the window...
                double Scale = (TylerPanel.this.curvature == 0. ? scale : scale / Math.abs(TylerPanel.this.curvature));
                focusX -= (movedTo.x - currentPosition.x)/Scale;
                focusY -= (movedTo.y - currentPosition.y)/Scale;
            }
            arrowToDraw = null;
            repaint();        
        }
        currentPosition = movedTo;
        return true;
    } // end mouseDrag
    
    public boolean mouseDown(Event e, int x, int y)
    {
        requestFocus(); // seems to be needed in Java 1.0 / Netscape 3.04 to get the keyboard focus in the panel
        press = new Point(x,y);
        if (e.controlDown())
        {
            // XXX duplicated above
            double windowCenterX = size().width*.5;
            double windowCenterY = size().height*.5;
            double Scale = (TylerPanel.this.curvature == 0. ? scale : scale / Math.abs(TylerPanel.this.curvature));
            double arrow[/*2*/][/*2*/] = pickArrow(
                (x-windowCenterX)/Scale+focusX, 
                (y-windowCenterY)/Scale+focusY,
                polys,
                ClosestArrowFinder.CLOSEST_ANGLE,
                TylerPanel.this.curvature,
                true);
            arrowToDraw = arrow;
            repaint();      
        }
        return true;
    } // end mouseDown
    
    public boolean mouseUp(Event e, int x, int y)
    {
        Point start = press;
        press = null;
        if (arrowToDraw != null)
        {
            arrowToDraw = null;
            TylerPanel.this.repaint();
        }
        if (start != null && hypotSqrd(x-start.x, y-start.y) < 10*10)
        {
            double windowCenterX = size().width*.5;
            double windowCenterY = size().height*.5;
            double Scale = (TylerPanel.this.curvature == 0. ? scale : scale / Math.abs(TylerPanel.this.curvature));
            if (e.shiftDown()
             && curvature == 0.) { // XXX shift-click not implemented except for Euclidean, may implement it some day

                if (polys.size() == 0)
                {
                    // Get rid of the seed edges
                    // since they are no longer necessary
                    perimeterEdges.removeAllElements();
                }

                double centerX = (double)(x-windowCenterX)/Scale+focusX;
                double centerY = (double)(y-windowCenterY)/Scale+focusY;
                Edge edge = new Edge(centerX-.5,centerY,
                                     centerX+.5,centerY,
                                     TylerPanel.this.curvature);
                addCurrentAtPerimeterEdge(edge,
                                       polys, perimeterEdges,
                                       TylerPanel.this.curvature);
            }
            else if (e.controlDown()) {
                // nothing at the moment
            }
            else if (recolorMode) {
                // recolor the tile under the cursor (currentColor==null clears
                // the override, reverting it to its size-based default)
                Poly poly = findClosestPoly(
                    (currentPosition.x-windowCenterX)/Scale+focusX,
                    (currentPosition.y-windowCenterY)/Scale+focusY,
                    polys);
                if (poly != null)
                    poly.color = currentColor;
            }
            else
                addCurrentAtClosestPerimeterEdge(
                    (currentPosition.x-windowCenterX)/Scale+focusX,
                    (currentPosition.y-windowCenterY)/Scale+focusY,
                    polys, perimeterEdges,
                    TylerPanel.this.curvature);
            TylerPanel.this.repaint();
        }
        else
        {
            // later might do rectangle select, etc.
        }
        if ((doArcs && curvature < 0.)
         || antiAlias)
            TylerPanel.this.repaint(); // higher quality when mouse up
        return true;
    } // end mouseUp
    
    public boolean keyDown(Event event, int c)
    {
        //System.out.println("key down: '"+(char)c+"'("+(int)c+") at " + currentPosition.x + "," + currentPosition.y + "  event="+event+", modifiers="+event.modifiers);
        double windowCenterX = size().width*.5;
        double windowCenterY = size().height*.5;
        switch (c)
        {
            case 'd':
                deleteClosest();
                TylerPanel.this.repaint();      
                break;
            case '3': case '4': case '5': case '6': case '7':case '8': case '9': case '0': case '1': case '2':
            {
                int n = c - '0';
                if(n < 3)
                    n += 10;
                Rational p = new Rational(n,1);
                double Scale = (TylerPanel.this.curvature == 0. ? scale : scale / Math.abs(TylerPanel.this.curvature));
                addPolyAtClosestPerimeterEdge(p, 
                    (currentPosition.x-windowCenterX)/Scale+focusX, 
                    (currentPosition.y-windowCenterY)/Scale+focusY,
                    polys, perimeterEdges,
                    TylerPanel.this.curvature);
                TylerPanel.this.repaint();      
                break;  
            }
            case 'q':
                //System.exit(0);
                break;
            case '\003': // ctrl-C
                if (event.shiftDown()
                 && event.controlDown()) // metaDown would be nice too, but seems to be different between 1.0, 1.1, and 1.4 (which represent it with modifiers 4, 12, 8 respectively)
                    initialize(); // repaints
                break;

            case 'e':
                addPolyExtrapolated(mostRecentP,
                                    secondMostRecentEdge, mostRecentEdge,
                                    polys, perimeterEdges,
                                    TylerPanel.this.curvature);
                TylerPanel.this.repaint();      
                break;
            case ' ':
            {
                double Scale = (TylerPanel.this.curvature == 0. ? scale : scale / Math.abs(TylerPanel.this.curvature));
                addCurrentAtClosestPerimeterEdge(
                    (currentPosition.x-windowCenterX)/Scale+focusX, 
                    (currentPosition.y-windowCenterY)/Scale+focusY,
                    polys, perimeterEdges,
                    TylerPanel.this.curvature);
                TylerPanel.this.repaint();      
                break;
            }

            case Event.UP:
                drift(-.5*Math.PI); // XXX reversed since Y is still reversed
                TylerPanel.this.repaint();
                break;
            case Event.DOWN:
                drift(.5*Math.PI); // XXX reversed since Y is still reversed
                TylerPanel.this.repaint();
                break;
            case Event.LEFT:
                if (event.controlDown())
                    spin(curvature==0.?focusX:0., curvature==0.?focusY:0., ClosestArrowFinder.CW); // XXX reversed since Y is still reversed
                else
                    drift(-Math.PI);
                TylerPanel.this.repaint();
                break;
            case Event.RIGHT:
                if (event.controlDown())
                    spin(curvature==0.?focusX:0., curvature==0.?focusY:0., ClosestArrowFinder.CCW); // XXX reversed since Y is still reversed
                else
                    drift(0.);
                TylerPanel.this.repaint();
                break;

            case 'x':
            case 'y':
            case 'X':
            case 'Y':
            {
                double Scale = (TylerPanel.this.curvature == 0. ? scale : scale / Math.abs(TylerPanel.this.curvature));
                double arrow[/*2*/][/*2*/] = pickArrow(
                    (currentPosition.x-windowCenterX)/Scale+focusX, 
                    (currentPosition.y-windowCenterY)/Scale+focusY,
                    polys,
                    ClosestArrowFinder.CLOSEST_ANGLE,
                    TylerPanel.this.curvature,
                    true);

                //System.out.println("arrow = ("+arrow[0][0]+","+arrow[0][1]+") --> ("+arrow[1][0]+","+arrow[1][1]+")");

                if (arrow != null)
                {
                    // Find the transformation
                    // that takes the arrow
                    // to the appropriate axis:
                    //    'x' -> +x axis
                    //    'y' -> +y axis
                    //    'X' -> -x axis
                    //    'Y' -> -y axis
                    // Apply this transformation to everything.
                    double targetAng = Math.PI * .5 * "xyXY".indexOf(c);

                    // XXX since we currently draw +y towards the
                    // XXX bottom of the screen... :-(
                    targetAng *= -1.;

                    snap(arrow, targetAng);

                }
                TylerPanel.this.repaint();      
                break;
            }
            case 'a':
                antiAlias = !antiAlias;
                TylerPanel.this.repaint();
                break;
            case 'A':
                doArcs = !doArcs;
                TylerPanel.this.repaint();
                break;
            case 'C':
                circleThickness = (circleThickness + 1) % 4;
                TylerPanel.this.repaint();
                break;
            case 's':
                saveAs("polydata.txt");
                break;
            case 'l':case 'o':
                open("polydata.txt");
                break;
            case 'S':
                saveAsCookie("polydata.txt");
                break;
            case 'L': case 'O':
                loadFromCookie("polydata.txt");
                break;
            case '\023': // ctrl-S
                //saveToServer("login0", "password0", "polydata.txt");
                createServerSaveOrLoadDialog();
                break;
            case '\014': // ctrl-L
                //loadFromServer("login0", "password0", "polydata.txt");
                createServerSaveOrLoadDialog();
                break;

            case 'u':
                if(polys.size() == 0)
                    break;
                deletePoly((Poly)polys.elementAt(polys.size()-1), polys, perimeterEdges, TylerPanel.this.curvature);
                TylerPanel.this.repaint();      
                break;
            case 'm':
                if(polys.size() == 0)
                    break;
                Poly lastPoly = (Poly)polys.elementAt(polys.size()-1);
                deletePoly(lastPoly, polys, perimeterEdges,
                           TylerPanel.this.curvature);
                double Scale = (TylerPanel.this.curvature == 0. ? scale : scale / Math.abs(TylerPanel.this.curvature));
                addPolyAtClosestPerimeterEdge(lastPoly.p, 
                    (currentPosition.x-windowCenterX)/Scale+focusX, 
                    (currentPosition.y-windowCenterY)/Scale+focusY,
                    polys, perimeterEdges,
                    TylerPanel.this.curvature);
                TylerPanel.this.repaint();      
                break;
            case 'T':
                // open a new Tyler window
                Tyler.launchTylerFrame(true); // XXX might be cool to clone state
                break;

            default:
                return false; // pass through to the browser? (XXX doesn't work; not sure what it means)
        }
        return true;
    } // end keyDown

    
    public String saveAs(String fname)
    {
        String lower = fname.toLowerCase();
        boolean postscript = lower.endsWith(".ps") || lower.endsWith(".eps");
        if( !postscript && !lower.endsWith(".txt"))
            fname += ".txt";
        pushCursor(Cursor.WAIT_CURSOR);
        try {
            requestFocus();
            PrintStream writer = new PrintStream(new FileOutputStream(fname));
            if (postscript)
                writePostScript(writer);
            else
                write(writer);
            writer.close();
        } catch(Exception e) { 
            System.out.println("Tyler.saveAs(" + fname + ") exception: " + e);
            fname = null;
        }
        popCursor();
        return fname;
    }
    
    public void open(String fname)
    {
        pushCursor(Cursor.WAIT_CURSOR);
        try {
            requestFocus();
            DataInputStream reader = new DataInputStream(new FileInputStream(fname));
            read(reader);
            reader.close();
            repaint();
        } catch(Exception e) { System.out.println("Tyler.open(" + fname + ") exception: " + e); }
        popCursor();
    }

    private String getSaveServerURLString()
    {
        try {
            String documentBaseString = applet.getDocumentBase().toExternalForm();
            // XXXhatch I thought the document base was the directory, not the htm file!? but it appears to be the htm file, so need to strip off the final filename
            documentBaseString = documentBaseString.substring(0, documentBaseString.lastIndexOf('/')+1); // trim everything after the last '/'
            return documentBaseString + "TylerSave.php";
        } catch(Exception e) { // empirically, NPE inside getDocumentBase :-(
            return "http://www.hadron.org/~hatch/TylerSave.php";
            //return "http://www.superliminal.com/geometry/tyler/TylerSave.php";
        }
    }

    // The magic for how to get this to work is in:
    //     http://www.infm.ulst.ac.uk/~kevin/com347/faq.htm
    // in the section
    // "How do I do a HTTP POST request in Java?
    //  Or, I wrote a POST using URLConnection, why wont it work in Netscape?
    public boolean saveToServer(String login, String password, String fileName)
    {
        pushCursor(Cursor.WAIT_CURSOR);
        try {
            ByteArrayOutputStream stringWriter = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(stringWriter);
            write(printStream);
            printStream.close();
            String rawContents = stringWriter.toString();
            //System.out.println("    rawContents='"+rawContents+"'");
            String cookedContents = urlencode(rawContents);
            //System.out.println("    sending cookedContents ("+cookedContents.length()+" chars): '"+cookedContents+"'");


            final boolean useGET = false; // GET works and is simple but url length is apparently limited to 8k
            java.net.URLConnection connection;
            if (useGET)
            {
                String urlString = getSaveServerURLString()
                                 + "?login=" + urlencode(login)
                                 + "&password=" + urlencode(password)
                                 + "&saveFileName=" + urlencode(fileName)
                                 + "&saveFileContents=" + cookedContents;
                java.net.URL url = new java.net.URL(urlString);
                System.out.println("    url = \""+url+"\"");
                                            
                // XXX the following gives a class cast error!?
                //connection = (java.net.HttpURLConnection)url.openConnection();
                connection = (java.net.URLConnection)url.openConnection();
                connection.setUseCaches(false);
                connection.setDoOutput(false);
                connection.setDoInput(true);
            }
            else // use POST
            {
                java.net.URL url = new java.net.URL(getSaveServerURLString());
                System.out.println("    url = \""+url+"\"");
                // XXX the following gives a class cast error!?
                //connection = (java.net.HttpURLConnection)url.openConnection();
                connection = (java.net.URLConnection)url.openConnection();
                connection.setUseCaches(false);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                OutputStream outputStream = connection.getOutputStream();

                PrintStream printstream = new PrintStream(new BufferedOutputStream(outputStream));
                printstream.println("login=" + urlencode(login)
                                 + "&password=" + urlencode(password)
                                 + "&saveFileName=" + urlencode(fileName)
                                 + "&saveFileContents=" + cookedContents);
                printstream.flush();
                printstream.close();
            }

            java.io.InputStream inputStream = connection.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            System.out.println("got response from server:");
            String line;
            while ((line = dataInputStream.readLine()) != null)
                System.out.println("    "+line);

            // XXX not sure whether we are leaking... I thought it should be a HttpURLConnection, in which case I thought maybe we should do this...
            //connection.disconnect();
        } catch(Exception e) { 
            System.out.println("Tyler.saveToServer(" + fileName + ") exception: " + e);
            e.printStackTrace(System.out);
            fileName = null;
        }
        popCursor();
        return true; // success   XXXhatch - don't really check, should 
    } // saveToServer
    public boolean loadFromServer(String login, String password, String fileName)
    {
        boolean result = true; // until proven otherwise
        pushCursor(Cursor.WAIT_CURSOR);
        try {
            String urlString = getSaveServerURLString()
                             + "?login=" + urlencode(login)
                             + "&password=" + urlencode(password)
                             + "&loadFileName=" + urlencode(fileName);
            java.net.URL url = new java.net.URL(urlString);
            System.out.println("    url = \""+url+"\"");

            java.net.URLConnection connection = (java.net.URLConnection)url.openConnection();
            connection.setUseCaches(false);
            connection.connect();
            java.io.InputStream inputStream = connection.getInputStream();
            DataInputStream reader = new DataInputStream(inputStream);
            try { // XXX lame way to detect empty or corrupt files
                read(reader);
            } catch(Exception e) {
                beep();
                {
                    System.out.println(e);
                    e.printStackTrace(System.out);
                }
            }
            reader.close();

            repaint();
        } catch(Exception e) {
            System.out.println("Tyler.loadFromServer(" + fileName + ") exception: " + e);
            e.printStackTrace(System.out);
            result = false; // failed
        }
        popCursor();
        return result;
    } // loadFromServer
    public String[] getFileListFromServer(String login, String password)
    {
        pushCursor(Cursor.WAIT_CURSOR);
        String strings[] = {};
        try {
            String urlString = getSaveServerURLString()
                             + "?login=" + urlencode(login)
                             + "&password=" + urlencode(password);
            java.net.URL url = new java.net.URL(urlString);
            System.out.println("    url = \""+url+"\"");

            java.net.URLConnection connection = (java.net.URLConnection)url.openConnection();
            connection.setUseCaches(false);
            connection.connect();
            java.io.InputStream inputStream = connection.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            Vector vector = new Vector();
            String line;
            while ((line = dataInputStream.readLine()) != null)
            {
                vector.addElement(urldecode(line));
            }
            strings = new String[vector.size()];
            Enumeration e = vector.elements();
            for (int i = 0; i < strings.length; ++i)
            {
                strings[i] = (String)e.nextElement();
            }
        } catch(Exception e) { 
            System.out.println("Tyler.getFileListFromServer() exception: " + e);
            e.printStackTrace(System.out);
        }
        popCursor();
        return strings;
    } // getFileListFromServer


    public void createServerSaveOrLoadDialog()
    {
        new ServerSaveOrLoadDialog() {
            protected boolean load(String login, String password, String name)
            {
                return loadFromServer(login, password, name);
            }
            protected boolean save(String login, String password, String name)
            {
                return saveToServer(login, password, name);
            }
            protected String[] getList(String login, String password)
            {
                return getFileListFromServer(login, password);
            }
        };
    } // CreateServerSaveOrLoadDialog

    //
    // This only works in netscape,
    // and requires "mayscript=true" in the applet specification in the html
    // file, and requires javascript to be enabled in the browser.
    // Taken from the example applet on:
    //     http://www.cookiecentral.com/code/javacook2.htm
    //
    public void saveAsCookie(String cookieName)
    {
        try {
            ByteArrayOutputStream stringWriter = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(stringWriter);
            write(printStream);
            printStream.close();
            String rawCookieValue = stringWriter.toString();
            System.out.println("rawCookieValue='"+rawCookieValue+"'");
            String cookieValue = urlencode(rawCookieValue);
            String cookie = cookieName+"="+cookieValue;
            cookie += "; Expires=Fri, 01-Jan-2035 01:00:00 GMT"; // I think that's about as far as we can go with netscape
            System.out.println("saving cookie ("+cookie.length()+" chars): '"+cookie+"'");
            // XXX uncomment the following line to make it work
            //netscape.javascript.JSObject.getWindow(applet).eval("document.cookie ='"+cookie+"';");

        } catch(Exception e) { System.out.println("problem saving cookie \""+cookieName+"\": "+e); }
    }

    public void loadFromCookie(String cookieName)
    {
        try {
            String cookies = null;
            // XXX uncomment the following line to make it work
            //cookies = (String)netscape.javascript.JSObject.getWindow(applet).eval("document.cookie");
            System.out.println("got cookies: '"+cookies+"'");
            StringTokenizer stringTokenizer = new StringTokenizer(cookies, ";");
            while (stringTokenizer.hasMoreTokens())
            {
                String cookie = stringTokenizer.nextToken().trim();
                if (cookie.startsWith(cookieName+"="))
                {
                    System.out.println("loaded cookie ("+cookie.length()+" chars): '"+cookie+"'");
                    String cookieValue = cookie.substring(cookieName.length()+1);
                    System.out.println("   cookie value: '"+cookieValue+"'");

                    String rawCookieValue = urldecode(cookieValue);
                    System.out.println("rawCookieValue='"+rawCookieValue+"'");


                    DataInputStream reader = new DataInputStream(new StringBufferInputStream(rawCookieValue));
                    read(reader);
                    reader.close();
                    break;
                }
            }

            repaint();
        } catch(Exception e) { System.out.println("problem loading cookie \""+cookieName+"\": "+e); }
    }

    // since java.net.URLEncoder.encode()'s signature changed incompatibly
    // from 1.1 to 1.4 or so...
    // The algorithm is taken from the doc for java.net.URLEncoder.encode().
    private static String urlencode(String in)
    {
        char[] inArray = in.toCharArray();
        StringBuffer outBuf = new StringBuffer();
        for (int i = 0; i < inArray.length; ++i)
        {
            char c = inArray[i];
            if ((c >= 'a' && c <= 'z')
             || (c >= 'A' && c <= 'Z')
             || (c >= '0' && c <= '9')
             || c == '.'
             || c == '-'
             //|| c == '*'    // XXX not sure about this, so be conservative
             || c == '_')
                outBuf.append(c);
            else if (c == ' ')
                outBuf.append('+');
            else
            {
                outBuf.append('%');
                outBuf.append("0123456789abcdef".charAt(((int)c>>4)&0xf));
                outBuf.append("0123456789abcdef".charAt( (int)c    &0xf));
                // XXX probably an easier way to do sprintf("%02x")
            }
        }
        return outBuf.toString();
    }

    // since java.net.URLDecoder.decode() doesn't seem to work
    // (throws a security exception!?)
    private static String urldecode(String in)
    {
        char[] inArray = in.toCharArray();
        StringBuffer outBuf = new StringBuffer();
        for (int i = 0; i < inArray.length; ++i)
        {
            char c = inArray[i];
            if (c == '%')
            {
                char a = Character.toLowerCase(inArray[++i]);
                char b = Character.toLowerCase(inArray[++i]);
                c = (char)("0123456789abcdef".indexOf(a) * 16
                         + "0123456789abcdef".indexOf(b));
                // XXX probably an easier way to parse a hexadecimal string...
            }
            else if (c == '+')
                c = ' ';
            outBuf.append(c);
        }
        return outBuf.toString();
    }

    public static void beep()
    {
        // Toolkit doesn't have a beep() in Java 1.0,
        // so in that case just catch the NoSuchMethodError and continue.
        try {
            // Would use getToolkit.beep() but then
            // this couldn't be static any more...
            Toolkit.getDefaultToolkit().beep();
        } catch (NoSuchMethodError e) {
            System.out.println("\007BEEP!");
        }
    }

    // XXX should try to do java 1.0 cursor operations on the ancestor Frame
    Stack cursorStack = new Stack();
    public void pushCursor(Cursor cursor)
    {
        if (cursor != null)
        {
            cursorStack.push(getCursor());
            setCursor(cursor);
        }
        else
            cursorStack.push(null);
    }
    public void pushCursor(int cursorNumber)
    {
        Cursor cursor = null;
        try {
            cursor = Cursor.getPredefinedCursor(cursorNumber);
        } catch (Throwable e) {} // NoSuchMethodError or SecurityException happens in 1.0
        pushCursor(cursor);
    }
    public void popCursor()
    {
        Object savedCursorObject = cursorStack.pop();
        if (savedCursorObject != null)
            setCursor((Cursor)savedCursorObject);
    }

} // end class TylerPanel
