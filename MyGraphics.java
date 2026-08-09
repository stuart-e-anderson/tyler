//
// Wrapper class for Graphics
// that does a subset of the Graphics2D API
// (But with saner API.)
//
public class MyGraphics
{
    public MyGraphics(java.awt.Graphics g,
                      java.awt.Dimension gsize,
                      double x0, double x1, double y0, double y1)
    {
        this.g = g;
        this.gsize = gsize;
        fitToWindow(x0, x1, y0, y1);
    }
    public MyGraphics(java.awt.Graphics g,
                      java.awt.Dimension gsize)
    {
        this.g = g;
        this.gsize = gsize;
        fitToWindow(-1., 1., -1., 1.);
    }

    //
    // Set translate and scale
    // so that x0,x1,y0,y0
    // map to left,right,bottom,top.
    //
    public void fit(double x0,   double x1,    double y0,     double y1,
                    double left, double right, double bottom, double top)
    {
        scaleX = (right-left) / (x1-x0);
        scaleY = (top-bottom) / (y1-y0);
        translateX = left - scaleX*x0;
        translateY = bottom - scaleY*y0;
    }
    public void fitToWindow(double x0, double x1, double y0, double y1)
    {
        fit(x0, x1,             y0,              y1,
            .5, gsize.width-.5, gsize.height-.5, .5);
    }
    public void fitToPixel(double centerX, double centerY,
                           double pixelSizeX, double pixelSizeY)
    {
        fit(centerX, centerX+pixelSizeX, centerY, centerY+pixelSizeY,
            gsize.width*.5, gsize.width*.5 + 1., gsize.height*.5, gsize.height*.5 + 1);
    }

    public void translate(double x, double y)
    {
        translateX += x*scaleX;
        translateY += y*scaleY;
    }

    public void pick(double x, double y, double result[/*2*/])
    {
        result[0] = (x-translateX)/scaleX;
        result[1] = (y-translateY)/scaleY;
    }


    public void fillWindow()
    {
        g.fillRect(0,0, gsize.width,gsize.height);
    }

    public void drawLineNonClipped(double x0, double y0, double x1, double y1,
                                   boolean antiAlias)
    {
        if (antiAlias)
        {
            if (x0 == x1 && y0 == y1)
            {
                drawPoint(x0, y0, 1);
                return;
            }

            // Transform to window coords...
            x0 = (x0 * scaleX + translateX);
            y0 = (y0 * scaleY + translateY);
            x1 = (x1 * scaleX + translateX);
            y1 = (y1 * scaleY + translateY);

            java.awt.Color savedColor = getColor();
            java.awt.Color[/*256*/] transparentColorCache = getTransparentColorCache(savedColor);

            if (Math.abs(y1-y0) >= Math.abs(x1-x0))
            {
                int Y0 = (int)Math.round(y0);
                int Y1 = (int)Math.round(y1);
                int dirY = (Y0 <= Y1 ? 1 : -1);
                double slope = (x1-x0)/(y1-y0);
                for (int Y = Y0; Y != Y1; Y += dirY)
                {
                    // XXX should use Wu's algorithm,
                    // XXX although I doubt this is the bottleneck
                    double x = x0 + (Y-y0) * slope;
                    int X = (int)Math.floor(x);
                    double frac = x-X;
                    int alpha = (int)(frac * 256.);
                    setColor(transparentColorCache[255-alpha]);
                    g.fillRect(X, Y, 1, 1);
                    setColor(transparentColorCache[alpha]);
                    g.fillRect(X+1, Y, 1, 1);
                }
            }
            else
            {
                int X0 = (int)Math.round(x0);
                int X1 = (int)Math.round(x1);
                int dirX = (X0 <= X1 ? 1 : -1);
                double slope = (y1-y0)/(x1-x0);
                for (int X = X0; X != X1; X += dirX)
                {
                    // XXX should use Wu's algorithm,
                    // XXX although I doubt this is the bottleneck
                    double y = y0 + (X-x0) * slope;
                    int Y = (int)Math.floor(y);
                    double frac = y-Y;
                    int alpha = (int)(frac * 256.);
                    setColor(transparentColorCache[255-alpha]);
                    g.fillRect(X, Y, 1, 1);
                    setColor(transparentColorCache[alpha]);
                    g.fillRect(X, Y+1, 1, 1);
                }
            }

            setColor(savedColor);
        }
        else
        {
            // window coordinates are all >= 0, so it should be okay
            // to truncate rather than round
            g.drawLine((int)(x0 * scaleX + translateX),
                       (int)(y0 * scaleY + translateY),
                       (int)(x1 * scaleX + translateX),
                       (int)(y1 * scaleY + translateY));
        }
    }

