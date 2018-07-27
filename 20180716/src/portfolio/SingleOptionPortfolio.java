package portfolio;

import calculator.derivatives.SingleOptionAnalysisCalculator;
import flanagan.math.DeepCopy;
import option.BaseSingleOption;
import underlying.gbm.BaseUnderlying;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author liangcy
 */
public class SingleOptionPortfolio implements Serializable {
    private HashMap<BaseUnderlying, HashMap<BaseSingleOption, Integer>> optionPortfolio;
    private HashMap<BaseUnderlying, Integer> underlyingPortfolio;

    public HashMap<BaseUnderlying, HashMap<BaseSingleOption, Integer>> getOptionPortfolio() {
        return optionPortfolio;
    }

    public void setOptionPortfolio(HashMap<BaseUnderlying, HashMap<BaseSingleOption, Integer>> optionPortfolio) {
        this.optionPortfolio = optionPortfolio;
    }

    public HashMap<BaseUnderlying, Integer> getUnderlyingPortfolio() {
        return underlyingPortfolio;
    }

    public void setUnderlyingPortfolio(HashMap<BaseUnderlying, Integer> underlyingPortfolio) {
        this.underlyingPortfolio = underlyingPortfolio;
    }

    private double getSingleOptionPortfolioCost(HashMap<BaseSingleOption, Integer> portfolio) {
        double[] cost = {0};
        SingleOptionAnalysisCalculator calculator = new SingleOptionAnalysisCalculator();
        portfolio.forEach((BaseSingleOption key, Integer value) -> {
            calculator.setOption(key);
            calculator.calculatePrice();
            if(calculator.isNormal()) {
                //多仓的cost为负, 代表花费
                cost[0] = cost[0] + calculator.getResult() * value * (-1);
            }
        });
        return cost[0];
    }

    private double getPortfolioCost() {
        double[] cost = {0};
        optionPortfolio.forEach((BaseUnderlying key, HashMap<BaseSingleOption, Integer> value) -> {
            double sum = getSingleOptionPortfolioCost(value);
            if(underlyingPortfolio.containsKey(key)) {
                sum = sum + key.getSpotPrice() * underlyingPortfolio.get(key) * (-1);
            }
           cost[0] = cost[0] + sum;
        });
        return cost[0];
    }

    private void reverseUnderlyingPortfolio() {
        underlyingPortfolio.forEach((key, value) -> underlyingPortfolio.put(key, -value));
    }

    private void reverseSingleOptionPortfolio(HashMap<BaseSingleOption, Integer> portfolio) {
        portfolio.forEach((key, value) -> portfolio.put(key, -value));
    }
    private void reverseOptionPortfolio() {
        optionPortfolio.forEach((key, value) -> reverseSingleOptionPortfolio(value));
    }

    public SingleOptionPortfolio reversePortfolio() {
        SingleOptionPortfolio portfolioCopy = (SingleOptionPortfolio) DeepCopy.copy(this);
        portfolioCopy.reverseOptionPortfolio();
        portfolioCopy.reverseUnderlyingPortfolio();
        return portfolioCopy;
    }

    private void updateOptionUnderlying(HashMap<BaseSingleOption, Integer> optionList, BaseUnderlying underlying) {
        optionList.forEach((key, value) -> key.setUnderlying(underlying));
    }

}
