package calculator.derivatives;

import calculator.utility.CalculateUtil;
import flanagan.math.DeepCopy;
import option.BaseSingleOption;
import underlying.Future;

import java.io.Serializable;

import static calculator.utility.CalculatorError.NORMAL;
import static calculator.utility.CalculatorError.UNSUPPORTED_METHOD;


class ShiftSingleOption implements Serializable {
    private BaseSingleOption option;
    private BaseSingleOption[] options = new BaseSingleOption[2];
    private double denominator = 0;

    ShiftSingleOption() {}

    ShiftSingleOption(BaseSingleOption option) {
        setOption(option);
    }

    public void setOption(BaseSingleOption option) {
        this.option = option;
    }

    public BaseSingleOption[] getOptions() {
        return options;
    }

    public double getDenominator() {
        return denominator;
    }

    private void setOptions(BaseSingleOption[] options) {
        this.options = options;
    }

    private void setDenominator(double denominator) {
        this.denominator = denominator;
    }

    private void initialOptions() {
        options[0] = (BaseSingleOption) DeepCopy.copy(option);
        options[1] = (BaseSingleOption) DeepCopy.copy(option);
    }

    void shiftUnderlyingPrice(boolean useVolatilitySurface) {
        if(useVolatilitySurface) {
            option.refreshVolSurface();
        }
        initialOptions();

        double precision = option.getPrecision().getUnderlyingPricePrecision();
        double s = option.getUnderlying().getSpotPrice();
        double[] diffSpotPrice = CalculateUtil.midDiffValue(s, precision);
        setDenominator(diffSpotPrice[1] - diffSpotPrice[0]);

        options[0].getUnderlying().setSpotPrice(diffSpotPrice[0]);
        options[1].getUnderlying().setSpotPrice(diffSpotPrice[1]);
        if(useVolatilitySurface) {
            double lowerVolatility = options[0].getVolatilityFromSurface();
            double upperVolatility = options[1].getVolatilityFromSurface();
            options[0].getVanillaOptionParams().setVolatility(lowerVolatility);
            options[1].getVanillaOptionParams().setVolatility(upperVolatility);
        }

    }

    void shiftVolatility() {
        initialOptions();

        double precision = option.getPrecision().getVolatilityPrecision();
        double vol = option.getVanillaOptionParams().getVolatility();
        double[] diffVol = CalculateUtil.midDiffValue(vol, precision);
        setDenominator(diffVol[1] - diffVol[0]);
        options[0].getVanillaOptionParams().setVolatility(diffVol[0]);
        options[1].getVanillaOptionParams().setVolatility(diffVol[1]);
    }

    void shiftTimeRemaining(boolean useVolatilitySurface) {
        if(useVolatilitySurface) {
            option.refreshVolSurface();
        }
        initialOptions();

        double precision = option.getPrecision().getTimeRemainingPrecision();
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double[] diffTime = CalculateUtil.backwardDiffValue(t, precision);
        setDenominator(diffTime[1] - diffTime[0]);

        options[0].getVanillaOptionParams().setTimeRemaining(diffTime[0]);
        if(useVolatilitySurface) {
            double volatility = options[0].getVolatilityFromSurface();
            options[0].getVanillaOptionParams().setVolatility(volatility);
        }
    }

    void shiftInterestRate() {
        initialOptions();

        double precision = option.getPrecision().getInterestRatePrecision();
        double r = option.getUnderlying().getRiskFreeRate();
        double[] diffRate = CalculateUtil.midDiffValue(r, precision);
        setDenominator(diffRate[1] - diffRate[0]);

        options[0].getUnderlying().setRiskFreeRate(diffRate[0]);
        options[1].getUnderlying().setRiskFreeRate(diffRate[1]);
    }

    void shiftDividendRate() {
        initialOptions();

        double precision = option.getPrecision().getInterestRatePrecision();
        double q = option.getUnderlying().getDividendRate();
        double[] diffRate = CalculateUtil.midDiffValue(q, precision);
        setDenominator(diffRate[1] - diffRate[0]);

        options[0].getUnderlying().setDividendRate(diffRate[0]);
        options[1].getUnderlying().setDividendRate(diffRate[1]);
    }

}

/**
 * @author liangcy
 */
public abstract class BaseSingleOptionCalculator extends BaseCalculator {
    BaseSingleOption option;
    private boolean useVolatilitySurface = false;

    public BaseSingleOptionCalculator() {

    }

    public BaseSingleOptionCalculator(BaseSingleOption option) {
        setOption(option);
    }

    public BaseSingleOption getOption() {
        return option;
    }

    public void setOption(BaseSingleOption option) {
        this.option = option;
    }