    public void drawLine(double x0, double y0, double x1, double y1,
                         boolean antiAlias)
    {
        // XXX clip!
        drawLineNonClipped(x0, y0, x1, y1, antiAlias);
    }

    public void drawPoint(double x, double y, int nPixels)
    {
        int x0 = (int)(x * scaleX + translateX - .5*nPixels);
        int y0 = (int)(y * scaleY + translateY - .5*nPixels);
        if (verbose >= 2) System.out.println("in drawPoint("+x+","+y+","+nPixels+") -> "+x0+","+y0);
        // have to check, otherwise java fillRect does weird wraparound stuff
        // XXX should check in floating-point
        if (x0 < -nPixels
         || x0 > gsize.width+nPixels
         || y0 < -nPixels
         || y0 > gsize.width+nPixels)
            return;
        g.fillRect(x0, y0, nPixels, nPixels);
    }
    public void drawString(String s, double x, double y)
    {
        g.drawString(s, (int)(x * scaleX + translateX),
                        (int)(y * scaleY + translateY));
    }

    //private static int recursionLevel = 0;


    public void smartDrawArcNonClipped(
        double focusX, double focusY, // a point on the arc
        double focusAngleRadians,     // from center to focus
        double c,                     // curvature, i.e. 1/radius
        double start, double end,     // arc goes from focusAngle+start to focusAngle+end in arc length
        boolean antiAlias)
    {
        //System.out.println(spaces(4*recursionLevel++)+"            in smartDrawArcNonClipped(focusX="+focusX+",focusY="+focusY+",focusAngleDegrees="+focusAngleRadians*180/Math.PI+",c="+c+"=1/"+1./c+",start="+start+",end="+end+"");

        final boolean useSegments = antiAlias; // implement antialiasing by drawing the circle as a sequence of antialiased segments with floating point coords
        if (useSegments)
        {
            int nSegs = MAX(10, (int)(ABS((end-start)*c) / (1*(Math.PI/180)))); // 1 degree-- delicate tradeoff, if we make it too fine, it looks all warbly because the endpoints are snapped to unit pixel coords XXX but this comment is no longer true since we are using our own antialiased line procedure
    //PRINT(nSegs);

            double cosFocusAngle = Math.cos(focusAngleRadians);
            double sinFocusAngle = Math.sin(focusAngleRadians);
            double normalAndTangent[][] = {
                { cosFocusAngle, sinFocusAngle}, // unit normal vector
                {-sinFocusAngle, cosFocusAngle}, // unit tangent vector
            };

            double prevPoint[] = new double[2];
            double point[] = new double[2];
            double scratch[] = new double[2];
            for (int i = 0; i < nSegs+1; ++i)
            {
                double t = LERP(start, end, (double)i/nSegs);
                findPointOnArc(t, focusX, focusY, c, normalAndTangent, scratch, point);

                if (i > 0)
                    this.drawLineNonClipped(prevPoint[0], prevPoint[1],
                                            point[0],     point[1],
                                            antiAlias);
                //VecMath.setvec(prevPoint, point);
                System.arraycopy(point, 0, prevPoint, 0, 2);
            }
            return;
        }


        // our end condition can only handle angles < 180 degrees,
        // so split in half if that is not the case
        // (a full circle will be recursively split again).
        if (c*Math.abs(start-end) > .75*Math.PI)
        {
            smartDrawArcNonClipped(focusX, focusY, focusAngleRadians, c,
                                   start, (start+end)*.5,
                                   antiAlias);
            smartDrawArcNonClipped(focusX, focusY, focusAngleRadians, c,
                                   (start+end)*.5, end,
                                   antiAlias);
            //System.out.println(spaces(4*--recursionLevel)+"            out smartDrawArcNonClipped");
            return;
        }


        //
        // Need to work in pixel space
        // so that we can be aware of the integer grid...
        //
        {
            focusX = focusX * scaleX + translateX;
            focusY = focusY * scaleY + translateY;
            if (scaleY < 0.)
                focusAngleRadians *= -1.;
            double scale = .5 * (Math.abs(scaleX) + Math.abs(scaleY)); // not really right unless scaleX == scaleY, but that's usually the case
            c /= scale;
            start *= scale;
            end *= scale;
        }

        //
        // Reverse/swap parameters if necessary so that we are
        // going CCW around an arc of positive radius...
        // XXX figure out how much of this is necessary
        //
        {
            if (c < 0.)
            {
                c *= -1.;
                focusAngleRadians += Math.PI;
                start *= -1.;
                end *= -1.;
            }
            if (start > end)
            {
                double temp = start;
                start = end;
                end = temp;
            }
        }

        //System.out.println("in smartDrawArcNonClipped(focusX="+focusX+",focusY="+focusY+",focusAngleDegrees="+focusAngleRadians*180/Math.PI+",c="+c+"=1/"+1./c+",start="+start+",end="+end+"");

        double normalX = Math.cos(focusAngleRadians);
        double normalY = Math.sin(focusAngleRadians);
        double normalAndTangent[][] = {
            { normalX, normalY}, // unit normal vector
            {-normalY, normalX}, // unit tangent vector
        };

        //
        // Find start point...
        //
        double startX, startY;
        double endX, endY;
        {
            double point[] = new double[2];
            double scratch[] = new double[2];

            findPointOnArc(start, focusX, focusY, c, normalAndTangent, scratch, point);
            startX = point[0];
            startY = point[1];

            findPointOnArc(end, focusX, focusY, c, normalAndTangent, scratch, point);
            endX = point[0];
            endY = point[1];
        }

        // At most one point drawn for every pixel width of arc length
        int maxPointsToDraw = (int)Math.ceil(Math.abs(end-start)) + 1;

        int nPointsDrawn = 0;
        int x = (int)Math.round(startX), y = (int)Math.round(startY);
        double X = x - focusX; // XXX clean this up
        double Y = y - focusY; // XXX clean this up
        //System.out.println("point at "+x+","+y);
        this.g.fillRect(x, y, 1, 1);
        nPointsDrawn++;

        if (c < 1) // i.e. radius > 1
        {
            while (true)
            {
                final int quantizedDirVectors[][] = {
                    {1,0}, {1,1}, {0,1}, {-1,1}, {-1,0}, {-1,-1}, {0,-1}, {1,-1}
                };
                // What direction are we most likely to be going in?
                int dir; // index into quantizedDirVectors
                {
                    // Figure out current normal and tangent (dir),
                    // not necessarily normalized to unit length
                    double thisNormalX = c*X + normalX;
                    double thisNormalY = c*Y + normalY;
                    double thisTangentX = -thisNormalY;
                    double thisTangentY = thisNormalX;
                    int quantizedDirX = (thisTangentX < 0. ? -1 : thisTangentX > 0. ? 1 : 0);
                    int quantizedDirY = (thisTangentY < 0. ? -1 : thisTangentY > 0. ? 1 : 0);
                    double absThisTangentX = Math.abs(thisTangentX);
                    double absThisTangentY = Math.abs(thisTangentY);
                    if (absThisTangentX > 2*absThisTangentY)
                        quantizedDirY = 0;
                    else if (absThisTangentY > 2*absThisTangentX)
                        quantizedDirX = 0;
                    // XXX figure out more efficient way!
                    for (dir = 0; dir < 8; ++dir)
                    {
                        if (quantizedDirVectors[dir][0] == quantizedDirX
                         && quantizedDirVectors[dir][1] == quantizedDirY)
                            break; // found it
                    }
                    if (dir == 8)
                    {
                        System.out.println("huh?????"); // XXX
                        break; // only happens when radius is very small
                    }
                }

                x += quantizedDirVectors[dir][0];
                y += quantizedDirVectors[dir][1];
                X = x - focusX;
                Y = y - focusY;

                // the following was worked out on paper...
                // XXX should show derivation for posterity
                double signedDistanceOutsideCircle =
                    (c * (X*X + Y*Y) + 2 * (X*normalX + Y*normalY))
                  / (MyMath.hypot(c*X + normalX, c*Y + normalY) + 1.);

                //
                // Make it "manhattan distance" from circle,
                // by inflating distance when normal is in diagonal direction.
                //
                double inflation;
                {
                    double thisNormalX = c*X + normalX;
                    double thisNormalY = c*Y + normalY;
                    inflation = MyMath.hypot(thisNormalX, thisNormalY)
                              / Math.max(Math.abs(thisNormalX), Math.abs(thisNormalY));
                }
                //System.out.println("        signedDistanceOutsideCircle = "+signedDistanceOutsideCircle);
                signedDistanceOutsideCircle *= inflation;
                //System.out.println("        inflated signedDistanceOutsideCircle = "+signedDistanceOutsideCircle);

                if (signedDistanceOutsideCircle > .5001) // too far outside circle XXX use GT?
                {
                    x -= quantizedDirVectors[dir][0];
                    y -= quantizedDirVectors[dir][1];
                    // pull inward, i.e. turn direction of travel CCW
                    dir = (dir + 1) % 8;
                    x += quantizedDirVectors[dir][0];
                    y += quantizedDirVectors[dir][1];
                    X = x - focusX;
                    Y = y - focusY;
                }
                else if (signedDistanceOutsideCircle < -.5001) // too far inside circle XXX use LT?
                {
                    x -= quantizedDirVectors[dir][0];
                    y -= quantizedDirVectors[dir][1];
                    // pull outward, i.e. turn direction of travel CW
                    dir = (dir - 1 + 8) % 8;
                    x += quantizedDirVectors[dir][0];
                    y += quantizedDirVectors[dir][1];
                    X = x - focusX;
                    Y = y - focusY;
                }

                //System.out.println("maybe point at "+x+","+y);

                // XXX if radius <= 2 or so, 
                // XXX may need to do the above multiple times!
                // XXX Ignoring this issue for now, so tiny arcs
                // XXX might draw some garbage (tiny garbage though)

                //
                // Determine whether we've past the end...
                // XXX this test is wrong for arcs > 180 degrees,
                // XXX and also when end < start!
                // XXX That is why we subdivided any arc > .75 * 180 degrees
                // XXX above, and we reversed start and end
                // XXX so that end <= start.
                //
                {
                    double thisNormalX = c*X + normalX;
                    double thisNormalY = c*Y + normalY;
                    signedDistanceOutsideCircle =
                        (c * (X*X + Y*Y) + 2 * (X*normalX + Y*normalY))
                      / (MyMath.hypot(c*X + normalX, c*Y + normalY) + 1.);
                    double arcPointX = focusX + X - signedDistanceOutsideCircle * thisNormalX;
                    double arcPointY = focusY + Y - signedDistanceOutsideCircle * thisNormalY;
                    double startToArcPoint = MyMath.hypot(arcPointX-startX,
                                                          arcPointY-startY);
                    double arcDistance = startToArcPoint * MyMath.asin_over_x(c*startToArcPoint*.5);
                    if (false)
                    {
                        System.out.println("    nPointsDrawn = "+nPointsDrawn);
                        System.out.println("        signedDistanceOutsideCircle = "+signedDistanceOutsideCircle);
                        System.out.println("        startX = "+startX);
                        System.out.println("        startY = "+startY);
                        System.out.println("        arcPointX = "+arcPointX);
                        System.out.println("        arcPointY = "+arcPointY);
                        System.out.println("        startToArcPoint = "+startToArcPoint);
                        System.out.println("        arcDistance = "+arcDistance);
                        System.out.println("        start = "+start);
                        System.out.println("        end = "+end);
                        System.out.println("        c = "+c);
                    }
                    if (arcDistance >= end-start) // XXX use GEQ I think
                        break;
                }

                this.g.fillRect(x, y, 1, 1);
                nPointsDrawn++;

                // XXX For some small circles, the above will never be satisfied
                // XXX so put a limit on number of points to draw.
                // XXX Shouldn't need this if we get the above condition right
                if (nPointsDrawn >= maxPointsToDraw-1)
                    break;
            }
            this.g.fillRect((int)Math.round(endX), (int)Math.round(endY), 1, 1);
            nPointsDrawn++;
        }
        //System.out.println(spaces(4*--recursionLevel)+"            out smartDrawArcNonClipped");
    } // smartDrawArcNonClipped

