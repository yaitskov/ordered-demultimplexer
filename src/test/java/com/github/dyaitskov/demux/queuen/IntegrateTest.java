package com.github.dyaitskov.demux.queuen;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class IntegrateTest {
    private static final Logger logger = LoggerFactory.getLogger(IntegrateTest.class);

    public static final int N_MESSAGES = 3000;
    public static final int IN_QUEUE_CAPACITY = 1000;

    @Test
    public void doIt() throws InterruptedException {
        for (int numThreads : Arrays.asList(1, 2, 3, 5, 7, 8)) {
            for (int numQueues : Arrays.asList(1, 2, 4, 6, 8)) {
                for (int queueSize : Arrays.asList(1000, 500, 100)) {
                    build(numThreads, numQueues, queueSize);
                }
            }
        }
    }

    void build(int numberThreads, int numberQueues, int queueCapacity)
            throws InterruptedException
    {
        logger.info("started threads f{}; queues {}; queue capacity {}",
                numberThreads, numberQueues, queueCapacity);
        final SyncBar bar = new SyncBar(numberQueues, numberThreads, queueCapacity);
        ExecutorService pool = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(numberQueues);
        List<IdHandler> handlers = new ArrayList<IdHandler>();
        for (int i = 0; i < numberQueues; ++i) {
            IdHandler handler = new IdHandler(latch, N_MESSAGES, i, numberQueues);
            handlers.add(handler);
            pool.submit(new OrderMultiplexer(handler, i, bar));
        }
        WaitPutQueue<Integer> in = new WaitPutQueueImpl<Integer>(
                new ArrayDeque<Integer>(IN_QUEUE_CAPACITY));
        for (int thrId = 0; thrId < numberThreads; ++thrId) {
            pool.submit(new ParallelImpl(in, bar, thrId, numberQueues));
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
