package volatility;

import calculator.derivatives.SingleOptionAnalysisCalculator;
import flanagan.math.VectorMaths;
import option.BaseSingleOption;
import underlying.BaseUnderlying;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


class SyntheticForwardParams implements Serializable{
    SyntheticForwardParams(double refForwardPrice, double ssr) {
        this.refForwardPrice = refForwardPrice;
        this.ssr = ssr;
    }

    private double refForwardPrice;
    /**
     * swimming skewness rate;
     */
    private double ssr;

    public void setRefForwardPrice(double refForwardPrice) {
        this.refForwardPrice = refForwardPrice;
    }

    public void setSsr(double ssr) {
        this.ssr = ssr;
    }

    public double getRefForwardPrice() {
        return refForwardPrice;
    }

    public double getSsr() {
        return ssr;
    }

    public double syntheticForward(double atmForwardPrice) {
        return Math.pow(atmForwardPrice, ssr / 100) * Math.pow(refForwardPrice, 1 - ssr / 100);
    }
}

class VolatilitySkewParams implements Serializable {
    private double refVolatility;
    private double refSlope;
    /**
     * volatility change rate;
     */
    private double vcr;
    /**
     * slope change rate;
     */
    private double scr;

    public void setRefVolatility(double refVolatility) {
        this.refVolatility = refVolatility;
    }

    public void setRefSlope(double refSlope) {
        this.refSlope = refSlope;
    }

    public void setVcr(double vcr) {
        this.vcr = vcr;
    }

    public void setScr(double scr) {
        this.scr = scr;
    }

    private double multi(SyntheticForwardParams syntheticForwardParams, double atmPrice) {
        double refPrice = syntheticForwardParams.getRefForwardPrice();
        double synPrice = syntheticForwardParams.syntheticForward(atmPrice);
        double ssr = syntheticForwardParams.getSsr();
        return ssr * (synPrice - refPrice) / refPrice;
    }

    public double getCurrentVolatility(SyntheticForwardParams syntheticForwardParams, double atmPrice) {
        return refVolatility - vcr * multi(syntheticForwardParams, atmPrice);
    }

    public double getCurrentSlope(SyntheticForwardParams syntheticForwardParams, double atmPrice) {
        return refSlope - scr * multi(syntheticForwardParams, atmPrice);
    }
}

class RangeParams implements Serializable {
    private double downCutoff;
    private double upCutoff;

    double getDownCutoff() {
        return downCutoff;
    }

    double getUpCutoff() {
        return upCutoff;
    }

    /**
     * down smoothing range;
     */
    private double dsm = 0.5;
    /**
     * up smoothing range;
     */
    private double usm = 0.5;
    private double dSlope;
    private double uSlope;

    public void setDsm(double dsm) {
        this.dsm = dsm;
    }

    public void setUsm(double usm) {
        this.usm = usm;
    }

    public void setdSlope(double dSlope) {
        this.dSlope = dSlope;
    }

    public void setuSlope(double uSlope) {
        this.uSlope = uSlope;
    }

    double downSmoothingEnd() {
        return downCutoff * (1 + dsm);
    }

    double upSmoothingEnd() {
        return upCutoff * (1 + usm);
    }

    private double[] paramsAtUpSmoothingRange(double volAtCutoff, double slopeAtCutoff) {
        double c = (slopeAtCutoff - uSlope) / (upCutoff - upSmoothingEnd()) / 2;
        double b = uSlope - (slopeAtCutoff - uSlope) / (upCutoff - upSmoothingEnd()) * upSmoothingEnd();
        double a = volAtCutoff - b * upCutoff - c * Math.pow(upCutoff, 2);
        return new double[] {a, b, c};
    }

    private double[] paramsAtDownSmoothingRange(double volAtCutoff, double slopeAtCutoff) {
        double c = (slopeAtCutoff - dSlope) / (downCutoff - downSmoothingEnd()) / 2;
        double b = dSlope - (slopeAtCutoff - dSlope) / (downCutoff - downSmoothingEnd()) * downSmoothingEnd();
        double a = volAtCutoff - b * downCutoff - c * Math.pow(downCutoff, 2);
        return new double[] {a, b, c};
    }

    public double volatilityAtUpSmoothingRange(double logMoneyness, double volAtUpCutoff, double slopeAtUpCutoff) {
        double[] params = paramsAtUpSmoothingRange(volAtUpCutoff, slopeAtUpCutoff);
        double[] logMoneynessVec = new double[] {1, logMoneyness, Math.pow(logMoneyness, 2)};
        return VectorMaths.dot(new VectorMaths(logMoneynessVec), new VectorMaths(params));
    }

