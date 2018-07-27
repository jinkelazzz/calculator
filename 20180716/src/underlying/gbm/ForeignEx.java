package underlying.gbm;

import java.io.Serializable;

/**
 * @author liangcy
 */
public class ForeignEx extends BaseUnderlying implements Serializable {

    public ForeignEx() {
    }

    public ForeignEx reverse() {
        ForeignEx currency = new ForeignEx();
        currency.setSpotPrice(1.0 / getSpotPrice());
        currency.setRiskFreeRate(getDividendRate());
        currency.setDividendRate(getRiskFreeRate());
        return currency;
    }

    @Override
    public String toString() {
        return "ForeignEx{" +
                "spotPrice=" + getSpotPrice() +
                ", riskFreeRate=" + getRiskFreeRate() +
                ", dividendRate=" + getDividendRate() +
                '}';
    }
}
