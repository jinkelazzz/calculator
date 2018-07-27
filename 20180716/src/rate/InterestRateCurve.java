package rate;

import calculator.utility.Interpolation;

/**
 * 利率曲线类
 * 利率的期限结构
 *
 * @author liangcy
 */
public class InterestRateCurve {

    private double[] interestRatePoints;
    private double[] timePoints;

    public double[] getInterestRatePoints() {
        return interestRatePoints;
    }

    public void setInterestRatePoints(double[] interestRatePoints) {
        this.interestRatePoints = interestRatePoints;
    }

    public double[] getTimePoints() {
        return timePoints;
    }

    public void setTimePoints(double[] timePoints) {
        this.timePoints = timePoints;
    }

    /**
     * 插值计算任意时间点的利率
     *
     * @param time 时间点
     * @return
     */
    public double getInterestRate(double time) {
        return Interpolation.interp1(timePoints, interestRatePoints, time,
                Interpolation.INTERPOLATION_METHOD_NATURE, Interpolation.EXTRAPOLATION_METHOD_NATURE);
    }

    public double getDiscountValue(double time) {
        return Math.exp(-getInterestRate(time) * time);
    }
}