    public double volatilityAtDownSmoothingRange(double logMoneyness, double volAtDownCutoff, double slopeAtDownCutoff) {
        double[] params = paramsAtDownSmoothingRange(volAtDownCutoff, slopeAtDownCutoff);
        double[] logMoneynessVec = new double[] {1, logMoneyness, Math.pow(logMoneyness, 2)};
        return VectorMaths.dot(new VectorMaths(logMoneynessVec), new VectorMaths(params));
    }

    private double volatilityAtUpSmoothingEnd(double volAtUpCutoff, double slopeAtUpCutoff) {
        return volatilityAtUpSmoothingRange(upSmoothingEnd(), volAtUpCutoff, slopeAtUpCutoff);
    }

    private double volatilityAtDownSmoothingEnd(double volAtDownCutoff, double slopeAtDownCutoff) {
        return volatilityAtDownSmoothingRange(downSmoothingEnd(), volAtDownCutoff, slopeAtDownCutoff);
    }

    public double volatilityAtUpAffineRange(double logMoneyness, double volAtUpCutoff, double slopeAtUpCutoff) {
        return uSlope * (logMoneyness - upSmoothingEnd())
                + volatilityAtUpSmoothingEnd(volAtUpCutoff, slopeAtUpCutoff);
    }

    public double volatilityAtDownAffineRange(double logMoneyness, double volAtDownCutoff, double slopeAtDownCutoff) {
        return dSlope * (logMoneyness - downSmoothingEnd())
                + volatilityAtDownSmoothingEnd(volAtDownCutoff, slopeAtDownCutoff);
    }
}

/**
 * @author liangcy
 */
public abstract class BaseVolatilitySkew implements Serializable {
    private BaseUnderlying underlying;
    private List<BaseSingleOption> optionList;
    private SyntheticForwardParams syntheticForwardParams;
    private VolatilitySkewParams volatilitySkewParams;
    private RangeParams rangeParams;

    public BaseUnderlying getUnderlying() {
        return underlying;
    }

    public void setUnderlying(BaseUnderlying underlying) {
        this.underlying = underlying;
    }

    public List<BaseSingleOption> getOptionList() {
        return optionList;
    }

    public void setOptionList(List<BaseSingleOption> optionList) {
        this.optionList = optionList;
    }

    public void updateOptionPrice(List<Double> optionPriceList) {
        for (int i = 0; i < optionPriceList.size(); i++) {
            optionList.get(i).getVanillaOptionParams().setTargetPrice(optionPriceList.get(i));
        }
    }

    public void updateImpliedVolatility() {
        SingleOptionAnalysisCalculator calculator = new SingleOptionAnalysisCalculator();
        for (BaseSingleOption option : optionList) {
            calculator.setOption(option);
            calculator.calculateImpliedVolatility();
        }
    }

    public SyntheticForwardParams getSyntheticForwardParams() {
        return syntheticForwardParams;
    }

    public void setSyntheticForwardParams(double refPrice, double ssr) {
        this.syntheticForwardParams = new SyntheticForwardParams(refPrice, ssr);
    }

    public VolatilitySkewParams getVolatilitySkewParams() {
        return volatilitySkewParams;
    }

    public void setVolatilitySkewParams(VolatilitySkewParams volatilitySkewParams) {
        this.volatilitySkewParams = volatilitySkewParams;
    }

    public RangeParams getRangeParams() {
        return rangeParams;
    }

    public void setRangeParams(RangeParams rangeParams) {
        this.rangeParams = rangeParams;
    }

    private double getAtmForward() {
        double t = optionList.get(0).getVanillaOptionParams().getTimeRemaining();
        return underlying.getFutureValue(t);
    }

    private double getSyntheticForwardPrice() {
        return syntheticForwardParams.syntheticForward(getAtmForward());
    }

    double getLogMoneyness(BaseSingleOption option) {
        return Math.log(option.getVanillaOptionParams().getStrikePrice() / getSyntheticForwardPrice());
    }

    boolean isOutOfMoney(BaseSingleOption option) {
        double syntheticForward = getSyntheticForwardPrice();
        //(call && k > s) || (put && k < s)
        return option.getVanillaOptionParams().isOptionTypeCall() ?
                option.getVanillaOptionParams().getStrikePrice() > syntheticForward :
                option.getVanillaOptionParams().getStrikePrice() < syntheticForward;
    }

    double getCurrentVolatility() {
        return volatilitySkewParams.getCurrentVolatility(syntheticForwardParams, getAtmForward());
    }

    double getCurrentSlope() {
        return volatilitySkewParams.getCurrentSlope(syntheticForwardParams, getAtmForward());
    }

    private List<Double> transformStrike() {
        List<Double> logStrikeList = new ArrayList<>(optionList.size());
        double synPrice = getSyntheticForwardPrice();
        for (BaseSingleOption option : optionList) {
            logStrikeList.add(Math.log(option.getVanillaOptionParams().getStrikePrice() / synPrice));
        }
        return logStrikeList;
    }

