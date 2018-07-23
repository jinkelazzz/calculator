package underlying;

import calculator.utility.ConstantString;

import java.io.Serializable;

/**
 * 这里的underlying要服从几何布朗运动;例如现货、期货等
 *
 * @author liangcy
 */
public abstract class BaseUnderlying implements Serializable {
    private double spotPrice;
    private double riskFreeRate = 0.0;
    private double dividendRate = 0.0;
    String sep = ConstantString.SEPARATOR;

    public BaseUnderlying() {
    }

    public double getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(double spotPrice) {
        this.spotPrice = spotPrice;
    }

    public double getRiskFreeRate() {
        return riskFreeRate;
    }

    public void setRiskFreeRate(double riskFreeRate) {
        this.riskFreeRate = riskFreeRate;
    }

    public double getDividendRate() {
        return dividendRate;
    }

    public void setDividendRate(double dividendRate) {
        this.dividendRate = dividendRate;
    }

    public double getFutureValue(double timeRemaining) {
        double r = this.getRiskFreeRate();
        double q = this.getDividendRate();
        double s = this.getSpotPrice();
        return s * Math.exp((r - q) * timeRemaining);
    }

    public double getPresentValue(double timeRemaining) {
        double r = this.getRiskFreeRate();
        double q = this.getDividendRate();
        double s = this.getSpotPrice();
        return s * Math.exp(-(r - q) * timeRemaining);
    }

    public double getCostOfCarry() {
        return getRiskFreeRate() - getDividendRate();
    }

    public void swapRQ() {
        double r = riskFreeRate;
        double q = dividendRate;
        setRiskFreeRate(q);
        setDividendRate(r);
    }

    /**
     * 打印参数
     * @return 打印参数
     */
    @Override
    public abstract String toString();

    public boolean isValid() {
        return spotPrice > 0;
    }

}
