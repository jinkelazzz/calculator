package option;

import java.io.Serializable;
import java.util.Objects;

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

    public boolean isOptionTypeCall() {
        return BaseOption.OPTION_TYPE_CALL.equals(optionType);
    }

    /**
     *
     * @return call:1, put:-1;
     */
    public int indexOfOptionType() {
        return isOptionTypeCall() ? 1 : -1;
    }

    void swapCallPut() {
        if(isOptionTypeCall()) {
            setOptionType(BaseOption.OPTION_TYPE_PUT);
        } else {
            setOptionType(BaseOption.OPTION_TYPE_CALL);
        }
    }

    public boolean isValid() {
        return timeRemaining > 0 &&
                volatility > 0 &&
                targetPrice >= 0 &&
                (BaseOption.OPTION_TYPE_CALL.equals(optionType) || BaseOption.OPTION_TYPE_PUT.equals(optionType));
    }

    @Override
    public String toString() {
        return "VanillaOptionParams{" +
                "strikePrice=" + strikePrice +
                ", timeRemaining=" + timeRemaining +
                ", volatility=" + volatility +
                ", targetPrice=" + targetPrice +
                ", optionType='" + optionType + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
