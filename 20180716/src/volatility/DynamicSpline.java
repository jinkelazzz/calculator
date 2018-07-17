package volatility;

import calculator.derivatives.SingleOptionAnalysisCalculator;
import calculator.utility.Interpolation;
import flanagan.math.ArrayMaths;
import flanagan.math.DeepCopy;
import option.BaseSingleOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author liangcy
 */
public class DynamicSpline extends BaseVolatilitySkew implements Serializable {

    /**
     * rigidity
     */
    private double mu;

    private double lambda() {
        return 1.0 / (1.5 * mu + 1);
    }

    private boolean isValidSplineLogMoneyness(BaseSingleOption option) {
        double logMoneyness = getLogMoneyness(option);
        return isOutOfMoney(option) && isAtMiddleRange(logMoneyness);
    }

    private List<BaseSingleOption> validOptionList() {
        List<BaseSingleOption> optionList = getOptionList();
        List<BaseSingleOption> copyOptionList = new ArrayList<>();
        for (BaseSingleOption option : optionList) {
            if(isValidSplineLogMoneyness(option)) {
                copyOptionList.add((BaseSingleOption) DeepCopy.copy(option));
            }
        }
        return copyOptionList;
    }


    /**
     *
     * @return [0]: log moneyness, [1]: implied volatility, [2]: vega(weight)
     */
    private double[][] getSplinePoints() {
        List<BaseSingleOption> optionList = validOptionList();
        int n = optionList.size();
        SingleOptionAnalysisCalculator calculator = new SingleOptionAnalysisCalculator();
        double[] logMoneynessList = new double[n];
        double[] impliedVolatilityList = new double[n];
        double[] vegaList = new double[n];
        BaseSingleOption option;
        for (int i = 0; i < n; i++) {
            option = optionList.get(i);
            logMoneynessList[i] = getLogMoneyness(option);
            //这里要先算隐含波动率再算vega, 因为此时可能没有隐含波动率, 无法计算vega;
            calculator.setOption(option);
            calculator.calculateImpliedVolatility();
            impliedVolatilityList[i] = calculator.getResult();
            calculator.calculateVega();
            vegaList[i] = calculator.getResult();
        }
        return sort(logMoneynessList, impliedVolatilityList, vegaList);
    }

    private double[][] sort(double[] logMoneynessList, double[] volatilityList, double[] vegaList) {
        int n = logMoneynessList.length;
        ArrayMaths copyLogMoneynessList = new ArrayMaths(DeepCopy.copy(logMoneynessList));
        int[] indices = new int[n];
        Arrays.sort(logMoneynessList);
        for (int i = 0; i < n; i++) {
            indices[i] = copyLogMoneynessList.indexOf(logMoneynessList[i]);
        }
        ArrayMaths copyVolList = new ArrayMaths(volatilityList);
        copyVolList.sortEquals(indices);
        ArrayMaths copyVegaList = new ArrayMaths(vegaList);
        copyVegaList.sortEquals(indices);
        return new double[][] {logMoneynessList, copyVolList.array(), copyVegaList.array()};
    }

    private void updateVolatilitySkew() {
        double[][] splinePoints = getSplinePoints();
        double[] logMoneynessList = splinePoints[0];
        double[] volatilityList = splinePoints[1];
        double refVol = Interpolation.interp1(logMoneynessList, volatilityList, 0,
                Interpolation.INTERPOLATION_METHOD_NATURE, Interpolation.EXTRAPOLATION_METHOD_NATURE);
        getVolatilitySkewParams().setRefVolatility(refVol);

    }


    @Override
    double getVolatilityInMiddle(double logMoneyness) {
        return 0;
    }

    @Override
    double getSlopeInMiddle(double logMoneyness) {
        return 0;
    }
}