    boolean isAtMiddleRange(double logMoneyness) {
        return logMoneyness <= rangeParams.getUpCutoff() && logMoneyness >= rangeParams.getDownCutoff();
    }

    private boolean isAtUpSmoothingRange(double logMoneyness) {
        return logMoneyness <= rangeParams.upSmoothingEnd() && logMoneyness > rangeParams.getUpCutoff();
    }

    private boolean isAtDownSmoothingRange(double logMoneyness) {
        return logMoneyness >= rangeParams.downSmoothingEnd() && logMoneyness < rangeParams.getDownCutoff();
    }

    private boolean isAtUpAffineRange(double logMoneyness) {
        return logMoneyness > rangeParams.upSmoothingEnd();
    }

    private boolean isAtDownAffineRange(double logMoneyness) {
        return logMoneyness < rangeParams.downSmoothingEnd();
    }

    /**
     * down cutoff <= log moneyness <= up cutoff;
     * @param logMoneyness log(k/s)
     * @return 在logMoneyness点处的volatility;
     */
    abstract double  getVolatilityInMiddle(double logMoneyness);

    /**
     * down cutoff <= log moneyness <= up cutoff;
     * @param logMoneyness log(k/s)
     * @return 在logMoneyness点处的斜率(slope)
     */
    abstract double getSlopeInMiddle(double logMoneyness);

    private double getVolatilityAtUpSmoothingRange(double logMoneyness) {
        double upCutoff = getRangeParams().getUpCutoff();
        double volAtCutoff = getVolatilityInMiddle(upCutoff);
        double slopeAtCutoff = getSlopeInMiddle(upCutoff);
        return getRangeParams().volatilityAtUpSmoothingRange(logMoneyness, volAtCutoff, slopeAtCutoff);
    }

    private double getVolatilityAtDownSmoothingRange(double logMoneyness) {
        double downCutoff = getRangeParams().getDownCutoff();
        double volAtCutoff = getVolatilityInMiddle(downCutoff);
        double slopeAtCutoff = getSlopeInMiddle(downCutoff);
        return getRangeParams().volatilityAtDownSmoothingRange(logMoneyness, volAtCutoff, slopeAtCutoff);
    }

    double getVolatilityAtUpAffineRange(double logMoneyness) {
        double upCutoff = getRangeParams().getUpCutoff();
        double volAtCutoff = getVolatilityInMiddle(upCutoff);
        double slopeAtCutoff = getSlopeInMiddle(upCutoff);
        return getRangeParams().volatilityAtUpAffineRange(logMoneyness, volAtCutoff, slopeAtCutoff);
    }

    double getVolatilityAtDownAffineRange(double logMoneyness) {
        double downCutoff = getRangeParams().getDownCutoff();
        double volAtCutoff = getVolatilityInMiddle(downCutoff);
        double slopeAtCutoff = getSlopeInMiddle(downCutoff);
        return getRangeParams().volatilityAtDownAffineRange(logMoneyness, volAtCutoff, slopeAtCutoff);
    }

    private double getVolatility(double logMoneyness) {
        if(isAtUpSmoothingRange(logMoneyness)) {
            return getVolatilityAtUpSmoothingRange(logMoneyness);
        }
        if(isAtUpAffineRange(logMoneyness)) {
            return getVolatilityAtUpAffineRange(logMoneyness);
        }
        if(isAtDownSmoothingRange(logMoneyness)) {
            return getVolatilityAtDownSmoothingRange(logMoneyness);
        }
        if(isAtDownAffineRange(logMoneyness)) {
            return getVolatilityAtDownAffineRange(logMoneyness);
        }
        return getVolatilityInMiddle(logMoneyness);
    }

    private double getWeight(BaseSingleOption option) {
        if(isOutOfMoney(option)) {
            return 1;
        }
        double power = 2 * Math.sqrt(option.getVanillaOptionParams().getTimeRemaining());
        double s = getAtmForward();
        double k = option.getVanillaOptionParams().getStrikePrice();
        return option.getVanillaOptionParams().isOptionTypeCall() ? Math.pow(k / s, power) : Math.pow(s / k, power);
    }

    double fitError() {
        List<Double> logMoneynessList = transformStrike();
        updateImpliedVolatility();
        int n = logMoneynessList.size();
        double sum = 0;
        double fittingVolatility;
        double impliedVolatility;
        double weight;
        BaseSingleOption option;
        for (int i = 0; i < n; i++) {
            option = optionList.get(i);
            fittingVolatility = getVolatility(logMoneynessList.get(i));
            impliedVolatility = option.getVanillaOptionParams().getVolatility();
            weight = getWeight(option);
            sum = sum + Math.pow(weight * (fittingVolatility - impliedVolatility), 2);
        }
        return sum;
    }




}
