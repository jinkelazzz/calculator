package volatility;

import adjusted.european.option.Heston;
import calculator.utility.ConstantNumber;
import flanagan.analysis.Stat;
import flanagan.math.Maximisation;
import flanagan.math.MaximisationFunction;
import option.EuropeanOption;

import java.io.Serializable;

/**
 * 用历史数据估计Heston参数。
 *
 * @author liangcy
 */
class HestonEstimation implements MaximisationFunction {
    private double[] price;
    private EuropeanOption option;
    private int tradingDay;

    void setPrice(double[] price) {
        this.price = price;
    }

    void setOption(EuropeanOption option) {
        this.option = option;
    }

    void setTradingDay(int tradingDay) {
        this.tradingDay = tradingDay;
    }

    private double[] logDifferencePrice() {
        double[] logDiffPrice = new double[price.length - 1];
        for (int i = 0; i < logDiffPrice.length; i++) {
            logDiffPrice[i] = Math.log(price[i + 1] / price[i]);
        }
        return logDiffPrice;
    }

    @Override
    public double function(double[] params) {
        double beta = params[0];
        double longVolatility = params[1];
        double rho = params[2];
        double volVolatility = params[3];
        double initialVar = params[4];

        double dt = 1.0 / tradingDay;
        double mu = option.getUnderlying().getCostOfCarry();
        double kappa = 1 - beta * dt;
        double alpha = beta * longVolatility * longVolatility;
        double denominator = 2 * Math.pow(volVolatility, 2) * (1 - rho * rho) * dt;
        double a = (kappa * kappa + rho * volVolatility * kappa * dt + Math.pow(volVolatility * dt, 2) / 4) /
                denominator;
        double d = 2 * Math.PI * volVolatility * Math.sqrt(1 - rho * rho);

        double[] likelihood = new double[price.length];
        double[] vSeries = new double[price.length];
        likelihood[0] = -initialVar;
        vSeries[0] = initialVar;

        double[] logDiffPrice = logDifferencePrice();
        double bt;
        double b;
        double c;
        double logDt;
        double diffPrice;
        for (int i = 0; i < logDiffPrice.length; i++) {
            diffPrice = logDiffPrice[i];
            b = -alpha * dt - rho * volVolatility * (diffPrice - mu * dt);
            c = Math.pow(alpha * dt, 2) * 2 * rho * volVolatility * alpha * dt * (diffPrice - mu * dt)
                    + Math.pow(volVolatility * (diffPrice - mu * dt), 2) - 2 * vSeries[i] * vSeries[i] * alpha *
                    Math.pow(volVolatility, 2) * (1 - rho * rho) * dt;

            if (b * b - c > 0) {
                vSeries[i + 1] = Math.sqrt(b * b - c) - b;
            } else {
                bt = Math.pow(vSeries[i] - alpha * dt, 2) - 2 * rho * volVolatility * (vSeries[i] - alpha * dt) *
                        (diffPrice - mu * dt) + Math.pow(volVolatility * (diffPrice - mu * dt), 2) / denominator;
                vSeries[i + 1] = bt / a > 0 ? Math.sqrt(bt / a) : vSeries[i];
            }

            bt = Math.pow(vSeries[i + 1] - alpha * dt, 2) - 2 * rho * volVolatility * (vSeries[i + 1] - alpha * dt)
                    * (diffPrice - mu * dt) + Math.pow(volVolatility * (diffPrice - mu * dt), 2) / denominator;

            logDt = ((2 * kappa + rho * volVolatility * dt) * (vSeries[i + 1] - alpha * dt) -
                    (2 * rho * volVolatility * kappa + Math.pow(volVolatility, 2) * dt) *
                            (diffPrice - mu * dt)) / denominator - Math.log(d);

            likelihood[i + 1] = logDt - Math.log(a * bt) / 4 - 2 * Math.sqrt(a * bt) + likelihood[i];
        }
        return -likelihood[likelihood.length - 1];
    }

