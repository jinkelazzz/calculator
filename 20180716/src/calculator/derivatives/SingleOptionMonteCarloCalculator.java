package calculator.derivatives;

import calculator.utility.MonteCarlo;
import flanagan.analysis.Stat;
import option.BaseSingleOption;

import java.util.List;
import static calculator.utility.CalculatorError.*;

/**
 * @author liangcy
 * 蒙特卡洛模拟可能会消耗大量时间和内存
 */
public class SingleOptionMonteCarloCalculator extends BaseSingleOptionCalculator {

    private MonteCarlo monteCarloParams = new MonteCarlo();
    private double monteCarloError = 0.0;

    public MonteCarlo getMonteCarloParams() {
        return monteCarloParams;
    }

    public void setMonteCarloParams(MonteCarlo monteCarloParams) {
        this.monteCarloParams = monteCarloParams;
    }

    public double getMonteCarloError() {
        return monteCarloError;
    }

    private void setMonteCarloError(double monteCarloError) {
        this.monteCarloError = monteCarloError;
    }

    @Override
    public boolean hasMethod() {
        return option.hasMonteCarloMethod();
    }

    @Override
    public void calculatePrice() {
        resetCalculator();
        if (!option.hasMonteCarloMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        List<double[]> randomNumbersList = monteCarloParams.generateStandardNormalRandomNumberList();
        calculatePrice(randomNumbersList);
    }

    private void calculatePrice(List<double[]> randomNumbersList) {
        List<double[]> pricePathList = monteCarloParams.generateMonteCarloPathList(option, randomNumbersList);
        int n = pricePathList.size();
        double[] resultList = new double[n];
        for (int i = 0; i < n; i++) {
            resultList[i] = option.monteCarloPrice(pricePathList.get(i));
        }
        setResult(Stat.mean(resultList));
        double sd = Stat.standardError(resultList);
        setMonteCarloError(sd * monteCarloParams.getMonteCarloErrorMult() /
                Math.sqrt(n));
        setError(NORMAL);
    }

    @Override
    public void calculateImpliedVolatility() {
        resetCalculator();
        setError(UNSUPPORTED_METHOD);
    }

    @Override
    public void calculateDelta() {
        resetCalculator();
        List<double[]> randomNumbersList = monteCarloParams.generateStandardNormalRandomNumberList();
        calculateDelta(randomNumbersList);
    }

    private void calculateDelta(List<double[]> randomNumbersList) {
        resetCalculator();
        BaseSingleOption[] options = shiftUnderlyingPrice();
        BaseSingleOption lowerOption = options[0];
        BaseSingleOption upperOption = options[1];

        setOption(lowerOption);
        calculatePrice(randomNumbersList);
        if (!isNormal()) {
            return;
        }
        double lowerPrice = getResult();

        setOption(upperOption);
        calculatePrice(randomNumbersList);
        if (!isNormal()) {
            return;
        }
        double upperPrice = getResult();
        //reset;
        setOption(option);
        double lowerSpotPrice = lowerOption.getUnderlying().getSpotPrice();
        double upperSpotPrice = upperOption.getUnderlying().getSpotPrice();
        double delta = (upperPrice - lowerPrice) / (upperSpotPrice - lowerSpotPrice);
        setResult(delta);
        setError(NORMAL);
    }

    @Override
    public void calculateVega() {

    }

    @Override
    public void calculateTheta() {

    }

    @Override
    public void calculateGamma() {


    }

    @Override
    public void calculateRho() {

    }

    @Override
    public void resetCalculator() {
        super.resetCalculator();
        setMonteCarloError(0.0);
    }
}
