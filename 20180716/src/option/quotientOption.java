package option;

import calculator.utility.CalculateUtil;

import java.io.Serializable;

/**
 * @author liangcy
 * 比价期权
 * call: max(s1 / s2 - k, 0); put: max(k - s1 / s2, 0)
 * option type/strike price/time remaining 均以 option1 为准
 */
public class quotientOption extends BaseDoubleOption implements Serializable {
    @Override
    public double bsm() {
        double[] d = getD();
        double discount = getOption1().getDiscountValueByRiskFreeRate();
        double k = getOption1().getVanillaOptionParams().getStrikePrice();
        return getOption1().getVanillaOptionParams().isOptionTypeCall() ?
                discount * (forwardPrice() * CalculateUtil.normalCDF(d[0]) - k * CalculateUtil.normalCDF(d[1])) :
                discount * (k * CalculateUtil.normalCDF(-d[1]) - forwardPrice() * CalculateUtil.normalCDF(-d[0]));
    }

    private double forwardPrice() {
        double quotient = getOption1().getForwardPrice() / getOption2().getForwardPrice();
        double vol1 = getOption1().getVanillaOptionParams().getVolatility();
        double vol2 = getOption2().getVanillaOptionParams().getVolatility();
        double t = getOption1().getVanillaOptionParams().getTimeRemaining();
        double mult = Math.exp((vol2 * vol2 - getRho() * vol1 * vol2) * t);
        return quotient * mult;
    }

    private double getVol() {
        double vol1 = getOption1().getVanillaOptionParams().getVolatility();
        double vol2 = getOption2().getVanillaOptionParams().getVolatility();
        return Math.sqrt(vol1 * vol1 + vol2 * vol2 - 2 * getRho() * vol1 * vol2);
    }

    private double[] getD() {
        double t = getOption1().getVanillaOptionParams().getTimeRemaining();
        double vol = getVol();
        double[] d = new double[2];
        d[0] = (Math.log(forwardPrice() / getOption1().getVanillaOptionParams().getStrikePrice()) +
                vol * vol * t / 2) / (vol * Math.sqrt(t));
        d[1] = d[0] - vol * Math.sqrt(t);
        return d;
    }


    @Override
    public String toString() {
        return "quotientOption{" +
                "option1=" + getOption1() +
                ", option2=" + getOption2() +
                ", rho=" + getRho() +
                '}';
    }
}
