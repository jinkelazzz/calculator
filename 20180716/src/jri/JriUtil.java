package jri;

import org.rosuda.JRI.RList;
import org.rosuda.JRI.Rengine;
import volatility.HistoryPrice;

import java.io.File;
import java.time.LocalDate;

/**
 * @author liangcy
 */
public class JriUtil {
    private static Rengine engine = RengineUtil.getRengineInstance();

    public HistoryPrice getHistoryPrice(String underlyingName, LocalDate startDate, LocalDate endDate) {
        String rFileName = "getWindData.R";
        File path = new File("");
        String filePath = path.getAbsolutePath() + "\\" + rFileName;
        if(!new File(filePath).exists()) {
            return null;
        }
        engine.assign("fileName", filePath);
        engine.assign("underlying", underlyingName);
        engine.assign("start", startDate.toString());
        engine.assign("end", endDate.toString());
        engine.eval("source(fileName)");
        RList list = engine.eval("getWindData(underlying, start, end)").asList();
        //0是ErrorCode;
        if(list.at(0).asInt() != 0) {
            return null;
        }
        //1是windData;
        RList dataList = list.at(1).asList();
        HistoryPrice historyPrice = new HistoryPrice();
        historyPrice.setOpen(dataList.at("OPEN").asDoubleArray());
        historyPrice.setHigh(dataList.at("HIGH").asDoubleArray());
        historyPrice.setLow(dataList.at("LOW").asDoubleArray());
        historyPrice.setClose(dataList.at("CLOSE").asDoubleArray());
        return historyPrice;
    }

}
