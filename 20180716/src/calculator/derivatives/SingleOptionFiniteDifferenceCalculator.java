package calculator.derivatives;

import calculator.utility.CalculateUtil;
import calculator.utility.CalculatorError;
import calculator.utility.FiniteDifference;
import flanagan.math.Matrix;
import option.EuropeanOption;
import java.io.Serializable;


/**
 * @author liangcy
 */
public class SingleOptionFiniteDifferenceCalculator extends BaseSingleOptionCalculator implements Serializable {

    private FiniteDifference finiteDifference = new FiniteDifference();

    @Override
    public boolean hasMethod() {
        return option.hasFiniteDifferenceMethod();
    }

    @Override
    public void calculatePrice() {
        resetCalculator();
        if (!option.hasFiniteDifferenceMethod()) {
            setError(CalculatorError.UNSUPPORTED_METHOD);
            return;
        }
        double result = optionPriceMatrix()[0][finiteDifference.getIndexOfInitialSpot()];
        setResult(result);
        setError(CalculatorError.NORMAL);
    }

    /**
     * @return n * m matrix;第一行是时刻为0时期权价格向量, 最后一行是时刻为t时期权价格向量;
     * 期权价格向量根据标的资产价格从小到大排列;
     */
    private double[][] optionPriceMatrix() {
        finiteDifference.generateFiniteDifferencePoints(option);
        int n = finiteDifference.getNumOfTimePoints();
        double[][] result = new double[n][];
        //记录行权时的期权价格;
        double[] exercisePrice = option.finiteDifferencePrice(finiteDifference.getPricePoints());
        //初始化矩阵最后一行;
        result[n - 1] = exercisePrice;
        //要把option price转成列矩阵;
        Matrix optionPriceMat;
        //对矩阵系数直接求逆;
        Matrix params = finiteDifference.paramsMatrix(option).inverse();
        //递推求解option price矩阵;
        for (int i = 1; i < n; i++) {
            optionPriceMat = Matrix.columnMatrix(result[n - i]);
            result[n - i - 1] = params.times(optionPriceMat).getColumnCopy(0);
            if (option.isEarlyExercise()) {
                result[n - i - 1] = CalculateUtil.maxVector(result[n - i - 1], exercisePrice);
            }
        }
        return result;
    }

    @Override
    public void calculateImpliedVolatility() {
        resetCalculator();
        setError(CalculatorError.UNSUPPORTED_METHOD);
    }

    @Override
    public void calculateDelta() {
        resetCalculator();
        if (!option.hasFiniteDifferenceMethod()) {
            setError(CalculatorError.UNSUPPORTED_METHOD);
            return;
        }
        int upperIndex = finiteDifference.getIndexOfInitialSpot() + 1;
        int lowerIndex = finiteDifference.getIndexOfInitialSpot() - 1;
        double[] optionPriceList = optionPriceMatrix()[0];
        double[] underlyingPriceList = finiteDifference.getPricePoints();

        double delta = (optionPriceList[upperIndex] - optionPriceList[lowerIndex]) /
                (underlyingPriceList[upperIndex] - underlyingPriceList[lowerIndex]);
        setResult(delta);
        setError(CalculatorError.NORMAL);
    }

    @Override
    public void calculateTheta() {
        resetCalculator();
        if (!option.hasFiniteDifferenceMethod()) {
            setError(CalculatorError.UNSUPPORTED_METHOD);
            return;
        }
        double priceT0 = optionPriceMatrix()[0][finiteDifference.getIndexOfInitialSpot()];
        double priceT1 = optionPriceMatrix()[1][finiteDifference.getIndexOfInitialSpot()];

        double theta = (priceT1 - priceT0) / finiteDifference.getDiffTime() / 365;
        setResult(theta);
        setError(CalculatorError.NORMAL);
    }

    @Override
    public void calculateGamma() {
        resetCalculator();
        if (!option.hasFiniteDifferenceMethod()) {
            setError(CalculatorError.UNSUPPORTED_METHOD);
            return;
        }
        int upperIndex1 = finiteDifference.getIndexOfInitialSpot() + 1;
        int upperIndex2 = finiteDifference.getIndexOfInitialSpot() + 2;
        int lowerIndex1 = finiteDifference.getIndexOfInitialSpot() - 1;
        int lowerIndex2 = finiteDifference.getIndexOfInitialSpot() - 2;
        int index = finiteDifference.getIndexOfInitialSpot();
        double[] optionPriceList = optionPriceMatrix()[0];
        double[] underlyingPriceList = finiteDifference.getPricePoints();

        double upperDelta = (optionPriceList[upperIndex2] - optionPriceList[index]) /
                (underlyingPriceList[upperIndex2] - underlyingPriceList[index]);

        double lowerDelta = (optionPriceList[index] - optionPriceList[lowerIndex2]) /
                (underlyingPriceList[index] - underlyingPriceList[lowerIndex2]);

        double gamma = (upperDelta - lowerDelta) /
                (underlyingPriceList[upperIndex1] - underlyingPriceList[lowerIndex1]);

        setResult(gamma);
        setError(CalculatorError.NORMAL);
    }

    /**
     * @return 普通欧式期权解析解和数值解的误差
     */
    public double getEuropeanOptionError() {
        EuropeanOption europeanOption = new EuropeanOption(option);
        SingleOptionFiniteDifferenceCalculator calculator = new SingleOptionFiniteDifferenceCalculator();
        calculator.setOption(europeanOption);
        calculator.calculatePrice();
        return europeanOption.bsm() - calculator.getResult();
    }
}
