package calculator.utility;

import flanagan.analysis.Stat;
import flanagan.math.Matrix;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.*;

/**
 * @author liangcy
 */

public class CalculateUtil {

    private CalculateUtil() {}

    public static double normalRandom() {
        return ThreadLocalRandom.current().nextGaussian();
    }

    /**
     * 计算正态分布累计密度函数的子函数
     * @param a 分子向量, 比分母向量长1;
     * @param b 分母向量;
     * @param x 乘数;
     * @return (((an * x + a0) * x + a1)...) / ((b0 * x + b1)...)
     */
    private static double rollMultiplyDivide(double[] a, double[] b, double x) {
        int n = b.length;
        double numerator = x * a[n];
        double denominator = x;
        for (int i = 0; i < n; i++) {
            numerator = (numerator + a[i]) * x;
            denominator = (denominator + b[i]) * x;
        }
        return numerator / denominator;
    }

    /**
     * 计算正态分布累计密度函数的子函数
     * @param x
     * @return
     */
    private static double delMult(double x) {
        double xsq = fint(16.0 * x) / 16.0;
        double del = (x - xsq) * (x + xsq);
        return Math.exp(-0.5 * xsq * xsq) * Math.exp(-0.5 * del);
    }

    /**
     * @param x 积分上界;
     * @return 正态分布累计密度函数, 从-∞积分到x;
     * @reference :
     * Cody, W. D. (1993).
     * ALGORITHM 715: SPECFUN - A Portable FORTRAN Package of
     * Special Function Routines and Test Drivers".
     * ACM Transactions on Mathematical Software. 19, 22-32.
     */
    public static double normalCDF(double x) {
        if(Double.isNaN(x)) {
            return Double.NaN;
        }

        if(!Double.isFinite(x)) {
            return (Math.signum(x) + 1) / 2;
        }

        final double[] a = {2.2352520354606839287, 161.02823106855587881, 1067.6894854603709582, 18154.981253343561249,
                0.065682337918207449113};
        final double[] b = {47.20258190468824187, 976.09855173777669322, 10260.932208618978205,
                45507.789335026729956};
        final double[] c = {0.39894151208813466764, 8.8831497943883759412, 93.506656132177855979,
                597.27027639480026226, 2494.5375852903726711, 6848.1904505362823326, 11602.651437647350124,
                9842.7148383839780218, 1.0765576773720192317e-8};
        final double[] d = {22.266688044328115691, 235.38790178262499861, 1519.377599407554805, 6485.558298266760755,
                18615.571640885098091, 34900.952721145977266, 38912.003286093271411, 19685.429676859990727};
        final double[] p = {0.21589853405795699, 0.1274011611602473639, 0.022235277870649807, 0.001421619193227893466,
                2.9112874951168792e-5, 0.02307344176494017303};
        final double[] q = {1.28426009614491121, 0.468238212480865118, 0.0659881378689285515, 0.00378239633202758244,
                7.29751555083966205e-5};
        //N^-1(0.75)
        double threshold = 0.67448975019608171;
        double root32 = Math.sqrt(32.0);
        double sqrtTwoPi = 1.0 / ConstantNumber.SQRT_TWO_PI;
        double min = Double.MIN_NORMAL;
        double y = Math.abs(x);
        double result;
        double xsq;
        if (y <= threshold) {
            if (y > ConstantNumber.EPS) {
                xsq = x * x;
                result = x * rollMultiplyDivide(a, b, xsq) + 0.5;
            } else {
                result = 0.5;
            }
            return result;
        } else if (y <= root32) {
            // evaluate for N^-1(0.75) <= |x| <= sqrt(32)
            result = delMult(y) * rollMultiplyDivide(c, d, y);
            if (x > 0.0) {
                result = 1 - result;
            }
            return result;
        } else {
            xsq = (1 / x) * (1 / x);
            result = xsq * rollMultiplyDivide(p, q, xsq);
            result = (sqrtTwoPi - result) / y;
            result = delMult(x) * result;
            if (x > 0.0) {
                result = 1 - result;
            }
            if (result < min) {
                result = 0.0;
            }
            return result;
        }
    }

    /**
     * @param x 实数
     * @return 向"0"取整 e.g: fint(2.6) = 2, fint(-2.6) = -2;
     */
    private static double fint(double x) {
        return (x >= 0.0) ? Math.floor(x) : -Math.floor(-x);
    }

