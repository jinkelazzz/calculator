package option;


import calculator.utility.CalculateUtil;
import calculator.utility.NewtonIterationParams;
import flanagan.math.DeepCopy;
import flanagan.roots.RealRoot;
import flanagan.roots.RealRootDerivFunction;

import java.io.Serializable;

class BsCalculator {
    private AmericanOption option;

    void setOption(AmericanOption option) {
        this.option = option;
    }

    private double rho = Math.sqrt((Math.sqrt(5) - 1) / 2);

    private double m(double x, double y, double rho) {
        return CalculateUtil.binormalCDF(x, y, rho);
    }

    private double beta() {
        double r = option.getUnderlying().getRiskFreeRate();
        double vol = option.getVanillaOptionParams().getVolatility();
        double part1 = 0.5 - option.getCostOfCarry() / (vol * vol);
        double part2 = 2 * r / (vol * vol);
        return part1 + Math.sqrt(part1 * part1 + part2);
    }

    private double b0() {
        double r = option.getUnderlying().getRiskFreeRate();
        double q = option.getUnderlying().getDividendRate();
        double k = option.getVanillaOptionParams().getStrikePrice();
        return Math.max(k, k * r / q);
    }

    private double bInf() {
        double k = option.getVanillaOptionParams().getStrikePrice();
        return k * beta() / (beta() - 1);
    }

    private double t1() {
        return rho * rho * option.getVanillaOptionParams().getTimeRemaining();
    }

    private double h(double t) {
        double vol = option.getVanillaOptionParams().getVolatility();
        double k = option.getVanillaOptionParams().getStrikePrice();
        double part1 = -(option.getCostOfCarry() * t + 2 * vol * Math.sqrt(t));
        double part2 = k * k / ((bInf() - b0()) * b0());
        return part1 * part2;
    }

    private double i(double t) {
        return b0() + (bInf() - b0()) * (1 - Math.exp(h(t)));
    }

    private double a(double t) {
        double k = option.getVanillaOptionParams().getStrikePrice();
        return (i(t) - k) * Math.pow(i(t), -beta());
    }

    private double sigmaT(double t) {
        return option.getVanillaOptionParams().getVolatility() * Math.sqrt(t);
    }

    private double lambda(double gamma) {
        double r = option.getUnderlying().getRiskFreeRate();
        double vol = option.getVanillaOptionParams().getVolatility();
        return (-r + gamma * option.getCostOfCarry() + 0.5 * gamma * (gamma - 1) * vol * vol);
    }

    private double kappa(double gamma) {
        double vol = option.getVanillaOptionParams().getVolatility();
        return 2 * option.getCostOfCarry() / (vol * vol) + (2 * gamma - 1);
    }

    private double p(double gamma, double t) {
        double vol = option.getVanillaOptionParams().getVolatility();
        return (option.getCostOfCarry() + (gamma - 0.5) * vol * vol) * t;
    }

    private double phi(double gamma, double h) {
        double lambda = lambda(gamma);
        double kappa = kappa(gamma);
        double sigmaT = sigmaT(t1());
        double p = p(gamma, t1());
        double s = option.getUnderlying().getSpotPrice();
        double i = i(option.getVanillaOptionParams().getTimeRemaining());

        double d1 = (Math.log(s / h) + p) / sigmaT;
        double d2 = (Math.log((i * i) / (s * h)) + p) / sigmaT;

        double part1 = CalculateUtil.normalCDF(-d1) - Math.pow((i / s), kappa) * CalculateUtil.normalCDF(-d2);
        return Math.exp(lambda * t1()) * Math.pow(s, gamma) * part1;
    }

