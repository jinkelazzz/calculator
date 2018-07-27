package portfolio;

import calculator.derivatives.SingleOptionAnalysisCalculator;
import option.BaseSingleOption;
import underlying.gbm.BaseUnderlying;

import java.io.Serializable;
import java.util.List;

/**
 * @author liangcy
 */
public class SingleOptionPortfolio implements Serializable {
    private List<BaseSingleOption> optionList;
    private List<Double> optionPosition;
    private double underlyingPosition;
    private BaseUnderlying underlying;
    private static SingleOptionAnalysisCalculator calculator = new SingleOptionAnalysisCalculator();

    public List<BaseSingleOption> getOptionList() {
        return optionList;
    }

    public void setOptionList(List<BaseSingleOption> optionList) {
        this.optionList = optionList;
    }

    public List<Double> getOptionPosition() {
        return optionPosition;
    }

    public void setOptionPosition(List<Double> optionPosition) {
        this.optionPosition = optionPosition;
    }

    public double getUnderlyingPosition() {
        return underlyingPosition;
    }

    public void setUnderlyingPosition(double underlyingPosition) {
        this.underlyingPosition = underlyingPosition;
    }

    public BaseUnderlying getUnderlying() {
        return underlying;
    }

    public void setUnderlying(BaseUnderlying underlying) {
        this.underlying = underlying;
    }



    double getCost() {
        int n = optionList.size();
        double totalCost = 0;
        for (int i = 0; i < n; i++) {
            calculator.setOption(optionList.get(i));
            calculator.calculatePrice();
            if(calculator.isNormal()) {
                totalCost = totalCost + calculator.getResult() * optionPosition.get(i) * (-1);
            }
        }
        if(underlyingPosition != 0) {
            totalCost = totalCost + underlying.getSpotPrice() * underlyingPosition * (-1);
        }
        return totalCost;
    }
}
