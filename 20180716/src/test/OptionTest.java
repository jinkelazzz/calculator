package test;

import adjusted.european.option.Sabr;
import calculator.derivatives.SingleOptionAnalysisCalculator;
import calculator.derivatives.SingleOptionFiniteDifferenceCalculator;
import calculator.derivatives.SingleOptionMonteCarloCalculator;
import flanagan.math.DeepCopy;
import option.*;
import org.junit.Assert;
import org.junit.Test;
import underlying.gbm.BaseUnderlying;
import underlying.gbm.Future;
import underlying.gbm.Spot;

import static test.CalculatorCase.analysisCalculator;
import static test.CalculatorCase.finiteDifferenceCalculator;
import static test.SingleOptionCase.americanOption;
import static test.SingleOptionCase.europeanOption;
import static test.UnderlyingCase.spot;

class CalculatorCase {
    static SingleOptionFiniteDifferenceCalculator finiteDifferenceCalculator =
            new SingleOptionFiniteDifferenceCalculator();
    static SingleOptionAnalysisCalculator analysisCalculator =
            new SingleOptionAnalysisCalculator();
    static SingleOptionMonteCarloCalculator monteCarloCalculator =
            new SingleOptionMonteCarloCalculator();
}

class UnderlyingCase {
    static Spot spot = new Spot();
    static Future future = new Future();
}

class SingleOptionCase {
    static EuropeanOption europeanOption = new EuropeanOption();
    static AmericanOption americanOption = new AmericanOption();
    static CashOrNothingOption cashOrNothingOption = new CashOrNothingOption();
    static AsianOption asianOption = new AsianOption();
    static BarrierOption barrierOption = new BarrierOption();
    static DoubleBarrierOption doubleBarrierOption = new DoubleBarrierOption();
    static BinaryBarrierOption binaryBarrierOption = new BinaryBarrierOption();
}


/**
 * @author liangcy
 */

public class OptionTest {
    private static VanillaOptionParams vanillaOptionParams = new VanillaOptionParams();

    private BaseUnderlying createUnderlyingCase(BaseUnderlying underlying, double s, double r, double q) {
        underlying.setSpotPrice(s);
        underlying.setRiskFreeRate(r);
        underlying.setDividendRate(q);
        return underlying;
    }

    private void createVanillaOptionParams(double k, double vol, double t, String optType) {
        vanillaOptionParams.setStrikePrice(k);
        vanillaOptionParams.setVolatility(vol);
        vanillaOptionParams.setTimeRemaining(t);
        vanillaOptionParams.setOptionType(optType);
    }

    @Test
    public void testFD() {
        americanOption.setUnderlying(createUnderlyingCase(spot, 100, 0.1, 0.1));
        createVanillaOptionParams(100, 0.3, 1, BaseOption.OPTION_TYPE_PUT);
        americanOption.setVanillaOptionParams(vanillaOptionParams);
        finiteDifferenceCalculator.setOption(americanOption);
        finiteDifferenceCalculator.calculatePrice();
        double error = finiteDifferenceCalculator.getEuropeanOptionError();
        System.out.println(error);
        System.out.println(finiteDifferenceCalculator.getResult() + error);
        System.out.println(finiteDifferenceCalculator.getResult());
        Assert.assertEquals(0, finiteDifferenceCalculator.getError().getIndex());
    }

    @Test
    public void test() {
        double targetPrice = 30;
        americanOption.setUnderlying(createUnderlyingCase(spot, 100, 0.1, 0.1));
        createVanillaOptionParams(105, 0.3, 1, BaseOption.OPTION_TYPE_PUT);
        americanOption.setVanillaOptionParams(vanillaOptionParams);
        americanOption.getVanillaOptionParams().setTargetPrice(targetPrice);
        analysisCalculator.setOption(americanOption);
        analysisCalculator.calculateImpliedVolatility();
        System.out.println(americanOption.getVanillaOptionParams().getVolatility());
        analysisCalculator.calculatePrice();
        System.out.println((targetPrice - analysisCalculator.getResult())/ targetPrice);
        System.out.println(analysisCalculator.getError().getIndex());
        Assert.assertEquals((targetPrice - analysisCalculator.getResult())/ targetPrice, 0, 1e-14);
    }

    @Test
    public void testEquals() {
        europeanOption.setUnderlying(createUnderlyingCase(spot, 100, 0.1, 0.1));
        createVanillaOptionParams(105, 0.3, 1, BaseOption.OPTION_TYPE_PUT);
        europeanOption.setVanillaOptionParams(vanillaOptionParams);
        EuropeanOption option = (EuropeanOption) DeepCopy.copy(europeanOption);
        Sabr sabr = new Sabr();
        sabr.setBeta(1);
        sabr.setRho(0.5);
        option.setSabrParams(sabr);
        BaseSingleOption option1 = (BaseSingleOption) option;
        Assert.assertTrue(option1.equals(europeanOption));

    }

}
