package calculator.derivatives;

import calculator.utility.CalculateUtil;
import calculator.utility.CalculatorError;
import calculator.utility.MonteCarlo;
import flanagan.analysis.Stat;
import option.BaseSingleOption;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


import static calculator.utility.CalculatorError.*;

/**
 * @author liangcy
 * 蒙特卡洛模拟可能会消耗大量时间和内存
 */
public class SingleOptionMonteCarloCalculator extends BaseSingleOptionCalculator {
    private MonteCarlo monteCarloParams = new MonteCarlo();
    private static int nums = 50;

    public MonteCarlo getMonteCarloParams() {
        return monteCarloParams;
    }

    public void setMonteCarloParams(MonteCarlo monteCarloParams) {
        this.monteCarloParams = monteCarloParams;
    }

    @Override
    public boolean hasMethod() {
        return option.hasMonteCarloMethod();
    }

    private MonteCarlo subMonteCarloParams() {
        return new MonteCarlo(monteCarloParams.getNodes(), monteCarloParams.getPathSize() / nums);
    }

    private List<Future<Double>> createTask(Callable<Double> call) {
        ExecutorService pool = CalculateUtil.createThreadPool(nums);
        List<Future<Double>> futureList = new ArrayList<>(nums);
        for (int i = 0; i < nums; i++) {
            futureList.add(pool.submit(call));
        }
        return futureList;
    }

    @Override
    public void calculatePrice() {
        resetCalculator();
        if (!option.hasMonteCarloMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }

        Callable<Double> callPricePathList = () -> {
            calculatePrice(subMonteCarloParams().generateStandardNormalRandomNumberList());
            return getResult();
        };
        List<Future<Double>> futureList = createTask(callPricePathList);

        double[] price = new double[nums];
        try {
            for (int i = 0; i < nums; i++) {
                price[i] = futureList.get(i).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            setError(CalculatorError.CALCULATE_FAILED);
        }
        setResult(Stat.mean(price));
    }

    private void calculatePrice(List<double[]> randomNumbersList) {
        List<double[]> pricePathList = monteCarloParams.generateMonteCarloPathList(option, randomNumbersList);
        int n = pricePathList.size();
        double[] resultList = new double[n];
        for (int i = 0; i < n; i++) {
            resultList[i] = option.monteCarloPrice(pricePathList.get(i));
        }
        setResult(Stat.mean(resultList));
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

}
