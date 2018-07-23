package option;

import calculator.utility.CalculateUtil;
import calculator.utility.MonteCarlo;

import java.io.Serializable;

class BarrierCalculator {
    private BarrierOption option;

    void setOption(BarrierOption option) {
        this.option = option;
    }

    private double sigmaT(double vol) {
        return vol * Math.sqrt(option.getVanillaOptionParams().getTimeRemaining());
    }

    private double c1() {
        double h = option.getBarrierOptionParams().getBarrierPrice();
        double s = option.getUnderlying().getSpotPrice();
        double a = 2 * option.getCostOfCarry() / (option.volAtBarrier() * option.volAtBarrier()) - 1;
        return Math.pow(h / s, a);
    }

    private double c2() {
        double h = option.getBarrierOptionParams().getBarrierPrice();
        double s = option.getUnderlying().getSpotPrice();
        double a = 2 * option.getVanillaOptionParams().getVolatility() / option.volAtBarrier();
        return c1() * Math.pow(h / s, a);
    }

    private double d1() {
        EuropeanOption europeanOption = new EuropeanOption(option);
        return europeanOption.d1();
    }

    private double d2() {
        EuropeanOption europeanOption = new EuropeanOption(option);
        return europeanOption.d2();
    }

    private double d3() {
        double h = option.getBarrierOptionParams().getBarrierPrice();
        double s = option.getUnderlying().getSpotPrice();
        return d1() + 2 * Math.log(h / s) / (sigmaT(option.volAtBarrier()));
    }

    private double d4() {
        double h = option.getBarrierOptionParams().getBarrierPrice();
        double s = option.getUnderlying().getSpotPrice();
        return d2() + 2 * Math.log(h / s) / (sigmaT(option.volAtBarrier()));
    }

    private double e1() {
        double vol = option.getVanillaOptionParams().getVolatility();
        return e2() - sigmaT(vol);
    }

    private double e2() {
        EuropeanOption europeanOption = new EuropeanOption(option);
        europeanOption.getVanillaOptionParams().setStrikePrice(option.getBarrierOptionParams().getBarrierPrice());
        europeanOption.getVanillaOptionParams().setVolatility(option.volAtBarrier());
        return -europeanOption.d2();
    }

    private double e3() {
        double vol = option.getVanillaOptionParams().getVolatility();
        return e4() - sigmaT(vol);
    }

    private double e4() {
        EuropeanOption europeanOption = new EuropeanOption(option);
        europeanOption.getVanillaOptionParams().setStrikePrice(option.getBarrierOptionParams().getBarrierPrice());
        europeanOption.getVanillaOptionParams().setVolatility(option.volAtBarrier());
        europeanOption.swapSpotStrike();
        return -europeanOption.d2();
    }

    /**
     * @return (d1 + e1 < 0 & & call) || (d1 + e1 > 0 && put)
     */
    private boolean useShortExpression() {
        return (d1() + e1() < 0) == (option.getVanillaOptionParams().isOptionTypeCall());
    }

    public int index() {
        if (useShortExpression()) {
            return 1;
        } else {
            return 0;
        }
    }

    private double getNewSpotPrice() {
        return option.getVanillaOptionParams().indexOfOptionType() *
                option.getDiscountValueByDividendRate() *
                option.getUnderlying().getSpotPrice();
    }

    private double getNewStrike() {
        return option.getVanillaOptionParams().indexOfOptionType() *
                option.getDiscountValueByRiskFreeRate() *
                option.getVanillaOptionParams().getStrikePrice();
    }

    private double getNormalCDFMin(double d, double e) {
        int phi = option.getVanillaOptionParams().indexOfOptionType();
        return CalculateUtil.normalCDF(Math.min(phi * d, phi * e));
    }

    private double getNormalCDFMinus(double d, double e) {
        int phi = option.getVanillaOptionParams().indexOfOptionType();
        return CalculateUtil.normalCDF(phi * d) - CalculateUtil.normalCDF(phi * e);
    }

    public double a() {
        double part1 = getNewSpotPrice() * getNormalCDFMin(d1(), -e1());
        double part2 = getNewStrike() * getNormalCDFMin(d2(), -e2());
        return part1 - part2;
    }

    public double b() {
        double part1 = getNewSpotPrice() * getNormalCDFMinus(d1(), -e1());
        double part2 = getNewStrike() * getNormalCDFMinus(d2(), -e2());
        return part1 - part2;
    }

    public double c() {
        double part1 = getNewSpotPrice() * c2() * getNormalCDFMin(d3(), -e3());
        double part2 = getNewStrike() * c1() * getNormalCDFMin(d4(), -e4());
        return part1 - part2;
    }

