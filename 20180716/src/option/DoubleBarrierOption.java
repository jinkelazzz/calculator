package option;

import calculator.utility.CalculateUtil;
import calculator.utility.MonteCarlo;
import flanagan.math.VectorMaths;

import java.io.Serializable;
import java.util.Objects;


class DoubleBarrierCalculator {
    private DoubleBarrierOption option;

    public void setOption(DoubleBarrierOption option) {
        this.option = option;
    }

    private double c1(int i) {
        double vol = option.getVanillaOptionParams().getVolatility();
        double cur1 = option.getBarrierOptionParams().getLowerCurve();
        double cur2 = option.getBarrierOptionParams().getUpperCurve();
        return 2 * (option.getCostOfCarry() - cur1 - i * (cur2 - cur1)) / (vol * vol) + 1;
    }

    private double c2(int i) {
        double vol = option.getVanillaOptionParams().getVolatility();
        double cur1 = option.getBarrierOptionParams().getLowerCurve();
        double cur2 = option.getBarrierOptionParams().getUpperCurve();
        return 2 * i * (cur2 - cur1) * (vol * vol);
    }

    private double c3(int i) {
        double vol = option.getVanillaOptionParams().getVolatility();
        double cur1 = option.getBarrierOptionParams().getLowerCurve();
        double cur2 = option.getBarrierOptionParams().getUpperCurve();
        return 2 * (option.getCostOfCarry() - cur1 + i * (cur2 - cur1)) / (vol * vol) + 1;
    }

    private double upperCurvePrice() {
        double cur = option.getBarrierOptionParams().getUpperCurve();
        double price = option.getBarrierOptionParams().getUpperBarrierPrice();
        double t = option.getVanillaOptionParams().getTimeRemaining();
        return price * Math.exp(cur * t);
    }

    private double lowerCurvePrice() {
        double cur = option.getBarrierOptionParams().getLowerCurve();
        double price = option.getBarrierOptionParams().getLowerBarrierPrice();
        double t = option.getVanillaOptionParams().getTimeRemaining();
        return price * Math.exp(cur * t);
    }

    private double addition() {
        double vol = option.getVanillaOptionParams().getVolatility();
        double t = option.getVanillaOptionParams().getTimeRemaining();
        return (option.getCostOfCarry() + vol * vol / 2) * t;
    }

    private double d(double logPrice) {
        return (logPrice + addition()) / option.getVanillaOptionParams().sigmaT();
    }

    /**
     * @param index 1, 2, 3, 4;
     * @param i     iteration number;
     * @return
     */
    private double[] multiOfLogPrice(int index, int i) {
        // index of s, k, upper barrier, lower barrier, barrier curve;
        double[] indexes = new double[5];
        // index of s; (1, 2, 3, 4)->(1, 1, -1, -1)
        indexes[0] = (index <= 2 ? 1 : -1);
        // index of k; (1, 2, 3, 4)->(-1, 0, -1, 0) or (0, -1, 0, -1)
        if (option.getVanillaOptionParams().isOptionTypeCall()) {
            indexes[1] = (index % 2 == 0 ? 0 : -1);
        } else {
            indexes[1] = (index % 2 == 0 ? -1 : 0);
        }
        // index of upper barrier price; (1, 2, 3, 4)->(2i, 2i, -2i, -2i)
        indexes[2] = (index <= 2 ? (2 * i) : (-2 * i));
        // index of lower barrier price; (1, 2, 3, 4)->(-2i, -2i, 2i+2, 2i+2)
        indexes[3] = (index <= 2 ? (-2 * i) : (2 * i + 2));
        // index of curve barrier price; (1, 2, 3, 4)->(0, -1, 0, -1) or (-1, 0, -1, 0)
        if (option.getVanillaOptionParams().isOptionTypeCall()) {
            indexes[4] = (index % 2 == 0 ? -1 : 0);
        } else {
            indexes[4] = (index % 2 == 0 ? 0 : -1);
        }
        return indexes;
    }

    private double[] logPrice() {
        double s = option.getUnderlying().getSpotPrice();
        double k = option.getVanillaOptionParams().getStrikePrice();
        double u = option.getBarrierOptionParams().getUpperBarrierPrice();
        double l = option.getBarrierOptionParams().getLowerBarrierPrice();
        double f = upperCurvePrice();
        double g = lowerCurvePrice();
        if (option.getVanillaOptionParams().isOptionTypeCall()) {
            return new double[]{Math.log(s), Math.log(k), Math.log(u), Math.log(l), Math.log(f)};
        } else {
            return new double[]{Math.log(s), Math.log(k), Math.log(u), Math.log(l), Math.log(g)};
        }
    }

    private double x(int index, int i) {
        VectorMaths priceList = new VectorMaths(logPrice());
        VectorMaths indexes = new VectorMaths(multiOfLogPrice(index, i));
        double logPrice = VectorMaths.dot(priceList, indexes);
        return d(logPrice);
    }

