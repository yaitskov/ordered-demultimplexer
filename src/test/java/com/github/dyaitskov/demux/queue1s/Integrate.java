package com.github.dyaitskov.demux.queue1s;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.junit.Assert;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class Integrate {
    private static final Logger logger = LoggerFactory.getLogger(SourceImpl.class);

    public void useCase(int threads, int windowSize, int nMessages)
            throws InterruptedException {
        ExecutorService pool = Executors.newCachedThreadPool();
        try {
            Window window = new Window(windowSize);
            Dispatcher dispatcher = new Dispatcher(new SourceImpl(nMessages),
                    window, pool, threads, new ProcessorImpl());

            pool.submit(dispatcher);
            int expected = 0;
            StopWatch watch = new StopWatch();

            while (true) {
                Integer n = (Integer) window.consume();
                Assert.assertEquals(-expected, (int) n);
                ++expected;
                if (expected == nMessages - 1) {
                    logger.debug("end");
                    break;
                }
            }
            watch.stop();
            PeriodFormatter periodFormatter = new PeriodFormatterBuilder()
                    .appendSeconds()
                    .appendSuffix("s ")
                    .appendMillis()
                    .appendSuffix("ms ")
                    .toFormatter();

            logger.debug("threads {}; window {}; duration {};",
                    threads, windowSize,
                    periodFormatter.print(
                            new Period(watch.getElapsedTime())));
        } finally {
            pool.shutdownNow();
        }
    }
}
