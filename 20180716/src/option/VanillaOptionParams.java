package option;

import calculator.utility.ConstantString;
import flanagan.math.DeepCopy;

import java.io.Serializable;

/**
 * @author liangcy
 */
public class VanillaOptionParams implements Serializable {
    private double strikePrice;
    private double timeRemaining;
    private double volatility;
    private double targetPrice = 0.0;
    private String optionType = BaseOption.OPTION_TYPE_PUT;
    private String methodName = BaseOption.OPTION_METHOD_BSM;

    public double getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(double strikePrice) {
        this.strikePrice = strikePrice;
    }

    public double getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(double timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public double getVolatility() {
        return volatility;
    }

    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public String getOptionType() {
        return optionType;
    }

    public void setOptionType(String optionType) {
        this.optionType = optionType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public double sigmaT() {
        return volatility * Math.sqrt(timeRemaining);
    }

    boolean isOptionTypeCall() {
        return BaseOption.OPTION_TYPE_CALL.equals(optionType);
    }

    @Override
    public String toString() {
        String sep = ConstantString.SEPARATOR;
        return "vanilla parameters: " +
                "option type: " + getOptionType() + sep +
                "strike price: " + getStrikePrice() + sep +
                "volatility: " + getVolatility() + sep +
                "time remaining: " + getTimeRemaining() + sep +
                "target price: " + getTargetPrice() + sep +
                "method: " + getMethodName();
    }

    public VanillaOptionParams copy() {
        return (VanillaOptionParams) DeepCopy.copy(this);
    }

    public int indexOfOptionType() {
        if (isOptionTypeCall()) {
            return 1;
        } else {
            return -1;
        }
    }


}
