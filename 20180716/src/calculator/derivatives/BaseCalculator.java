package calculator.derivatives;

import calculator.utility.CalculatorError;

/**
 * @author liangcy
 * 最基本的计算器，计算各种衍生品
 */
public abstract class BaseCalculator {
    /**
     * 枚举类，返回错误类型和错误值。
     */
    private CalculatorError error = CalculatorError.NOT_CALCULATE;

    /**
     * 返回结果值
     */
    private double result = 0.0;


    public CalculatorError getError() {
        return error;
    }

    void setError(CalculatorError error) {
        this.error = error;
    }

    public double getResult() {
        return result;
    }

    void setResult(double result) {
        this.result = result;
    }

    /**
     * 计算价格
     */
    public abstract void calculatePrice();

    /**
     * 重置计算器
     */
    public void resetCalculator() {
        setError(CalculatorError.NOT_CALCULATE);
        setResult(0.0);
    }

    public boolean isNormal() {
        return 0 == error.getIndex();
    }


}
