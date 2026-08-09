// Author: Don Hatch (hatch@hadron.org)
// This code may be used for any purpose as long as it is good and not evil.

/*
 * From http://www.acm.org/sigchi/chi95/Electronic/documnts/papers/jl_bdy.htm#appendix:
 * Any isometry of the Poincare disk
 * can be expressed as a complex function of z of the form
 *      (T*z + P)/(1 - conj(P)*T*z)
 * where T and P are complex numbers, |P| < 1 and |T| = 1.
 * This indicates a rotation by T around the origin followed
 * by moving the origin to P (and -P to the origin).
 * NOTE the paper was missing the T in the denominator!
 *
 * I added an "R" (reflect) term, which can be 1 or -1.
 * If it's -1, the isometry starts by conjugating z
 * (i.e. reflecting it about the x axis).
 * This allows the full extended symmetry group
 * of the hyperbolic plane to be expressed.
 */

// Depends on Complex.java

public class SphericalIsometry2
{
    public Complex T, P;
    public int R;

    //
    // Constructors...
    //

    // Note that T and P are referenced and not copied,
    // so be careful about altering them.
    // This should be used primarily when T and P have been
    // constructed for the single purpose of constructing the Isometry.
    public SphericalIsometry2(Complex T, Complex P, int R)
    {
        this.T = T;
        this.P = P;
        this.R = R;
    }
    // no-arg constructor allocates but does not initialize...
    public SphericalIsometry2()
    {
        this.T = new Complex();
        this.P = new Complex();
    }
    // Copy constructor copies (does not reference) from's stuff.
    // I have a feeling I'm being random and inconsistent,
    // but Isometry.mul() currently relies on these semantics.
    public SphericalIsometry2(SphericalIsometry2 from)
    {
        this.T = new Complex(from.T);
        this.P = new Complex(from.P);
        this.R = from.R;
    }

    public SphericalIsometry2 set(SphericalIsometry2 from)
    {
        T.set(from.T);
        P.set(from.P);
        R = from.R;
        return this;
    }

    // Attempted to optimize somewhat;
    // however, note that 7 temporaries are created and destroyed.
    // (If we really wanted to get rid of allocations,
    // the array of temporaries would be supplied by the caller.)
    // result is allowed to be equal to _z.
    public Complex apply(Complex _z, Complex result)
    {
        /*
         * In C++, this was easy:
         *   if (R < 0)
         *       z.y = -z.y;
         *   return (T*z + P) / (1 - P.conj()*T*z);
         */
        Complex temp[] = new Complex[7]; // XXX not optimal
        {
            int i, n = temp.length;
            //FOR (i, n)
            for (i = 0; i < n; ++i)
                temp[i] = new Complex();
        }
        Complex z = temp[0];

        z.set(_z);
        if (R < 0)
            z.y = -z.y;

        return Complex.div(
                   Complex.add(
                       Complex.mul(T,z, temp[1]),
                       P,
                       temp[2]),
                   Complex.sub(
                       Complex.one,
                       Complex.mul(
                           Complex.mul(
                               Complex.conj(P, temp[3]),
                               T,
                               temp[4]),
                           z,
                           temp[5]),
                       temp[6]),
                   result);
    }

    public Complex apply(Complex z)
    {
        return apply(z, new Complex());
    }

    public Complex apply(double x, double y)
    {
        return apply(new Complex(x,y), new Complex());
    }

    public Complex apply(double x, double y, Complex result)
    {
        return apply(new Complex(x,y), result);
    }

    public double[/*2*/] apply(double x, double y, double result[/*2*/])
    {
        Complex in = new Complex(x,y);
        Complex out = new Complex();
        apply(in,out);
        result[0] = out.x;
        result[1] = out.y;
        return result;
    }

    public double[/*2*/] apply(double z[/*2*/], double result[/*2*/])
    {
        return apply(z[0],z[1],result);
    }

    public double[/*2*/] apply(double z[/*2*/])
    {
        return apply(z[0], z[1], new double[2]);
    }

