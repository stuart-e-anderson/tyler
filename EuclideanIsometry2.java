// Author: Don Hatch (hatch@hadron.org)
// This code may be used for any purpose as long as it is good and not evil.

// Depends on Complex.java

// Euclidean Isometry
public class EuclideanIsometry2 extends AbstractIsometry2
{
    public Complex rot, trans;
    public int refl;

    //
    // Constructors...
    //

    // Note that rot and trans are referenced and not copied,
    // so be careful about altering them.
    // This should be used primarily when rot and trans have been
    // constructed for the single purpose of constructing the Isometry.
    public EuclideanIsometry2(Complex rot, Complex trans, int refl)
    {
        this.rot = rot;
        this.trans = trans;
        this.refl = refl;
    }
    // no-arg constructor allocates but does not initialize...
    public EuclideanIsometry2()
    {
        this.rot = new Complex();
        this.trans = new Complex();
    }
    // Copy constructor copies (does not reference) from's stuff.
    // I have a feeling I'm being random and inconsistent,
    // but Isometry.mul() currently relies on these semantics.
    public EuclideanIsometry2(EuclideanIsometry2 from)
    {
        this.rot = new Complex(from.rot);
        this.trans = new Complex(from.trans);
        this.refl = from.refl;
    }

    public EuclideanIsometry2 set(EuclideanIsometry2 from)
    {
        rot.set(from.rot);
        trans.set(from.trans);
        refl = from.refl;
        return this;
    }

    // result is allowed to be equal to _z.
    public Complex apply(Complex _z, Complex result)
    {
        /*
         * In C++, this was easy:
         *   if (refl < 0)
         *       z.y = -z.y;
         *   return (rot*z + trans);
         */
        Complex z = new Complex(_z);
        if (refl < 0)
            z.y = -z.y;

        return Complex.add(
                       Complex.mul(rot,z),
                       trans,
                       result);
    }

    // Note order: (F1*F0)(x,y) = F1(F0(x,y))
    // Attempted to optimize somewhat, but ... temporaries are created
    // and destroyed.
    public AbstractIsometry2 times(AbstractIsometry2 _F0, AbstractIsometry2 _result)
    {
        EuclideanIsometry2 F1 = this;
        EuclideanIsometry2 F0 = new EuclideanIsometry2((EuclideanIsometry2)_F0); // copy so can change
        EuclideanIsometry2 result = (EuclideanIsometry2)_result;
        /*
         * In C++, this was:
         *  if (F1.refl < 0)
         *  {
         *      F0.rot.y = -F0.rot.y;
         *      F0.trans.y = -F0.trans.y;
         *  }
         *  EuclideanIsometry2 result;
         *  result.refl = F1.refl * F0.refl;
         *  result.trans = (F1.rot*F0.trans + F1.trans);
         *  result.rot = (F0.rot*F1.rot + F0.rot*F0.trans.conj()*F1.trans);
         *  // renormalize, as recommended in the paper.
         *  // use the fact that sqrt(1+eps) is approximately 1+eps/2 for small eps.
         *  /* double invLenT = 1./hypot(result.rot.x, result.rot.y); * /
         *  double invLenT = 1. / ((1+result.rot.x*result.rot.x+result.rot.y*result.rot.y)*.5);
         *  result.rot.x *= invLenT;
         *  result.rot.y *= invLenT;
         *  return result;
         */
        Complex temp[] = new Complex[13]; // XXX not optimal
        {
            int i, n = temp.length;
            //FOR (i, n)
            for (i = 0; i < n; ++i)
                temp[i] = new Complex();
        }

        if (F1.refl < 0)
        {
            F0.rot.y = -F0.rot.y;
            F0.trans.y = -F0.trans.y;
        }

        result.refl = F1.refl * F0.refl;
        Complex.add(
            Complex.mul(
                F1.rot,
                F0.trans,
                temp[6]),
            F1.trans,
            result.trans);
        Complex.add(
            Complex.mul(F0.rot,
                        F1.rot,
                        temp[8]),
            Complex.mul(
                Complex.mul(F0.rot,
                            Complex.conj(F0.trans, temp[9]),
                            temp[10]),
                F1.trans,
                temp[11]),
            result.rot);

        // renormalize, as recommended in the paper.
        // use the fact that sqrt(1+eps) is approximately 1+eps/2 for small eps.
        /* double invLenT = 1./hypot(result.rot.x, result.rot.y); */
        double invLenT = 1. / ((1. + result.rot.x*result.rot.x
                                   + result.rot.y*result.rot.y) * .5);
        result.rot.x *= invLenT;
        result.rot.y *= invLenT;

        return result;
    } // mul

    // less optimized easier-to-use version that returns
    // a new EuclideanIsometry2
    public AbstractIsometry2 times(AbstractIsometry2 F0)
    {
        return times(F0, new EuclideanIsometry2());
    }

    // not optimized, shouldn't be used in inner loops
    public AbstractIsometry2 inverse()
    {
        /*
         * In C++, this was easy:
         *     EuclideanIsometry2 F0(1,-trans), F1(rot.conj(),0), F2(1,0,refl);
         *     return F2 * F1 * F0;
         */
        EuclideanIsometry2 F0 = new EuclideanIsometry2(Complex.one,Complex.neg(trans),1);
        EuclideanIsometry2 F1 = new EuclideanIsometry2(Complex.conj(rot),Complex.zero,1);
        EuclideanIsometry2 F2 = new EuclideanIsometry2(Complex.one,Complex.zero,refl);
        return F2.times(F1).times(F0);
    }

    public static EuclideanIsometry2 pureRotation(double angle)
    {
        return new EuclideanIsometry2(new Complex(Math.cos(angle),Math.sin(angle)),
                             Complex.zero,
                             1);

    }
    // the simple isometry that takes 0,0 to x1,y1, and takes -x1,-y1 to 0,0
    public static EuclideanIsometry2
    pureTranslation(double x1, double y1)
    {
        return new EuclideanIsometry2(Complex.one,
                             new Complex(x1,y1),
                             1);
    }

    public boolean equals(AbstractIsometry2 _other, double eps)
    {
        EuclideanIsometry2 other = (EuclideanIsometry2)_other;
        // XXX clean up-- this is error-prone!
        return refl == other.refl
            && rot.x-other.rot.x <= eps
            && other.rot.x-rot.x <= eps
            && rot.y-other.rot.y <= eps
            && other.rot.y-rot.y <= eps
            && trans.x-other.trans.x <= eps
            && other.trans.x-trans.x <= eps
            && trans.y-other.trans.y <= eps
            && other.trans.y-trans.y <= eps;
    }

    public String toString()
    {
        return "[rot=("+rot.x+","+rot.y+") trans=("+trans.x+","+trans.y+") refl="+refl+"]";
    }
    public static final EuclideanIsometry2 identity = new EuclideanIsometry2(Complex.one,Complex.zero,1);
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
        hash = _accumulateHash(hash, _hashCode1(rot.x), i++);
        hash = _accumulateHash(hash, _hashCode1(rot.y), i++);
        hash = _accumulateHash(hash, _hashCode1(trans.x), i++);
        hash = _accumulateHash(hash, _hashCode1(trans.y), i++);
        hash = _accumulateHash(hash, refl, i++);
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
} // public class EuclideanIsometry2
