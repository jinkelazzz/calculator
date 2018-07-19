package option;

import calculator.utility.CalculateUtil;
import calculator.utility.MonteCarlo;
import flanagan.math.DeepCopy;
import underlying.BaseUnderlying;
import volatility.VolatilitySurface;

import java.io.Serializable;

class BinaryBarrierCalculator {
    private BinaryBarrierOption option;

    void setOption(BinaryBarrierOption option) {
        this.option = option;
    }

    private double mu() {
        double vol = option.getVanillaOptionParams().getVolatility();
        return option.getCostOfCarry() / (vol * vol) - 0.5;
    }

    private double lambda() {
        double vol = option.getVanillaOptionParams().getVolatility();
        return Math.sqrt(mu() * mu() + 2 * option.getUnderlying().getRiskFreeRate() / (vol * vol));
    }

    private double sigmaT() {
        return option.getVanillaOptionParams().getVolatility() *
                Math.sqrt(option.getVanillaOptionParams().getTimeRemaining());
    }

    private double p() {
        return (mu() + 1) * sigmaT();
    }

    private double x1() {
        double s = option.getUnderlying().getSpotPrice();
        double k = option.getVanillaOptionParams().getStrikePrice();
        return Math.log(s / k) / sigmaT() + p();
    }

    private double x2() {
        double s = option.getUnderlying().getSpotPrice();
        double h = option.getBarrierOptionParams().getBarrierPrice();
        return Math.log(s / h) / sigmaT() + p();
    }

    private double y1() {
        double s = option.getUnderlying().getSpotPrice();
        double k = option.getVanillaOptionParams().getStrikePrice();
        double h = option.getBarrierOptionParams().getBarrierPrice();
        return Math.log(h * h / (s * k)) / sigmaT() + p();
    }

    private double y2() {
        double s = option.getUnderlying().getSpotPrice();
        double h = option.getBarrierOptionParams().getBarrierPrice();
        return Math.log(h / s) / sigmaT() + p();
    }

    private double z() {
        double s = option.getUnderlying().getSpotPrice();
        double h = option.getBarrierOptionParams().getBarrierPrice();
        return Math.log(h / s) / sigmaT() + lambda() * sigmaT();
    }

    private double yita() {
        if (option.getBarrierOptionParams().isUp()) {
            return -1.0;
        } else {
            return 1.0;
        }
    }

    public double a() {
        double h = option.getBarrierOptionParams().getBarrierPrice();
        double s = option.getUnderlying().getSpotPrice();
        double p1 = Math.pow(h / s, mu() + lambda()) * CalculateUtil.normalCDF(yita() * z());
        double p2 = Math.pow(h / s, mu() - lambda()) *
                CalculateUtil.normalCDF(yita() * z() - 2 * yita() * lambda() * sigmaT());
        return p1 + p2;
    }

    public double b1() {
        int phi = option.getVanillaOptionParams().indexOfOptionType();
        return option.getDiscountValueByRiskFreeRate() *
                CalculateUtil.normalCDF(phi * x1() - phi * sigmaT());
    }

    public double b2() {
        int phi = option.getVanillaOptionParams().indexOfOptionType();
        return option.getDiscountValueByRiskFreeRate() *
                CalculateUtil.normalCDF(phi * x2() - phi * sigmaT());
    }

    public double b3() {
        double h = option.getBarrierOptionParams().getBarrierPrice();
        double s = option.getUnderlying().getSpotPrice();
        return option.getDiscountValueByRiskFreeRate() * Math.pow(h / s, 2 * mu()) *
                CalculateUtil.normalCDF(yita() * y1() - yita() * sigmaT());
    }

    public double b4() {
        double h = option.getBarrierOptionParams().getBarrierPrice();
        double s = option.getUnderlying().getSpotPrice();
        return option.getDiscountValueByRiskFreeRate() * Math.pow(h / s, 2 * mu()) *
                CalculateUtil.normalCDF(yita() * y2() - yita() * sigmaT());
    }
}

/**
 * (美式)障碍二元期权;没有执行价;
 * 触碰障碍根据障碍类型和给付时间计算收益;不会转为普通二元期权;
 *
 * @author liangcy
 */
public class BinaryBarrierOption extends BaseSingleOption implements Serializable {
    public BinaryBarrierOption() {
    }

    /**
     * 计算障碍期权回扣值的构造函数,;
     *
     * @param option 障碍期权
     */
    BinaryBarrierOption(BarrierOption option) {
        this.setUnderlying((BaseUnderlying) DeepCopy.copy(option.getUnderlying()));
        this.setVanillaOptionParams((VanillaOptionParams) DeepCopy.copy(option.getVanillaOptionParams()));
        this.setVolatilitySurface((VolatilitySurface) DeepCopy.copy(option.getVolatilitySurface()));
        this.setBarrierOptionParams((BarrierOptionParams) DeepCopy.copy(option.getBarrierOptionParams()));
        this.setCash(option.getRebate());
    }

    private double cash = 1.0;

    private BarrierOptionParams barrierOptionParams;

    public double getCash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    public BarrierOptionParams getBarrierOptionParams() {
        return barrierOptionParams;
    }

    public void setBarrierOptionParams(BarrierOptionParams barrierOptionParams) {
        this.barrierOptionParams = barrierOptionParams;
    }

    /**
     * 美式期权不分看涨看跌, 为了计算方便将向上的期权设置成看涨, 向下的期权设置成看跌;
     */
    public void refreshOptionType() {
        if (barrierOptionParams.isUp()) {
            getVanillaOptionParams().setOptionType(OPTION_TYPE_CALL);
        } else {
            getVanillaOptionParams().setOptionType(OPTION_TYPE_PUT);
        }
    }

    /**
     * @param pricePath 蒙特卡洛模拟路径
     * @return 触碰障碍时间, 如果未触碰, 返回0
     */
    private double hitBarrierTime(double[] pricePath) {
        int n = pricePath.length;
        double[] timePoints = MonteCarlo.getTimePoints(getVanillaOptionParams().getTimeRemaining(), pricePath);
        for (int i = 0; i < n; i++) {
            if (barrierOptionParams.isTouchSingleBarrier(pricePath[i])) {
                return timePoints[i];
            }
        }
        return -1;
    }

    private boolean isHit(double[] pricePath) {
        return hitBarrierTime(pricePath) != -1;
    }

    @Override
    public boolean hasMonteCarloMethod() {
        return true;
    }

    @Override
    public double monteCarloPrice(double[] pricePath) {
        if (barrierOptionParams.isPayAtHit()) {
            double hitTime = hitBarrierTime(pricePath);
            return isHit(pricePath) ? cash * Math.exp(-hitTime * getUnderlying().getRiskFreeRate()) : 0;
        } else {
            //如果敲入期权触碰障碍或者敲出期权未触碰障碍
            return isHit(pricePath) == barrierOptionParams.isIn() ? cash * getDiscountValueByRiskFreeRate() : 0;
        }
    }

    @Override
    public double bsm() {
        BinaryBarrierCalculator calculator = new BinaryBarrierCalculator();
        calculator.setOption(this);
        if (barrierOptionParams.isPayAtHit()) {
            return calculator.a() * cash;
        } else {
            if (barrierOptionParams.isIn()) {
                return (calculator.b2() + calculator.b4()) * cash;
            } else {
                return (getDiscountValueByDividendRate() - calculator.b2() - calculator.b4()) * cash;
            }
        }
    }


    @Override
    public String toString() {
        return super.toString() + sep +
                getBarrierOptionParams().singleBarrierToString() + sep +
                "cash: " + cash;
    }
}
