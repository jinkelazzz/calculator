package calculator.derivatives;

import calculator.utility.CalculateUtil;
import calculator.utility.NewtonIterationParams;
import flanagan.math.DeepCopy;
import option.BaseSingleOption;
import static calculator.utility.CalculatorError.*;

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

    /**
     * 计算Delta;
     */
    public void calculateDelta() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }

        BaseSingleOption[] options = shiftUnderlyingPrice();
        BaseSingleOption lowerOption = options[0];
        BaseSingleOption upperOption = options[1];

        setOption(lowerOption);
        calculatePrice();
        if (!isNormal()) {
            return;
        }
        double lowerPrice = getResult();

        setOption(upperOption);
        calculatePrice();
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

    /**
     * 计算1%的Vega
     */
    public void calculateVega() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }

        BaseSingleOption[] options = shiftVolatility();
        BaseSingleOption lowerOption = options[0];
        BaseSingleOption upperOption = options[1];

        setOption(lowerOption);
        calculatePrice();
        if (!isNormal()) {
            return;
        }
        double lowerPrice = getResult();

        setOption(upperOption);
        calculatePrice();
        if (!isNormal()) {
            return;
        }
        double upperPrice = getResult();

        setOption(option);
        double lowerVol = lowerOption.getVanillaOptionParams().getVolatility();
        double upperVol = upperOption.getVanillaOptionParams().getVolatility();
        double vega = (upperPrice - lowerPrice) / (upperVol - lowerVol) / 100;
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
        BaseSingleOption[] options = shiftTimeRemaining();
        BaseSingleOption lowerOption = options[0];
        BaseSingleOption upperOption = options[1];

        setOption(lowerOption);
        calculatePrice();
        if (!isNormal()) {
            return;
        }
        double lowerPrice = getResult();

        setOption(upperOption);
        calculatePrice();
        if (!isNormal()) {
            return;
        }
        double price = getResult();
        setOption(option);
        double lowerT = lowerOption.getVanillaOptionParams().getTimeRemaining();
        double upperT = upperOption.getVanillaOptionParams().getTimeRemaining();
        double theta = (lowerPrice - price) / (upperT - lowerT) / 365;
        setResult(theta);
        setError(NORMAL);
    }

    public void calculateGamma() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }
        BaseSingleOption[] options = shiftUnderlyingPrice();
        BaseSingleOption lowerOption = options[0];
        BaseSingleOption upperOption = options[1];

        setOption(lowerOption);
        calculateDelta();
        if (!isNormal()) {
            return;
        }
        double lowerDelta = getResult();

        setOption(upperOption);
        calculateDelta();
        if (!isNormal()) {
            return;
        }
        double upperDelta = getResult();
        //reset;
        setOption(option);
        double lowerSpotPrice = lowerOption.getUnderlying().getSpotPrice();
        double upperSpotPrice = upperOption.getUnderlying().getSpotPrice();
        double gamma = (upperDelta - lowerDelta) / (upperSpotPrice - lowerSpotPrice);
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

        BaseSingleOption[] options = shiftInterestRate();
        BaseSingleOption lowerOption = options[0];
        BaseSingleOption upperOption = options[1];

        setOption(lowerOption);
        calculatePrice();
        if (!isNormal()) {
            return;
        }
        double lowerPrice = getResult();

        setOption(upperOption);
        calculatePrice();
        if (!isNormal()) {
            return;
        }
        double upperPrice = getResult();

        setOption(option);
        double lowerRate = lowerOption.getUnderlying().getRiskFreeRate();
        double upperRate = upperOption.getUnderlying().getRiskFreeRate();
        double rho = (upperPrice - lowerPrice) / (upperRate - lowerRate) / 10000;
        setResult(rho);
        setError(NORMAL);
    }

    public void calculateRho2() {
        resetCalculator();
        if(!hasMethod()) {
            setError(UNSUPPORTED_METHOD);
            return;
        }

        BaseSingleOption[] options = shiftDividendRate();
        BaseSingleOption lowerOption = options[0];
        BaseSingleOption upperOption = options[1];

        setOption(lowerOption);
        calculatePrice();
        if (!isNormal()) {
            return;
        }
        double lowerPrice = getResult();

        setOption(upperOption);
        calculatePrice();
        if (!isNormal()) {
            return;
        }
        double upperPrice = getResult();

        setOption(option);
        double lowerRate = lowerOption.getUnderlying().getDividendRate();
        double upperRate = upperOption.getUnderlying().getDividendRate();
        double rho2 = (upperPrice - lowerPrice) / (upperRate - lowerRate) / 10000;
        setResult(rho2);
        setError(NORMAL);
    }

    private boolean canUseVolatilitySurface() {
        return useVolatilitySurface && option.getVolatilitySurface().isValidSurface();
    }

    BaseSingleOption[] shiftUnderlyingPrice() {
        BaseSingleOption lowerOption = (BaseSingleOption) DeepCopy.copy(option);
        BaseSingleOption upperOption = (BaseSingleOption) DeepCopy.copy(option);

        double precision = option.getPrecision().getUnderlyingPricePrecision();
        double s = option.getUnderlying().getSpotPrice();
        double[] diffSpotPrice = CalculateUtil.midDiffValue(s, precision);

        double lowerSpotPrice = diffSpotPrice[0];
        double upperSpotPrice = diffSpotPrice[1];

        lowerOption.getUnderlying().setSpotPrice(lowerSpotPrice);
        upperOption.getUnderlying().setSpotPrice(upperSpotPrice);

        if(canUseVolatilitySurface()) {
            double k = option.getVanillaOptionParams().getStrikePrice();
            double t = option.getVanillaOptionParams().getTimeRemaining();
            double lowerVolatility = option.getVolatilitySurface().getVolatility(k / lowerSpotPrice, t);
            double upperVolatility = option.getVolatilitySurface().getVolatility(k / upperSpotPrice, t);
            lowerOption.getVanillaOptionParams().setVolatility(lowerVolatility);
            upperOption.getVanillaOptionParams().setVolatility(upperVolatility);
        }

        return new BaseSingleOption[] {lowerOption, upperOption};
    }

    private BaseSingleOption[] shiftVolatility() {
        BaseSingleOption lowerOption = (BaseSingleOption) DeepCopy.copy(option);
        BaseSingleOption upperOption = (BaseSingleOption) DeepCopy.copy(option);

        double precision = option.getPrecision().getVolatilityPrecision();
        double vol = option.getVanillaOptionParams().getVolatility();
        double[] diffVol = CalculateUtil.midDiffValue(vol, precision);

        lowerOption.getVanillaOptionParams().setVolatility(diffVol[0]);
        upperOption.getVanillaOptionParams().setVolatility(diffVol[1]);

        return new BaseSingleOption[] {lowerOption, upperOption};
    }

    private BaseSingleOption[] shiftTimeRemaining() {
        BaseSingleOption lowerOption = (BaseSingleOption) DeepCopy.copy(option);
        BaseSingleOption upperOption = (BaseSingleOption) DeepCopy.copy(option);

        double precision = option.getPrecision().getTimeRemainingPrecision();
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double[] diffTime = CalculateUtil.backwardDiffValue(t, precision);

        double t1 = diffTime[0];
        lowerOption.getVanillaOptionParams().setTimeRemaining(t1);

        if(canUseVolatilitySurface()) {
            double s = option.getUnderlying().getSpotPrice();
            double k = option.getVanillaOptionParams().getStrikePrice();
            double vol = lowerOption.getVolatilitySurface().getVolatility(k / s, t1);
            lowerOption.getVanillaOptionParams().setVolatility(vol);
        }
        return new BaseSingleOption[] {lowerOption, upperOption};
    }

    private BaseSingleOption[] shiftInterestRate() {
        BaseSingleOption lowerOption = (BaseSingleOption) DeepCopy.copy(option);
        BaseSingleOption upperOption = (BaseSingleOption) DeepCopy.copy(option);

        double precision = option.getPrecision().getInterestRatePrecision();
        double r = option.getUnderlying().getRiskFreeRate();
        double[] diffRate = CalculateUtil.midDiffValue(r, precision);

        lowerOption.getUnderlying().setRiskFreeRate(diffRate[0]);
        upperOption.getUnderlying().setRiskFreeRate(diffRate[1]);

        return new BaseSingleOption[] {lowerOption, upperOption};
    }

    private BaseSingleOption[] shiftDividendRate() {
        BaseSingleOption lowerOption = (BaseSingleOption) DeepCopy.copy(option);
        BaseSingleOption upperOption = (BaseSingleOption) DeepCopy.copy(option);

        double precision = option.getPrecision().getInterestRatePrecision();
        double q = option.getUnderlying().getDividendRate();
        double[] diffRate = CalculateUtil.midDiffValue(q, precision);

        lowerOption.getUnderlying().setDividendRate(diffRate[0]);
        upperOption.getUnderlying().setDividendRate(diffRate[1]);

        return new BaseSingleOption[] {lowerOption, upperOption};
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

}
