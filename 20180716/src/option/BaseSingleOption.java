package option;

import underlying.BaseUnderlying;
import underlying.Future;
import volatility.VolatilitySurface;
import java.io.Serializable;
import java.util.Objects;


/**
 * @author liangcy
 */
public abstract class BaseSingleOption extends BaseOption implements Serializable {

    private VanillaOptionParams vanillaOptionParams = new VanillaOptionParams();
    private SingleOptionGreekParams precision = new SingleOptionGreekParams();
    private BaseUnderlying underlying;
    private VolatilitySurface volatilitySurface;

    public BaseSingleOption() {
    }

    public SingleOptionGreekParams getPrecision() {
        return precision;
    }

    public void setPrecision(SingleOptionGreekParams precision) {
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
        underlying.swapRQ();
    }

    void swapCallPut() {
        vanillaOptionParams.swapCallPut();
    }

    double getCostOfCarry() {
        return underlying.getCostOfCarry();
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
    public void refreshVolSurface() {
        double vol = getVanillaOptionParams().getVolatility();
        if (volatilitySurface == null || !volatilitySurface.isValidSurface()) {
            setVolatilitySurface(new VolatilitySurface(vol));
        } else {
            double moneyness = underlying.getSpotPrice() / vanillaOptionParams.getStrikePrice();
            double t = vanillaOptionParams.getTimeRemaining();
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

    public boolean isValid() {
        return underlying.isValid() && vanillaOptionParams.isValid();
    }

    public double getVolatilityFromSurface() {
        if(volatilitySurface.isValidSurface()) {
            double s = underlying.getSpotPrice();
            double k = vanillaOptionParams.getStrikePrice();
            double t = vanillaOptionParams.getTimeRemaining();
            return volatilitySurface.getVolatility(k / s, t);
        }
        return vanillaOptionParams.getVolatility();
    }

    public boolean isUnderlyingFuture() {
        return underlying instanceof Future;
    }

    /**
     * 用于计算隐含波动率构造初始波动率
     * @return 初始波动率
     */
    public double getInitialVol() {
        double t = vanillaOptionParams.getTimeRemaining();
        double k = vanillaOptionParams.getStrikePrice();
        double futureValue = underlying.getFutureValue(t);
        double logMoneyness = Math.log(futureValue / k);
        return Math.sqrt(Math.abs(logMoneyness) * 2 / t) + 0.1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BaseSingleOption option = (BaseSingleOption) obj;
        return Objects.equals(vanillaOptionParams, option.vanillaOptionParams) &&
                Objects.equals(precision, option.precision) &&
                Objects.equals(underlying, option.underlying) &&
                Objects.equals(volatilitySurface, option.volatilitySurface);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vanillaOptionParams, precision, underlying, volatilitySurface);
    }

    public double europeanD1() {
        EuropeanOption option = new EuropeanOption(this);
        return option.d1();
    }

    public double europeanD2() {
        EuropeanOption option = new EuropeanOption(this);
        return option.d2();
    }
}

