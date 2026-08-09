//
// SphericalUtils.java
// Utilities for dealing with the canonical stereographic projection
// of a sphere onto a plane
// (i.e. perspective projection from an eye point lying on the sphere).
// Note, This file is VERY similar to HyperbolicUtils.java-- diff them to see.
//
// Author: Don Hatch (hatch@hadron.org)
// This code may be used for any purpose as long as it is good and not evil.

public class SphericalUtils
{
    // spherical to euclidean norm (distance from 0,0)
    // in stereographic projection.
    // in the limit (approaching 0), it's half the spherical norm.
    public static double
    s2eNorm(double sNorm)
    {
        return (2.*Math.sin(sNorm)) / (1.+Math.cos(sNorm));
    }

    // euclidean to spherical norm (distance from 0,0)
    // in stereographic projection.
    // in the limit (approaching 0), it's twice the euclidean norm.
    public static double
    e2sNorm(double eNorm)
    {
        return Math.atan2(4.*eNorm, 4-eNorm*eNorm);
    }

    //
    // Calculate half-edge-length of semiregular tesselation with each
    // vertex surrounded by polygons p[0]..p[nps-1] q times.
    // (If you want a covering density other than 1,
    // then divide q by the density.)
    // XXX The paper "Uniform Solution for Uniform Polyhedra" by Zvi Har'el
    //      http://www.math.technion.ac.il/~rl/uniform.pdf
    // says this is not the most stable way to do this,
    // but I think it's okay for the configurations we are interested in,
    // namely the ones that make pretty pictures
    //
    public static double
    calcUniformTilingHalfEdgeLength(double p[], double q)
    {
        int nps = p.length;
        if (nps == 0)
            return 0.;
        // We want edge length e such that
        //         SUM(i==0..nps-1) polygonAngle(p[i],e) = 2*PI
        // where polygonAngle(p,e) = 2*asin(cos(pi/p)/cos(e/2)).
        // Find cos(e/2) by binary search (it's a decreasing function of e).
        double minp = Double.POSITIVE_INFINITY;
        for (int i = 0; i < nps; ++i)
        {
            if (p[i] != 0) // 0 means infinity
                minp = Math.min(minp, p[i]);
        }
        double hi = 1.; /* corresponds to e = 0. */
        double lo = Math.cos(Math.PI/minp)/Math.sin(Math.PI/(nps*q)); /* corresponds to e = edgeLength(minp, nps*q) */
        double mid;
        while ((mid = lo + (hi-lo)*.5) != lo && mid != hi)
        {
            double sum = 0.;
            for (int i = 0; i < nps; ++i)
                sum += q*2.*Math.asin((p[i]==0?1.:Math.cos(Math.PI/p[i]))/mid);
            if (sum > 2*Math.PI) /* then e is too big, and therefore mid is too small */
                lo = mid;
            else
                hi = mid;
        }
        double result = Math.acos(mid);
        Assert(!Double.isNaN(result));
        Assert(!Double.isInfinite(result));
        return result;
    } // calcUniformTilingHalfEdgeLength

    public static double
    polygonCircumRadius(double p, double halfEdgeLength)
    {
        return Math.asin( Math.sin(halfEdgeLength) / Math.sin(Math.PI/p));
    }

    public static double
    polygonInRadius(double p, double halfEdgeLength)
    {
        return Math.asin( Math.tan(halfEdgeLength) / Math.tan(Math.PI/p) );
    }

    public static Complex
    slerp(double x0, double y0, double x1, double y1, double t, Complex result)
    {
        SphericalIsometry2 take_p0_to_origin = SphericalIsometry2.pureTranslation(-x0,-y0);
        SphericalIsometry2 take_origin_to_p0 = SphericalIsometry2.pureTranslation(x0,y0);
        Complex p1_ = take_p0_to_origin.apply(x1,y1);
        double normSqrd = p1_.x*p1_.x + p1_.y*p1_.y;
        if (normSqrd < 1e-12)
        {
            result.x = x0 + t*(x1-x0);
            result.y = y0 + t*(y1-y0);
            return result; // success
        }
        double norm = Math.sqrt(normSqrd);
        double atanNorm = Math.atan(norm);
        if (result != null)
        {
            double desiredNorm = Math.tan(t * atanNorm);

            Complex p_ = p1_; // use same space
            double scale = desiredNorm/norm;
            p_.x = p1_.x * scale;
            p_.y = p1_.y * scale;
            take_origin_to_p0.apply(p_, result);
        }
        return result;
    } // hlerp2

    public static Complex
    slerp(double x0, double y0, double x1, double y1, double t)
    {
        return slerp(x0,y0, x1,y1, t, new Complex());
    }

    public static double
    sdist(double x0, double y0, double x1, double y1)
    {
        SphericalIsometry2 take_p0_to_origin = SphericalIsometry2.pureTranslation(-x0,-y0);
        Complex p1_ = take_p0_to_origin.apply(x0,y1);
        return e2sNorm(MyMath.hypot(p1_.x,p1_.y));
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
