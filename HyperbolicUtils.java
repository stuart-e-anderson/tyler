// Author: Don Hatch (hatch@hadron.org)
// This code may be used for any purpose as long as it is good and not evil.

// Depends on Complex.java, Isometry2.java, MyMath.java

public class HyperbolicUtils
{
    // hyperbolic to euclidean norm (distance from 0,0) in Poincare disk.
    // in the limit (approaching 0), it's half the hyperbolic norm.
    public static double
    h2eNorm(double hNorm)
    {
        if (Double.isInfinite(hNorm))
            return 1.;
        return MyMath.tanh(.5*hNorm);
    }

    // euclidean to hyperbolic norm (distance from 0,0) in Poincare disk.
    // in the limit (approaching 0), it's twice the euclidean norm.
    public static double
    e2hNorm(double eNorm)
    {
        return 2*MyMath.atanh(eNorm);
    }

    //
    // Calculate half-edge-length of semiregular tesselation with each
    // vertex surrounded by polygons p[0]..p[nps-1] q times,
    // with given covering density.
    // XXX The paper "Uniform Solution for Uniform Polyhedra" by Zvi Har'el
    //      http://www.math.technion.ac.il/~rl/uniform.pdf
    // implies this is not the most stable way to do this,
    // but I think it's okay for the configurations we are interested in,
    // namely the ones that make pretty pictures
    //
    // XXX I don't think zero density works yet, even though
    // I think it's well-defined
    //
    public static double
    calcUniformTilingHalfEdgeLength(double p[], double q, double density)
    {
        int nps = p.length;
        if (nps == 0)
            return 0.;
        // We want edge length e such that
        //         SUM(i==0..nps-1) polygonAngle(p[i],e) = 2*PI
        // where polygonAngle(p,e) = 2*asin(cos(pi/p)/cosh(e/2)).
        // Find cosh(e/2) by binary search (it's an increasing function of e).

        double max_cos_pi_over_p = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < nps; ++i)
        {
            if (p[i] == 0.) // means infinity (or minus infinity)
            {
                max_cos_pi_over_p = 1.;
                break;
            }
            double cos_pi_over_p = Math.cos(Math.PI/p[i]);
            if (cos_pi_over_p > max_cos_pi_over_p)
                max_cos_pi_over_p = cos_pi_over_p;
        }

