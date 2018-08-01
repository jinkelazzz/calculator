package test;

import jri.JriUtil;
import jri.RengineUtil;
import org.junit.Test;
import org.rosuda.JRI.Rengine;
import volatility.HistoryPrice;

import java.time.LocalDate;
import java.util.Arrays;

public class JriTest {
    @Test
    public void testJri() {
        Rengine engine = RengineUtil.getRengineInstance();
        if(!engine.waitForR()) {
            System.out.println("load R failed!");
        } else {
            System.out.println("load R success");
        }
        System.out.println(engine.isAlive());
    }

    @Test
    public void test2() {
        LocalDate start = LocalDate.of(2018, 1, 1);
        LocalDate end  = LocalDate.of(2018, 7, 31);
        HistoryPrice historyPrice = new JriUtil().getHistoryPrice("000001.SZ", start, end);
        System.out.println(Arrays.toString(historyPrice.getOpen()));
        System.out.println(Arrays.toString(historyPrice.getClose()));
        System.out.println(Arrays.toString(historyPrice.getHigh()));
        System.out.println(Arrays.toString(historyPrice.getLow()));
    }
}