    public void smartDrawArcClipped(
        double focusX, double focusY, // a point on the arc
        double focusAngleRadians,     // from center to focus
        double c,                     // curvature, i.e. 1/radius
        double start, double end,     // arc goes from focusAngle+start to focusAngle+end in arc length
        boolean antiAlias,
        double clipX0, double clipX1, double clipY0, double clipY1,
        double slack)
    {
        //System.out.println("        in smartDrawArcClipped");
        // I don't know how to clip analytically yet,
        // so do the following instead:
        // while an endpoint is outside the clip area,
        // cut off that length of the arc.
        // This will exponentially approach the clip rectangle
        // (or exponentially recede from it).
        // Stop when our distance from the clip rectangle
        // is <= slack (typically 2 pixels).

        double clipMinX = Math.min(clipX0, clipX1);
        double clipMaxX = Math.max(clipX0, clipX1);
        double clipMinY = Math.min(clipY0, clipY1);
        double clipMaxY = Math.max(clipY0, clipY1);

        if (c * Math.abs(end-start) > 2*Math.PI)
        {
            start = 0.;
            end = 2*Math.PI / c;
        }

        int dir = (start <= end ? 1 : -1);

        double normalX = Math.cos(focusAngleRadians);
        double normalY = Math.sin(focusAngleRadians);
        double normalAndTangent[][] = {
            { normalX, normalY}, // unit normal vector
            {-normalY, normalX}, // unit tangent vector
        };
        double point[] = new double[2];
        double scratch[] = new double[2];

        //System.out.println("clipMinX = "+clipMinX);
        //System.out.println("clipMaxX = "+clipMaxX);
        //System.out.println("clipMinY = "+clipMinY);
        //System.out.println("clipMaxY = "+clipMaxY);
        //System.out.println("slack = "+slack);
        while (true)
        {
            //System.out.println("start="+start);
            //System.out.println("end="+end);
            findPointOnArc(start, focusX, focusY, c, normalAndTangent, scratch, point);
            double startX = point[0];
            double startY = point[1];
            //System.out.println("startX = "+startX);
            //System.out.println("startY = "+startY);
            double dist = MAX4(startX-clipMaxX,
                               clipMinX-startX,
                               startY-clipMaxY,
                               clipMinY-startY);
            //System.out.println("dist = "+dist);
            if (dist <= slack)
                break;
            start += dir * dist;
            //System.out.println("new start="+start);
            if (dir * (end-start) < 0)
            {
                //System.out.println("        out smartDrawArcClipped (without drawing)");
                return;
            }
        }
        while (true)
        {
            //System.out.println("start="+start);
            //System.out.println("end="+end);
            findPointOnArc(end, focusX, focusY, c, normalAndTangent, scratch, point);
            double endX = point[0];
            double endY = point[1];
            //System.out.println("endX = "+endX);
            //System.out.println("endY = "+endY);
            double dist = MAX4(endX-clipMaxX,
                               clipMinX-endX,
                               endY-clipMaxY,
                               clipMinY-endY);
            if (dist <= slack)
                break;
            end -= dir * dist;
            //System.out.println("new end="+end);
            if (dir * (end-start) < 0)
            {
                //System.out.println("        out smartDrawArcClipped (without drawing.");
                return;
            }
        }

        // Prevent huge circles that start and end in the clip rectangle.
        // Note this doesn't clip away all arc segments
        // that leave the clip rectangle, but it does
        // guarantee that any it misses won't be much larger
        // than the clip rectangle (and usually much smaller).
        {
            double mid = (start+end)*.5;
            findPointOnArc(mid, focusX, focusY, c, normalAndTangent, scratch, point);
            double midX = point[0];
            double midY = point[1];
            double dist = MAX4(midX-clipMaxX,
                               clipMinX-midX,
                               midY-clipMaxY,
                               clipMinY-midY);
            if (dist > slack)
            {
                smartDrawArcClipped(focusX, focusY, focusAngleRadians, c,
                                    start, mid,
                                    antiAlias,
                                    clipX0, clipX1, clipY0, clipY1, slack);
                smartDrawArcClipped(focusX, focusY, focusAngleRadians, c,
                                    mid, end,
                                    antiAlias,
                                    clipX0, clipX1, clipY0, clipY1, slack);
                //System.out.println("        out smartDrawArcClipped");
                return;
            }
        }

        smartDrawArcNonClipped(focusX,focusY, focusAngleRadians, c, start,end,
                               antiAlias);
        //System.out.println("        out smartDrawArcClipped");
    } // smartDrawArcClipped

