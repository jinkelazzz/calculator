package option;

import java.io.Serializable;
import java.util.Objects;

class DoubleBinaryBarrierOptionCalculator {
    private DoubleBinaryBarrierOption option;

    public void setOption(DoubleBinaryBarrierOption option) {
        this.option = option;
    }

    private double z() {
        return z1(option.getBarrierOptionParams().getUpperBarrierPrice(),
                option.getBarrierOptionParams().getLowerBarrierPrice());
    }

    private double z1(double upperBarrierPrice, double lowerBarrierPrice) {
        return Math.log(upperBarrierPrice / lowerBarrierPrice);
    }

    private double alpha() {
        double mu = option.getCostOfCarry();
        double vol = option.getVanillaOptionParams().getVolatility();
        return -mu / (vol * vol) - 0.5;
    }

    private double beta() {
        double r = option.getUnderlying().getRiskFreeRate();
        double vol = option.getVanillaOptionParams().getVolatility();
        return -Math.pow(alpha(), 2) - 2 * r / (vol * vol);
    }

    private double p(int i) {
        return Math.PI * i / z();
    }

    private double p1(int i) {
        return 2 * option.getCash() * p(i) / z();
    }

    private double p2(int i) {
        double s = option.getUnderlying().getSpotPrice();
        double u = option.getBarrierOptionParams().getUpperBarrierPrice();
        double l = option.getBarrierOptionParams().getLowerBarrierPrice();
        double numerator = Math.pow(s / l, alpha()) - Math.pow(-1, i) * Math.pow(s / u, alpha());
        double denominator = Math.pow(alpha(), 2) + Math.pow(p(i), 2);
        return numerator / denominator;
    }

    private double p3(int i) {
        double u = option.getBarrierOptionParams().getUpperBarrierPrice();
        double l = option.getBarrierOptionParams().getLowerBarrierPrice();
        return q3(i, u, l);
    }

    private double p4(int i) {
        double a = -(Math.pow(p(i), 2) - beta()) * Math.pow(option.getVanillaOptionParams().sigmaT(), 2) / 2;
        return Math.exp(a);
    }

    public double outPriceAtExpire() {
        int maxIteration = option.getBarrierOptionParams().getMaxIterationTimes();
        double tol = option.getBarrierOptionParams().getTolerance();
        double price = 0;
        for (int i = 1; i <= maxIteration; i++) {
            double add = p1(i) * p2(i) * p3(i) * p4(i);
            price = price + add;
            if (Math.abs(add) < tol) {
                return price;
            }
        }
        return price;
    }

    private double q(int i, double upperBarrierPrice, double lowerBarrierPrice) {
        return Math.PI * i / z1(upperBarrierPrice, lowerBarrierPrice);
    }

    private double q1(double lowerBarrierPrice) {
        double s = option.getUnderlying().getSpotPrice();
        return option.getCash() * Math.pow(s / lowerBarrierPrice, alpha());
    }

    private double q2(int i, double upperBarrierPrice, double lowerBarrierPrice) {
        double numerator = beta() - Math.pow(q(i, upperBarrierPrice, lowerBarrierPrice), 2) * p4(i);
        double denominator = Math.pow(q(i, upperBarrierPrice, lowerBarrierPrice), 2) - beta();
        return 2 / (i * Math.PI) * (numerator / denominator);
    }

    private double q3(int i, double upperBarrierPrice, double lowerBarrierPrice) {
        double s = option.getUnderlying().getSpotPrice();
        return Math.sin(q(i, upperBarrierPrice, lowerBarrierPrice) * Math.log(s / lowerBarrierPrice));
    }

    private double q4(double upperBarrierPrice, double lowerBarrierPrice) {
        double s = option.getUnderlying().getSpotPrice();
        return 1 - Math.log(s / lowerBarrierPrice) / z1(upperBarrierPrice, lowerBarrierPrice);
    }

    /**
     * @return option price;
     * @reference Hui (1996)
     * 原文是触碰到下障碍给cash, 触碰到上障碍敲出;
     * 这里计算触碰到下障碍给cash, 触碰到上障碍敲出 和 触碰到上障碍给cash, 触碰到下障碍敲出的两个期权之和;
     * 也就是只要触碰到障碍就会给cash;
     */
    public double priceAtHit() {
        double u = option.getBarrierOptionParams().getUpperBarrierPrice();
        double l = option.getBarrierOptionParams().getLowerBarrierPrice();
        int maxIteration = option.getBarrierOptionParams().getMaxIterationTimes();
        double tol = option.getBarrierOptionParams().getTolerance();
        double price = 0;
        double price1;
        double price2;
        double add1;
        double add2;
        for (int i = 1; i <= maxIteration; i++) {
            add1 = q2(i, u, l) * q3(i, u, l);
            price1 = q1(l) * (add1 + q4(u, l));
            add2 = q2(i, l, u) * q3(i, l, u);
            price2 = q1(u) * (add2 + q4(l, u));
            price = price + price1 + price2;
            if (Math.abs(add1 + add2) < tol) {
                return option.getCash() * price;
            }
        }
        return option.getCash() * price;
    }
}


/**
 * @author liangcy
 * @reference Hui (1996)
 * 如果是pay at hit, 就是触碰到障碍就给cash;
 * 如果是pay at expire, 到期根据是否触碰和敲入敲出给cash;
 */
public class DoubleBinaryBarrierOption extends BaseSingleOption implements Serializable {
    private double cash = 1.0;
    private BarrierOptionParams barrierOptionParams = new BarrierOptionParams();

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

    @Override
    public double bsm() {
        DoubleBinaryBarrierOptionCalculator calculator = new DoubleBinaryBarrierOptionCalculator();
        calculator.setOption(this);
        if (barrierOptionParams.isPayAtHit()) {
            return calculator.priceAtHit();
        }
        double outPrice = calculator.outPriceAtExpire();
        return barrierOptionParams.isIn() ? cash * getDiscountValueByRiskFreeRate() - outPrice : outPrice;
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
                cash > 0 &&
                barrierOptionParams.isValidDoubleBarrierParams(getVanillaOptionParams().getTimeRemaining());
    }

    @Override
    public String toString() {
        return "DoubleBinaryBarrierOption{" +
                "cash=" + cash +
                ", barrierOptionParams=" + barrierOptionParams.doubleBarrierToString() +
                "} " + super.toString();
    }
}
