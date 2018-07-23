package calculator.derivatives;

import calculator.utility.CalculateUtil;
import calculator.utility.CalculatorError;
import calculator.utility.MonteCarlo;
import flanagan.analysis.Stat;
import option.BaseSingleOption;

import java.io.Serializable;
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
 * 利用多线程提高计算速度
 */
public class SingleOptionMonteCarloCalculator extends BaseSingleOptionCalculator implements Serializable {
    private MonteCarlo monteCarloParams = new MonteCarlo();
    /**
     * 线程池线程数
     */
    private static final int THREAD_NUMS = 50;

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
        return new MonteCarlo(monteCarloParams.getNodes(), monteCarloParams.getPathSize() / THREAD_NUMS);
    }

    private List<Future<Double>> createTask(Callable<Double> call) {
        ExecutorService pool = CalculateUtil.createThreadPool(THREAD_NUMS);
        List<Future<Double>> futureList = new ArrayList<>(THREAD_NUMS);
        for (int i = 0; i < THREAD_NUMS; i++) {
            futureList.add(pool.submit(call));
        }
        return futureList;
    }

    private double getAverage(List<Future<Double>> futureList) throws ExecutionException, InterruptedException {
        int n = futureList.size();
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = futureList.get(i).get();
        }
        return Stat.mean(result);
    }

    private void calculateAverage(List<Future<Double>> futureList) {
        try {
            setResult(getAverage(futureList));
            setError(CalculatorError.NORMAL);
        } catch (InterruptedException | ExecutionException e) {
            setError(CalculatorError.CALCULATE_FAILED);
        }
    }

    @Override
    public void calculatePrice() {
        resetCalculator();
        if (!option.hasMonteCarloMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        calculateAverage(createTask(callSubPrice()));
    }

    private Callable<Double> callSubPrice() {
        return () -> calculatePrice(subMonteCarloParams().generateStandardNormalRandomNumberList());
    }

    private double calculatePrice(List<double[]> randomNumbersList) {
        List<double[]> pricePathList = monteCarloParams.generateMonteCarloPathList(option, randomNumbersList);
        int n = pricePathList.size();
        double[] resultList = new double[n];
        for (int i = 0; i < n; i++) {
            resultList[i] = option.monteCarloPrice(pricePathList.get(i));
        }
        return Stat.mean(resultList);
    }

    @Override
    public void calculateImpliedVolatility() {
        resetCalculator();
        setError(UNSUPPORTED_METHOD);
    }

    @Override
    public void calculateDelta() {
        resetCalculator();
        if (!option.hasMonteCarloMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        calculateAverage(createTask(callSubDelta()));
    }

    private Callable<Double> callSubDelta() {
        return () -> calculateDelta(subMonteCarloParams().generateStandardNormalRandomNumberList());
    }

    private double calculateDelta(List<double[]> randomNumbersList) {
        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftUnderlyingPrice(canUseVolatilitySurface());
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions(), randomNumbersList);
        return diffPrice / shiftSingleOption.getDenominator();
    }

    @Override
    public void calculateVega() {
        resetCalculator();
        if (!option.hasMonteCarloMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        calculateAverage(createTask(callSubVega()));
    }

    private Callable<Double> callSubVega() {
        return () -> calculateVega(subMonteCarloParams().generateStandardNormalRandomNumberList());
    }

    private double calculateVega(List<double[]> randomNumbersList) {
        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftVolatility();
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions(), randomNumbersList);
        return diffPrice / shiftSingleOption.getDenominator() / 100;
    }

    @Override
    public void calculateTheta() {
        resetCalculator();
        if (!option.hasMonteCarloMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        calculateAverage(createTask(callSubTheta()));
    }

    private Callable<Double> callSubTheta() {
        return () -> calculateTheta(subMonteCarloParams().generateStandardNormalRandomNumberList());
    }

    private double calculateTheta(List<double[]> randomNumbersList) {
        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftTimeRemaining(canUseVolatilitySurface());
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions(), randomNumbersList);
        return -diffPrice / shiftSingleOption.getDenominator() / 365;
    }

    @Override
    public void calculateGamma() {
        resetCalculator();
        if (!option.hasMonteCarloMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        calculateAverage(createTask(callSubGamma()));
    }

    private Callable<Double> callSubGamma() {
        return () -> calculateGamma(subMonteCarloParams().generateStandardNormalRandomNumberList());
    }

    private double calculateGamma(List<double[]> randomNumbersList) {
        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftUnderlyingPrice(canUseVolatilitySurface());
        double diffDelta = getDiffDelta(shiftSingleOption.getOptions(), randomNumbersList);
        return diffDelta / shiftSingleOption.getDenominator();
    }

    @Override
    public void calculateRho() {
        resetCalculator();
        if (!option.hasMonteCarloMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        calculateAverage(createTask(callSubRho()));
    }

    private Callable<Double> callSubRho() {
        return () -> calculateRho(subMonteCarloParams().generateStandardNormalRandomNumberList());
    }

    private double calculateRho(List<double[]> randomNumbersList) {
        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftInterestRate();
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions(), randomNumbersList);
        return diffPrice / shiftSingleOption.getDenominator() / 10000;
    }

    @Override
    public void calculateRho2() {
        resetCalculator();
        if (!option.hasMonteCarloMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        calculateAverage(createTask(callSubRho2()));
    }

    private Callable<Double> callSubRho2() {
        return () -> calculateRho2(subMonteCarloParams().generateStandardNormalRandomNumberList());
    }

    private double calculateRho2(List<double[]> randomNumbersList) {
        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftDividendRate();
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions(), randomNumbersList);
        return diffPrice / shiftSingleOption.getDenominator() / 10000;
    }

    private double getDiffPrice(BaseSingleOption[] options, List<double[]> randomNumbersList) {
        setOption(options[0]);
        double lowerPrice = calculatePrice(randomNumbersList);
        setOption(options[1]);
        double upperPrice = calculatePrice(randomNumbersList);
        setOption(option);
        return upperPrice - lowerPrice;
    }

    private double getDiffDelta(BaseSingleOption[] options, List<double[]> randomNumbersList) {
        setOption(options[0]);
        double lowerDelta = calculateDelta(randomNumbersList);
        setOption(options[1]);
        double upperDelta = calculateDelta(randomNumbersList);
        setOption(option);
        return upperDelta - lowerDelta;
    }

}
