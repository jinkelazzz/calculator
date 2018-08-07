package jri;

import org.rosuda.JRI.Rengine;

/**
 * 枚举类实现单例
 * @author liangcy
 */
public enum RengineSingleton {
    /**
     * 枚举实例
     */
    INSTANCE;

    private final Rengine rengine = new Rengine(null, false, null);

    public Rengine getRengineInstance() {
        return rengine;
    }
}
