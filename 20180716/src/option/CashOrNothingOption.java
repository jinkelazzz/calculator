package option;

import calculator.utility.CalculateUtil;

/**
 * 现金或空手期权
 *
 * @author liangcy
 */
public class CashOrNothingOption extends BaseSingleOption {
    private double cash = 1.0;

    public double getCash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    /**
     * 判断是否行权
     *
     * @param s 期末价格
     * @return 1(行权), 0(不行权);
     */
    private double indexOfExercise(double s) {
        double k = getVanillaOptionParams().getStrikePrice();
        if (getVanillaOptionParams().isOptionTypeCall()) {
            return Math.max(Math.signum(s - k), 0);
        } else {
            return Math.max(Math.signum(k - s), 0);
        }
    }

    @Override
    public boolean hasMonteCarloMethod() {
        return true;
    }

    @Override
    public double monteCarloPrice(double[] pricePath) {
        double st = pricePath[pricePath.length - 1];
        return cash * indexOfExercise(st) * getDiscountValueByRiskFreeRate();
    }

    @Override
    public double bsm() {
        double d2 = new EuropeanOption(this).d2();
        return getDiscountValueByRiskFreeRate() * cash *
                CalculateUtil.normalCDF(getVanillaOptionParams().indexOfOptionType() * d2);
    }

    @Override
    public boolean hasFiniteDifferenceMethod() {
        return true;
    }

    @Override
    public double[] finiteDifferencePrice(double[] spotPrice) {
        int n = spotPrice.length;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = cash * indexOfExercise(spotPrice[i]);
        }
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + sep +
                "cash" + getCash();
    }
}
