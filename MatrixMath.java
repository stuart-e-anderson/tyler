//
// Basic matrix utilities
//
public class MatrixMath
{
    public static double[][] identityMatrix(int n)
    {
        double M[][] = new double[n][n];
        for (int i = 0; i < n; ++i)
            for (int j = 0; j < n; ++j)
                M[i][j] = (i==j ? 1. : 0.);
        return M;
    } // identityMatrix

    // matrix multiply
    public static double[][] mxm(double A[][], double B[][])
    {
        int I = A.length;
        int J = B[0].length;
        int K = B.length; // == A[0].length;
        double M[][] = new double[I][J];
        for (int i = 0; i < I; ++i)
            for (int j = 0; j < J; ++j)
            {
                double sum = 0.;
                for (int k = 0; k < K; ++k)
                    sum += A[i][k] * B[k][j];
                M[i][j] = sum;
            }
        return M;
    } // mxm

    // vector times matrix
    public static void vxm(double result[], double v[], double m[][])
    {
        int nRows = m.length; // = v.length;
        if (nRows == 0)
            return;
        int nCols = m[0].length;
        for (int i = 0; i < nCols; ++i)
        {
            double sum = 0.;
            for (int j = 0; j < nRows; ++j)
            {
                sum += v[j] * m[j][i];
            }
            result[i] = sum;
        }
    } // vxm

    // matrix times vector
    public static void mxv(double result[], double m[][], double v[])
    {
        int nRows = m.length;
        if (nRows == 0)
            return;
        int nCols = m[0].length; // = v.length;
        for (int i = 0; i < nRows; ++i)
        {
            double m_i[] = m[i];
            double sum = 0.;
            for (int j = 0; j < nCols; ++j)
            {
                sum += m_i[j] * v[j];
            }
            result[i] = sum;
        }
    } // vxm

    // invert a square rotate-scale-translate matrix
    public static double[][] invertOrtho(double M[][])
    {
        int n = M.length; // == M[0].length
        double scaleSqrd = 0.;
        {
            for (int j = 0; j < n-1; ++j)
                scaleSqrd += M[0][j]*M[0][j];
        }
        double invRotateScalePart[][] = identityMatrix(n);
        {
            for (int i = 0; i < n-1; ++i)
                for (int j = 0; j < n-1; ++j)
                    invRotateScalePart[i][j] = M[j][i] * (1./scaleSqrd);
        }
        double invTranslatePart[][] = identityMatrix(n);
        {
            for (int j = 0; j < n-1; ++j)
                invTranslatePart[n-1][j] = -M[n-1][j];
        }
        return mxm(invTranslatePart, invRotateScalePart);
    } // invertOrtho

} // class MatrixMath
