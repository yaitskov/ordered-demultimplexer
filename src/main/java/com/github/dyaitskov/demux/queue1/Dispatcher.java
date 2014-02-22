package com.github.dyaitskov.demux.queue1;

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

    public Dispatcher(Source source, Window window,
                      ExecutorService pool,
                      int threads, Processor processor) {
        this.source = source;
        this.window = window;
        workers = new Worker[threads];
        for (int i = 0; i < threads; ++i) {
            workers[i] = new Worker(processor);
            pool.submit(workers[i]);
            logger.debug("started worker {}", i);
        }
    }

    @Override
    public void run() {
        while (true) {
            Object input = take();
            if (input != null) {
                submit(input);
            }
        }
    }

    private Object take() {
        logger.trace("wait next message");
        try {
            return source.next();
        } catch (Throwable e) {
            logger.error("source threw", e);
            return null;
        }
    }

    private void submit(Object input) {
        Job job = new Job(input, nextMessageId++);
        window.newMessage();
        while (job != null) {
            int i = 0;
            for (; i < workers.length; ++i) {
                if (workers[i].in == null) {
                    logger.debug("message {} is sent to worker {}.", job, i);
                    workers[i].in = job;
                    job = null;
                    break;
                }
                insert(i);
            }
            for (; i < workers.length; ++i) {
                insert(i);
            }
        }
    }

    private void insert(int i) {
        logger.trace("collect out of worker {}.", i);
        Job out = workers[i].out;
        if (out != null) {
            logger.debug("worker {} finished message {}.", i, out);
            window.insert(out.id, out.input);
            workers[i].out = null;
        }
    }
}
