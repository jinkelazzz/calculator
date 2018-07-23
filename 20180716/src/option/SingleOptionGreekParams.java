package option;

import calculator.utility.ConstantNumber;

import java.io.Serializable;

/**
 * 设置Greeks计算精度
 * 默认值全部为万分之一
 *
 * @author liangcy
 */
public class SingleOptionGreekParams implements Serializable {

    private double underlyingPricePrecision = 1e-4;
    private double volatilityPrecision = 1e-4;
    private double timeRemainingPrecision = 1e-4;
    private double interestRatePrecision = 1e-4;
    private double eps = ConstantNumber.EPS * 2;



    public double getUnderlyingPricePrecision() {
        return underlyingPricePrecision;
    }

    public void setUnderlyingPricePrecision(double underlyingPricePrecision) {
        this.underlyingPricePrecision = Math.max(eps, underlyingPricePrecision);
    }

    public double getVolatilityPrecision() {
        return volatilityPrecision;
    }

    public void setVolatilityPrecision(double volatilityPrecision) {
        this.volatilityPrecision = Math.max(eps, volatilityPrecision);
    }

    public double getTimeRemainingPrecision() {
        return timeRemainingPrecision;
    }

    public void setTimeRemainingPrecision(double timeRemainingPrecision) {
        this.timeRemainingPrecision = Math.max(eps, timeRemainingPrecision);
    }

    public double getInterestRatePrecision() {
        return interestRatePrecision;
    }

    public void setInterestRatePrecision(double interestRatePrecision) {
        this.interestRatePrecision = Math.max(eps, interestRatePrecision);
    }


}
