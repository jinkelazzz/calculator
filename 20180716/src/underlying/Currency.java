package underlying;

import flanagan.math.DeepCopy;

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
        Currency currency = (Currency) DeepCopy.copy(this);
        currency.setSpotPrice(1.0 / getSpotPrice());
        currency.swapRQ();
        return currency;
    }
}
