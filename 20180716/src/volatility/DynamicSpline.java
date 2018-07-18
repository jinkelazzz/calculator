package volatility;

import calculator.derivatives.SingleOptionAnalysisCalculator;
import calculator.utility.Interpolation;
import flanagan.math.ArrayMaths;
import flanagan.math.DeepCopy;
import option.BaseSingleOption;

import java.io.Serializable;
import java.util.*;

class SmoothSpline implements Serializable {
    private double[] xPoints;
    private double[] yPoints;
    private double[] weightPoints;
    private double lambda;

    public void setxPoints(double[] xPoints) {
        this.xPoints = xPoints;
    }

    public void setyPoints(double[] yPoints) {
        this.yPoints = yPoints;
    }

    public void setWeightPoints(double[] weightPoints) {
        this.weightPoints = weightPoints;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    private double[] enlargeVector(double[] vector) {
        double[] extendVec = new double[vector.length + 2];
        System.arraycopy(vector, 0, extendVec, 2, vector.length);
        return extendVec;
    }

    private void quincunx(double[] u, double[] v, double[] w, double[] q) {
        double[] u1 = enlargeVector(u);
        double[] v1 = enlargeVector(v);
        double[] w1 = enlargeVector(w);
        double[] q1 = enlargeVector(q);
        int startIndex = 3;
        int n = u1.length;
        for (int i = startIndex; i < n - 1; i++) {
            u1[i] = u1[i] - u1[i - 2] * (w1[i - 2] * w1[i - 2]) - u1[i - 1] * (v1[i - 1] * v1[i - 1]);
            v1[i] = (v1[i] - u1[i - 1] * v1[i - 1] * w1[i - 1]) / u1[i];
            w1[i] = w1[i] / u1[i];
        }
        for (int i = startIndex; i < n - 1; i++) {
            q1[i] = q1[i] - v1[i - 1] * q1[i - 1] - w1[i - 2] * q1[i - 2];
        }
        for (int i = startIndex; i < n - 1; i++) {
            q1[i] = q1[i] / u1[i];
        }
        q1[n - 1] = 0;
        for (int i = n - (startIndex - 1); i > startIndex - 1; i--) {
            if(i == n - 2) {
                q1[i] = q1[i] - v1[i] * q1[i + 1];
            } else {
                q1[i] = q1[i] - v1[i] * q1[i + 1] - w1[i] * q1[i + 2];
            }
        }
        System.arraycopy(u1, 2, u, 0, u.length);
        System.arraycopy(v1, 2, v, 0, v.length);
        System.arraycopy(w1, 2, w, 0, w.length);
        System.arraycopy(q1, 2, q, 0, q.length);
    }

    private HashMap<String, double[]> splineParameters() {
        int n = xPoints.length;
        double mu = 2 * (1 - lambda) / (3 * lambda);
        double[] h = new double[n];
        double[] r = new double[n];
        double[] f = new double[n];
        double[] p = new double[n];
        double[] q = new double[n];
        double[] u = new double[n];
        double[] v = new double[n];
        double[] w = new double[n];
        h[0] = xPoints[1] - xPoints[0];
        r[0] = 3 / h[0];
        for (int i = 1; i < n - 1; i++) {
            h[i] = xPoints[i + 1] - xPoints[i];
            r[i] = 3 / h[i];
            f[i] = -(r[i] + r[i - 1]);
            p[i] = 2 * (xPoints[i + 1] - xPoints[i - 1]);
            q[i] = 3 * (yPoints[i + 1] - yPoints[i]) / h[i] - 3 * (yPoints[i] - yPoints[i - 1]) / h[i - 1];
        }

        for (int i = 1; i < n - 1; i++) {
            u[i] = r[i - 1] * r[i - 1] * weightPoints[i - 1] + f[i] * f[i] * weightPoints[i] +
                    r[i] * r[i] * weightPoints[i + 1];
            u[i] = mu * u[i] + p[i];
            v[i] = r[i] * (f[i] * weightPoints[i] + f[i + 1] * weightPoints[i + 1]);
            v[i] = mu * v[i] + h[i];
            w[i] = mu * r[i] * r[i + 1] * weightPoints[i + 1];
        }
        quincunx(u, v, w, q);

        double[] a = new double[n];
        double[] b = new double[n];
        double[] c = new double[n];
        double[] d = new double[n];
        d[0] = yPoints[0] - mu * r[0] * q[1] * weightPoints[0];
        d[1] = yPoints[1] - mu * (f[1] * q[1] + r[1] * q[2]) * weightPoints[1];
        a[0] = q[1] / (3 * h[0]);
        b[0] = 0;
        c[0] = (d[1] - d[0]) / h[0] - q[1] * h[0] / 3;
        r[0] = 0;
        for (int i = 1; i < n - 1; i++) {
            a[i] = (q[i + 1] - q[i]) / (3 * h[i]);
            b[i] = q[i];
            c[i] = (q[i] + q[i - 1]) * h[i - 1] + c[i - 1];
            d[i] = r[i - 1] * q[i - 1] + f[i] * q[i] + r[i] * q[i + 1];
            d[i] = yPoints[i] - mu * d[i] * weightPoints[i];
        }
        HashMap<String, double[]> result = new HashMap<>(6);
        result.put("a", a);
        result.put("b", b);
        result.put("c", c);
        result.put("d", d);
        result.put("x", xPoints);
        result.put("y", yPoints);
        return result;
    }

    double getSplinePoint(double logMoneyness) {
        return Interpolation.getSplinePoint(splineParameters(), logMoneyness,
                Interpolation.INTERPOLATION_METHOD_NATURE);
    }

    double getSplineSlope(double logMoneyness) {
        return Interpolation.getSplineSlope(splineParameters(), logMoneyness,
                Interpolation.EXTRAPOLATION_METHOD_NATURE);
    }

}

/**
 * @reference ORC quant guide;
 * @author liangcy
 */
public class DynamicSpline extends BaseVolatilitySkew implements Serializable {

