package com.github.dyaitskov.demux.queue1s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 */
public class Dispatcher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);
    private final Source source;
    private final Window window;
    private int nextMessageId;
    private final Worker[] workers;
    private final SyncBitMap freeWorkerMap;

    public Dispatcher(Source source, Window window,
                      ExecutorService pool,
                      int threads, Processor processor) {
        this.source = source;
        this.window = window;
        freeWorkerMap = new SyncBitMap(threads);
        workers = new Worker[threads];

        for (int i = 0; i < threads; ++i) {
            workers[i] = new Worker(processor, i, freeWorkerMap, window);
            pool.submit(workers[i]);
            logger.debug("started worker {}", i);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                logger.trace("wait next message");
                Object input = source.next();
                if (input != null) {
                    submit(input);
                }
            }
        } catch (InterruptedException e) {
            logger.info("interrupted");
        } catch (Throwable e) {
            logger.error("interrupted", e);
        }
    }

    private void submit(Object input) throws InterruptedException {
        Job job = new Job(input, nextMessageId++);
        window.reserveCell();
        while (true) {
            for (int wid : freeWorkerMap.findSetBits()) {
                if (workers[wid].submit(job)) {
                    return;
                }
            }
        }
    }
}