    public static SphericalIsometry2 pureRotation(double angle)
    {
        return new SphericalIsometry2(new Complex(Math.cos(angle),Math.sin(angle)),
                             Complex.zero,
                             1);

    }
    // the simple isometry that takes 0,0 to x1,y1, and takes -x1,-y1 to 0,0
    public static SphericalIsometry2
    pureTranslation(double x1, double y1)
    {
        return new SphericalIsometry2(Complex.one,
                             new Complex(x1,y1),
                             1);
    }

    public boolean equals(SphericalIsometry2 other, double eps)
    {
        // XXX clean up-- this is error-prone!
        return R == other.R
            && T.x-other.T.x <= eps
            && other.T.x-T.x <= eps
            && T.y-other.T.y <= eps
            && other.T.y-T.y <= eps
            && P.x-other.P.x <= eps
            && other.P.x-P.x <= eps
            && P.y-other.P.y <= eps
            && other.P.y-P.y <= eps;
    }

    // implementing Object's...
    public boolean equals(Object other)
    {
        if (XXXdebug)
            System.out.println("!!!!!!!!!!!!!!!!!!!!!SphericalIsometry2.equals returning "+ equals((SphericalIsometry2)other, 1e-6));
        return equals((SphericalIsometry2)other, 1e-6); // XXX lame
    }

    public String toString()
    {
        return "[T=("+T.x+","+T.y+") P=("+P.x+","+P.y+") R="+R+"]";
    }
    public static final SphericalIsometry2 identity = new SphericalIsometry2(Complex.one,Complex.zero,1);


    //
    // This isn't foolproof, but is a quick and dirty way
    // to hash a tuple of doubles in the range [-1..1] (approximately).
    // The algorithm is:
    //    for each element
    //        1. Add a small uncommon irrational constant > 2, to make sure
    //           the result is > 1 and to minimize the chance that truncating
    //           the low bits will be unstable.
    //           We use the constant PI*E/4 =~ 2.1349336...
    //        2. The result is a small double > 1.
    //           Take the high 16 bits of the fractional part,
    //           and throw the result into the hash.
    //           (If we use too many bits, we raise the chance of
    //           instability which can make us violate
    //           the contract that objects for which equal() is true
    //           should have the same hash code; I don't know what will
    //           happen then.  But if we use too few
    //           bits, we will get hash collisions, which aren't fatal
    //           but which degrade performance).
    public int hashCode()
    {
        int hash = 0;
        int i = 0;
        hash = _accumulateHash(hash, _hashCode1(T.x), i++);
        hash = _accumulateHash(hash, _hashCode1(T.y), i++);
        hash = _accumulateHash(hash, _hashCode1(P.x), i++);
        hash = _accumulateHash(hash, _hashCode1(P.y), i++);
        hash = _accumulateHash(hash, R, i++);
        if (XXXdebug)
        {
            System.out.print("------------------> ");
            PRINT(hash);
        }
        return hash;
    }
    private int _hashCode1(double x)
    {
        if (XXXdebug) System.out.println();
        if (XXXdebug) PRINT(x);
        x += uncommonConstantForHash;
        // now x > 2
        if (XXXdebug) PRINT(x);
        x -= (int)x;
        // now 0 <= x < 1
        if (XXXdebug) PRINT(x);
        x *= (1<<16);
        if (XXXdebug) PRINT(x);
        if (XXXdebug) PRINT((int)x);
        // now 0 <= x < (1<<16)
        return (int)x;
    }
    private int _accumulateHash(int hash, int incr, int i)
    {
        hash = hash * (((i&1)!=0)?11:13) + incr;
        return hash;
    }

    private static final double uncommonConstantForHash = Math.PI * Math.E / 4;

    public static boolean XXXdebug = false;


    // XXX there's a macro for this in macros.h that shows the name too
    public static void PRINT(Object x)
    {
        System.out.println("??? = " + x);
    }
    public static void PRINT(double x)
    {
        System.out.println("??? = " + x);
    }
} // public class SphericalSphericalIsometry2
