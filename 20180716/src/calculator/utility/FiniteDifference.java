package calculator.utility;

import flanagan.math.Matrix;
import option.BaseSingleOption;

import java.io.Serializable;

/**
 * @author liangcy
 */
public class FiniteDifference implements Serializable {
    private int numOfTimePoints = 501;
    private int numOfLowerPricePoints = 100;
    private double[] pricePoints;
    private double[] timePoints;
    private double diffPrice;
    private double diffTime;
    private boolean hasGeneratedPoints = false;


    public void setNumOfTimePoints(int numOfTimePoints) {
        this.numOfTimePoints = numOfTimePoints;
        this.hasGeneratedPoints = false;
    }

    public int getNumOfTimePoints() {
        return numOfTimePoints;
    }

    public int getNumOfLowerPricePoints() {
        return numOfLowerPricePoints;
    }

    public void setNumOfLowerPricePoints(int numOfLowerPricePoints) {
        this.numOfLowerPricePoints = numOfLowerPricePoints;
        this.hasGeneratedPoints = false;
    }

    public double[] getPricePoints() {
        return pricePoints;
    }

    public double[] getTimePoints() {
        return timePoints;
    }

    public double getDiffPrice() {
        return diffPrice;
    }

    public double getDiffTime() {
        return diffTime;
    }


    /**
     * @param option
     */
    private void generatePricePoints(BaseSingleOption option) {
        double s = option.getUnderlying().getSpotPrice();
        int priceRange = 5;
        //[0, s] ∪ [s, 5s]; 之所以不对称是为了计算看涨期权, 因为标的资产价格越高, 看涨期权越贵, 越影响期权价格;
        this.diffPrice = s / numOfLowerPricePoints;
        this.pricePoints = new double[numOfLowerPricePoints * priceRange + 1];
        for (int i = 0; i < pricePoints.length; i++) {
            pricePoints[i] = s - diffPrice * (numOfLowerPricePoints - i);
        }
    }

    /**
     * 生成时间点, 从0到T;
     *
     * @param option
     */
    private void generateTimePoints(BaseSingleOption option) {
        double t = option.getVanillaOptionParams().getTimeRemaining();
        this.diffTime = t / (numOfTimePoints - 1);
        this.timePoints = new double[numOfTimePoints];
        for (int i = 0; i < numOfTimePoints; i++) {
            this.timePoints[i] = i * this.diffTime;
        }
    }

    public void generateFiniteDifferencePoints(BaseSingleOption option) {
        generateTimePoints(option);
        generatePricePoints(option);
        this.hasGeneratedPoints = true;
    }

    /**
     * @return spot的下标
     */
    public int getIndexOfInitialSpot() {
        return numOfLowerPricePoints;
    }

    /**
     * @param option BaseSingleOption
     * @return 差分系数矩阵, 该矩阵求逆计算速度较慢;
     * 是一个三对角矩阵,形如[[b0,c0,0,...,0],[a1,b1,c1,...,0],...,[0,...,0,an,bn]];
     */
    public Matrix paramsMatrix(BaseSingleOption option) {
        double[][] params = paramsArray(option);
        double[] a = params[0];
        double[] b = params[1];
        double[] c = params[2];
        int n = a.length;
        double[][] matrix = new double[n][n];
        //主对角系数b
        for (int j = 0; j < n; j++) {
            matrix[j][j] = b[j];
        }
        //上对角系数c
        for (int j = 0; j < n - 1; j++) {
            matrix[j][j + 1] = c[j];
        }
        //下对角系数a
        for (int j = 1; j < n; j++) {
            matrix[j][j - 1] = a[j];
        }
        return new Matrix(matrix);
    }

    private double[][] paramsArray(BaseSingleOption option) {
        if (!hasGeneratedPoints) {
            generateFiniteDifferencePoints(option);
        }
        double vol = option.getVanillaOptionParams().getVolatility();
        double mu = option.getUnderlying().getCostOfCarry();
        double r = option.getUnderlying().getRiskFreeRate();
        int n = pricePoints.length;
        double[] b = new double[n];
        for (int j = 0; j < n; j++) {
            b[j] = 1 + Math.pow(vol * j, 2) * diffTime + r * diffTime;
        }
        double[] a = new double[n];
        for (int j = 0; j < n; j++) {
            a[j] = mu * j * diffTime / 2 - Math.pow(vol * j, 2) * diffTime / 2;
        }
        double[] c = new double[n];
        for (int j = 0; j < n; j++) {
            c[j] = -mu * j * diffTime / 2 - Math.pow(vol * j, 2) * diffTime / 2;
        }
        return new double[][]{a, b, c};
    }

}
