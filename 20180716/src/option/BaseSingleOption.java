package option;

import calculator.utility.ConstantString;
import underlying.BaseUnderlying;
import volatility.VolatilitySurface;

import java.io.Serializable;

/**
 * @author liangcy
 */
public abstract class BaseSingleOption extends BaseOption implements Serializable {

    private VanillaOptionParams vanillaOptionParams = new VanillaOptionParams();
    private GreekPrecisionParams precision = new GreekPrecisionParams();
    private BaseUnderlying underlying;
    private VolatilitySurface volatilitySurface;


    public BaseSingleOption() {
    }

    public GreekPrecisionParams getPrecision() {
        return precision;
    }

    public void setPrecision(GreekPrecisionParams precision) {
        this.precision = precision;
    }

    public BaseUnderlying getUnderlying() {
        return underlying;
    }

    public void setUnderlying(BaseUnderlying underlying) {
        this.underlying = underlying;
    }

    public VanillaOptionParams getVanillaOptionParams() {
        return vanillaOptionParams;
    }

    public void setVanillaOptionParams(VanillaOptionParams vanillaOptionParams) {
        this.vanillaOptionParams = vanillaOptionParams;
    }

    public VolatilitySurface getVolatilitySurface() {
        return volatilitySurface;
    }

    public void setVolatilitySurface(VolatilitySurface volatilitySurface) {
        this.volatilitySurface = volatilitySurface;
    }

    public double getDiscountValueByRiskFreeRate() {
        double r = underlying.getRiskFreeRate();
        double t = vanillaOptionParams.getTimeRemaining();
        return getDiscountValue(r, t);
    }

    double getDiscountValueByDividendRate() {
        double q = underlying.getDividendRate();
        double t = vanillaOptionParams.getTimeRemaining();
        return this.getDiscountValue(q, t);
    }

    private double getDiscountValue(double rate, double timeRemaining) {
        return Math.exp(-rate * timeRemaining);
    }

    void swapSpotStrike() {
        double s = getUnderlying().getSpotPrice();
        double k = getVanillaOptionParams().getStrikePrice();
        getVanillaOptionParams().setStrikePrice(s);
        getUnderlying().setSpotPrice(k);
    }

    void swapRQ() {
        double r = getUnderlying().getRiskFreeRate();
        double q = getUnderlying().getDividendRate();
        getUnderlying().setRiskFreeRate(q);
        getUnderlying().setDividendRate(r);
    }

    void swapCallPut() {
        if (getVanillaOptionParams().isOptionTypeCall()) {
            getVanillaOptionParams().setOptionType(OPTION_TYPE_PUT);
        } else {
            getVanillaOptionParams().setOptionType(OPTION_TYPE_CALL);
        }
    }

    double getCostOfCarry() {
        return getUnderlying().getCostOfCarry();
    }

    double europeanVanillaPrice() {
        EuropeanOption option = new EuropeanOption(this);
        return option.bsm();
    }

    /**
     * 刷新波动率曲面,
     * 如果没有波动率曲面, 生成波动率平面;
     * 如果有波动率曲面, 以波动率为基准平移波动率曲面;
     */
    public void refreshVolsurface() {
        double vol = getVanillaOptionParams().getVolatility();
        if (volatilitySurface == null) {
            setVolatilitySurface(new VolatilitySurface(vol));
        } else {
            double moneyness = getUnderlying().getSpotPrice() / getVanillaOptionParams().getStrikePrice();
            double t = getVanillaOptionParams().getTimeRemaining();
            double volFromSurface = volatilitySurface.getVolatility(moneyness, t);
            double diffVol = vol - volFromSurface;
            volatilitySurface.shiftVolatility(diffVol);
        }
    }

    public boolean isEarlyExercise() {
        return false;
    }

    public boolean hasMonteCarloMethod() {
        return false;
    }

    public double monteCarloPrice(double[] pricePath) {
        return 0;
    }

    public boolean hasFiniteDifferenceMethod() {
        return false;
    }

    public double[] finiteDifferencePrice(double[] spotPrice) {
        return new double[spotPrice.length];
    }

    @Override
    public String toString() {
        return getUnderlying().toString() + sep +
                getVanillaOptionParams().toString();
    }
}

