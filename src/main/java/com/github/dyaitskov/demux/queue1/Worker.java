package com.github.dyaitskov.demux.queue1;

/**
 */
public class Worker implements Runnable {
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
                Thread.yield();
            } else {
                out = new Job(processor.process(in.input), in.id);
                in = null;
            }
        }
    }
}
