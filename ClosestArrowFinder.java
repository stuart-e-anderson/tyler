public class ClosestArrowFinder
{
    public static final int CW = -1;
    public static final int CLOSEST_ANGLE = 0;
    public static final int CCW = 1;
    public static int reverseMode(int mode)
    {
        return -mode; // CCW turns into CW, CW turns into CCW
    }

    private double pickX, pickY;
    private double minDistSqrd = Double.POSITIVE_INFINITY;
    private double bestTailX = Double.NaN, bestTailY = Double.NaN;
    private Object bestTailObj = null;
    private double pickAngle = Double.NaN;
    private double minAngleDiff = Double.POSITIVE_INFINITY;
    private double bestHeadX = Double.NaN, bestHeadY = Double.NaN;
    private int mode;
    private double eps;

    // Creates a ClosestArrowFinder
    // for finding the closest "arrow" to pick position pickX, pickY.
    // Mode for selecting the head is CLOSEST_ANGLE, CCW, or CW.
    public ClosestArrowFinder(double pickX, double pickY, int mode, double eps)
    {
        this.pickX = pickX;
        this.pickY = pickY;
        this.mode = mode;
        this.eps = eps;
    }

    // Call this for every possible tail...
    public void anotherPossibleTail(double x, double y, Object obj)
    {
        double thisDistSqrd = hypotSqrd(x-this.pickX, y-this.pickY);
        if (LT(thisDistSqrd, minDistSqrd, eps))
        {
            minDistSqrd = thisDistSqrd;
            bestTailX = x;
            bestTailY = y;
            bestTailObj = obj;
        }
    }

    // Then get the best tail using this...
    public Object getBestTailObj()
    {
        return bestTailObj;
    }

    // Then call this for every possible head...
    public void anotherPossibleHead(double x, double y)
    {
        if (Double.isNaN(pickAngle))
            pickAngle = Math.atan2(pickY-bestTailY,
                                   pickX-bestTailX);
        double thisAngle = Math.atan2(y-bestTailY, x-bestTailX);
        double thisAngleDiff = thisAngle - pickAngle;
        switch (mode)
        {
            case CLOSEST_ANGLE:
                while (thisAngleDiff <= -Math.PI)
                    thisAngleDiff += 2*Math.PI;
                while (thisAngleDiff > Math.PI)
                    thisAngleDiff -= 2*Math.PI;
                break;
            case CCW:
                while (LEQ(thisAngleDiff, 0., eps))
                    thisAngleDiff += 2*Math.PI;
                break;
            case CW:
                while (GEQ(thisAngleDiff, 0., eps))
                    thisAngleDiff -= 2*Math.PI;
                break;
        }
        thisAngleDiff = Math.abs(thisAngleDiff);

        if (LT(thisAngleDiff, minAngleDiff, eps))
        {
            minAngleDiff = thisAngleDiff;
            bestHeadX = x;
            bestHeadY = y;
        }
    }

    // Then get the arrow tail and head coords using this...
    public double[/*2*/][/*2*/] getBestArrow()
    {
        return new double[][]{{bestTailX, bestTailY},{bestHeadX,bestHeadY}};
    }



    //
    // Private Utilities...
    //
        private static double hypotSqrd(double x, double y)
        {
            return x*x + y*y;
        }
        // fuzzy less-than
        private static boolean LT(double a, double b, double eps)
        {
            return b-a > eps;
        }
        // fuzzy less-or-equal
        private static boolean LEQ(double a, double b, double eps)
        {
            return a-b <= eps;
        }
        // fuzzy greater-or-equal
        private static boolean GEQ(double a, double b, double eps)
        {
            return b-a <= eps;
        }
    
} // class closestArrowFinder


