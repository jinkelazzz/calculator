package calculator.utility;

/**
 * @author liangcy
 */
public class NewtonIterationParams {
    private int iterations = 1000;
    private double tol = 1e-12;

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = Math.min(10000, iterations);
    }

    public double getTol() {
        return tol;
    }

    public void setTol(double tol) {
        this.tol = Math.max(1e-10, tol);
    }
}
