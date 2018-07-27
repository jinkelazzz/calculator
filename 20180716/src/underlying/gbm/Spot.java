package underlying.gbm;

import java.io.Serializable;

/**
 * 现货类 也可以是指数等
 *
 * @author liangcy
 */
public class Spot extends BaseUnderlying implements Serializable {
    public Spot() {

    }

    public Spot(double spotPrice, double riskFreeRate) {
        this.setSpotPrice(spotPrice);
        this.setRiskFreeRate(riskFreeRate);
    }

    public Spot(double spotPrice, double riskFreeRate, double dividendRate) {
        this.setSpotPrice(spotPrice);
        this.setRiskFreeRate(riskFreeRate);
        this.setDividendRate(dividendRate);
    }


    @Override
    public String toString() {
        return "Spot{" +
                "spotPrice=" + getSpotPrice() +
                ", riskFreeRate=" + getRiskFreeRate() +
                ", dividendRate=" + getDividendRate() +
                '}';
    }
}
