package com.github.dyaitskov.demux.queue1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class Worker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);
    public volatile Job in;
    public volatile Job out;
    private final Processor processor;

    public Worker(Processor processor) {
        this.processor = processor;
    }

    @Override
    public void run() {
        while (true) {
            if (in == null || out != null) {
                if (Thread.interrupted()) {
                    logger.info("interruption request. break cycle.");
                    break;
                }
//                Thread.yield();
            } else {
                try {
                    out = new Job(processor.process(in.input), in.id);
                } catch (Throwable e) {
                    logger.error("lost job id {}", in.id, e);
                }
                in = null;
            }
        }
    }
}
