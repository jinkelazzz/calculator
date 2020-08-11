package option;

import calculator.utility.MonteCarlo;

/**
 * @author liangcy
 */
public class AutocallOption extends BaseSingleOption{
    private double knockInPrice;
    private double knockOutPrice;
    private double couponRate;
    private int[] observeDays;

    private double refPrice;

    private double decayRate = 0;
    private double floor = 0;
    private double discountRate = 0;
    private double refundRate = 0;
    private int tradingDays;

    public double getKnockInPrice() {
        return knockInPrice;
    }

    public void setKnockInPrice(double knockInPrice) {
        this.knockInPrice = knockInPrice;
    }

    public double getKnockOutPrice() {
        return knockOutPrice;
    }

    public void setKnockOutPrice(double knockOutPrice) {
        this.knockOutPrice = knockOutPrice;
    }

    public double getCouponRate() {
        return couponRate;
    }

    public void setCouponRate(double couponRate) {
        this.couponRate = couponRate;
    }

    public int[] getObserveDays() {
        return observeDays;
    }

    public void setObserveDays(int[] observeDays) {
        this.observeDays = observeDays;
    }

    public double getRefPrice() {
        return refPrice;
    }

    public void setRefPrice(double refPrice) {
        this.refPrice = refPrice;
    }

    public double getDecayRate() {
        return decayRate;
    }

    public void setDecayRate(double decayRate) {
        this.decayRate = decayRate;
    }

    public double getFloor() {
        return floor;
    }

    public void setFloor(double floor) {
        this.floor = floor;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(double discountRate) {
        this.discountRate = discountRate;
    }

    public double getRefundRate() {
        return refundRate;
    }

    public void setRefundRate(double refundRate) {
        this.refundRate = refundRate;
    }

    public int getTradingDays() {
        return tradingDays;
    }

    public void setTradingDays(int tradingDays) {
        this.tradingDays = tradingDays;
    }

    @Override
    public boolean hasMonteCarloMethod() {
        return true;
    }

    private double[] formatToAnnualDays() {
        double[] annualDays = new double[observeDays.length];
        for (int i = 0; i < observeDays.length; i++) {
            annualDays[i] = observeDays[i] * 1.0 / tradingDays;
        }
        return annualDays;
    }

    @Override
    public double monteCarloPrice(double[] pricePath) {
        int n = pricePath.length;
        int knockOutFlag = 0;
        int knockInFlag = 0;
        double payOff = 0;
        double couponPaidTime = 0;
        double k = knockOutPrice;
        double[] timePoints = MonteCarlo.getTimePoints(getVanillaOptionParams().getTimeRemaining(), pricePath);
        for(int i = 0; i < n; i++) {
            if(knockOutFlag == 0) {
                if(pricePath[i] < knockInPrice) {
                    knockInFlag = 1;
                }

                if(isKnockOutDay(timePoints[i], formatToAnnualDays())) {
                    if(pricePath[i] > k) {
                        couponPaidTime = timePoints[i] * tradingDays;
                        knockOutFlag = 1;
                    } else {
                        k = k - refPrice * decayRate;
                    }
                }
            }
        }

        if(knockOutFlag != 0) {
            payOff = couponRate * couponPaidTime / tradingDays;
        } else if(knockInFlag == 0) {
            couponPaidTime = observeDays[observeDays.length - 1];
            payOff = couponRate * couponPaidTime / tradingDays;
        } else {
            couponPaidTime = observeDays[observeDays.length - 1];
            payOff = Math.max(floor - 1, Math.min(0, pricePath[n - 1] / refPrice - 1));
        }


        payOff = payOff - refundRate * couponPaidTime / tradingDays;

        payOff = payOff * Math.exp(-discountRate * couponPaidTime / tradingDays);
        System.out.println(payOff);
        return payOff;
    }

    private boolean isKnockOutDay(double day, double[] observeDays) {
        double eps = 1.0 / tradingDays;
        for (double observeDay : observeDays) {
            if(Math.abs(day - observeDay) < eps) {
                return true;
            }
        }
        return false;
    }

    /**
     * 最简单的解析解统称 bsm;
     *
     * @return option price (每个option都至少应有一个解析解);
     */
    @Override
    public double bsm() {
        return 0;
    }

    /**
     * 打印参数
     *
     * @return 参数
     */
    @Override
    public String toString() {
        return null;
    }


}
