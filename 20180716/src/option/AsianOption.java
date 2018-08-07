package option;

import calculator.utility.CalculateUtil;
import calculator.utility.MonteCarlo;
import flanagan.analysis.Stat;
import flanagan.interpolation.LinearInterpolation;
import flanagan.math.DeepCopy;
import flanagan.math.Maximisation;
import flanagan.math.MaximisationFunction;
import underlying.gbm.Future;
import volatility.VolatilitySurface;

import java.io.Serializable;
import java.util.Arrays;

class CurranCalculator implements MaximisationFunction {

    private AsianOption asianOption;

    void setAsianOption(AsianOption asianOption) {
        this.asianOption = asianOption;
    }

    private double volG() {
        double[] observeTimePoints = asianOption.getObserveTimePoints();
        double[] futureVolPoints = asianOption.generateFutureVolatilityPoints();
        double sum = 0.0;
        int n = observeTimePoints.length;
        for (int i = 0; i < n; i++) {
            sum = sum + futureVolPoints[i] * futureVolPoints[i] * observeTimePoints[i] *
                    (2 * (n - i) - 1);
        }
        return Math.sqrt(sum) / n;
    }

    private double mu() {
        double[] observeTimePoints = asianOption.getObserveTimePoints();
        double[] futureVolPoints = asianOption.generateFutureVolatilityPoints();
        double[] futurePricePoints = asianOption.generateFuturePricePoints();
        int n = observeTimePoints.length;
        double sum1 = 0.0;
        for (int i = 0; i < n; i++) {
            sum1 = sum1 + futureVolPoints[i] * futureVolPoints[i] * observeTimePoints[i];
        }
        double sum2 = 0.0;
        for (int i = 0; i < n; i++) {
            sum2 = sum2 + Math.log(futurePricePoints[i]);
        }
        return sum2 / n - 0.5 * sum1 / n;
    }

    @Override
    public double function(double[] args) {
        double kappa = args[0];
        double[] futurePricePoints = asianOption.generateFuturePricePoints();
        double[] futureCorPoints = asianOption.generateFutureCorPoints();
        int n = futurePricePoints.length;
        double sum = 0.0;
        double x;
        for (int i = 0; i < n; i++) {
            x = (mu() + futureCorPoints[i] - kappa) / volG();
            sum = sum + futurePricePoints[i] * CalculateUtil.normalCDF(x);
        }
        double x1 = (mu() - kappa) / volG();
        return asianOption.getDiscountValueByRiskFreeRate() *
                (sum / n - asianOption.transformStrike() * CalculateUtil.normalCDF(x1));
    }

    double curranPrice() {
        Maximisation maximisation = new Maximisation();
        double[] start = {asianOption.transformStrike()};
        double[] step = {0.001};
        maximisation.nelderMead(this, start, step);
        return maximisation.getMaximum();
    }
}


/**
 * @author liangcy
 */
public class AsianOption extends BaseSingleOption implements Serializable {

    private double pastTime = 0.0;
    private double pastAvgPrice = 0.0;
    private double[] observeTimePoints;

    public double getPastTime() {
        return pastTime;
    }

    public void setPastTime(double pastTime) {
        this.pastTime = pastTime;
    }

    public double getPastAvgPrice() {
        return pastAvgPrice;
    }

    public void setPastAvgPrice(double pastAvgPrice) {
        this.pastAvgPrice = pastAvgPrice;
    }

    public double[] getObserveTimePoints() {
        return observeTimePoints;
    }

    public void setObserveTimePoints(double[] observeTimePoints) {
        this.observeTimePoints = observeTimePoints;
    }

    /**
     * 要对strike进行转换
     *
     * @return 转换后的strike
     */
    double transformStrike() {
        double t = getVanillaOptionParams().getTimeRemaining();
        double k = getVanillaOptionParams().getStrikePrice();
        return (pastTime + t) * k / t - pastTime * pastAvgPrice / t;
    }

    /**
     * @return 最终结果转换乘数
     */
    private double multi() {
        double t = getVanillaOptionParams().getTimeRemaining();
        return t / (pastTime + t);
    }

    double[] generateFuturePricePoints() {
        double[] futurePricePoints = new double[observeTimePoints.length];
        for (int i = 0; i < observeTimePoints.length; i++) {
            futurePricePoints[i] = getUnderlying().getFutureValue(observeTimePoints[i]);
        }
        return futurePricePoints;
    }

    double[] generateFutureVolatilityPoints() {
        double[] futureVolPoints = new double[observeTimePoints.length];
        double[] futurePricePoints = generateFuturePricePoints();
        double k = getVanillaOptionParams().getStrikePrice();
        if (null == getVolatilitySurface()) {
            setVolatilitySurface(new VolatilitySurface(getVanillaOptionParams().getVolatility()));
        }
        for (int i = 0; i < observeTimePoints.length; i++) {
            futureVolPoints[i] = getVolatilitySurface().getVolatility(futurePricePoints[i] / k,
                    observeTimePoints[i]);
        }
        return futureVolPoints;
    }

    double[] generateFutureCorPoints() {
        double[] volPoints = generateFutureVolatilityPoints();
        int m = volPoints.length;
        double[] corPoints = new double[m];
        for (int i = 0; i < m; i++) {
            double sum = 0;
            for (int j = 0; j < i; j++) {
                sum = sum + volPoints[j] * volPoints[j] * observeTimePoints[j];
            }
            corPoints[i] = (sum + (m - i) * volPoints[i] * volPoints[i] * observeTimePoints[i]) / m;
        }
        return corPoints;
    }

