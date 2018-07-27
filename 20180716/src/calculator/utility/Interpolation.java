package calculator.utility;

import flanagan.math.Matrix;

import java.util.*;

/**
 * @author liangcy
 */
public class Interpolation {

    final public static String INTERPOLATION_METHOD_NATURE = "nature";
    /**
     * not a knot;
     */
    final public static String INTERPOLATION_METHOD_NAK = "nak";
    final public static String EXTRAPOLATION_METHOD_NATURE = "nature";
    final public static String EXTRAPOLATION_METHOD_TANGENT = "tangent";
    final public static String EXTRAPOLATION_METHOD_HORIZONTAL = "horizontal";

    private static void uniqueAndSort(ArrayList<Integer> index, boolean decreasing) {
        HashSet<Integer> uniqueRow = new HashSet<>(index);
        index.clear();
        index.addAll(uniqueRow);
        Collections.sort(index);
        if (decreasing) {
            Collections.reverse(index);
        }
    }

    private static double[] removeElements(ArrayList<Integer> index, double[] x) {
        ArrayList<Double> y = new ArrayList<>();
        for (Double d : x) {
            y.add(d);
        }
        uniqueAndSort(index, true);
        int m = y.size();
        for (int i : index) {
            if (i < m) {
                y.remove(i);
            }
        }
        if (y.isEmpty()) {
            return null;
        } else {
            int n = y.size();
            double[] z = new double[n];
            for (int i = 0; i < n; i++) {
                z[i] = y.get(i);
            }
            return z;
        }
    }

    private static double[][] removeRows(ArrayList<Integer> index, double[][] x) {
        ArrayList<double[]> y = new ArrayList<>(Arrays.asList(x));
        uniqueAndSort(index, true);
        int m = y.size();
        for (int i : index) {
            if (i < m) {
                y.remove(i);
            }
        }
        if (y.isEmpty()) {
            return null;
        } else {
            double[][] result = new double[y.size()][x[0].length];
            y.toArray(result);
            return result;
        }
    }

    private static double[][] removeCols(ArrayList<Integer> index, double[][] x) {
        return CalculateUtil.transpose(removeRows(index, CalculateUtil.transpose(x)));
    }

