package jri;

import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import java.util.List;

/**
 * 构造Rengine
 * @author liangcy
 */
public class RengineUtil {
    private static Rengine engine;

    private RengineUtil() {}

    public static Rengine getRengineInstance() {
        if(engine == null) {
            engine = new Rengine(null, false, null);
            requireLib("WindR");
        }
        return engine;
    }

    private static boolean requireLib(String libName) {
        if(engine == null) {
            return false;
        }
        REXP exp = engine.eval(rLibExpression(libName));
        return exp.asBool().isTRUE();
    }

    private static String rLibExpression(String libName) {
        return "require" + "(" + libName + ")";
    }

}
