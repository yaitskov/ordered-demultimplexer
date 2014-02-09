package fibo;

import java.math.BigInteger;
import java.util.concurrent.RecursiveTask;

/**
 */
public class ForkJoinFibo extends RecursiveTask<BigInteger> {

    private final ClassicFibonacci base;
    private final BigInteger n;

    public ForkJoinFibo(ClassicFibonacci base, BigInteger n) {
        this.base = base;
        this.n = n;
    }

    @Override
    protected BigInteger compute() {
        if (n.compareTo(BigInteger.TEN.add(BigInteger.TEN)) == 1) {
            // more than 10
            ForkJoinFibo a = new ForkJoinFibo(base, n.subtract(BigInteger.ONE));
            a.fork();
            ForkJoinFibo b = new ForkJoinFibo(base, n.subtract(BigInteger.ONE).subtract(BigInteger.ONE));
            return b.compute().add(a.join());
        } else {
            return base.calc(n);
        }
    }
}
