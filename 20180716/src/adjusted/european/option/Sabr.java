package adjusted.european.option;

import flanagan.complex.Complex;
import flanagan.math.Polynomial;
import flanagan.math.VectorMaths;
import option.EuropeanOption;

/**
 * @author liangcy
 * @reference Hagan, Kumar, Lesniewski, Woodward (2002)
 * adjust option price when underlying is future;
 * dF = alpha * F ^ beta * dz;
 * d(alpha) = volVol * alpha * dw;
 * E[dz, dw] = rho * dt;
 * alpha is estimated by using ATM vol, see West (2005)
 */
public class Sabr {
    private EuropeanOption option;
    private double beta;
    private double volVolatility;
    private double rho;

    public EuropeanOption getOption() {
        return option;
    }

    public void setOption(EuropeanOption option) {
        this.option = option;
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public double getVolVolatility() {
        return volVolatility;
    }

    public void setVolVolatility(double volVolatility) {
        this.volVolatility = volVolatility;
    }

    public double getRho() {
        return rho;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    private double z() {
        double s = option.getUnderlying().getSpotPrice();
        double k = option.getVanillaOptionParams().getStrikePrice();
        return volVolatility / solveAlpha() * Math.pow(s * k, (1 - beta) / 2) * Math.log(s / k);
    }

    private double xz() {
        return Math.log((Math.sqrt(1 - 2 * rho * z() + z() * z()) + z() - rho) / (1 - rho));
    }

    private double findRoot(Complex[] roots) {
        double minPositiveRoot = Double.MAX_VALUE;
        for (Complex root : roots) {
            //寻找最小正实根
            if (root.getImag() == 0 && root.getReal() > 0) {
                minPositiveRoot = Math.min(minPositiveRoot, root.getReal());
            }
        }
        if (Double.MAX_VALUE == minPositiveRoot) {
            return 0;
        } else {
            return minPositiveRoot;
        }
    }

    private double multi() {
        double s = option.getUnderlying().getSpotPrice();
        double k = option.getVanillaOptionParams().getStrikePrice();
        double x = (1 - beta) * Math.log(s / k);
        double denominator = Math.pow(s * k, (1 - beta) / 2) * (1 + Math.pow(x, 2) / 24 + Math.pow(x, 4) / 1920);
        return z() / xz() / denominator;
    }

    private double[] polynomialParams() {
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double s = option.getUnderlying().getSpotPrice();
        double cubic = Math.pow((1 - beta), 2) * t / (24 * Math.pow(s, 2 - 2 * beta));
        double quadratic = rho * beta * volVolatility * t / (4 * Math.pow(s, 1 - beta));
        double linear = 1 + (2 - 3 * rho * rho) * Math.pow(volVolatility, 2) * t / 24;
        double constant = volAtMoney() * Math.pow(s, 1 - beta);
        return new double[]{constant, linear, quadratic, cubic};
    }

    private double solveAlpha() {
        //solve cubic * alpha^3 + quadratic * alpha^2 + linear * alpha + constant = 0;
        double cubic = polynomialParams()[3];
        double quadratic = polynomialParams()[2];
        double linear = polynomialParams()[1];
        double constant = polynomialParams()[0];
        if (cubic != 0) {
            Complex[] roots = Polynomial.cubic(constant, linear, quadratic, cubic);
            return findRoot(roots);
        }
        if (quadratic != 0) {
            Complex[] roots = Polynomial.quadratic(constant, linear, quadratic);
            return findRoot(roots);
        }
        if (linear != 0) {
            return -constant / linear;
        }
        return 0;
    }

    private double volAtMoney() {
        if (option.getVolatilitySurface() == null) {
            return option.getVanillaOptionParams().getVolatility();
        } else {
            return option.getVolatilitySurface().getVolatility(1.0, option.getVanillaOptionParams().getTimeRemaining());
        }
    }

    public double sabrVolatility() {
        double a = solveAlpha();
        if (a == 0) {
            return option.getVanillaOptionParams().getVolatility();
        }
        VectorMaths aVec = new VectorMaths(new double[]{0, a, a * a, a * a * a});
        VectorMaths paramVec = new VectorMaths(polynomialParams());
        return multi() * (VectorMaths.dot(aVec, paramVec));
    }

}
