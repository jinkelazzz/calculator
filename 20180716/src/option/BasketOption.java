package option;

import flanagan.analysis.Stat;
import flanagan.math.ArrayMaths;
import flanagan.math.DeepCopy;
import flanagan.math.Matrix;
import underlying.gbm.Future;

import java.util.List;

/**
 * @author liangcy
 * option type/strike price/time remaining 均以 optionList.get(0) 为准
 */
public class BasketOption {
    private List<BaseSingleOption> optionList;
    private double[][] correlationMatrix;

    public List<BaseSingleOption> getOptionList() {
        return optionList;
    }

    public void setOptionList(List<BaseSingleOption> optionList) {
        this.optionList = optionList;
    }

    public double[][] getCorrelationMatrix() {
        return correlationMatrix;
    }

    public void setCorrelationMatrix(double[][] correlationMatrix) {
        this.correlationMatrix = correlationMatrix;
    }

    private double[] forwardPrice() {
        int n = optionList.size();
        double[] forwards = new double[n];
        for (int i = 0; i < n; i++) {
            forwards[i] = optionList.get(i).getForwardPrice();
        }
        return forwards;
    }

    private double[] forwardPriceWithVol() {
        int n = optionList.size();
        double[] forwards = new double[n];
        BaseSingleOption option;
        double vol;
        double t;
        for (int i = 0; i < n; i++) {
            option = optionList.get(i);
            vol = option.getVanillaOptionParams().getVolatility();
            t = option.getVanillaOptionParams().getTimeRemaining();
            forwards[i] = option.getForwardPrice() * Math.exp(vol * Math.sqrt(t));
        }
        return forwards;
    }

    private double m1() {
        return new ArrayMaths(forwardPrice()).sum();
    }

    private double m2() {
        Matrix row = Matrix.rowMatrix(forwardPriceWithVol());
        Matrix col = Matrix.columnMatrix(forwardPriceWithVol());
        Matrix cor = new Matrix(correlationMatrix);
        return row.times(cor).times(col).getElement(0, 0);
    }


    public BaseSingleOption syntheticOption() {
        BaseSingleOption option = (BaseSingleOption) DeepCopy.copy(optionList.get(0));
        double r = option.getUnderlying().getRiskFreeRate();
        Future future = new Future(m1(), r);
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double vol = Math.sqrt(Math.log(m2() / (m1() * m1())) / t);
        option.setUnderlying(future);
        option.getVanillaOptionParams().setVolatility(vol);
        return option;
    }

}
