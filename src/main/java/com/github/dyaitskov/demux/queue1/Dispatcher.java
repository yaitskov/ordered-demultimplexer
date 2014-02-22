package com.github.dyaitskov.demux.queue1;

import java.util.concurrent.ExecutorService;

/**
 */
public class Dispatcher implements Runnable {
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
        }
    }

    @Override
    public void run() {
        while (true) {
            Job job = new Job(source.next(), nextMessageId++);
            window.newMessage();
            while (job != null) {
                int i = 0;
                for (; i < workers.length; ++i) {
                    if (workers[i].in == null) {
                        workers[i].in = job;
                        job = null;
                        break;
                    }
                    insert(i);
                }
                for (; i < workers.length; ++i) {
                    insert(i);
                }
                Thread.yield();
            }
        }
    }

    private void insert(int i) {
        Job out = workers[i].out;
        if (out != null) {
            window.insert(out.id, out.input);
            workers[i].out = null;
        }
    }
}
