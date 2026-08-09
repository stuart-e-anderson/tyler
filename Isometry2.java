// Author: Don Hatch (hatch@hadron.org)
// This code may be used for any purpose as long as it is good and not evil.

/*
 * From http://www.acm.org/sigchi/chi95/Electronic/documnts/papers/jl_bdy.htm#appendix:
 * Any isometry of the Poincare disk
 * can be expressed as a complex function of z of the form
 *      (T*z + P)/(1 + conj(P)*T*z)
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

//#include "macros.h"

// Hyperbolic Isometry
// XXX should change the name to HyperbolicIsometry2
public class Isometry2 extends AbstractIsometry2
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
    public Isometry2(Complex T, Complex P, int R)
    {
        this.T = T;
        this.P = P;
        this.R = R;
    }
    // no-arg constructor allocates but does not initialize...
    public Isometry2()
    {
        this.T = new Complex();
        this.P = new Complex();
    }
    // Copy constructor copies (does not reference) from's stuff.
    // I have a feeling I'm being random and inconsistent,
    // but Isometry.mul() currently relies on these semantics.
    public Isometry2(Isometry2 from)
    {
        this.T = new Complex(from.T);
        this.P = new Complex(from.P);
        this.R = from.R;
    }

    public Isometry2 set(Isometry2 from)
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
         *   return (T*z + P) / (P.conj()*T*z + 1);
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
                   Complex.add(
                       Complex.mul(
                           Complex.mul(
                               Complex.conj(P, temp[3]),
                               T,
                               temp[4]),
                           z,
                           temp[5]),
                       Complex.one,
                       temp[6]),
                   result);
    }

    // Note order: (F1*F0)(x,y) = F1(F0(x,y))
    // Attempted to optimize somewhat, but ... temporaries are created
    // and destroyed.
    public AbstractIsometry2 times(AbstractIsometry2 _F0, AbstractIsometry2 _result)
    {
        Isometry2 F1 = this;
        Isometry2 F0 = new Isometry2((Isometry2)_F0); // copy so can change
        Isometry2 result = (Isometry2)_result;
        /*
         * In C++, this was:
         *  if (F1.R < 0)
         *  {
         *      F0.T.y = -F0.T.y;
         *      F0.P.y = -F0.P.y;
         *  }
         *  complexd denom = F1.T*F0.P*F1.P.conj() + 1;
         *  Isometry2 result;
         *  result.R = F1.R * F0.R;
         *  result.P = (F1.T*F0.P + F1.P) / denom;
         *  result.T = (F0.T*F1.T + F0.T*F0.P.conj()*F1.P) / denom;
         *  // renormalize, as recommended in the paper.
         *  // use the fact that sqrt(1+eps) is approximately 1+eps/2 for small eps.
         *  /* double invLenT = 1./hypot(result.T.x, result.T.y); * /
         *  double invLenT = 1. / ((1+result.T.x*result.T.x+result.T.y*result.T.y)*.5);
         *  result.T.x *= invLenT;
         *  result.T.y *= invLenT;
         *  return result;
         */
        Complex temp[] = new Complex[13]; // XXX not optimal
        {
            int i, n = temp.length;
            //FOR (i, n)
            for (i = 0; i < n; ++i)
                temp[i] = new Complex();
        }

        if (F1.R < 0)
        {
            F0.T.y = -F0.T.y;
            F0.P.y = -F0.P.y;
        }

        Complex denom = Complex.add(
                            Complex.mul(
                                Complex.mul(F1.T,
                                            F0.P,
                                            temp[2]),
                                Complex.conj(F1.P,
                                             temp[3]),
                                temp[4]),
                            Complex.one,
                            temp[5]);
        result.R = F1.R * F0.R;
        Complex.div(
            Complex.add(
                Complex.mul(
                    F1.T,
                    F0.P,
                    temp[6]),
                 F1.P,
                 temp[7]),
            denom,
            result.P);
        Complex.div(
                Complex.add(
                    Complex.mul(F0.T,
                                F1.T,
                                temp[8]),
                    Complex.mul(
                        Complex.mul(F0.T,
                                    Complex.conj(F0.P, temp[9]),
                                    temp[10]),
                        F1.P,
                        temp[11]),
                    temp[12]),
            denom,
            result.T);

        // renormalize, as recommended in the paper.
        // use the fact that sqrt(1+eps) is approximately 1+eps/2 for small eps.
        /* double invLenT = 1./hypot(result.T.x, result.T.y); */
        double invLenT = 1. / ((1. + result.T.x*result.T.x
                                   + result.T.y*result.T.y) * .5);
        result.T.x *= invLenT;
        result.T.y *= invLenT;

        return result;
    } // mul

    // less optimized easier-to-use version that returns
    // a new Isometry2
    public AbstractIsometry2 times(AbstractIsometry2 F0)
    {
        return times(F0, new Isometry2());
    }

    // not optimized, shouldn't be used in inner loops
    public AbstractIsometry2 inverse()
    {
        /*
         * In C++, this was easy:
         *     Isometry2 F0(1,-P), F1(T.conj(),0), F2(1,0,R);
         *     return F2 * F1 * F0;
         */
        Isometry2 F0 = new Isometry2(Complex.one,Complex.neg(P),1);
        Isometry2 F1 = new Isometry2(Complex.conj(T),Complex.zero,1);
        Isometry2 F2 = new Isometry2(Complex.one,Complex.zero,R);
        return F2.times(F1).times(F0);
    }

    public static Isometry2 pureRotation(double angle)
    {
        return new Isometry2(new Complex(Math.cos(angle),Math.sin(angle)),
                             Complex.zero,
                             1);

    }
    // the simple isometry that takes 0,0 to x1,y1, and takes -x1,-y1 to 0,0
    public static Isometry2
    pureTranslation(double x1, double y1)
    {
        return new Isometry2(Complex.one,
                             new Complex(x1,y1),
                             1);
    }

    //
    // Find the pure translation (i.e. moves the origin straight in some
    // direction) that takes p0 to p1.
    // This is a result of calculation on paper, and it can probably
    // be simplified.
    // It's ambiguous if both points are on the circumference of the circle.
    // (Note, this is completely non-optimized and shouldn't be used
    // in heavy computation.  Mostly it's good for interaction.)
    //
    public static Isometry2
    pureTranslation(Complex p0, Complex p1)
    {
//#if 0 // XXX this was the C++ way
//        complexd A = p1-p0;
//        complexd B = p1*p0;
//        double denom = 1. - (B.x*B.x + B.y*B.y);
//        Isometry2 result = Isometry2(1,complexd(A.x*(1+B.x)+A.y*B.y,
//                                                A.y*(1-B.x)+A.x*B.y)/denom);
//        /*
//        printf("p0  = %.17g %.17g\n", p0.x,p0.y);
//        printf("p1  = %.17g %.17g\n", p1.x,p1.y);
//        printf("Fp0 = %.17g %.17g\n", result(p0).x,result(p0).y);
//        printf("F   = %.17g %.17g\n", result.P.x,result.P.y);
//        printf("\n");
//        */
//#endif // 0
        Complex A = Complex.sub(p1,p0);
        Complex B = Complex.mul(p1,p0);
        double denom = 1. - (B.x*B.x + B.y*B.y);
        Isometry2 result = new Isometry2(
                                   Complex.one,
                                   new Complex((A.x*(1+B.x)+A.y*B.y)/denom,
                                               (A.y*(1-B.x)+A.x*B.y)/denom),
                                   1);
        return result;
    }

    // utility... not optimized XXX probably a much more direct way
    public static Isometry2
    reflectionAcrossLine(Complex l0, Complex l1)
    {
        Isometry2 takel0ToOrigin = Isometry2.pureTranslation(-l0.x,-l0.y);
        Isometry2 takeOriginTol0 = Isometry2.pureTranslation(l0.x,l0.y);

        Complex l1_ = takel0ToOrigin.apply(l1);
        double angle = Math.atan2(l1_.y, l1_.x);
        Isometry2 rotatel1_ToXAxis = Isometry2.pureRotation(-angle);
        Isometry2 rotateXAxisTol1_ = Isometry2.pureRotation(angle);
        Isometry2 reflectAboutXAxis = new Isometry2(Complex.one,
                                                    Complex.zero,
                                                    -1);
        AbstractIsometry2 result = takel0ToOrigin;
        result = Isometry2.mul(rotatel1_ToXAxis, result);
        result = Isometry2.mul(reflectAboutXAxis, result);
        result = Isometry2.mul(rotateXAxisTol1_, result);
        result = Isometry2.mul(takeOriginTol0, result);
        return (Isometry2)result;
    }

    public boolean equals(AbstractIsometry2 _other, double eps)
    {
        Isometry2 other = (Isometry2)_other;
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

    public String toString()
    {
        return "[T=("+T.x+","+T.y+") P=("+P.x+","+P.y+") R="+R+"]";
    }
    public static final Isometry2 identity = new Isometry2(Complex.one,Complex.zero,1);
    public AbstractIsometry2 getIdentity()
    {
        return identity;
    }


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
} // public class Isometry2