    public Heston hestonEstimate() {
        double initialBeta = 5.0;
        double initialLongVol = 0.25;
        double initialRho = 0.0;
        double initialVolVol = 0.5;
        double initialVariance = Math.pow(option.getVanillaOptionParams().getVolatility(), 2);
        Maximisation maximisation = new Maximisation();
        double[] start = {initialBeta, initialLongVol, initialRho, initialVolVol, initialVariance};
        double[] step = {0.01, 0.01, 0.01, 0.01, 0.01};
        double tol = ConstantNumber.EPS;
        maximisation.nelderMead(this, start, step, tol);
        Heston heston = new Heston();
        heston.setBeta(maximisation.getParamValues()[0]);
        heston.setLongVolatility(maximisation.getParamValues()[1]);
        heston.setRho(maximisation.getParamValues()[2]);
        heston.setVolVolatility(maximisation.getParamValues()[3]);
        EuropeanOption hestonOption = new EuropeanOption(option);
        hestonOption.getVanillaOptionParams().setVolatility(Math.sqrt(maximisation.getParamValues()[5]));
        return heston;
    }
}

/**
 * 用GARCH模型计算波动率
 *
 * @author liangcy
 */
class Garch implements MaximisationFunction {
    private double[] price;

    void setPrice(double[] price) {
        this.price = price;
    }

    private double[] logChange() {
        int n = price.length;
        double[] logReturn = new double[n - 1];
        for (int i = 0; i < logReturn.length; i++) {
            logReturn[i] = Math.log(price[i + 1] / price[i]);
        }
        return logReturn;
    }

    private double longVariance() {
        return Stat.variance(logChange());
    }

    @Override
    public double function(double[] parameters) {
        double alpha = parameters[0];
        double beta = parameters[1];
        double gamma = 1 - alpha - beta;
        int n = price.length;
        double[] logChange = logChange();
        double omega = longVariance() * gamma;
        double[] var = new double[n - 2];
        var[0] = Math.pow(logChange[0], 2);
        for (int i = 1; i < var.length; i++) {
            var[i] = omega + beta * var[i - 1] + alpha * Math.pow(logChange[i], 2);
        }
        double[] likelihood = new double[n - 2];
        for (int i = 0; i < likelihood.length; i++) {
            likelihood[i] = -Math.log(var[i]) - Math.pow(logChange[i + 1], 2) / var[i];
        }
        double sum = 0.0;
        for (double aLikelihood : likelihood) {
            sum = sum + aLikelihood;
        }
        return sum;
    }

    public double garchVol(double estimatedVariance, double t) {
        Maximisation maximisation = new Maximisation();
        double[] start = {0.06, 0.9};
        double[] step = {0.01, 0.01};
        double tol = ConstantNumber.EPS;
        maximisation.nelderMead(this, start, step, tol);

        double alpha = maximisation.getParamValues()[0];
        double beta = maximisation.getParamValues()[1];

        double dailyVariance = longVariance() + Math.pow((alpha + beta), t) * (estimatedVariance - longVariance());
        return Math.sqrt(dailyVariance);
    }
}

/**
 * @author liangcy
 */
public class HistoryPrice implements Serializable {
    private double[] close;
    private double[] open;
    private double[] high;
    private double[] low;
    /**
     * 年化系数
     */
    private int tradingDay = 252;

    public double[] getClose() {
        return close;
    }

    public void setClose(double[] close) {
        this.close = close;
    }

    public double[] getOpen() {
        return open;
    }

    public void setOpen(double[] open) {
        this.open = open;
    }

    public double[] getHigh() {
        return high;
    }

    public void setHigh(double[] high) {
        this.high = high;
    }

    public double[] getLow() {
        return low;
    }

    public void setLow(double[] low) {
        this.low = low;
    }

    public int getTradingDay() {
        return tradingDay;
    }

    public void setTradingDay(int tradingDay) {
        this.tradingDay = tradingDay;
    }

    private double annualizedVolatility(double dailyVol) {
        return dailyVol * Math.sqrt(tradingDay);
    }

    public double closeVolatility() {
        return annualizedVolatility(Stat.volatilityLogChange(close));
    }

