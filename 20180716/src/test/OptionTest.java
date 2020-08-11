package test;

import calculator.derivatives.SingleOptionAnalysisCalculator;
import calculator.derivatives.SingleOptionFiniteDifferenceCalculator;
import calculator.derivatives.SingleOptionMonteCarloCalculator;
import option.*;
import org.junit.Assert;
import org.junit.Test;
import underlying.gbm.BaseUnderlying;
import underlying.gbm.Future;
import underlying.gbm.Spot;
import volatility.VolatilitySurface;

import java.time.Instant;

import static test.CalculatorCase.finiteDifferenceCalculator;
import static test.CalculatorCase.monteCarloCalculator;
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

class AutocallOptionCase {
    static AutocallOption autocallOption = new AutocallOption();
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
        System.out.println(americanOption.bsm());
    }

    @Test
    public void testMonteCarlo() {
        AutocallOptionCase.autocallOption.setUnderlying(createUnderlyingCase(spot, 24.95, 0.05, 0));
        createVanillaOptionParams(100, 0.4, 246.0/240.0, BaseOption.OPTION_TYPE_CALL);
        AutocallOptionCase.autocallOption.setVanillaOptionParams(vanillaOptionParams);
        AutocallOptionCase.autocallOption.setVolatilitySurface(new VolatilitySurface());

        AutocallOptionCase.autocallOption.setCouponRate(0.0195833 * 12);
        AutocallOptionCase.autocallOption.setKnockInPrice(19.405);
        AutocallOptionCase.autocallOption.setKnockOutPrice(27.6564);
        AutocallOptionCase.autocallOption.setRefPrice(25.88);
        AutocallOptionCase.autocallOption.setTradingDays(240);
        int[] days = new int[] {22,42,60,80,101,121,143,164,186,208,223,246};
        AutocallOptionCase.autocallOption.setObserveDays(days);

        AutocallOptionCase.autocallOption.setDecayRate(0);
        AutocallOptionCase.autocallOption.setFloor(0);

        monteCarloCalculator.setOption(AutocallOptionCase.autocallOption);
        long time = Instant.now().toEpochMilli();
        System.out.println(Instant.now());
        monteCarloCalculator.calculatePrice();
        System.out.println(Instant.now());
        System.out.println((Instant.now().toEpochMilli() - time));
        System.out.println(monteCarloCalculator.getResult());
    }

}
