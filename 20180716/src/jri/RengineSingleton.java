package jri;

import org.rosuda.JRI.Rengine;

public enum RengineSingleton {
    /**
     * 枚举实例
     */
    INSTANCE;

    private Rengine rengine;

    private RengineSingleton() {
        rengine = new Rengine(null, false, null);
    }

    public Rengine getRengineInstance() {
        return rengine;
    }
}
