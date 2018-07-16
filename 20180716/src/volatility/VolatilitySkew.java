package volatility;

import calculator.derivatives.SingleOptionAnalysisCalculator;
import option.BaseSingleOption;
import underlying.BaseUnderlying;

import java.io.Serializable;
import java.util.List;

/**
 * @author liangcy
 */
public class VolatilitySkew implements Serializable{
    private BaseUnderlying underlying;
    private List<BaseSingleOption> optionList;
    private static final SingleOptionAnalysisCalculator CALCULATOR = new SingleOptionAnalysisCalculator();

    public BaseUnderlying getUnderlying() {
        return underlying;
    }

    public void setUnderlying(BaseUnderlying underlying) {
        this.underlying = underlying;
    }

    public List<BaseSingleOption> getOptionList() {
        return optionList;
    }

    public void setOptionList(List<BaseSingleOption> optionList) {
        this.optionList = optionList;
    }

    public void updateImpliedVolatility() {
        for (BaseSingleOption option : optionList) {
            CALCULATOR.setOption(option);
            CALCULATOR.calculateImpliedVolatility();
        }
    }
}
