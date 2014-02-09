package fibo;

import org.perf4j.StopWatch;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

/**
 */
public class MainFibonacci {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        if (args.length != 1) {
            System.err.println("pass fibonacci number");
            return;
        }
        if (!args[0].matches("[0-9]+")) {
            System.err.println("not a integer number");
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.err.println("exit hook");
                System.exit(1);
            }
        });
        int presentedProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println("processors " + presentedProcessors);
        ForkJoinPool pool = new ForkJoinPool(presentedProcessors);
        BigInteger n = new BigInteger(args[0]);
        ClassicFibonacci classic = new ClassicFibonacci();
        ForkJoinFibo forkJoin = new ForkJoinFibo(classic, n);
        StopWatch perf = new StopWatch();
        pool.invoke(forkJoin);
        String result2 = forkJoin.get().toString();
        perf.stop();
        System.out.println("forkJoi " + perf.getElapsedTime() + "; result " + result2);
        perf.start();
        String result = classic.calc(n).toString();
        perf.stop();
        System.out.println("classic " + perf.getElapsedTime() + "; result " + result);

        pool.shutdown();


    }
}
