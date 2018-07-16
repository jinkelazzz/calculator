package calculator.utility;

import option.BaseSingleOption;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liangcy
 */
public class MonteCarlo {
    /**
     * nodes 不包含初始的 s0, 实际上 price path 的长度是 nodes+1;
     * [1 - 10,000]
     */
    private int nodes = 500;
    /**
     * 蒙特卡洛模拟路径条数, [1 - 500,000]
     */
    private int pathSize = 10000;
    /**
     * 用来计算误差。默认三倍标准差。
     */
    private double monteCarloErrorMult = 3.0;




    public MonteCarlo() {

    }

    public MonteCarlo(int nodes, int pathSize) {
        this.setNodes(nodes);
        this.setPathSize(pathSize);
    }

    public double getMonteCarloErrorMult() {
        return monteCarloErrorMult;
    }

    public void setMonteCarloErrorMult(double monteCarloErrorMult) {
        this.monteCarloErrorMult = monteCarloErrorMult;
    }

    public int getNodes() {
        return nodes;
    }

    public void setNodes(int nodes) {
        nodes = Math.max(1, nodes);
        nodes = Math.min(10000, nodes);
        this.nodes = nodes;
    }

    public int getPathSize() {
        return pathSize;
    }

    public void setPathSize(int pathSize) {
        pathSize = Math.max(1, pathSize);
        pathSize = Math.min(500000, pathSize);
        this.pathSize = pathSize;
    }

    private double[] generateStandardNormalRandomNumber() {
        double[] result = new double[nodes];
        for (int i = 0; i < nodes; i++) {
            result[i] = CalculateUtil.normalRandom();
        }
        return result;
    }

    public List<double[]> generateStandardNormalRandomNumberList() {
        List<double[]> list = new ArrayList<>(pathSize);
        for (int i = 0; i < pathSize; i++) {
            list.add(generateStandardNormalRandomNumber());
        }
        return list;
    }

    public List<double[]> generateMonteCarloPathList(BaseSingleOption option, List<double[]> randomNumsList) {
        List<double[]> list = new ArrayList<>(randomNumsList.size());
        for (double[] randomNums : randomNumsList) {
            list.add(generateMonteCarloPath(option, randomNums));
        }
        return list;
    }

    private double[] generateMonteCarloPath(BaseSingleOption option, double[] randomNums) {
        double s = option.getUnderlying().getSpotPrice();
        double r = option.getUnderlying().getRiskFreeRate();
        double q = option.getUnderlying().getDividendRate();
        double t = option.getVanillaOptionParams().getTimeRemaining();
        double vol = option.getVanillaOptionParams().getVolatility();
        double deltaT = t / nodes;
        double[] pricePath = new double[nodes + 1];
        pricePath[0] = s;
        //create log normal return using random numbers;
        double rtn;
        for (int i = 0; i < nodes; i++) {
            rtn = randomNums[i] * vol * Math.sqrt(deltaT) + ((r - q) - vol * vol / 2) * deltaT;
            pricePath[i + 1] = pricePath[i] * Math.exp(rtn);
        }
        return pricePath;
    }

    public static double[] getTimePoints(double t, double[] pricePath) {
        double[] timePoints = new double[pricePath.length];
        for (int i = 0; i < pricePath.length; i++) {
            timePoints[i] = t * i / (pricePath.length - 1);
        }
        return timePoints;
    }

}
