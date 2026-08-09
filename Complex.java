// Author: Don Hatch (hatch@hadron.org)
// This code may be used for any purpose as long as it is good and not evil.

// XXX idea: this doesn't need to have member variables; all operations
// XXX can operate on arrays, which would ease compatibility to higher
// XXX dimensions.  Isometry2 could be a subclass of Isometry
// XXX (or Isometry could be just an interface),
// XXX with optimized functions and representation.

public class Complex {
    public double x, y;

    //
    // Constructors...
    //
    public Complex()
    {
        // nothing
    }
    public Complex(Complex z)
    {
        this.x = z.x;
        this.y = z.y;
    }
    public Complex(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public Complex set(Complex z)
    {
        this.x = z.x;
        this.y = z.y;
        return this;
    }
    public Complex set(double x, double y)
    {
        this.x = x;
        this.y = y;
        return this;
    }

    //
    // Hard-to-use versions that return stuff in params,
    // so caller can allocate once in case this is called in an inner loop.
    // The result param may be the same as one of the operands.
    //
    public static Complex conj(Complex z, Complex result)
    {
        result.x = z.x;
        result.y = -z.y;
        return result;
    }
    public static Complex neg(Complex z, Complex result)
    {
        result.x = -z.x;
        result.y = -z.y;
        return result;
    }
    public static Complex sub(Complex a, Complex b, Complex result)
    {
        result.x = a.x - b.x;
        result.y = a.y - b.y;
        return result;
    }
    public static Complex add(Complex a, Complex b, Complex result)
    {
        result.x = a.x + b.x;
        result.y = a.y + b.y;
        return result;
    }
    public static Complex mul(Complex a, Complex b, Complex result)
    {
        double x = a.x*b.x - a.y*b.y;
        double y = a.x*b.y + a.y*b.x;
        result.x = x;
        result.y = y;
        return result;
    }
    public static Complex div(Complex a, Complex b, Complex result)
    {
        double invLenSqrd = 1./(b.x*b.x + b.y*b.y);
        double x = (a.x*b.x + a.y*b.y) * invLenSqrd;
        double y = (-a.x*b.y + a.y*b.x) * invLenSqrd;
        result.x = x;
        result.y = y;
        return result;
    }
    public static Complex mul(Complex a, double b, Complex result)
    {
        result.x = a.x * b;
        result.y = a.y * b;
        return result;
    }

    //
    // Easy-to-use but slower versions
    // that allocate and return a new Complex.
    //
    public static Complex conj(Complex z)
    {
        return conj(z, new Complex());
    }
    public static Complex neg(Complex z)
    {
        return neg(z, new Complex());
    }
    public static Complex sub(Complex a, Complex b)
    {
        return sub(a, b, new Complex());
    }
    public static Complex add(Complex a, Complex b)
    {
        return add(a, b, new Complex());
    }
    public static Complex mul(Complex a, Complex b)
    {
        return mul(a, b, new Complex());
    }
    public static Complex mul(Complex a, double b)
    {
        return mul(a, b, new Complex());
    }
    public static Complex div(Complex a, Complex b)
    {
        return div(a, b, new Complex());
    }

    public String toString()
    {
        return ""+x+","+y;
    }

    static final Complex zero = new Complex(0,0);
    static final Complex one = new Complex(1,0);
} // public class Complex
