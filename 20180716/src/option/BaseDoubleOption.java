package option;

import flanagan.math.DeepCopy;

import java.io.Serializable;

/**
 * @author liangcy
 */
public abstract class BaseDoubleOption extends BaseOption implements Serializable {
    private BaseSingleOption option1;
    private BaseSingleOption option2;
    /**
     * correlation of two underlyings;
     */
    private double rho;

    public void setOption1(BaseSingleOption option1) {
        this.option1 = option1;
    }

    public void setOption2(BaseSingleOption option2) {
        this.option2 = option2;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    public BaseSingleOption getOption1() {
        return option1;
    }

    public BaseSingleOption getOption2() {
        return option2;
    }

    public double getRho() {
        return rho;
    }

    void swapOption() {
        BaseSingleOption optionTmp = (BaseSingleOption) DeepCopy.copy(option2);
        option2 = (BaseSingleOption) DeepCopy.copy(option1);
        option1 = (BaseSingleOption) DeepCopy.copy(optionTmp);
    }

}