    public static double normalPDF(double x) {
        return 1.0 / ConstantNumber.SQRT_TWO_PI * Math.exp(-x * x / 2);
    }

    /**
     * @param x   积分上界
     * @param y   积分上界
     * @param rho 相关系数
     * @return 二元正态分布累计密度函数值
     * @reference Alan Genz
     */
    public static double binormalCDF(double x, double y, double rho) {
        //Gauss Legendre Points and Weights, n = 3, 6, 10;
        final double[] p0 = {-0.9324695142031522, -0.6612093864662647, -0.2386191860831970};
        final double[] p1 = {-0.9815606342467191, -0.9041172563704750, -0.7699026741943050, -0.5873179542866171,
                -0.3678314989981802, -0.1252334085114692};
        final double[] p2 = {-0.9931285991850949, -0.9639719272779138, -0.9122344282513259, -0.8391169718222188,
                -0.7463319064601508, -0.6360536807265150, -0.5108670019508271, -0.3737060887154196,
                -0.2277858511416451, -0.07652652113349733};

        final double[] w0 = {0.1713244923791705, 0.3607615730481384, 0.4679139345726904};
        final double[] w1 = {0.04717533638651177, 0.1069393259953183, 0.1600783285433464, 0.2031674267230659,
                0.2334925365383547, 0.2491470458134029};
        final double[] w2 = {0.01761400713915212, 0.04060142980038694, 0.06267204833410906, 0.08327674157670475,
                0.1019301198172404, 0.1181945319615184, 0.1316886384491766, 0.1420961093183821,
                0.1491729864726037, 0.1527533871307259};

        double absR = Math.abs(rho);
        double threshold1 = 0.3;
        double threshold2 = 0.75;
        double threshold3 = 0.925;
        double[] p;
        double[] w;
        if (absR < threshold1) {
            p = p0;
            w = w0;
        } else if (absR < threshold2) {
            p = p1;
            w = w1;
        } else {
            p = p2;
            w = w2;
        }
        int n = p.length;

        double h = -x;
        double k = -y;
        double hk = h * k;
        double bvn = 0.0;
        double asr;

        if (absR < threshold3) {
            double hs = (h * h + k * k) / 2;
            double sn;
            asr = Math.asin(rho);
            for (int i = 0; i < n; ++i) {
                sn = Math.sin(asr * (p[i] + 1) / 2);
                bvn = bvn + w[i] * Math.exp((sn * hk - hs) / (1 - sn * sn));
                sn = Math.sin(asr * (-p[i] + 1) / 2);
                bvn = bvn + w[i] * Math.exp((sn * hk - hs) / (1 - sn * sn));
            }
            bvn = bvn * asr / (4 * Math.PI) + normalCDF(-h) * normalCDF(-k);
        } else {
            if (rho < 0) {
                k = -k;
                hk = -hk;
            }
            if (absR < 1) {
                double threshold = -160;
                double ass = (1 - rho) * (1 + rho);
                double a = Math.sqrt(ass);
                double bs = (h - k) * (h - k);
                double c = (4 - hk) / 8;
                double d = (12 - hk) / 16;
                asr = -(bs / ass + hk) / 2;
                bvn = a * Math.exp(asr) * (1 - c * (bs - ass) * (1 - d * bs / 5) / 3
                        + c * d * ass * ass / 5);
                if (hk > threshold) {
                    double b = Math.sqrt(bs);
                    bvn = bvn - Math.exp(-hk / 2) * ConstantNumber.SQRT_TWO_PI * normalCDF(-b / a) * b
                            * (1 - c * bs * (1 - d * bs / 5) / 3);
                }
                a = a / 2;
                double xs;
                double rs;
                for (int i = 0; i < n; ++i) {
                    xs = (a * (p[i] + 1)) * (a * (p[i] + 1));
                    rs = Math.sqrt(1 - xs);
                    asr = -(bs / ass + hk) / 2;
                    bvn = bvn + a * w[i] * (Math.exp(-bs / (2 * xs) - hk / (1 + rs)) / rs
                            - Math.exp(asr) * (1 + c * xs * (1 + d * xs)));
                    xs = ass * (-p[i] + 1) * (-p[i] + 1) / 4;
                    rs = Math.sqrt(1 - xs);
                    asr = -(bs / ass + hk) / 2;
                    bvn = bvn + a * w[i] * Math.exp(asr)
                            * (Math.exp(-hk * xs / (2 * Math.pow((1 + rs), 2))) / rs
                            - (1 + c * xs * (1 + d * xs)));
                }
                bvn = -bvn / (2 * Math.PI);
            }
            if (rho > 0) {
                bvn = bvn + normalCDF(-Math.max(h, k));
            } else {
                bvn = -bvn;
                if (k > h) {
                    if (h < 0) {
                        bvn = bvn + normalCDF(k) - normalCDF(h);
                    } else {
                        bvn = bvn + normalCDF(-h) - normalCDF(-k);
                    }
                }
            }
        }
        return bvn;
    }

