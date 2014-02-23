package com.github.dyaitskov.demux.queue1s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Worker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);
    private volatile Job in;
    private final Processor processor;
    private final int index;
    private final SyncBitMap freeWorkerMap;
    private final Object lock = new Object();
    private final Window window;

    public Worker(Processor processor, int index,
                  SyncBitMap freeWorkerMap,
                  Window window) {
        this.processor = processor;
        this.index = index;
        this.freeWorkerMap = freeWorkerMap;
        this.window = window;
    }

    @Override
    public void run() {
        try {
            while (true) {
                freeWorkerMap.setBit(index);
                synchronized (lock) {
                    while (in == null) {
                        logger.debug("{} in is null. sleep.", index);
                        lock.wait();
                    }
                }
                freeWorkerMap.clearBit(index);
                window.insert(in.id, processor.process(in.input));
                in = null;
            }
        } catch (InterruptedException e) {
            logger.info("interrupted");
        } catch (Throwable e) {
            logger.error("interrupted", e);
        }
    }

    public boolean submit(Job job) {
        if (in == null) {
            synchronized (lock) {
                logger.debug("message {} is sent to worker {}.", job, index);
                in = job;
                lock.notify();
                return true;
            }
        }
        return false;
    }
}
