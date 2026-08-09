
import java.awt.*;

public class DoubleBufferedCanvas extends Canvas {
    private Image mActiveOffscreenImage = null;
    private Dimension mOffscreenSize = new Dimension(-1,-1);
    private Graphics mActiveOffscreenGraphics = null;
    private Graphics mSystemGraphics = null;
    private Image backgroundImage = null;
    private Graphics backgroundGraphics = null;
    
    public Graphics getBackgroundGraphics() {
        if (backgroundGraphics == null) {
            backgroundImage = createImage(mOffscreenSize.width, mOffscreenSize.height);
            backgroundGraphics = backgroundImage.getGraphics();
        }
        clearBackground();
        return backgroundGraphics;
    }
    public Image getBackgroundImage() {
        return backgroundImage;
    }
    private void clearBackground() {
        backgroundGraphics.setColor(Color.white);
        backgroundGraphics.fillRect(0, 0, mOffscreenSize.width, mOffscreenSize.height);
        backgroundGraphics.setColor(Color.black);
    }

    
    DoubleBufferedCanvas() {
        /*
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { 
                repaint(); 
            }
        });*/
    }
    
    /**      * NOTE: when extending applets:
     * this overrides update() to *not* erase the background before painting
     */
    public void update(Graphics g) {
        paint(g);
    }
    
    public Graphics startPaint (Graphics sysgraph) {
        mSystemGraphics = sysgraph;
        // Initialize if this is the first pass or the size has changed
        Dimension d = size();
        if ((mActiveOffscreenImage == null) ||
            (d.width != mOffscreenSize.width) ||
            (d.height != mOffscreenSize.height)) 
        {
            mActiveOffscreenImage = createImage(d.width, d.height);
            mActiveOffscreenGraphics = mActiveOffscreenImage.getGraphics();
            mOffscreenSize = d;
            mActiveOffscreenGraphics.setFont(getFont());
            //backgroundImage = null;
            //backgroundGraphics = null;
        }
        //mActiveOffscreenGraphics.clearRect(0, 0, mOffscreenSize.width, mOffscreenSize.height);
        return mActiveOffscreenGraphics;
    }
    
    public void endPaint () {
        // Start copying the offscreen image to this canvas
        // The application will begin drawing into the other one while this happens
        mSystemGraphics.drawImage(mActiveOffscreenImage, 0, 0, null);
    }
} // end class DoubleBufferedCanvas
