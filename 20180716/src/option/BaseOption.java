package option;

import java.io.Serializable;

/**
 * @author liangcy
 */
public abstract class BaseOption implements Serializable {
    /**
     * 期权类型: 看涨/看跌
     */
    public static final String OPTION_TYPE_CALL = "call";
    public static final String OPTION_TYPE_PUT = "put";
    /**
     * 期权模型名称
     */
    public static final String OPTION_METHOD_BSM = "bsm";
    public static final String OPTION_METHOD_HESTON = "heston";
    public static final String OPTION_METHOD_BS = "bs";
    public static final String OPTION_METHOD_SABR = "sabr";
    public static final String OPTION_METHOD_BAW = "baw";
    public static final String OPTION_METHOD_CURRAN = "curran";

    /**
     * 障碍类型: 敲入/敲出
     */
    public static final String BARRIER_TYPE_IN = "in";
    public static final String BARRIER_TYPE_OUT = "out";

    /**
     * 障碍方向: 向上/向下
     */
    public static final String BARRIER_DIRECTION_UP = "up";
    public static final String BARRIER_DIRECTION_DOWN = "down";

    /**
     * 给付时间: 触碰给付/到期给付
     */
    public static final String PAYOFF_TYPE_HIT = "hit";
    public static final String PAYOFF_TYPE_EXPIRE = "expire";


    /**
     * 最简单的解析解统称 bsm;
     *
     * @return option price (每个option都至少应有一个解析解);
     */
    public abstract double bsm();

    /**
     * 打印参数
     *
     * @return
     */
    @Override
    public abstract String toString();

}