    /**
     * rigidity
     */
    private double mu;

    public double getMu() {
        return mu;
    }

    public void setMu(double mu) {
        this.mu = mu;
    }

    private double lambda() {
        return 1.0 / (1.5 * mu + 1);
    }

    private boolean isValidSplineLogMoneyness(BaseSingleOption option) {
        double logMoneyness = getLogMoneyness(option);
        return isOutOfMoney(option) && isAtMiddleRange(logMoneyness);
    }

    private List<BaseSingleOption> validOptionList() {
        List<BaseSingleOption> optionList = getOptionList();
        List<BaseSingleOption> copyOptionList = new ArrayList<>();
        for (BaseSingleOption option : optionList) {
            if(isValidSplineLogMoneyness(option)) {
                copyOptionList.add((BaseSingleOption) DeepCopy.copy(option));
            }
        }
        return copyOptionList;
    }


    /**
     * @return [0]: log moneyness, [1]: implied volatility, [2]: vega(weight)
     */
    private double[][] getSplinePoints() {
        List<BaseSingleOption> optionList = validOptionList();
        int n = optionList.size();
        SingleOptionAnalysisCalculator calculator = new SingleOptionAnalysisCalculator();
        double[] logMoneynessList = new double[n];
        double[] impliedVolatilityList = new double[n];
        double[] vegaList = new double[n];
        BaseSingleOption option;
        for (int i = 0; i < n; i++) {
            option = optionList.get(i);
            logMoneynessList[i] = getLogMoneyness(option);
            //这里要先算隐含波动率再算vega, 因为此时可能没有隐含波动率, 无法计算vega;
            calculator.setOption(option);
            calculator.calculateImpliedVolatility();
            impliedVolatilityList[i] = calculator.getResult();
            calculator.calculateVega();
            vegaList[i] = calculator.getResult();
        }
        return sort(logMoneynessList, impliedVolatilityList, vegaList);
    }

    private double[][] sort(double[] logMoneynessList, double[] volatilityList, double[] vegaList) {
        int n = logMoneynessList.length;
        ArrayMaths copyLogMoneynessList = new ArrayMaths(DeepCopy.copy(logMoneynessList));
        int[] indices = new int[n];
        Arrays.sort(logMoneynessList);
        for (int i = 0; i < n; i++) {
            indices[i] = copyLogMoneynessList.indexOf(logMoneynessList[i]);
        }
        ArrayMaths copyVolList = new ArrayMaths(volatilityList);
        copyVolList.sortEquals(indices);
        ArrayMaths copyVegaList = new ArrayMaths(vegaList);
        copyVegaList.sortEquals(indices);
        return new double[][] {logMoneynessList, copyVolList.array(), copyVegaList.array()};
    }

    private void updateVolatilitySkew(double refVol, double refSlope) {
        getVolatilitySkewParams().setRefVolatility(refVol);
        getVolatilitySkewParams().setRefSlope(refSlope);
    }

    private double[] getEps(double[] logMoneynessList, double[] volatilityList,
                            double currentVol, double currentSlope) {
        int n = logMoneynessList.length;
        double[] eps = new double[n];
        for (int i = 0; i < n; i++) {
            eps[i] = volatilityList[i] - currentVol + currentSlope * logMoneynessList[i];
        }
        return eps;
    }

    @Override
    double getVolatilityInMiddle(double logMoneyness) {
        return spline(logMoneyness)[0];
    }

    @Override
    double getSlopeInMiddle(double logMoneyness) {
        return spline(logMoneyness)[1];
    }

    private double[] spline(double logMoneyness) {
        int maxIter = 20;
        double[][] splinePoints = getSplinePoints();
        double[] logMoneynessList = splinePoints[0];
        double[] volatilityList = splinePoints[1];
        double[] vegaList = splinePoints[2];
        //初始化smooth spline;
        SmoothSpline smoothSpline = new SmoothSpline();
        smoothSpline.setxPoints(logMoneynessList);
        smoothSpline.setWeightPoints(vegaList);
        smoothSpline.setLambda(lambda());
        //初始化current volatility和current slope;
        updateVolatilitySkew(0, 0);
        double currentVolatility = Interpolation.interp1(logMoneynessList, volatilityList, 0,
                Interpolation.INTERPOLATION_METHOD_NATURE, Interpolation.EXTRAPOLATION_METHOD_NATURE) -
                getCurrentVolatility();
        double currentSlope = Interpolation.interp1Slope(logMoneynessList, volatilityList, 0,
                Interpolation.INTERPOLATION_METHOD_NATURE, Interpolation.EXTRAPOLATION_METHOD_NATURE) -
                getCurrentSlope();
        double[] eps = getEps(logMoneynessList, volatilityList, currentVolatility, currentSlope);
        //迭代20次;
        for (int i = 0; i < maxIter; i++) {
            smoothSpline.setyPoints(eps);
            updateVolatilitySkew(currentVolatility, currentSlope);
            currentVolatility = getCurrentVolatility() + smoothSpline.getSplinePoint(0);
            currentSlope = getCurrentSlope() + smoothSpline.getSplineSlope(0);
            eps = getEps(logMoneynessList, volatilityList, currentVolatility, currentSlope);
        }

        double volatility = currentVolatility + currentSlope * logMoneyness + smoothSpline.getSplinePoint(logMoneyness);
        double slope = currentSlope + smoothSpline.getSplineSlope(logMoneyness);
        return new double[] {volatility, slope};
    }
}
