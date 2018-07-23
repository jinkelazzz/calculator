package option;

import adjusted.european.option.CorradoSu;
import adjusted.european.option.Heston;
import adjusted.european.option.Sabr;
import calculator.utility.CalculateUtil;
import flanagan.math.DeepCopy;
import underlying.BaseUnderlying;
import volatility.VolatilitySurface;

import java.io.Serializable;

/**
 * @author liangcy
 */
public class EuropeanOption extends BaseSingleOption implements Serializable {
    public EuropeanOption() {
    }

    public EuropeanOption(BaseSingleOption option) {
        if (option instanceof BinaryBarrierOption) {
            ((BinaryBarrierOption) option).refreshOptionType();
        }
        this.setUnderlying((BaseUnderlying) DeepCopy.copy(option.getUnderlying()));
        this.setVanillaOptionParams((VanillaOptionParams) DeepCopy.copy(option.getVanillaOptionParams()));
        this.setVolatilitySurface((VolatilitySurface) DeepCopy.copy(option.getVolatilitySurface()));
        this.setPrecision((SingleOptionGreekParams) DeepCopy.copy(option.getPrecision()));
    }

    private Heston hestonParams = new Heston();
    private Sabr sabrParams = new Sabr();
    private CorradoSu corradoSuParams = new CorradoSu();

    public CorradoSu getCorradoSuParams() {
        return corradoSuParams;
    }

    public void setCorradoSuParams(CorradoSu corradoSuParams) {
        this.corradoSuParams = corradoSuParams;
    }

    public Heston getHestonParams() {
        return hestonParams;
    }

    public void setHestonParams(Heston hestonParams) {
        this.hestonParams = hestonParams;
    }

    public Sabr getSabrParams() {
        return sabrParams;
    }

    public void setSabrParams(Sabr sabrParams) {
        this.sabrParams = sabrParams;
    }

    public double d1() {
        double k = getVanillaOptionParams().getStrikePrice();
        double t = getVanillaOptionParams().getTimeRemaining();
        double sigmaT = getVanillaOptionParams().sigmaT();
        double st = getUnderlying().getFutureValue(t);
        return (Math.log(st / k) + sigmaT * sigmaT / 2) / sigmaT;
    }

    double d2() {
        return d1() - getVanillaOptionParams().sigmaT();
    }

    private double callLowerLimit() {
        double s = getUnderlying().getSpotPrice();
        double k = getVanillaOptionParams().getStrikePrice();
        return s * getDiscountValueByDividendRate() - k * getDiscountValueByRiskFreeRate();
    }

    @Override
    public double bsm() {
        double s = getUnderlying().getSpotPrice();
        double k = getVanillaOptionParams().getStrikePrice();
        double callPrice = s * getDiscountValueByDividendRate() * CalculateUtil.normalCDF(d1()) -
                k * getDiscountValueByRiskFreeRate() * CalculateUtil.normalCDF(d2());
        return getVanillaOptionParams().isOptionTypeCall() ? callPrice : (callPrice - callLowerLimit());
    }

    @Override
    public boolean hasFiniteDifferenceMethod() {
        return true;
    }

    @Override
    public double[] finiteDifferencePrice(double[] spotPrice) {
        double[] optionPrice = new double[spotPrice.length];
        double k = getVanillaOptionParams().getStrikePrice();
        int index = getVanillaOptionParams().indexOfOptionType();
        for (int i = 0; i < spotPrice.length; i++) {
            optionPrice[i] = Math.max(0, (spotPrice[i] - k) * index);
        }
        return optionPrice;
    }

    @Override
    public boolean hasMonteCarloMethod() {
        return true;
    }

    @Override
    public double monteCarloPrice(double[] pricePath) {
        double st = pricePath[pricePath.length - 1];
        double k = getVanillaOptionParams().getStrikePrice();
        int index = getVanillaOptionParams().indexOfOptionType();
        return getDiscountValueByRiskFreeRate() * Math.max(index * (st - k), 0);
    }

    public double sabr() {
        sabrParams.setOption(this);
        double sabrVolatility = sabrParams.sabrVolatility();
        getVanillaOptionParams().setVolatility(sabrVolatility);
        return bsm();
    }

    public double heston() {
        hestonParams.setOption(this);
        double integration = hestonParams.hestonIntegration();
        double s = getUnderlying().getSpotPrice();
        double k = getVanillaOptionParams().getStrikePrice();
        double callPrice = 0.5 * s * getDiscountValueByDividendRate() -
                0.5 * k * getDiscountValueByRiskFreeRate() + integration;

        if (Double.isNaN(callPrice) || callPrice <= callLowerLimit()) {
            callPrice = bsm();
        }

        return getVanillaOptionParams().isOptionTypeCall() ? callPrice : (callPrice - callLowerLimit());
    }

    public double corradoSu() {
        corradoSuParams.setOption(this);
        double addition = corradoSuParams.corradoSuAddition();
        double callPrice = Math.max(bsm() + addition, callLowerLimit());
        return getVanillaOptionParams().isOptionTypeCall() ? callPrice : (callPrice - callLowerLimit());
    }
}