    public double d() {
        double part1 = getNewSpotPrice() * c2() * getNormalCDFMinus(d3(), -e3());
        double part2 = getNewStrike() * c1() * getNormalCDFMinus(d4(), -e4());
        return part1 - part2;
    }
}

/**
 * @author liangcy
 * 障碍期权
 */
public class BarrierOption extends BaseSingleOption implements Serializable {

    public BarrierOption() {
    }

    private BarrierOptionParams barrierOptionParams;
    /**
     * 回扣: 如果敲出期权已敲出, 敲出时支付; 如果敲入期权未敲入, 到期时支付
     */
    private double rebate = 0;

    public BarrierOptionParams getBarrierOptionParams() {
        return barrierOptionParams;
    }

    public void setBarrierOptionParams(BarrierOptionParams barrierOptionParams) {
        this.barrierOptionParams = barrierOptionParams;
    }

    public double getRebate() {
        return rebate;
    }

    public void setRebate(double rebate) {
        this.rebate = rebate;
    }

    @Override
    public boolean hasMonteCarloMethod() {
        return true;
    }

    @Override
    public double monteCarloPrice(double[] pricePath) {
        EuropeanOption option = new EuropeanOption(this);
        if (getBarrierOptionParams().isIn()) {
            for (double price : pricePath) {
                if (getBarrierOptionParams().isTouchSingleBarrier(price)) {
                    return option.monteCarloPrice(pricePath);
                }
            }
            return rebate * getDiscountValueByRiskFreeRate();
        } else {
            double[] timePoints = MonteCarlo.getTimePoints(getVanillaOptionParams().getTimeRemaining(), pricePath);
            for (int i = 0; i < pricePath.length; i++) {
                if (getBarrierOptionParams().isTouchSingleBarrier(pricePath[i])) {
                    double r = getUnderlying().getRiskFreeRate();
                    double hitTime = timePoints[i];
                    return rebate * Math.exp(-r * hitTime);
                }
            }
            return option.monteCarloPrice(pricePath);
        }
    }

    /**
     * @return 障碍价的波动率
     */
    double volAtBarrier() {
        if (null == getVolatilitySurface()) {
            return getVanillaOptionParams().getVolatility();
        }
        double h = getBarrierOptionParams().getBarrierPrice();
        double s = getUnderlying().getSpotPrice();
        double t = getVanillaOptionParams().getTimeRemaining();
        return getVolatilitySurface().getVolatility(h / s, t);
    }

    private double getRealRebate() {
        double realRebate = 0;
        if (rebate > 0) {
            BinaryBarrierOption binaryBarrierOption = new BinaryBarrierOption(this);
            binaryBarrierOption.getBarrierOptionParams().swapInOut();
            binaryBarrierOption.getVanillaOptionParams().setVolatility(volAtBarrier());

            if (getBarrierOptionParams().isIn()) {
                binaryBarrierOption.getBarrierOptionParams().setPayoffType(PAYOFF_TYPE_EXPIRE);
            } else {
                //敲出期权 敲出时就支付;
                binaryBarrierOption.getBarrierOptionParams().setPayoffType(PAYOFF_TYPE_HIT);
            }
            realRebate = binaryBarrierOption.bsm();
        }
        return realRebate;
    }

    @Override
    public double bsm() {
        if (barrierOptionParams.isIn()) {
            return bsmIn() + getRealRebate();
        }
        return europeanVanillaPrice() - bsmIn() + getRealRebate();
    }

    /**
     * 只计算敲入期权的bsm价格, 敲出期权用平价公式;
     *
     * @return 敲入期权的bsm价格
     */
    private double bsmIn() {
        //敲入期权如果已敲入, 按普通欧式期权计算;
        if (barrierOptionParams.isTouchSingleBarrier(getUnderlying().getSpotPrice())) {
            return europeanVanillaPrice();
        }
        BarrierCalculator calculator = new BarrierCalculator();
        calculator.setOption(this);
        //(up&&call) || (down&&put)
        if (barrierOptionParams.isUp() == getVanillaOptionParams().isOptionTypeCall()) {
            return calculator.a() + calculator.d() * calculator.index();
        }
        return calculator.c() + calculator.b() * calculator.index();
    }

    @Override
    public String toString() {
        return super.toString() + sep +
                getBarrierOptionParams().singleBarrierToString() + sep +
                "rebate: " + rebate;
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
                barrierOptionParams.isValidSingleBarrierParams();
    }
}
