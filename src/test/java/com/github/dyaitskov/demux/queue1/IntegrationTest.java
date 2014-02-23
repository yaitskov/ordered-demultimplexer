package com.github.dyaitskov.demux.queue1;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class IntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
    public static final int N_MESSAGES = 950;
    public static final int WINDOW_SIZE = 5;
    public static final int THREADS = 8;

    static class SourceImpl implements Source {
        int n;
        int i;
        SourceImpl(int n) {
            this.n = n;
        }

        public Object next() throws InterruptedException {
            if (i < n) {
                logger.debug("source return {}", i);
                return i++;
            }
            throw new InterruptedException("stop");
        }
    }

    static class ProcessorImpl implements Processor {
        public Object process(Object input) {
            logger.debug("processor {}", input);
            return - ((Integer) input);
        }
    }

    @Test
    public void integrate() {
        for (int threads = 1; threads < 8; ++threads) {
            for (int windowSize = 1; windowSize < 16; ++windowSize) {
                logger.debug("--window {} --threads {} ------------------",
                        windowSize, threads);
                useCase(threads, windowSize, 2000);
            }
        }
    }

    public void useCase(int threads, int windowSize, int nMessages) {
        ExecutorService pool = Executors.newCachedThreadPool();
        try {
            Window window = new Window(windowSize);
            Dispatcher dispatcher = new Dispatcher(new SourceImpl(nMessages),
                    window, pool, threads, new ProcessorImpl());

            pool.submit(dispatcher);
            int expected = 0;
            StopWatch watch = new StopWatch();
            int nullInLine = 0;
            while (true) {
                Integer n = (Integer) window.consume();
                if (n == null) {
                    Assert.assertTrue("lock", ++nullInLine < 1000);
                    logger.debug("null got for {}", expected);
                    continue;
                } else {
                    logger.debug("got {}", n);
                    nullInLine = 0;
                }
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