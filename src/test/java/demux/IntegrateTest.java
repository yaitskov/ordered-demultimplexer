package demux;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class IntegrateTest {
    private static final Logger logger = LoggerFactory.getLogger(IntegrateTest.class);

    public static final int QUEUE_CAPACITY = 1000;
    public static final int N_MESSAGES = 3;
    public static final int NUM_QUEUES = 1;
    public static final int NUM_THREADS = 2;
    public static final int IN_QUEUE_CAPACITY = 1000;

    @Test
    public void doIt() throws InterruptedException {
        logger.info("doIt started; max messages {}", N_MESSAGES);
        final SyncBar bar = new SyncBar(NUM_QUEUES, NUM_THREADS,
                QUEUE_CAPACITY, Integer.MIN_VALUE);
        ExecutorService pool = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(NUM_QUEUES);
        List<IdHandler> handlers = new ArrayList<IdHandler>();
        for (int i = 0; i < NUM_QUEUES; ++i) {
            IdHandler handler = new IdHandler(latch, N_MESSAGES, i, NUM_QUEUES);
            handlers.add(handler);
            pool.submit(new OrderMultiplexer(handler, i, bar));
        }
        BlockingQueue<Integer> in = new ArrayBlockingQueue<Integer>(IN_QUEUE_CAPACITY);
        for (int i = 0; i < NUM_THREADS; ++i) {
            pool.submit(new Parallel(in, bar, NUM_QUEUES, i));
        }
        pool.submit(new InFiller(in, N_MESSAGES));
        latch.await();
        logger.info("All messages arrived. Checking...");
        for (IdHandler handler : handlers) {
            Assert.assertEquals(0, handler.getNumProblems());
        }
        pool.shutdown();
    }
}