    public void smartDrawArc(
        double focusX, double focusY, // a point on the arc
        double focusAngleRadians,     // from center to focus
        double c,                     // curvature, i.e. 1/radius
        double start, double end,     // arc goes from focusAngle+start to focusAngle+end in arc length
        boolean antiAlias)
    {
        //System.out.println("scaleX="+scaleX);
        //System.out.println("scaleY="+scaleY);
        //System.out.println("    in smartDrawArc");
        smartDrawArcClipped(focusX, focusY, focusAngleRadians, c, start, end,
                            antiAlias,
                            (0-translateX)/scaleX,
                            (gsize.width-translateX)/scaleX,
                            (0-translateY)/scaleY,
                            (gsize.height-translateY)/scaleY,
                            2./((Math.abs(scaleX)+Math.abs(scaleY))*.5));   // 2 pixels
        //System.out.println("    out smartDrawArc");
    } // smartDrawArc



    // Arc drawing on Linux/Netscape is so broken it's unbelievable...
    // so, even though the simple finitistic API is inherently bad for
    // circles of unlimited radius,
    // this is still much much better than Netscape's.
    public void drawArc(double x, double y, double width, double height,
                        double startRadians, double arcRadians,
                        boolean antiAlias)
    {
        //System.out.println("in drawArc");
        double centerX = x + width*.5;
        double centerY = y + height*.5;
        double radius = .25*(width+height); // XXX ignoring aspect for now
        double curvature = 1./radius;
        double focusAngleRadians = startRadians;
        double start = 0.;
        double end = arcRadians * radius;
        double focusX = centerX + radius * Math.cos(focusAngleRadians);
        double focusY = centerY + radius * Math.sin(focusAngleRadians);
        smartDrawArc(focusX, focusY,
                     focusAngleRadians,
                     curvature,
                     start, end,
                     antiAlias);
        //System.out.println("out drawArc");
        return;
    } // drawArc

