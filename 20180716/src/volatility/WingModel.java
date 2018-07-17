package volatility;

import java.io.Serializable;

public class WingModel extends BaseVolatilitySkew implements Serializable{
    private double putCurvature;
    private double callCurvature;

    public double getPutCurvature() {
        return putCurvature;
    }

    public void setPutCurvature(double putCurvature) {
        this.putCurvature = putCurvature;
    }

    public double getCallCurvature() {
        return callCurvature;
    }

    public void setCallCurvature(double callCurvature) {
        this.callCurvature = callCurvature;
    }

    @Override
    double getVolatilityInMiddle(double logMoneyness) {
        return getVolatilityAtWing(logMoneyness);
    }

    @Override
    double getSlopeInMiddle(double logMoneyness) {
        return getSlopeAtWing(logMoneyness);
    }


    private double getVolatilityAtWing(double logMoneyness) {
        double curvature = logMoneyness > 0 ? callCurvature : putCurvature;
        double currentVol = getCurrentVolatility();
        double currentSlope = getCurrentSlope();
        return currentVol + currentSlope * logMoneyness + curvature * Math.pow(logMoneyness, 2);
    }

    private double getSlopeAtWing(double logMoneyness) {
        double curvature = logMoneyness > 0 ? callCurvature : putCurvature;
        double currentSlope = getCurrentSlope();
        return currentSlope + 2 * curvature * logMoneyness;
    }

    @Override
    double getVolatilityAtUpAffineRange(double logMoneyness) {
        getRangeParams().setuSlope(0);
        return super.getVolatilityAtUpAffineRange(logMoneyness);
    }

    @Override
    double getVolatilityAtDownAffineRange(double logMoneyness) {
        getRangeParams().setdSlope(0);
        return super.getVolatilityAtDownAffineRange(logMoneyness);
    }

}