    private double part1(int i) {
        double s = option.getUnderlying().getSpotPrice();
        double u = option.getBarrierOptionParams().getUpperBarrierPrice();
        double l = option.getBarrierOptionParams().getLowerBarrierPrice();

        double p1 = Math.pow(u / l, i);
        double p2 = l / s;
        double p3 = p2 / p1;

        double q1 = Math.pow(p1, c1(i)) * Math.pow(p2, c2(i))
                * (CalculateUtil.normalCDF(x(1, i)) - CalculateUtil.normalCDF(x(2, i)));
        double q2 = Math.pow(p3, c3(i)) * (CalculateUtil.normalCDF(x(3, i)) - CalculateUtil.normalCDF(x(4, i)));

        return s * option.getDiscountValueByDividendRate() * (q1 - q2);
    }

    private double part2(int i) {
        double s = option.getUnderlying().getSpotPrice();
        double k = option.getVanillaOptionParams().getStrikePrice();
        double u = option.getBarrierOptionParams().getUpperBarrierPrice();
        double l = option.getBarrierOptionParams().getLowerBarrierPrice();

        double p1 = Math.pow(u / l, i);
        double p2 = l / s;
        double p3 = p2 / p1;
        double sigmaT = option.getVanillaOptionParams().sigmaT();

        double q1 = Math.pow(p1, c1(i) - 2) * Math.pow(p2, c2(i))
                * (CalculateUtil.normalCDF(x(1, i) - sigmaT) - CalculateUtil.normalCDF(x(2, i) - sigmaT));
        double q2 = Math.pow(p3, c3(i) - 2)
                * (CalculateUtil.normalCDF(x(3, i) - sigmaT) - CalculateUtil.normalCDF(x(4, i) - sigmaT));

        return -k * option.getDiscountValueByRiskFreeRate() * (q1 - q2);
    }

    /**
     * @return 敲出的价格
     */
    public double outPrice() {
        int indexOfOptionType = option.getVanillaOptionParams().indexOfOptionType();
        int maxIteration = option.getBarrierOptionParams().getMaxIterationTimes();
        double tol = option.getBarrierOptionParams().getTolerance();
        double price = indexOfOptionType * (part1(0) + part2(0));
        double positiveAdd;
        double negativeAdd;
        for (int i = 1; i < maxIteration; i++) {
            positiveAdd = indexOfOptionType * (part1(i) + part2(i));
            negativeAdd = indexOfOptionType * (part1(-i) + part2(-i));
            price = price + positiveAdd + negativeAdd;
            if (Math.abs(positiveAdd + negativeAdd) < tol) {
                return price;
            }
        }
        return price;
    }
}

/**
 * @author liangcy
 */
public class DoubleBarrierOption extends BaseSingleOption implements Serializable {

    private BarrierOptionParams barrierOptionParams;

    public BarrierOptionParams getBarrierOptionParams() {
        return barrierOptionParams;
    }

    public void setBarrierOptionParams(BarrierOptionParams barrierOptionParams) {
        this.barrierOptionParams = barrierOptionParams;
    }

    @Override
    public boolean hasMonteCarloMethod() {
        return true;
    }

    @Override
    public double monteCarloPrice(double[] pricePath) {
        EuropeanOption option = new EuropeanOption(this);
        double[] timePoints = MonteCarlo.getTimePoints(getVanillaOptionParams().getTimeRemaining(), pricePath);
        double u = barrierOptionParams.getUpperBarrierPrice();
        double l = barrierOptionParams.getLowerBarrierPrice();
        double uCurve = barrierOptionParams.getUpperCurve();
        double lCurve = barrierOptionParams.getLowerCurve();
        for (int i = 0; i < pricePath.length; i++) {
            double price = pricePath[i];
            double t = timePoints[i];
            if (price < l * Math.exp(lCurve * t) || price > u * Math.exp(uCurve * t)) {
                return barrierOptionParams.isIn() ? option.monteCarloPrice(pricePath) : 0;
            }
        }
        return barrierOptionParams.isIn() ? 0 : option.monteCarloPrice(pricePath);
    }

    @Override
    public double bsm() {
        return barrierOptionParams.isIn() ? europeanVanillaPrice() - bsmOut() : bsmOut();
    }

    private double bsmOut() {
        if (barrierOptionParams.isTouchDoubleBarrier(getUnderlying().getSpotPrice())) {
            return 0;
        }
        DoubleBarrierCalculator calculator = new DoubleBarrierCalculator();
        calculator.setOption(this);
        return calculator.outPrice();
    }


    @Override
    public boolean isValid() {
        return super.isValid() &&
                barrierOptionParams.isValidDoubleBarrierParams(getVanillaOptionParams().getTimeRemaining());
    }

    @Override
    public String toString() {
        return "DoubleBarrierOption{" +
                "barrierOptionParams=" + barrierOptionParams.doubleBarrierToString() +
                "} " + super.toString();
    }
}
