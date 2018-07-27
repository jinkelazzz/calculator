package calculator.portfolio;

import calculator.utility.CalculateUtil;
import portfolio.SingleOptionPortfolio;
import underlying.gbm.BaseUnderlying;
import volatility.HistoryPrice;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author liangcy
 */
public class VarHistoryCalculator extends BaseVarCalculator implements Serializable {

    private SingleOptionPortfolio portfolio;
    private HashMap<BaseUnderlying, HistoryPrice> historyPriceHashMap;

    public SingleOptionPortfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(SingleOptionPortfolio portfolio) {
        this.portfolio = portfolio;
    }

    public HashMap<BaseUnderlying, HistoryPrice> getHistoryPriceHashMap() {
        return historyPriceHashMap;
    }

    public void setHistoryPriceHashMap(HashMap<BaseUnderlying, HistoryPrice> historyPriceHashMap) {
        this.historyPriceHashMap = historyPriceHashMap;
    }

    private HashMap<BaseUnderlying, double[]> getHistoryCloseReturn() {
        HashMap<BaseUnderlying, double[]> historyReturn = new HashMap<>(historyPriceHashMap.size());
        historyPriceHashMap.forEach((key, value) ->
                historyReturn.put(key, CalculateUtil.getLogReturn(value.getClose())));
        return historyReturn;
    }

}
