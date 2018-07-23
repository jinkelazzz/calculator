package calculator.derivatives;

import calculator.utility.NewtonIterationParams;
import flanagan.roots.RealRoot;
import flanagan.roots.RealRootDerivFunction;
import option.BaseSingleOption;

import javax.swing.text.html.Option;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static calculator.utility.CalculatorError.*;

/**
 * 构造计算隐含波动率的Newton方程。
 */
class ImpliedVolFunction implements RealRootDerivFunction {
    private BaseSingleOption option;

    public void setOption(BaseSingleOption option) {
        this.option = option;
    }

    @Override
    public double[] function(double estimateVol) {
        option.getVanillaOptionParams().setVolatility(estimateVol);
        SingleOptionAnalysisCalculator calculator = new SingleOptionAnalysisCalculator(option);
        calculator.calculatePrice();
        double[] y = new double[2];
        y[0] = calculator.getResult() - option.getVanillaOptionParams().getTargetPrice();
        calculator.calculateVega();
        // 计算的是1%的Vega, 计算斜率要乘100.
        y[1] = calculator.getResult() * 100;
        return y;
    }
}


/**
 * 计算解析解的价格, implied volatility.
 * @author liangcy
 */
public class SingleOptionAnalysisCalculator extends BaseSingleOptionCalculator {

    /**
     * 牛顿迭代参数
     */
    private NewtonIterationParams iterParams = new NewtonIterationParams();

    public void setIterParams(NewtonIterationParams iterParams) {
        this.iterParams = iterParams;
    }

    public SingleOptionAnalysisCalculator() {
        super();
    }

    public SingleOptionAnalysisCalculator(BaseSingleOption option) {
        super(option);
    }

    /**
     * @return 获取对象的方法，先检查自身的方法，再检查继承来的方法
     */
    private Optional<Method> getMethod() {
        Class<?> c = option.getClass();
        String methodName = option.getVanillaOptionParams().getMethodName();
        try {
            return Optional.of(c.getDeclaredMethod(methodName));
        } catch (NoSuchMethodException e) {
            try {
                return Optional.of(c.getMethod(methodName));
            } catch (NoSuchMethodException e1) {
                return Optional.empty();
            }
        }
    }

    @Override
    public boolean hasMethod() {
        return getMethod().isPresent();
    }

    /**
     * 根据方法来获取结果。
     *
     * @param method 方法
     * @return 根据方法计算的价格。
     * @throws InvocationTargetException 内部异常捕获
     * @throws IllegalAccessException 非法访问方法
     */
    private double getPrice(Method method) throws InvocationTargetException, IllegalAccessException {
        method.setAccessible(true);
        return (double) method.invoke(option);
    }

    private double getInitialVol() {
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double k = option.getVanillaOptionParams().getStrikePrice();
        double futureValue = option.getUnderlying().getFutureValue(t);
        double logMoneyness = Math.log(futureValue / k);
        return Math.sqrt(Math.abs(logMoneyness) * 2 / t) + 0.1;
    }

    /**
     * 牛顿二分法计算隐含波动率, 下限0.005, 上限3.000
     * 这里会把隐含波动率赋值给volatility。
     */
    @Override
    public void calculateImpliedVolatility() {
        resetCalculator();
        ImpliedVolFunction function = new ImpliedVolFunction();
        function.setOption(option);
        RealRoot realRoot = new RealRoot();
        realRoot.setTolerance(iterParams.getTol());
        realRoot.setIterMax(iterParams.getIterations());
        double lowerLimit = 0.005;
        double upperLimit = 3.000;
        realRoot.setLowerBound(lowerLimit);
        realRoot.setUpperBound(upperLimit);
        //迭代初值
        double estimateVol = option.getVanillaOptionParams().getVolatility();
        if (estimateVol <= lowerLimit || estimateVol >= upperLimit) {
            estimateVol = getInitialVol();
        }
        realRoot.setEstimate(estimateVol);
        try {
            double root = realRoot.bisectNewtonRaphson(function);
            //root是NaN,计算失败
            if (Double.isNaN(root)) {
                setError(CALCULATE_NAN);
                return;
            }
            //达到迭代上限
            if (realRoot.getIterN() > realRoot.getIterMax()) {
                setResult(root);
                setError(REACH_MAX_ITERATION);
                return;
            }
            //计算成功
            {
                option.getVanillaOptionParams().setVolatility(root);
                setResult(root);
                setError(NORMAL);
            }
        } catch (Exception e) {
            setError(CALCULATE_FAILED);
        }
    }

    @Override
    public void calculatePrice() {
        resetCalculator();

        if(!hasMethod()) {
            setError(NOT_FOUND_METHOD);
            return;
        }

        Method method = getMethod().get();

        try {
            double price = getPrice(method);
            if (Double.isNaN(price)) {
                setError(CALCULATE_NAN);
                return;
            }
            setResult(price);
            setError(NORMAL);
        } catch (IllegalAccessException | InvocationTargetException e) {
            setError(CALCULATE_FAILED);
        }
    }
}
