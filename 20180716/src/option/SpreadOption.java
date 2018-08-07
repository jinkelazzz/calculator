package option;

import calculator.utility.CalculateUtil;
import flanagan.math.DeepCopy;
import flanagan.math.Maximisation;
import flanagan.math.MaximisationFunction;

import java.io.Serializable;


class SpreadOptionCalculator implements MaximisationFunction {
    private SpreadOption spreadOption;

    void setSpreadOption(SpreadOption spreadOption) {
        this.spreadOption = spreadOption;
    }

    private double bsOptimizePrice(double a, double b) {
        double vol1 = spreadOption.getOption1().getVanillaOptionParams().getVolatility();
        double vol2 = spreadOption.getOption2().getVanillaOptionParams().getVolatility();
        double t = spreadOption.getOption1().getVanillaOptionParams().getTimeRemaining();
        double k = spreadOption.getOption1().getVanillaOptionParams().getStrikePrice();
        double f1 = spreadOption.getOption1().getUnderlying().getFutureValue(t);
        double f2 = spreadOption.getOption2().getUnderlying().getFutureValue(t);
        double bVol2 = b * vol2;
        double vol = Math.sqrt(vol1 * vol1 - 2 * spreadOption.getRho() * vol1 * bVol2 + bVol2 * bVol2);
        double sigmaT = vol * Math.sqrt(t);
        double logMoneyness = Math.log(f1 / a);
        double d1 = (logMoneyness + (vol1 * vol1 / 2 - spreadOption.getRho() * vol1 * bVol2 + bVol2 * bVol2 / 2) * t)
                / sigmaT;
        double d2 = (logMoneyness + (-vol1 * vol1 / 2 + spreadOption.getRho() * vol1 * vol2
                + bVol2 * bVol2 / 2 - bVol2 * vol2) * t) / sigmaT;
        double d3 = (logMoneyness + (-vol1 * vol1 / 2 + bVol2 * bVol2 / 2) * t) / sigmaT;
        double callPrice = spreadOption.getOption1().getDiscountValueByRiskFreeRate()
                * (f1 * CalculateUtil.normalCDF(d1) - f2 * CalculateUtil.normalCDF(d2) - k * CalculateUtil.normalCDF(d3));
        if (spreadOption.getOption1().getVanillaOptionParams().isOptionTypeCall()) {
            return callPrice;
        } else {
            return callPrice - spreadOption.getOption1().getDiscountValueByRiskFreeRate() * (f1 - f2 - k);
        }
    }

    @Override
    public double function(double[] params) {
        return bsOptimizePrice(params[0], params[1]);
    }

    double bsmPrice() {
        double k = spreadOption.getOption1().getVanillaOptionParams().getStrikePrice();
        double t = spreadOption.getOption1().getVanillaOptionParams().getTimeRemaining();
        double f2 = spreadOption.getOption2().getUnderlying().getFutureValue(t);
        double a = f2 + k;
        double b = f2 / (f2 + k);
        Maximisation maximisation = new Maximisation();
        double[] start = {a, b};
        double[] step = {0.1, 0.1};
        maximisation.nelderMead(this, start, step);
        return maximisation.getMaximum();
    }
}

/**
 * @author liangcy
 */
public class SpreadOption extends BaseDoubleOption implements Serializable {

    @Override
    public void setOption2(BaseSingleOption option) {
        super.setOption2(option);
        // 确保到期时间、执行价、期权类型和option1一致;
        getOption2().getVanillaOptionParams().setStrikePrice(getOption1().getVanillaOptionParams().getStrikePrice());
        getOption2().getVanillaOptionParams().setTimeRemaining(getOption1().getVanillaOptionParams().getTimeRemaining());
        getOption2().getVanillaOptionParams().setOptionType(getOption1().getVanillaOptionParams().getOptionType());
    }

    private SpreadOption transform() {
        SpreadOption option = (SpreadOption) DeepCopy.copy(this);
        option.swapOption();
        option.getOption1().swapCallPut();
        double k = getOption1().getVanillaOptionParams().getStrikePrice();
        option.getOption1().getVanillaOptionParams().setStrikePrice(-k);
        return option;
    }

    @Override
    public double bsm() {
        double k = getOption1().getVanillaOptionParams().getStrikePrice();
        SpreadOptionCalculator calculator = new SpreadOptionCalculator();
        if (k < 0) {
            calculator.setSpreadOption(transform());
        } else {
            calculator.setSpreadOption(this);
        }
        return calculator.bsmPrice();
    }


    @Override
    public String toString() {
        return "SpreadOption{" +
                "option1=" + getOption1() +
                ", option2=" + getOption2() +
                ", rho=" + getRho() +
                "} ";
    }
}