    private double m1() {
        double[] futurePricePoints = generateFuturePricePoints();
        return Stat.mean(futurePricePoints);
    }

    private double m2() {
        double[] futurePricePoints = generateFuturePricePoints();
        double[] futureVolatilityPoints = generateFutureVolatilityPoints();
        double sum1 = 0.0;
        int m = futurePricePoints.length;
        double volT;
        for (int i = 0; i < m; i++) {
            volT = futureVolatilityPoints[i] * futureVolatilityPoints[i] * observeTimePoints[i];
            sum1 = sum1 + futurePricePoints[i] * futurePricePoints[i] * Math.exp(volT);
        }
        double sum2 = 0.0;
        for (int i = 1; i < m; i++) {
            for (int j = 0; j < i; j++) {
                volT = futureVolatilityPoints[j] * futureVolatilityPoints[j] * observeTimePoints[j];
                sum2 = sum2 + 2 * futurePricePoints[i] * futurePricePoints[j] * Math.exp(volT);
            }
        }
        return (sum1 + sum2) / (m * m);
    }

    /**
     * 当K*<0 时,看涨期权肯定被行使; 看跌期权肯定不行使
     * @return 将期权当成远期合约的定价
     */
    private double futurePrice() {
        return getVanillaOptionParams().isOptionTypeCall() ?
                multi() * (m1() - transformStrike()) * getDiscountValueByRiskFreeRate() : 0;
    }

    /**
     * Turnbull & Wakeman 1991;
     *
     * @return option price
     */
    @Override
    public double bsm() {
        double k = transformStrike();
        if (k <= 0) {
            return futurePrice();
        }

        double vol = getVanillaOptionParams().getVolatility();
        double t = getVanillaOptionParams().getTimeRemaining();
        double m = m2() / (m1() * m1());
        double aVol = m <= 1 ? vol / Math.sqrt(3.0) : Math.sqrt(Math.log(m) / t);

        VanillaOptionParams newVanillaOptionParams = (VanillaOptionParams) DeepCopy.copy(getVanillaOptionParams());
        newVanillaOptionParams.setStrikePrice(k);
        newVanillaOptionParams.setVolatility(aVol);
        EuropeanOption europeanOption = new EuropeanOption();
        europeanOption.setUnderlying(new Future(m1(), getUnderlying().getRiskFreeRate()));
        europeanOption.setVanillaOptionParams(newVanillaOptionParams);
        return europeanOption.bsm() * multi();
    }

    /**
     * curran 1992;
     *
     * @return option price
     */
    public double curran() {
        double k = transformStrike();
        if (k <= 0) {
            return futurePrice();
        }

        CurranCalculator curranCalculator = new CurranCalculator();
        curranCalculator.setAsianOption(this);
        double curranPrice = curranCalculator.curranPrice();
        if (getVanillaOptionParams().isOptionTypeCall()) {
            return multi() * curranPrice;
        } else {
            // asian call put parity;
            return multi() * (curranPrice - getDiscountValueByRiskFreeRate() * (m1() - k));
        }
    }

    /**
     * 蒙特卡洛模拟路径的时间点可能和实际观察时间点不匹配, 利用线性插值取得观察时间点的价格;
     *
     * @param pricePath 蒙特卡洛模拟路径
     * @return 计算蒙特卡洛模拟underlying price均值
     */
    private double calculateAvgPriceWithMonteCarloPath(double[] pricePath) {
        double t = getVanillaOptionParams().getTimeRemaining();
        double[] monteCarloTimePoints = MonteCarlo.getTimePoints(t, pricePath);
        LinearInterpolation interpolation = new LinearInterpolation(monteCarloTimePoints, pricePath);
        double[] futurePrice = new double[observeTimePoints.length];
        for (int i = 0; i < observeTimePoints.length; i++) {
            futurePrice[i] = interpolation.interpolate(observeTimePoints[i]);
        }
        return (pastAvgPrice * pastTime + Stat.mean(futurePrice) * t) / (pastTime + t);
    }

    @Override
    public boolean hasMonteCarloMethod() {
        return true;
    }

    @Override
    public double monteCarloPrice(double[] pricePath) {
        double sAvg = calculateAvgPriceWithMonteCarloPath(pricePath);
        //这里的strike不用转换
        double k = getVanillaOptionParams().getStrikePrice();
        if (getVanillaOptionParams().isOptionTypeCall()) {
            return Math.max(sAvg - k, 0.0) * getDiscountValueByRiskFreeRate();
        } else {
            return Math.max(k - sAvg, 0.0) * getDiscountValueByRiskFreeRate();
        }
    }



    @Override
    public boolean isValid() {
        return super.isValid() &&
                pastAvgPrice >= 0 &&
                pastTime >= 0 &&
                observeTimePoints.length > 0;
    }

    @Override
    public String toString() {
        return "AsianOption{" +
                "pastTime=" + pastTime +
                ", pastAvgPrice=" + pastAvgPrice +
                ", observeTimePoints=" + Arrays.toString(observeTimePoints) +
                ", underlying=" + getUnderlying() +
                ", vanillaOptionParams=" + getVanillaOptionParams() +
                ", volatilitySurface=" + getVolatilitySurface() +
                '}';
    }
}
