package test;

import calculator.derivatives.SingleOptionAnalysisCalculator;
import calculator.derivatives.SingleOptionMonteCarloCalculator;
import calculator.utility.CalculateUtil;
import calculator.utility.MonteCarlo;
import option.EuropeanOption;
import option.VanillaOptionParams;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.ws.RequestWrapper;
import java.util.*;
import java.util.concurrent.*;



/**
 * 多线程测试类
 * @author liangcy
 */
public class ThreadTest {
    private static MonteCarlo monteCarloParams = new MonteCarlo(500, 10000);
    private static MonteCarlo subMonteCarloParams = new MonteCarlo(500, 1000);
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static SingleOptionMonteCarloCalculator calculator = new SingleOptionMonteCarloCalculator();
    private static underlying.gbm.Future future = new underlying.gbm.Future(100, 0.1);
    private static VanillaOptionParams optionParams = new VanillaOptionParams();
    private static Callable<Double> monteCarloList = () -> {
        calculator.setOption(getOption());
        calculator.setMonteCarloParams(subMonteCarloParams);
        calculator.calculatePrice();
        return calculator.getResult();
    };
    private static List<Future<Double>> taskList = new ArrayList<>(10);

    private static void updateTask() {
        for (int i = 0; i < 10; i++) {
            taskList.add(threadPool.submit(monteCarloList));
        }
    }

    private static void updateParams() {
        optionParams.setVolatility(0.3);
        optionParams.setTimeRemaining(0.5);
        optionParams.setStrikePrice(100);
    }

    private static EuropeanOption getOption() {
        EuropeanOption option = new EuropeanOption();
        updateParams();
        option.setUnderlying(future);
        option.setVanillaOptionParams(optionParams);
        return option;
    }

    @Test
    public void test() {
        SingleOptionAnalysisCalculator analysisCalculator = new SingleOptionAnalysisCalculator(getOption());
        analysisCalculator.calculatePrice();
        System.out.println(analysisCalculator.getResult());
        Assert.assertEquals(analysisCalculator.getError().getIndex(), 1);
    }

    @Test
    public void test1() {
        calculator.setMonteCarloParams(monteCarloParams);
        calculator.setOption(getOption());
        calculator.calculateTheta();
        SingleOptionAnalysisCalculator analysisCalculator = new SingleOptionAnalysisCalculator(getOption());
        analysisCalculator.calculateTheta();
        System.out.println(analysisCalculator.getResult());
        System.out.println(calculator.getResult());
        Assert.assertEquals(calculator.getError().getIndex(), 0);
    }

    @Test
    public void test2() {
        double[][] a = {{1, 2}, {3, 4}};
        System.out.println(CalculateUtil.twoDArrayToString(a));
    }





}
