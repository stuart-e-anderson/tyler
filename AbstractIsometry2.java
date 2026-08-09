// Author: Don Hatch (hatch@hadron.org)
// This code may be used for any purpose as long as it is good and not evil.

// Depends on Complex.java

public abstract class AbstractIsometry2
{
    public abstract Complex apply(Complex _z, Complex result);
    public abstract AbstractIsometry2 times(AbstractIsometry2 F0, AbstractIsometry2 result);
    public abstract AbstractIsometry2 times(AbstractIsometry2 F0);
    public abstract AbstractIsometry2 inverse();
    public abstract boolean equals(AbstractIsometry2 other, double eps);
    public abstract String toString();
    public abstract AbstractIsometry2 getIdentity();

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


    // Note order: (F1*F0)(x,y) = F1(F0(x,y))
    public static AbstractIsometry2 mul(AbstractIsometry2 F1, AbstractIsometry2 F0, AbstractIsometry2 result)
    {
        return F1.times(F0, result);
    }

    public static AbstractIsometry2 mul(AbstractIsometry2 F1, AbstractIsometry2 F0)
    {
        return F1.times(F0);
    }

    public static AbstractIsometry2 pow(AbstractIsometry2 F, int e)
    {
        if (e > 0)
        {
            AbstractIsometry2 temp = pow(F, e/2);
            if ((e % 2) == 0)
                return AbstractIsometry2.mul(temp, temp);
            else
                return AbstractIsometry2.mul(AbstractIsometry2.mul(F,temp),temp);
        }
        else if (e < 0)
        {
            return pow(F.inverse(), -e);
        }
        else
        {
            return F.getIdentity();
        }
    }

    // implementing Object's...
    public boolean equals(Object other)
    {
        return equals((AbstractIsometry2)other, 1e-6); // XXX lame
    }

} // public class AbstractIsometry2