    /**
     *
     * @param x 二维数组
     * @return 二维数组的转置
     */
    public static double[][] transpose(double[][] x) {
        Matrix y = new Matrix(x);
        return y.transpose().getArrayCopy();
    }

    /**
     * @param value 被差分的值
     * @param diff  宽度,正数
     * @return 对value进行相对宽度为diff的中间差分。
     */
    public static double[] midDiffValue(double value, double diff) {
        double[] result = new double[2];
        double halfDiff = diff / 2;

        if (value == 0) {
            result[0] = -halfDiff;
            result[1] = halfDiff;
        } else {
            result[0] = value * (1 - halfDiff);
            result[1] = value * (1 + halfDiff);
        }

        Arrays.sort(result);
        return result;
    }

    /**
     * @param value 被差分的值
     * @param diff  宽度,正数
     * @return 对value进行相对宽度为diff的向后差分。
     */
    public static double[] backwardDiffValue(double value, double diff) {
        double[] result = new double[2];
        if (value == 0) {
            result[0] = -diff;
        } else {
            result[0] = value * (1 - diff);
        }
        result[1] = value;
        Arrays.sort(result);
        return result;
    }

    /**
     * e.g: a = [1, 2, 3], b = [3, 2, 1] -> return [3, 2, 3]
     * @param a 数组
     * @param b 数组
     * @return a, b中最大值组成的新数组
     */
    public static double[] maxVector(double[] a, double[] b) {
        int n = a.length;
        double[] z = new double[n];
        for (int i = 0; i < n; i++) {
            z[i] = Math.max(a[i], b[i]);
        }
        return z;
    }

    /**
     *
     * @param x 一维数组
     * @param q
     * @return 一维数组的q分位数
     */
    public static double quantile(double[] x, double q) {
        int n = x.length;
        Arrays.sort(x);
        if(q <= 0) {
            return x[0];
        }
        if(q >= 1) {
            return x[n - 1];
        }
        double index = q * (n - 1);
        if(index == (int) index) {
            return x[(int) index];
        }
        int u = (int) Math.ceil(index);
        int d = (int) Math.floor(index);
        double weight = u - index;
        return weight * x[d] + (1 - weight) * x[u];
    }

    /**
     *
     * @param threadNums 线程个数
     * @return 线程池
     */
    public static ExecutorService createThreadPool(int threadNums) {
        ThreadFactory factory = Executors.defaultThreadFactory();
        return new ThreadPoolExecutor(threadNums, 2 * threadNums, 0, TimeUnit.NANOSECONDS,
                new LinkedBlockingDeque<>(1024), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     *
     * @param startMillionSec 起始时间
     * @return 总用时（以毫秒计）
     */
    public static double getPastMillionSec(long startMillionSec) {
        return System.currentTimeMillis() - startMillionSec;
    }

    /**
     *
     * @param priceList 价格
     * @return 对数收益率
     */
    public static double[] getLogReturn(double[] priceList) {
        int n = priceList.length;
        if(n <= 1) {
            return new double[] {0};
        }
        double[] logReturn = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            logReturn[i] = Math.log(priceList[i + 1] / priceList[i]);
        }
        return logReturn;
    }

    /**
     *
     * @param xPriceList
     * @param yPriceList
     * @return 对数收益相关系数
     */
    public static double getLogReturnCorrelation(double[] xPriceList, double[] yPriceList) {
        double[] xLogReturn = getLogReturn(xPriceList);
        double[] yLogReturn = getLogReturn(yPriceList);
        return Stat.corrCoeff(xLogReturn, yLogReturn);
    }

}
