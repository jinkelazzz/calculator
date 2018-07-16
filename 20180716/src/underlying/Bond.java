package underlying;

import java.io.Serializable;
import java.util.Calendar;

class CountDaysCalculator {
    private Calendar previousPayDay;
    private Calendar nextPayDay;
    private Calendar today;

    public void setPreviousPayDay(Calendar previousPayDay) {
        this.previousPayDay = previousPayDay;
    }

    public void setNextPayDay(Calendar nextPayDay) {
        this.nextPayDay = nextPayDay;
    }

    public void setToday(Calendar today) {
        this.today = today;
    }

    private static final long MILLION_SEC_ONE_DAY = 24 * 60 * 60 * 1000;

    private long getActDays(Calendar startDate, Calendar endDate) {
        long startDateTimeInMillis = startDate.getTimeInMillis();
        long endDateTimeInMillis = endDate.getTimeInMillis();
        return (endDateTimeInMillis - startDateTimeInMillis) / MILLION_SEC_ONE_DAY;
    }

    private long get30DaysIn360(Calendar startDate, Calendar endDate) {
        int startYear = startDate.get(Calendar.YEAR);
        int startMonth = startDate.get(Calendar.MONTH);
        int startDay = startDate.get(Calendar.DAY_OF_MONTH);

        int endYear = endDate.get(Calendar.YEAR);
        int endMonth = endDate.get(Calendar.MONTH);
        int endDay = endDate.get(Calendar.DAY_OF_MONTH);

        int daysInOneYear = 360;
        int daysInOneMonth = 30;

        startDay = Math.min(daysInOneMonth, startDay);

        if (endDay > daysInOneMonth && startDay == daysInOneMonth) {
            endDay = daysInOneMonth;
        }

        return (endYear - startYear) * daysInOneYear + (endMonth - startMonth) * daysInOneMonth + (endDay - startDay);
    }

    public double actAct() {
        return getActDays(previousPayDay, today) / getActDays(previousPayDay, nextPayDay);
    }

    public double act360() {
        return getActDays(previousPayDay, today) / get30DaysIn360(previousPayDay, nextPayDay);
    }

    public double thirty360() {
        return get30DaysIn360(previousPayDay, today) / get30DaysIn360(previousPayDay, nextPayDay);
    }

    public double timeOfYears(Calendar startDate, Calendar endDate) {
        return getActDays(startDate, endDate) / 365;
    }
}

/**
 * @author liangcy
 */
public class Bond implements Serializable {
    /**
     * 计息日计算方式
     */
    public static final String COUNT_DAYS_METHOD_ACT_ACT = "act/act";
    public static final String COUNT_DAYS_METHOD_ACT_360 = "act/360";
    public static final String COUNT_DAYS_METHOD_30_360 = "30/360";

    /**
     * 面值
     */
    private double parValue = 100;
    /**
     * 起始日(发行日)
     */
    private Calendar startDate;
    /**
     * 起始日 <= 定价日 < 到期日
     */
    private Calendar pricingDate;
    /**
     * 到期日
     */
    private Calendar maturityDate;
    /**
     * 全部付息时间, 包括到期日(如果到期日那天也付利息的话);
     */
    private Calendar[] allDatesToPayInterest;
    /**
     * 转换因子
     */
    private double conversionFactor;
    /**
     * 利息, 长度要和付息时间一样
     */
    private double[] interestToPay;

    /**
     * 日期计算方式
     */
    private String countDaysMethod = "COUNT_DAYS_METHOD_ACT_ACT";

    /**
     * 利率曲线用于计算现值等等;
     */
    private InterestRateCurve interestRateCurve;

    /**
     * 报价(净价)
     */
    private double cleanPrice;

    public double getParValue() {
        return parValue;
    }

    public void setParValue(double parValue) {
        this.parValue = parValue;
    }

    public Calendar getStartDate() {
        return startDate;
    }

    public void setStartDate(Calendar startDate) {
        this.startDate = startDate;
    }

    public Calendar getPricingDate() {
        return pricingDate;
    }

    public void setPricingDate(Calendar pricingDate) {
        this.pricingDate = pricingDate;
    }

    public Calendar getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(Calendar maturityDate) {
        this.maturityDate = maturityDate;
    }

    public Calendar[] getAllDatesToPayInterest() {
        return allDatesToPayInterest;
    }

    public void setAllDatesToPayInterest(Calendar[] allDatesToPayInterest) {
        this.allDatesToPayInterest = allDatesToPayInterest;
    }

    public double getConversionFactor() {
        return conversionFactor;
    }

    public void setConversionFactor(double conversionFactor) {
        this.conversionFactor = conversionFactor;
    }

    public double[] getInterestToPay() {
        return interestToPay;
    }

    public void setInterestToPay(double[] interestToPay) {
        this.interestToPay = interestToPay;
    }

    public String getCountDaysMethod() {
        return countDaysMethod;
    }

    public void setCountDaysMethod(String countDaysMethod) {
        this.countDaysMethod = countDaysMethod;
    }

    public InterestRateCurve getInterestRateCurve() {
        return interestRateCurve;
    }

    public void setInterestRateCurve(InterestRateCurve interestRateCurve) {
        this.interestRateCurve = interestRateCurve;
    }

    /**
     * @return 下次付息日的下标; 如果找不到, 返回-1;
     */
    private int findIndexOfNextPayDate() {
        for (int i = 0; i < allDatesToPayInterest.length; i++) {
            if (pricingDate.before(allDatesToPayInterest[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return 上一次付息日到定价日的累计利息
     */
    public double getAccumulateInterest() {
        int indexOfNextPayDate = findIndexOfNextPayDate();
        if (indexOfNextPayDate == -1) {
            return 0;
        }
        Calendar previousDate = (indexOfNextPayDate == 0 ? startDate : allDatesToPayInterest[indexOfNextPayDate - 1]);
        Calendar nextDate = allDatesToPayInterest[indexOfNextPayDate];
        CountDaysCalculator calculator = new CountDaysCalculator();
        calculator.setPreviousPayDay(previousDate);
        calculator.setNextPayDay(nextDate);
        calculator.setToday(pricingDate);
        double interest = interestToPay[indexOfNextPayDate];
        switch (countDaysMethod) {
            case COUNT_DAYS_METHOD_ACT_ACT: {
                return calculator.actAct() * interest;
            }
            case COUNT_DAYS_METHOD_30_360: {
                return calculator.thirty360() * interest;
            }
            case COUNT_DAYS_METHOD_ACT_360: {
                return calculator.act360() * interest;
            }
            default: {
                return 0;
            }
        }
    }

    /**
     * @return 现金价格(带息价格)
     */
    public double getDirtyPrice() {
        return cleanPrice + getAccumulateInterest();
    }


}