        double lo = 1.; /* corresponds to e = 0. */
        double hi = max_cos_pi_over_p/Math.sin(density*Math.PI/(nps*q)); /* corresponds to e = edgeLength(maxp, nps*q) */
        if (Double.isInfinite(hi))
            hi = 1e9; // XXX HACK! this sucks worse and worse, and I don't think it even fixes the problem which is when density == 0
        double mid;
        while ((mid = lo + (hi-lo)*.5) != lo && mid != hi)
        {
            double sum = 0.;
            for (int i = 0; i < nps; ++i)
            {
                // XXX do the following more elegantly?
                if (1./(p[i] == 0. ? p[i] : Math.sin(Math.PI/p[i])) < 0.) // invert so we test the sign even when 0
                    sum -= q*2.*Math.asin((p[i]==0.?1.:Math.cos(Math.PI/p[i]))/mid);
                else
                    sum += q*2.*Math.asin((p[i]==0.?1.:Math.cos(Math.PI/p[i]))/mid);
            }
            if (sum > density*2*Math.PI)  /* then e, and therefore mid, is too small */
                lo = mid;
            else
                hi = mid;
        }
        double result = MyMath.acosh(mid);
        Assert(!Double.isNaN(result));
        Assert(!Double.isInfinite(result));
        return result;
    } // calcUniformTilingHalfEdgeLength

    //
    // Label a hyperbolic triangle's sides A,B,C
    // and the opposite angles alpha,beta,gamma.
    // Given alpha,C,beta, find A.
    //
    public static double
    solveTriangleAFromAlphaCBeta(double alpha, double C, double beta)
    {
        double sinAlpha = Math.sin(alpha);
        double temp = MyMath.cosh(C)*sinAlpha*Math.sin(beta) - Math.cos(alpha)*Math.cos(beta);
        double A = MyMath.asinh(MyMath.sinh(C)*sinAlpha / Math.sqrt(1. - temp*temp));
        return A;
    }

    //
    // Label a hyperbolic triangle's sides A,B,C
    // and the opposite angles alpha,beta,gamma.
    // Given A,gamma,B, find C.
    //
    public static double
    solveTriangleCFromAGammaB(double A, double gamma, double B)
    {
        return MyMath.acosh(MyMath.cosh(A)*MyMath.cosh(B)
                          - Math.cos(gamma)*MyMath.sinh(A)*MyMath.sinh(B));
    }

    //
    // Calculate the edge length for a tiling
    // of p0-gons and p1-gons
    // with a distance of distanceBetweenTiles
    // between their centers.
    //
    public static double
    calcJimsTilingHalfEdgeLength(double p0, double p1, double distanceBetweenTileCenters)
    {
        double p1_circumRadius = solveTriangleAFromAlphaCBeta(Math.PI/p0, distanceBetweenTileCenters, Math.PI/p1);
        double halfEdgeLength = MyMath.asinh(MyMath.sinh(p1_circumRadius) * Math.sin(Math.PI/p1));
        return halfEdgeLength;
    } // calcJimsTilingHalfEdgeLength


    //
    // Same as above,
    // but for distanceBetweenTiles,
    // use any two (distinct) vertices
    // of a schwarz polygon.
    //
    public static double
    calcJimsTilingHalfEdgeLength(double schwarzPolygon[],
                                 int i0, double p0, int i1, double p1)
    {
        int n = schwarzPolygon.length;
        double dualVertexConfig[] = new double[n];
        {
            for (int i = 0; i < n; ++i)
                dualVertexConfig[i] = 2 * schwarzPolygon[i];
        }
        double dualHalfEdgeLength = calcUniformTilingHalfEdgeLength(dualVertexConfig, 1, 1);

        double ang = 0;
        {
            for (int i = i0; i != i1; i = (i+1)%n)
                ang += polygonHalfAngle(dualVertexConfig[i], dualHalfEdgeLength)
                     + polygonHalfAngle(dualVertexConfig[(i+1)%n], dualHalfEdgeLength);
        }

        double r0 = polygonCircumRadius(dualVertexConfig[i0], dualHalfEdgeLength);
        double r1 = polygonCircumRadius(dualVertexConfig[i1], dualHalfEdgeLength);

        double distanceBetweenTileCenters = solveTriangleCFromAGammaB(r0,ang,r1);

        return calcJimsTilingHalfEdgeLength(p0, p1, distanceBetweenTileCenters);
    } // calcJimsTilingHalfEdgeLength



    public static double
    polygonHalfAngle(double p, double halfEdgeLength)
    {
        return Math.asin((p == 0. ? 1. : Math.cos(Math.PI/p))
                       / MyMath.cosh(halfEdgeLength));
    }

    public static double
    polygonCircumRadius(double p, double halfEdgeLength)
    {
        if (p == 0.) // means infinity
            return Double.POSITIVE_INFINITY;
        else
            return MyMath.asinh( MyMath.sinh(halfEdgeLength) / Math.sin(Math.PI/p));
    }

    public static double
    polygonInRadius(double p, double halfEdgeLength)
    {
        if (p == 0.) // means infinity
            return Double.POSITIVE_INFINITY;
        else
            return MyMath.asinh( MyMath.tanh(halfEdgeLength) / Math.tan(Math.PI/p));
    }

    public static Complex
    hlerp(double x0, double y0, double x1, double y1, double t, Complex result)
    {
        Isometry2 take_p0_to_origin = Isometry2.pureTranslation(-x0,-y0);
        Isometry2 take_origin_to_p0 = Isometry2.pureTranslation(x0,y0);
        Complex p1_ = take_p0_to_origin.apply(x1,y1);
        double normSqrd = p1_.x*p1_.x + p1_.y*p1_.y;
        if (normSqrd < 1e-12)
        {
            result.x = x0 + t*(x1-x0);
            result.y = y0 + t*(y1-y0);
            return result; // success
        }
        if (normSqrd > 1.)
        {
            result.x = 0.;
            result.y = 0.;
            return null; // failure; points are on opposite sides of the wall
        }
        double norm = Math.sqrt(normSqrd);
        double atanNorm = MyMath.atanh(norm);
        if (result != null)
        {
            double desiredNorm = MyMath.tanh(t * atanNorm);

            Complex p_ = p1_; // use same space
            double scale = desiredNorm/norm;
            p_.x = p1_.x * scale;
            p_.y = p1_.y * scale;
            take_origin_to_p0.apply(p_, result);
        }
        return result;
    } // hlerp2

    public static Complex
    hlerp(double x0, double y0, double x1, double y1, double t)
    {
        return hlerp(x0,y0, x1,y1, t, new Complex());
    }

    /*  (This works and is simple but is more work than necessary)
    public static double
    hdist(double x0, double y0, double x1, double y1)
    {
        Isometry2 take_p0_to_origin = Isometry2.pureTranslation(-x0,-y0);
        Complex p1_ = take_p0_to_origin.apply(x1,y1);
        return e2hNorm(MyMath.hypot(p1_.x,p1_.y));
    }
    */

    // From
    //    http://www.maths.gla.ac.uk/~wws/cabripages/hyperbolic/hdistance.html
    // hdist(p0,p1) = e2hNorm(|p1-p0|/|conj(p0)*p1-1|)
    public static double
    hdist(double x0, double y0, double x1, double y1)
    {
        double numX = x1-x0;
        double numY = y1-y0;
        double denomX = x0*x1 + y0*y1 - 1.;
        double denomY = x0*y1 - y0*x1;
        double eNorm = Math.sqrt((numX*numX + numY*numY)
                               / (denomX*denomX + denomY*denomY));
        double hNorm = e2hNorm(eNorm);
        return hNorm;
    }

    // The following is a nice macro if we had a preprocessor...
    //#define assert(expr) { if (!(expr)) throw new Error("Assertion failed at "+__FILE__+"("+__LINE__+"): " + #expr + ""); }
    // (capitalized to avoid conflict with new built-in assert)
    private static void Assert(boolean expr)
    {
        if (!expr)
            throw new Error("Assertion failed");
    }
} // public class HyperbolicUtils
