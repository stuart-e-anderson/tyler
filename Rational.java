
/**
 * Rational numbers, not necessarily reduced to lowest terms
 */
public class Rational
{
    public final int n, d; // numerator, denominator

    public Rational(int n, int d)
    {
        this.n = n;
        this.d = d;
    }
    public Rational(int n)
    {
        this.n = n;
        this.d = 1;
    }

    public double toDouble()
    {
        return (double)n/(double)d;
    }

    public String toString()
    {
        return d==1 ? n+"" : n+"/"+d;
    }
      
    public static Rational parseRational(String str)
    {
        int ind = str.indexOf('/');
        if (ind >= 0)
            return new Rational(Integer.parseInt(str.substring(0,ind)),
                                Integer.parseInt(str.substring(ind+1)));
        else
            return new Rational(Integer.parseInt(str), 1);
    }

    public static Rational[] parseRationalList(String str)
    {
        java.util.StringTokenizer st = new java.util.StringTokenizer(str, ", ");
        Rational list[] = new Rational[st.countTokens()];
        for (int i = 0; i < list.length; ++i)
            list[i] = parseRational(st.nextToken());
        return list;
    }

    public static String rationalListToString(Rational[] list)
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < list.length; ++i)
        {
            buf.append(list[i]);
            if (i+1 < list.length)
                buf.append(',');
        }
        return buf.toString();
    }
} // end class Rational