    private double[] volConfidenceInterval(double[] volList, double confidenceLevel) {
        int df = volList.length - 1;
        double lowerThreshold = Stat.chiSquareInverseCDF(df, 1 - confidenceLevel / 2);
        double upperThreshold = Stat.chiSquareInverseCDF(df, confidenceLevel / 2);
        double mean = Stat.mean(volList);
        double lower = mean * Math.sqrt(df / lowerThreshold);
        double upper = mean * Math.sqrt(df / upperThreshold);
        double[] result = new double[3];
        result[0] = mean;
        result[1] = lower;
        result[2] = upper;
        return result;
    }

    /**
     * 计算滚动波动率, 返回波动率均值, 波动率的置信区间
     *
     * @param width           观察窗口宽度
     * @param confidenceLevel 置信度, 通常为0.05或者0.01;
     * @return
     */
    public double[] rollCloseVolatility(int width, double confidenceLevel) {
        HistoryPrice historyPrice = new HistoryPrice();
        historyPrice.setTradingDay(tradingDay);
        int m = close.length - width + 1;
        double[] volList = new double[m];
        double[] rollClosePrice = new double[width];
        for (int i = 0; i < m; i++) {
            System.arraycopy(close, i, rollClosePrice, 0, width);
            historyPrice.setClose(rollClosePrice);
            volList[i] = historyPrice.closeVolatility();
        }
        return volConfidenceInterval(volList, confidenceLevel);
    }

    /**
     * @return volatility
     * @reference Parkinson 1980;
     */
    public double highLowVolatility() {
        int n = high.length;
        double[] logHighLow = new double[n];
        for (int i = 0; i < n; i++) {
            logHighLow[i] = Math.log(high[i] / low[i]);
        }
        double dailyVol = Stat.mean(logHighLow) / (2 * Math.sqrt(Math.log(2)));
        return annualizedVolatility(dailyVol);
    }

    /**
     * @return volatility
     * @reference Garman, Klass 1980;
     */
    public double highLowCloseVolatility() {
        int n = close.length;
        double[] logHighLow = new double[n - 1];
        double[] logClose = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            logHighLow[i] = Math.pow(Math.log(high[i + 1] / low[i + 1]), 2) / 2;
            logClose[i] = Math.pow(Math.log(close[i + 1] / close[i]), 2) * (2 * Math.log(2) - 1);
        }
        double dailyVol = Math.sqrt(Stat.mean(logHighLow) - Stat.mean(logClose));
        return annualizedVolatility(dailyVol);
    }

    /**
     * @return volatility
     * @reference Rogers, Satchell, Yoon 1994;
     */
    public double highLowOpenCloseVolatility() {
        int n = close.length;
        double[] logPrice = new double[n];
        for (int i = 0; i < n; i++) {
            logPrice[i] = Math.log(high[i] / close[i]) * Math.log(high[i] / open[i])
                    + Math.log(low[i] / close[i]) * Math.log(low[i] / open[i]);
        }
        double dailyVol = Math.sqrt(Stat.mean(logPrice));
        return annualizedVolatility(dailyVol);
    }

    /**
     * 利用收盘价估计GARCH volatility;
     * @param initialVol 即期波动率估计;
     * @param days          这里的t不用年化;
     * @return garch volatility, 其中long variance是历史方差
     */
    public double garchVolatility(double initialVol, double days) {
        Garch garch = new Garch();
        garch.setPrice(close);
        double dailyVolatility = garch.garchVol(initialVol * initialVol, days);
        return annualizedVolatility(dailyVolatility);
    }

    /**
     * 极大似然估计Heston参数
     *
     * @param option 欧式期权, 初始化即期波动率用。
     * @return Heston参数(不估计lambda);
     * 同时估计即期波动率, 放在Heston下面option的波动率里面
     */
    public Heston hestonEstimation(EuropeanOption option) {
        HestonEstimation estimation = new HestonEstimation();
        estimation.setPrice(close);
        estimation.setTradingDay(tradingDay);
        estimation.setOption(option);
        return estimation.hestonEstimate();
    }



}
