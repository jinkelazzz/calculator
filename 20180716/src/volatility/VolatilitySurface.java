package volatility;

import adjusted.european.option.Heston;
import adjusted.european.option.Sabr;
import calculator.derivatives.SingleOptionAnalysisCalculator;
import calculator.utility.Interpolation;
import option.EuropeanOption;
import underlying.BaseUnderlying;

import java.io.Serializable;

/**
 * @author liangcy
 */
public class VolatilitySurface implements Serializable {
    private double[][] volSurface;
    private double[] timeList = new double[]{1.0 / 12, 2.0 / 12, 3.0 / 12, 6.0 / 12, 12.0 / 12};
    private double[] moneynessList = new double[]{0.8, 0.9, 1.0, 1.1, 1.2};
    private String interpolationMethod = Interpolation.INTERPOLATION_METHOD_NATURE;
    private String extrapolationMethod = Interpolation.EXTRAPOLATION_METHOD_NATURE;

    public double[][] getVolSurface() {
        return volSurface;
    }

    public void setVolSurface(double[][] volSurface) {
        this.volSurface = volSurface;
    }

    public double[] getTimeList() {
        return timeList;
    }

    public void setTimeList(double[] timeList) {
        this.timeList = timeList;
    }

    public double[] getMoneynessList() {
        return moneynessList;
    }

    public void setMoneynessList(double[] moneynessList) {
        this.moneynessList = moneynessList;
    }

    public VolatilitySurface() {
    }

    public String getInterpolationMethod() {
        return interpolationMethod;
    }

    public void setInterpolationMethod(String interpolationMethod) {
        this.interpolationMethod = interpolationMethod;
    }

    public String getExtrapolationMethod() {
        return extrapolationMethod;
    }

    public void setExtrapolationMethod(String extrapolationMethod) {
        this.extrapolationMethod = extrapolationMethod;
    }

    public VolatilitySurface(double volatility) {
        volSurface = new double[moneynessList.length][timeList.length];
        for (int i = 0; i < moneynessList.length; i++) {
            for (int j = 0; j < timeList.length; j++) {
                volSurface[i][j] = volatility;
            }
        }
    }

    public double getVolatility(double moneyness, double timeRemaining) {
        return Interpolation.interp2(moneynessList, timeList, volSurface, moneyness, timeRemaining,
                interpolationMethod, extrapolationMethod);
    }

    public void shiftVolatility(double diffVol) {
        for (int i = 0; i < volSurface.length; i++) {
            for (int j = 0; j < volSurface[0].length; j++) {
                volSurface[i][j] = volSurface[i][j] + diffVol;
            }
        }
    }

    private double hestonImpliedVolatility(EuropeanOption option) {
        double hestonPrice = option.heston();
        option.getVanillaOptionParams().setTargetPrice(hestonPrice);
        option.getVanillaOptionParams().setMethodName(EuropeanOption.OPTION_METHOD_BSM);
        SingleOptionAnalysisCalculator calculator = new SingleOptionAnalysisCalculator(option);
        calculator.calculateImpliedVolatility();
        return calculator.getResult();
    }

    public boolean isValidSurface() {
        if(volSurface == null) {
            return false;
        }
        int minLength = 5;
        if(timeList.length < minLength || moneynessList.length < minLength) {
            return false;
        }
        if(volSurface.length != moneynessList.length || volSurface[0].length != timeList.length) {
            return false;
        }
        return true;
    }

    public VolatilitySurface hestonVolatilitySurface(Heston heston, BaseUnderlying underlying,
                                                     double initialVolatility) {
        volSurface = new double[moneynessList.length][timeList.length];
        EuropeanOption option = new EuropeanOption();
        option.setHestonParams(heston);
        option.setUnderlying(underlying);
        double s = underlying.getSpotPrice();
        option.getVanillaOptionParams().setVolatility(initialVolatility);
        for (int i = 0; i < moneynessList.length; i++) {
            double moneyness = moneynessList[i];
            for (int j = 0; j < timeList.length; j++) {
                option.getVanillaOptionParams().setStrikePrice(s * moneyness);
                option.getVanillaOptionParams().setTargetPrice(timeList[j]);
                volSurface[i][j] = hestonImpliedVolatility(option);
            }
        }
        return this;
    }

    public VolatilitySurface sabrVolatilitySurface(Sabr sabr, BaseUnderlying underlying, double initialVolatility) {
        volSurface = new double[moneynessList.length][timeList.length];
        EuropeanOption option = new EuropeanOption();
        option.setUnderlying(underlying);
        option.getVanillaOptionParams().setVolatility(initialVolatility);
        double s = underlying.getSpotPrice();
        for (int i = 0; i < moneynessList.length; i++) {
            double moneyness = moneynessList[i];
            for (int j = 0; j < timeList.length; j++) {
                option.getVanillaOptionParams().setStrikePrice(s * moneyness);
                option.getVanillaOptionParams().setTargetPrice(timeList[j]);
                sabr.setOption(option);
                volSurface[i][j] = sabr.sabrVolatility();
            }
        }
        return this;
    }

}
