package calculator.utility;

/**
 * 计算器错误枚举类
 *
 * @author liangcy
 * @date 2018-06-15
 */
public enum CalculatorError {

    /**
     * 未计算
     */
    NOT_CALCULATE(-1),
    /**
     * 计算正常
     */
    NORMAL(0),
    /**
     * 方法不存在
     */
    NOT_FOUND_METHOD(1),
    /**
     * 计算失败, 通常是空指针报错
     */
    CALCULATE_FAILED(2),
    /**
     * 达到迭代次数上限, 牛顿法用
     */
    REACH_MAX_ITERATION(3),
    /**
     * 结果为NaN, 通常是数据有误, 例如波动率传入负数
     */
    CALCULATE_NAN(4),
    /**
     * 不支持的方法, 例如蒙特卡洛计算隐含波动率或者美式期权用蒙特卡洛计算
     */
    UNSUPPORTED_METHOD(5);

    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    CalculatorError(int index) {
        this.index = index;
    }
}