    public void enableVolSurface() {
        this.useVolatilitySurface = true;
    }

    public void disableVolSurface() {
        this.useVolatilitySurface = false;
    }

    /**
     * 检查是否存在计算的方法
     * @return true:存在, false:不存在
     */
    public abstract boolean hasMethod();

    /**
     * 计算价格
     */
    @Override
    public abstract void calculatePrice();

    /**
     * 计算隐含波动率, 并把隐含波动率赋给volatility
     */
    public abstract void calculateImpliedVolatility();

    ShiftSingleOption shiftOption() {
        return new ShiftSingleOption(option);
    }

    /**
     * 计算Delta;
     */
    public void calculateDelta() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }

        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftUnderlyingPrice(canUseVolatilitySurface());
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions());
        if(!isNormal()) {
            return;
        }
        double delta = diffPrice / shiftSingleOption.getDenominator();
        setResult(delta);
        setError(NORMAL);
    }

    /**
     * 计算1%的Vega
     */
    public void calculateVega() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftVolatility();
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions());
        if(!isNormal()) {
            return;
        }
        double vega = diffPrice / shiftSingleOption.getDenominator() / 100;
        setResult(vega);
        setError(NORMAL);
    }

    /**
     * 计算1天的Theta(按365天计算), 而且是单边差分计算
     */
    public void calculateTheta() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftTimeRemaining(canUseVolatilitySurface());
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions());
        if(!isNormal()) {
            return;
        }
        double theta = -diffPrice / shiftSingleOption.getDenominator() / 365;
        setResult(theta);
        setError(NORMAL);
    }

    public void calculateGamma() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftUnderlyingPrice(canUseVolatilitySurface());
        double diffDelta = getDiffDelta(shiftSingleOption.getOptions());
        if(!isNormal()) {
            return;
        }
        double gamma = diffDelta / shiftSingleOption.getDenominator();
        setResult(gamma);
        setError(NORMAL);
    }

    /**
     * 计算1个BP的Rho
     */
    public void calculateRho() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }

        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftInterestRate();
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions());
        if(!isNormal()) {
            return;
        }
        double rho = diffPrice / shiftSingleOption.getDenominator() / 10000;
        setResult(rho);
        setError(NORMAL);
    }

    public void calculateRho2() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }

        if(option.isUnderlyingFuture()) {
            setResult(0);
            setError(NORMAL);
            return;
        }

        ShiftSingleOption shiftSingleOption = shiftOption();
        shiftSingleOption.shiftDividendRate();
        double diffPrice = getDiffPrice(shiftSingleOption.getOptions());
        if(!isNormal()) {
            return;
        }
        double rho2 = diffPrice / shiftSingleOption.getDenominator() / 10000;
        setResult(rho2);
        setError(NORMAL);
    }

    boolean canUseVolatilitySurface() {
        return useVolatilitySurface && option.getVolatilitySurface().isValidSurface();
    }

    /**
     * 计算spot price变动1%带来的delta value也就是delta * 1% * spot price
     */
    public void calculateDeltaValue() {
        double deltaS = option.getUnderlying().getSpotPrice() * 0.01;
        calculateDelta();
        double deltaValue = getResult() * deltaS;
        setResult(deltaValue);
    }

    /**
     * 计算spot price变动1%带来的gamma value也就是0.5 * gamma * (1% * spot price) ^ 2
     */
    public void calculateGammaValue() {
        double deltaS = option.getUnderlying().getSpotPrice();
        calculateGamma();
        double gammaValue = 0.5 * getResult() * Math.pow(deltaS, 2);
        setResult(gammaValue);
    }

    /**
     * 计算volatility 变动1%带来的vega value也就是vega(因为vega已经是1%的变动了)
     */
    public void calculateVegaValue() {
        calculateVega();
    }

    public void calculateThetaValue() {
        calculateTheta();
    }

    public void calculateRhoValue() {
        calculateRho();
    }

    public void calculateRho2Value() {
        calculateRho2();
    }

    private double getDiffPrice(BaseSingleOption[] options) {
        setOption(options[0]);
        calculatePrice();
        double lowerPrice = getResult();
        setOption(options[1]);
        calculatePrice();
        double upperPrice = getResult();
        //reset;
        setOption(option);
        return upperPrice - lowerPrice;
    }

    private double getDiffDelta(BaseSingleOption[] options) {
        setOption(options[0]);
        calculateDelta();
        double lowerDelta = getResult();
        setOption(options[1]);
        calculateDelta();
        double upperDelta = getResult();
        //reset;
        setOption(option);
        return upperDelta - lowerDelta;
    }

}