    private double chi(double gamma, double h) {
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double s = option.getUnderlying().getSpotPrice();
        double lambda = lambda(gamma);
        double kappa = kappa(gamma);

        double e1 = (Math.log(s / i(t1())) + p(gamma, t1())) / sigmaT(t1());
        double e2 = (Math.log((i(t) * i(t)) / (s * i(t1()))) + p(gamma, t1())) / sigmaT(t1());
        double e3 = (Math.log(s / i(t1())) - p(gamma, t1())) / sigmaT(t1());
        double e4 = (Math.log((i(t) * i(t)) / (s * i(t1()))) - p(gamma, t1())) / sigmaT(t1());

        double f1 = (Math.log(s / h) + p(gamma, t)) / sigmaT(t);
        double f2 = (Math.log((i(t) * i(t)) / (s * h)) + p(gamma, t)) / sigmaT(t);
        double f3 = (Math.log((i(t1()) * i(t1())) / (s * h)) + p(gamma, t)) / sigmaT(t);
        double f4 = (Math.log((s * i(t1()) * i(t1())) / (h * i(t) * i(t))) + p(gamma, t)) / sigmaT(t);

        double g1 = m(-e1, -f1, rho);
        double g2 = m(-e2, -f2, rho) * Math.pow((i(t) / s), kappa);
        double g3 = m(-e3, -f3, -rho) * Math.pow((i(t1()) / s), kappa);
        double g4 = m(-e4, -f4, -rho) * Math.pow((i(t1()) / i(t)), kappa);

        return Math.exp(lambda * t) * Math.pow(s, gamma) * (g1 - g2 - g3 + g4);
    }

    private boolean shouldEarlyExercise() {
        double boundary = i(option.getVanillaOptionParams().getTimeRemaining());
        return option.getUnderlying().getSpotPrice() >= boundary;
    }

    double bsPrice() {
        double s = option.getUnderlying().getSpotPrice();
        double k = option.getVanillaOptionParams().getStrikePrice();

        if (shouldEarlyExercise()) {
            return s - k;
        }

        double t = option.getVanillaOptionParams().getTimeRemaining();

        double p1 = a(t) * phi(beta(), i(t));
        double p2 = phi(1, i(t));
        double p3 = phi(1, i(t1()));
        double p4 = k * phi(0, i(t));
        double p5 = k * phi(0, i(t1()));
        double p6 = a(t1()) * phi(beta(), i(t1()));

        double q1 = a(t1()) * chi(beta(), i(t1()));
        double q2 = chi(1, i(t1()));
        double q3 = chi(1, k);
        double q4 = k * chi(0, i(t1()));
        double q5 = k * chi(0, k);
        double q6 = a(t) * Math.pow(s, beta());

        double part1 = -p1 + p2 - p3 - p4 + p5 + p6;
        double part2 = -q1 + q2 - q3 - q4 + q5 + q6;
        return part1 + part2;
    }
}

/**
 * @author liangcy
 */
class BawCalculator implements RealRootDerivFunction {
    private AmericanOption option;

    void setOption(AmericanOption option) {
        this.option = option;
    }

    private double bawN() {
        double vol = option.getVanillaOptionParams().getVolatility();
        return 2 * option.getCostOfCarry() / (vol * vol);
    }

    private double bawK() {
        double vol = option.getVanillaOptionParams().getVolatility();
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double r = option.getUnderlying().getRiskFreeRate();
        if (r == 0) {
            return 2 / (vol * vol * t);
        } else {
            return 2 * r / (vol * vol * (1 - Math.exp(-r * t)));
        }
    }

    private double q() {
        return (1 - bawN() + Math.sqrt((bawN() - 1) * (bawN() - 1) + 4 * bawK())) / 2;
    }

    private double estimate() {
        double k = option.getVanillaOptionParams().getStrikePrice();
        double r = option.getUnderlying().getRiskFreeRate();
        double vol = option.getVanillaOptionParams().getVolatility();
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double m = 2 * r / (vol * vol);
        double sInf = k / (1 - 2 / (1 - bawN() + Math.sqrt((1 - bawN()) * (1 - bawN()) + 4 * m)));
        double h = -k / (sInf - k) * (option.getCostOfCarry() * t + 2 * vol * Math.sqrt(t));
        return k + (sInf - k) * (1 - Math.exp(h));
    }

    private double boundary() {
        NewtonIterationParams iterationParams = new NewtonIterationParams();
        RealRoot realRoot = new RealRoot();
        realRoot.setTolerance(iterationParams.getTol());
        realRoot.setIterMax(iterationParams.getIterations());
        realRoot.setEstimate(estimate());
        return realRoot.newtonRaphson(this);
    }

    private double a(double boundary) {
        EuropeanOption europeanOption = new EuropeanOption(option);
        europeanOption.getUnderlying().setSpotPrice(boundary);
        double t = europeanOption.getVanillaOptionParams().getTimeRemaining();
        double q = europeanOption.getUnderlying().getDividendRate();
        return (1 - Math.exp(-q * t) * CalculateUtil.normalCDF(europeanOption.d1())) *
                boundary / q();
    }

