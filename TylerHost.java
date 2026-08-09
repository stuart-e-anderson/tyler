//
// TylerHost is the small set of callbacks TylerPanel makes back into whatever
// is hosting it.  Historically that host was always the Tyler applet (AWT); by
// naming the contract as an interface, a Swing UI (TylerSwing) can host the very
// same canvas without TylerPanel needing to know which one it is talking to.
//
public interface TylerHost
{
    // Show / hide the "Curvature based on" control (curved geometry only).
    void addHyperbolicControls();
    void removeHyperbolicControls();

    // Keep the "Rhombus" toggle in sync when a polygon becomes the current tile.
    void rhombusOff();

    // Legacy applet server-save support; a desktop host may return null or a
    // dummy URL (server save/load isn't used off the web).
    java.net.URL getDocumentBase();
}
