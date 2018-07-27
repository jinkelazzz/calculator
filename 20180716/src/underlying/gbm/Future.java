package underlying.gbm;

import java.io.Serializable;

/**
 * @author liangcy
 */
public class Future extends BaseUnderlying implements Serializable {
    public Future() {

    }

    public Future(double spot, double riskFreeRate) {
        this.setSpotPrice(spot);
        this.setRiskFreeRate(riskFreeRate);
    }

    private void doNothing() {
    }

    @Override
    public void setDividendRate(double dividendRate) {
        doNothing();
    }

    @Override
    public double getDividendRate() {
        return getRiskFreeRate();
    }

    @Override
    public void setRiskFreeRate(double riskFreeRate) {
        super.setRiskFreeRate(riskFreeRate);
        super.setDividendRate(riskFreeRate);
    }

    @Override
    public String toString() {
        return "Future{" +
                "spotPrice=" + getSpotPrice() +
                ", riskFreeRate=" + getRiskFreeRate() +
                '}';
    }
}
