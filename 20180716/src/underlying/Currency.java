package underlying;

import java.io.Serializable;

/**
 * @author liangcy
 */
public class Currency extends BaseUnderlying implements Serializable {

    public Currency() {
    }

    @Override
    public String toString() {
        return "Underlying type: currency" + sep +
                "spot price:" + getSpotPrice() + sep +
                "native risk-free rate:" + getRiskFreeRate() + sep +
                "foreign risk-free rate:" + getDividendRate();
    }

    public Currency reverse() {
        Currency currency = new Currency();
        currency.setSpotPrice(1.0 / getSpotPrice());
        double r = getRiskFreeRate();
        currency.setRiskFreeRate(getDividendRate());
        currency.setDividendRate(r);
        return currency;
    }
}