    private double addition(double boundary) {
        return a(boundary) * Math.pow(option.getUnderlying().getSpotPrice() / boundary, q());
    }

    double bawPrice() {
        double s = option.getUnderlying().getSpotPrice();
        double k = option.getVanillaOptionParams().getStrikePrice();
        //这里存boundary是为了只计算一次,提高计算速度
        double boundary = boundary();
        if (shouldEarlyExercise(boundary)) {
            return s - k;
        }
        EuropeanOption europeanOption = new EuropeanOption(option);
        return europeanOption.bsm() + addition(boundary);
    }

    private boolean shouldEarlyExercise(double boundary) {
        return option.getUnderlying().getSpotPrice() >= boundary;
    }

    @Override
    public double[] function(double s0) {
        EuropeanOption europeanOption = new EuropeanOption(option);
        europeanOption.getUnderlying().setSpotPrice(s0);
        double t = europeanOption.getVanillaOptionParams().getTimeRemaining();
        double q = europeanOption.getUnderlying().getDividendRate();
        double k = europeanOption.getVanillaOptionParams().getStrikePrice();

        double[] y = new double[2];
        double rValue = europeanOption.bsm() + (1 - Math.exp(-q * t) * CalculateUtil.normalCDF(europeanOption.d1())) *
                s0 / q();
        double lValue = s0 - k;
        y[0] = (lValue - rValue);

        //这里为逐项求导 并没有合并同类项
        double rDerive = Math.exp(-q * t) * CalculateUtil.normalCDF(europeanOption.d1()) +
                (1 - Math.exp(-q * t) * CalculateUtil.normalCDF(europeanOption.d1())) / q() -
                Math.exp(-q * t) * CalculateUtil.normalPDF(europeanOption.d1()) /
                        (europeanOption.getVanillaOptionParams().sigmaT()) / q();
        double lDerive = 1.0;
        y[1] = (lDerive - rDerive);
        return y;
    }
}

/**
 * @author liangcy
 */
public class AmericanOption extends BaseSingleOption implements Serializable {

    @Override
    public boolean isEarlyExercise() {
        return true;
    }


    /**
     * 其实这里是baw模型
     * @return baw()
     */
    @Override
    public double bsm() {
        return baw();
    }

    @Override
    public boolean hasFiniteDifferenceMethod() {
        return true;
    }

    @Override
    public double[] finiteDifferencePrice(double[] spotPrice) {
        double[] optionPrice = new double[spotPrice.length];
        double k = getVanillaOptionParams().getStrikePrice();
        int index = getVanillaOptionParams().indexOfOptionType();
        for (int i = 0; i < spotPrice.length; i++) {
            optionPrice[i] = Math.max(0, (spotPrice[i] - k) * index);
        }
        return optionPrice;
    }

    /**
     * @reference Barone-Adesi and Whaley 1987
     * @return price
     */
    private double baw() {
        //put -> call
        if (!getVanillaOptionParams().isOptionTypeCall()) {
            return callPutTransform().baw();
        }
        //无分红的call不应提前行权
        if (getUnderlying().getDividendRate() <= 0) {
            return europeanVanillaPrice();
        }

        BawCalculator calculator = new BawCalculator();
        calculator.setOption(this);
        return calculator.bawPrice();
    }

    /**
     * @reference Bjerksund and Stensland 2002
     * @return 美式期权的下界
     */
    private double bs() {
        //put -> call
        if (!getVanillaOptionParams().isOptionTypeCall()) {
            return callPutTransform().bs();
        }
        //无分红的call不应提前行权
        if (getUnderlying().getDividendRate() <= 0) {
            return europeanVanillaPrice();
        }

        BsCalculator calculator = new BsCalculator();
        calculator.setOption(this);
        return calculator.bsPrice();
    }

    private AmericanOption callPutTransform() {
        //DeepCopy保证下面set的时候不会改变源对象的值, 一定要确保class下面每个对象都实现了Serializable接口;
        AmericanOption option = (AmericanOption) DeepCopy.copy(this);
        option.swapSpotStrike();
        option.swapRQ();
        option.swapCallPut();
        return option;
    }


    @Override
    public String toString() {
        return "AmericanOption{" +
                "underlying=" + getUnderlying() +
                ", vanillaOptionParams=" + getVanillaOptionParams() +
                ", volatilitySurface=" + getVolatilitySurface() +
                '}';
    }
}