    public java.awt.Color getColor() 
    {
        return g.getColor();
    }
    public void setColor(java.awt.Color color) 
    {
        g.setColor(color);
    }

    private java.awt.Graphics g;
    private java.awt.Dimension gsize;
    private double scaleX = 1;
    private double scaleY = 1;
    private double translateX = 0;
    private double translateY = 0;
    public int verbose = 0; // XXX interface to this?


        //
        // Return an array of the given Color
        // with each of 256 transparencies.
        // The array is cached from one call to the next,
        // so consecutive calls using the same color
        // will not suffer alllocation overhead.
        //
            private java.awt.Color[/*256*/] cache = null;
            private java.awt.Color cacheColor = null;
            private int cacheRGB;
        private java.awt.Color[/*256*/] getTransparentColorCache(java.awt.Color color)
        {
            if (color != cacheColor)
            {
                cacheColor = color;

                int colorRGB = color.getRGB();

                if (cache == null || colorRGB != cacheRGB)
                {
                    cacheRGB = colorRGB;
                    if (cache == null)
                        cache = new java.awt.Color[256];
                    int r = (colorRGB >> 16) & 0xff;
                    int g = (colorRGB >> 8) & 0xff;
                    int b = (colorRGB >> 0) & 0xff;
                    for (int i = 0; i < 256; ++i)
                        cache[i] = new java.awt.Color(r, g, b,
                                                      (float)(i*(1./255.)));
                    //System.out.println("MISS");
                }
                else
                {
                    //System.out.println("    HIT (colors equal)");
                }
            }
            else
            {
                //System.out.println("    HIT (address equal)");
            }

            return cache;
        } // getTransparencyColorCache