    private static HashMap<String, double[][]> ignoreNaN(double[] x, double[] y, double[][] z) {
        HashMap<String, double[][]> result = new HashMap<>(3);
        int n = x.length;
        int m = y.length;
        //记录含有NaN的行和列
        ArrayList<Integer> row = new ArrayList<>();
        ArrayList<Integer> col = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (Double.isNaN(z[i][j])) {
                    row.add(i);
                    col.add(j);
                }
            }
        }

        int nRow = row.size();
        int nCol = col.size();
        if (nRow == 0 && nCol == 0) {
            double[][] x1 = {x};
            double[][] y1 = {y};
            result.put("x", x1);
            result.put("y", y1);
            result.put("z", z);
            return result;
        }
        uniqueAndSort(row, false);
        uniqueAndSort(col, false);
        if (nRow <= nCol) {
            // remove row(s) with NaN;
            double[][] z1 = removeRows(row, z);
            double[][] y1 = {removeElements(row, y)};
            double[][] x1 = {x};
            result.put("x", x1);
            result.put("y", y1);
            result.put("z", z1);
            return result;
        } else {
            // remove col(s) with NaN;
            double[][] z1 = removeCols(col, z);
            double[][] y1 = {y};
            double[][] x1 = {removeElements(col, x)};
            result.put("x", x1);
            result.put("y", y1);
            result.put("z", z1);
            return result;
        }
    }

    /**
     * @param x      x-axis;
     * @param y      y-axis;
     * @param method cubic spline method, default method is nature;
     * @return cubic spline parameter a, b, c, d, x, y;
     * a denotes cubic coefficient and d denotes utility coefficient;
     */
    private static HashMap<String, double[]> cubicSplineParameter(double[] x, double[] y, String method) {
        int n = x.length;
        double[] a = new double[n];
        double[] b = new double[n];
        double[] c = new double[n];
        double[] d = new double[n];
        double[] h = new double[n];
        double[] z = new double[n];
        for (int i = 0; i < n - 1; ++i) {
            h[i] = x[i + 1] - x[i];
        }
        z[0] = 0;
        z[n - 1] = 0;
        for (int i = 1; i < n - 1; ++i) {
            z[i] = 6 * ((y[i + 1] - y[i]) / h[i] - (y[i] - y[i - 1]) / h[i - 1]);
        }
        Matrix matrix = getCubicSplineMatrix(h, method);
        double[][] zz = {z};
        Matrix matZ = new Matrix(zz);
        //solve matrix * m = z;
        Matrix solveM = matrix.inverse().times(matZ.transpose());
        double[] m = new double[n];
        for (int i = 0; i < n; ++i) {
            m[i] = solveM.getElement(i, 0);
        }
        for (int i = 0; i < n; ++i) {
            d[i] = y[i];
            b[i] = m[i] / 2;
        }
        a[n - 1] = 0;
        c[n - 1] = 0;
        for (int i = 0; i < n - 1; ++i) {
            a[i] = (m[i + 1] - m[i]) / (6 * h[i]);
            c[i] = (y[i + 1] - y[i]) / h[i] - m[i] * h[i] / 2 - (m[i + 1] - m[i]) * h[i] / 6;
        }
        HashMap<String, double[]> result = new HashMap<>(6);
        result.put("a", a);
        result.put("b", b);
        result.put("c", c);
        result.put("d", d);
        result.put("x", x);
        result.put("y", y);
        return result;
    }

    private static Matrix getCubicSplineMatrix(double[] h, String method) {
        int n = h.length;
        double[][] mat = new double[n][n];
        for (int i = 0; i < n; i++) {
            if (i == 0) {
                if (INTERPOLATION_METHOD_NAK.equals(method)) {
                    mat[i][0] = -h[1];
                    mat[i][1] = h[0] + h[1];
                    mat[i][2] = -h[0];
                } else {
                    mat[i][0] = 1;
                }
            } else if (i == (n - 1)) {
                if (INTERPOLATION_METHOD_NAK.equals(method)) {
                    mat[i][n - 3] = -h[n - 2];
                    mat[i][n - 2] = h[n - 2] + h[n - 3];
                    mat[i][n - 1] = -h[n - 3];
                } else {
                    mat[i][n - 1] = 1;
                }
            } else {
                mat[i][i - 1] = h[i - 1];
                mat[i][i] = 2 * (h[i - 1] + h[i]);
                mat[i][i + 1] = h[i];
            }
        }
        return new Matrix(mat);
    }

    public static double getSplinePoint(HashMap<String, double[]> cs, double x0, String method) {
        double[] x = cs.get("x");
        double[] a = cs.get("a");
        double[] b = cs.get("b");
        double[] c = cs.get("c");
        double[] d = cs.get("d");
        int n = x.length;
        double y0 = Double.NaN;
        if (x0 < x[0]) {
            if (EXTRAPOLATION_METHOD_NATURE.equals(method)) {
                double h = x0 - x[0];
                y0 = a[0] * (Math.pow(h, 3)) + b[0] * (Math.pow(h, 2)) + c[0] * h + d[0];
            } else if (EXTRAPOLATION_METHOD_TANGENT.equals(method)) {
                double h = x0 - x[0];
                y0 = d[0] + c[0] * h;
            } else {
                y0 = d[0];
            }
            return y0;
        } else if (x0 > x[n - 1]) {
            if (EXTRAPOLATION_METHOD_NATURE.equals(method)) {
                double h = x0 - x[n - 2];
                y0 = a[n - 2] * (Math.pow(h, 3)) + b[n - 2] * (Math.pow(h, 2)) + c[n - 2] * h + d[n - 2];
            } else if (EXTRAPOLATION_METHOD_TANGENT.equals(method)) {
                double h = x0 - x[n - 1];
                double h1 = x[n - 1] - x[n - 2];
                y0 = ((3 * a[n - 2] * h1 + 2 * b[n - 2]) * h1 + c[n - 2]) * h + d[n - 1];
            } else {
                y0 = d[n - 1];
            }
            return y0;
        } else {
            for (int i = 0; i < n - 1; ++i) {
                if (x0 >= x[i] && x0 <= x[i + 1]) {
                    double h = x0 - x[i];
                    y0 = a[i] * (Math.pow(h, 3)) + b[i] * (Math.pow(h, 2)) + c[i] * h + d[i];
                    return y0;
                }
            }
            return y0;
        }
    }

    public static double getSplineSlope(HashMap<String, double[]> cs, double x0, String method) {
        double[] x = cs.get("x");
        double[] a = cs.get("a");
        double[] b = cs.get("b");
        double[] c = cs.get("c");
        int n = x.length;
        double y0 = Double.NaN;
        if (x0 < x[0]) {
            if (EXTRAPOLATION_METHOD_NATURE.equals(method)) {
                double h = x0 - x[0];
                y0 = 3 * a[0] * Math.pow(h, 2) + 2 * b[0] * h + c[0];
            } else if (EXTRAPOLATION_METHOD_TANGENT.equals(method)) {
                double h = x0 - x[0];
                y0 = c[0];
            } else {
                y0 = 0;
            }
            return y0;
        } else if (x0 > x[n - 1]) {
            if (EXTRAPOLATION_METHOD_NATURE.equals(method)) {
                double h = x0 - x[n - 2];
                y0 = 3 * a[n - 2] * Math.pow(h, 2) + 2 * b[n - 2] * h + c[n - 2];
            } else if (EXTRAPOLATION_METHOD_TANGENT.equals(method)) {
                double h1 = x[n - 1] - x[n - 2];
                y0 = (3 * a[n - 2] * h1 + 2 * b[n - 2]) * h1 + c[n - 2];
            } else {
                y0 = 0;
            }
            return y0;
        } else {
            for (int i = 0; i < n - 1; ++i) {
                if (x0 >= x[i] && x0 <= x[i + 1]) {
                    double h = x0 - x[i];
                    y0 = 3 * a[i] * (Math.pow(h, 2)) + 2 * b[i] * h + c[i];
                    return y0;
                }
            }
            return y0;
        }
    }

    public static double interp1(double[] x, double[] y, double x0, String csMethod, String exMethod) {
        return getSplinePoint(cubicSplineParameter(x, y, csMethod), x0, exMethod);
    }

    public static double interp1Slope(double[] x, double[] y, double x0, String csMethod, String exMethod) {
        return getSplineSlope(cubicSplineParameter(x, y, csMethod), x0, exMethod);
    }


    /**
     *
     * @param x n维数组
     * @param y m维数组
     * @param z m * n维数组(m行 n列)
     * @param x0 插值的x点
     * @param y0 插值的y点
     * @param csMethod 内插法
     * @param exMethod 外插法
     * @return 曲面z上插值的结果
     */
    public static double interp2(double[] x, double[] y, double[][] z, double x0, double y0,
                                 String csMethod, String exMethod) {
        //first ignore NaN and create a new surface;
        HashMap<String, double[][]> hm = ignoreNaN(x, y, z);

        double[] newX = hm.get("x")[0];
        double[] newY = hm.get("y")[0];
        double[][] newZ = hm.get("z");
        int m = newY.length;
        double[] yInterp = new double[m];
        for (int i = 0; i < m; i++) {
            //interpolation rows first;
            yInterp[i] = interp1(newX, newZ[i], x0, csMethod, exMethod);
        }
        return interp1(newY, yInterp, y0, csMethod, exMethod);
    }


}
