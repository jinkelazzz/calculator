package adjusted.european.option;

import calculator.utility.CalculateUtil;
import option.EuropeanOption;

import java.io.Serializable;

/**
 * @author liangcy
 */
public class CorradoSu implements Serializable {
    private double skew = 0;
    private double kurtosis = 3;
    private EuropeanOption option;

    public double getSkew() {
        return skew;
    }

    public void setSkew(double skew) {
        this.skew = skew;
    }

    public double getKurtosis() {
        return kurtosis;
    }

    public void setKurtosis(double kurtosis) {
        this.kurtosis = kurtosis;
    }

    public EuropeanOption getOption() {
        return option;
    }

    public void setOption(EuropeanOption option) {
        this.option = option;
    }

    public double corradoSuAddition() {
        double sigmaT = option.getVanillaOptionParams().sigmaT();
        double w = skew * Math.pow(sigmaT, 3) / 6 + kurtosis * Math.pow(sigmaT, 4) / 24;
        double d = option.d1() - Math.log(1 + w) / sigmaT;
        double s = option.getUnderlying().getSpotPrice();
        double q3 = s * sigmaT * (2 * sigmaT - d) * CalculateUtil.normalPDF(d) / (6 * (1 + w));
        double q4 = s * sigmaT * (d * d - 3 * d * sigmaT + 3 * sigmaT * sigmaT - 1) *
                CalculateUtil.normalPDF(d) / (24 * (1 + w));
        return skew * q3 + (kurtosis - 3) * q4;
    }
}