    // Utility...
        private static void findPointOnArc(double t,
                                           double focusX, double focusY,
                                           double c,
                                           double normalAndTangent[][],
                                           double scratch[/*2*/],
                                           double result[/*2*/])
        {
            // coeff of normal vector: -(1-cos(t*c))/c
            scratch[0] = -MyMath.cosf1_over_x(t*c) * t;
            // coeff of tangent vector: sin(t*c)/c
            scratch[1] = MyMath.sin_over_x(t*c) * t;
            MatrixMath.vxm(result, scratch, normalAndTangent);
            result[0] += focusX;
            result[1] += focusY;
        } // findPointOnArc

        /*
        private static String spaces(int n)
        {
            String s = "";
            for (int i = 0; i < n; ++i)
                s += " ";
            return s;
        } // spaces
        */

    //
    // Utilities to replace macros...
    //
        private static double LERP(double a, double b, double t)
        {
            return a + t * (b-a);
        }
        private static double MAX(double a, double b) // same as Math.max
        {
            return a >= b ? a : b;
        }
        private static int MAX(int a, int b) // same as Math.max
        {
            return a >= b ? a : b;
        }
        private static double MIN(double a, double b) // same as Math.min
        {
            return a <= b ? a : b;
        }
        private static int MIN(int a, int b) // same as Math.min
        {
            return a <= b ? a : b;
        }
        private static double ABS(double x) // same as Math.abs
        {
            return x >= 0. ? x : -x;
        }
        private static double MAX4(double a, double b, double c, double d)
        {
            return MAX(MAX(a,b),MAX(c,d));
        }
} // class MyGraphics
