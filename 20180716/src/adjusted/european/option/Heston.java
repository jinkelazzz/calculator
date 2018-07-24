package adjusted.european.option;

import flanagan.complex.Complex;
import flanagan.integration.IntegralFunction;
import flanagan.integration.Integration;
import option.BaseSingleOption;
import option.EuropeanOption;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author liangcy
 * @reference Heston 1993
 * d(ln(s)) = (r - q) * dt + vol * dw(1, t);
 * d(vol^2) = beta * (longVariance - vol^2) * dt + volVol * vol * dw(2, t);
 * E[w(1, t), w(2, t)] = rho * dt;
 * longVariance = longVolatility ^ 2;
 */
public class Heston implements IntegralFunction, Serializable {

    private BaseSingleOption option;

    public BaseSingleOption getOption() {
        return option;
    }

    public void setOption(BaseSingleOption option) {
        this.option = option;
    }

    private double beta;
    private double longVolatility;
    private double rho;
    private double volVolatility;
    private int blocks = 10000;
    private double accuracy = 1e-4;

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = Math.max(1e-6, accuracy);
    }

    public int getBlocks() {
        return blocks;
    }

    public void setBlocks(int blocks) {
        this.blocks = Math.min(1000000, blocks);
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public double getLongVolatility() {
        return longVolatility;
    }

    public void setLongVolatility(double longVolatility) {
        this.longVolatility = longVolatility;
    }

    public double getRho() {
        return rho;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    public double getVolVolatility() {
        return volVolatility;
    }

    public void setVolVolatility(double volVolatility) {
        this.volVolatility = volVolatility;
    }

    private double quadUpperLimit() {
        double sigmaT = option.getVanillaOptionParams().sigmaT();
        return Math.max(100.0, 10.0 / (sigmaT));
    }

    private double a() {
        return beta * longVolatility * longVolatility;
    }

    private double u(int index) {
        if (index == 1) {
            return 0.5;
        } else {
            return -0.5;
        }
    }

    private double b(int index) {
        if (index == 1) {
            return beta - rho * volVolatility;
        } else {
            return beta;
        }
    }

    private Complex d(double phi, int index) {
        Complex complexPhi = new Complex(0, phi);
        Complex d1 = (complexPhi.times(volVolatility).times(rho).minus(b(index))).pow(2);
        Complex d2 = (complexPhi.times(2 * u(index)).minus(phi * phi)).times(volVolatility * volVolatility);
        return (d1.minus(d2)).sqrt();
    }

    private Complex g(double phi, int index) {
        Complex complexPhi = new Complex(0, phi);
        Complex g1 = d(phi, index).minus(complexPhi.times(volVolatility).times(rho)).plus(b(index));
        Complex g2 = d(phi, index).times(-1).minus(complexPhi.times(volVolatility).times(rho)).plus(b(index));
        return g1.over(g2);
    }

    private Complex dFun(double phi, int index) {
        Complex complexPhi = new Complex(0, phi);
        Complex complexOne = new Complex(1, 0);
        double t = option.getVanillaOptionParams().getTimeRemaining();
        Complex result;
        if (volVolatility != 0) {
            Complex part1 = (d(phi, index).minus(complexPhi.times(volVolatility).times(rho)).
                    plus(b(index))).over(volVolatility * volVolatility);
            // 1 - exp(d * t)
            Complex part2 = complexOne.minus((d(phi, index).times(t)).exp());
            // 1 - g * exp(d * t)
            Complex part3 = complexOne.minus(g(phi, index).times((d(phi, index).times(t)).exp()));
            result = part1.times(part2.over(part3));
        } else {
            result = (complexPhi.times(u(index)).minus(phi * phi / 2)).
                    times((1 - Math.exp(-b(index) * t)) / b(index));
        }
        return result;
    }

    private Complex cFun(double phi, int index) {
        Complex complexPhi = new Complex(0, phi);
        Complex complexOne = new Complex(1, 0);
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double r = option.getUnderlying().getRiskFreeRate();
        double q = option.getUnderlying().getDividendRate();
        Complex result;
        if (volVolatility != 0) {
            Complex part1 = complexPhi.times((r - q) * t);
            Complex part2 = (d(phi, index).minus(complexPhi.times(volVolatility).times(rho)).
                    plus(b(index))).times(t);
            Complex part3 = ((complexOne.minus(g(phi, index).times((d(phi, index).times(t)).exp()))).
                    over(complexOne.minus(g(phi, index)))).log().times(2);
            result = part1.plus((part2.minus(part3)).times(a() / (volVolatility * volVolatility)));
        } else {
            Complex part1 = complexPhi.times((r - q) * t);
            Complex part2 = complexPhi.times(u(index)).minus(phi * phi / 2).times(a() / b(index)).
                    times(t - (1 - Math.exp(-b(index) * t)) / b(index));
            result = part1.plus(part2);
        }
        return result;
    }

    private double hestonFun(double phi) {
        if (phi == 0) {
            return 0.0;
        }
        double logSpotPrice = Math.log(option.getUnderlying().getSpotPrice());
        double logStrike = Math.log(option.getVanillaOptionParams().getStrikePrice());
        double volatility = option.getVanillaOptionParams().getVolatility();
        double timeRemaining = option.getVanillaOptionParams().getTimeRemaining();
        double r = option.getUnderlying().getRiskFreeRate();
        double q = option.getUnderlying().getDividendRate();

        // create 0 + φi
        Complex complexPhi = new Complex(0, phi);
        Complex part1 = (cFun(phi, 1).plus(dFun(phi, 1).times(volatility * volatility)).
                plus(complexPhi.times(logSpotPrice))).exp().times(Math.exp(logSpotPrice - q * timeRemaining));

        Complex part2 = (cFun(phi, 2).plus(dFun(phi, 2).times(volatility * volatility)).
                plus(complexPhi.times(logSpotPrice))).exp().times(Math.exp(logStrike - r * timeRemaining));

        Complex fun = part1.minus(part2).times((complexPhi.times(-logStrike)).exp().over(complexPhi));

        return fun.getReal();
    }

    @Override
    public double function(double phi) {
        return hestonFun(phi);
    }

    public double hestonIntegration() {
        Integration integration = new Integration(this, 0.0, quadUpperLimit());
        return integration.trapezium(accuracy, blocks) / Math.PI;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Heston heston = (Heston) obj;
        return Double.compare(heston.beta, beta) == 0 &&
                Double.compare(heston.longVolatility, longVolatility) == 0 &&
                Double.compare(heston.rho, rho) == 0 &&
                Double.compare(heston.volVolatility, volVolatility) == 0 &&
                blocks == heston.blocks &&
                Double.compare(heston.accuracy, accuracy) == 0 &&
                Objects.equals(option, heston.option);
    }

    @Override
    public int hashCode() {

        return Objects.hash(option, beta, longVolatility, rho, volVolatility, blocks, accuracy);
    }
}
