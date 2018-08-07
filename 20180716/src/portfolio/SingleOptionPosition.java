package portfolio;

import calculator.derivatives.SingleOptionAnalysisCalculator;
import option.BaseSingleOption;

/**
 * @author liangcy
 */
public class SingleOptionPosition {
    private BaseSingleOption option;
    private double openInterest = 0;
    private String underlyingName;

    private static SingleOptionAnalysisCalculator calculator = new SingleOptionAnalysisCalculator();

    public BaseSingleOption getOption() {
        return option;
    }

    public void setOption(BaseSingleOption option) {
        this.option = option;
    }

    public double getOpenInterest() {
        return openInterest;
    }

    public void setOpenInterest(double openInterest) {
        this.openInterest = openInterest;
    }

    public String getUnderlyingName() {
        return underlyingName;
    }

    public void setUnderlyingName(String underlyingName) {
        this.underlyingName = underlyingName;
    }

    public void closePosition() {
        this.openInterest = 0;
    }

    public void addLongPosition(double amount) {
        this.openInterest = this.openInterest + amount;
    }

    public void addShortPosition(double amount) {
        this.openInterest = this.openInterest - amount;
    }

    public double getValue() {
        calculator.setOption(option);
        calculator.calculatePrice();
        if(calculator.isNormal()) {
            return -openInterest * calculator.getResult();
        }
        return 0;
    }
}
