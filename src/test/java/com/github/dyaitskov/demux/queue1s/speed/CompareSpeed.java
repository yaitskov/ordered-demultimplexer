package com.github.dyaitskov.demux.queue1s.speed;

import com.github.dyaitskov.demux.queue1s.Dispatcher;
import com.github.dyaitskov.demux.queue1s.Processor;
import com.github.dyaitskov.demux.queue1s.SourceImpl;
import com.github.dyaitskov.demux.queue1s.Window;
import com.github.dyaitskov.demux.queuen.Handler;
import com.github.dyaitskov.demux.queuen.Message;
import com.github.dyaitskov.demux.queuen.OrderMultiplexer;
import com.github.dyaitskov.demux.queuen.Parallel;
import com.github.dyaitskov.demux.queuen.SyncBar;
import com.github.dyaitskov.demux.queuen.WaitPutQueue;
import com.github.dyaitskov.demux.queuen.WaitPutQueueImpl;
import org.jboss.netty.buffer.ChannelBuffer;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.junit.Test;
import org.perf4j.StopWatch;
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
public class CompareSpeed {
    private static final Logger logger = LoggerFactory.getLogger(CompareSpeed.class);
    public static final int NUMBER_MESSAGES = 4000;

    class UnitWork {
        public void doit(int max) {
            byte b;
            for (long i = 0; i < max; ++i) {
                b = (byte) (i % 100);
                if (b > 1000) {
                    return;
                }
            }
        }
    }

    class SingleThreaded implements Runnable {
        private final int max;
        private final int numberMessages;
        private final UnitWork unzipper;
        public SingleThreaded(int max,
                              UnitWork unzipper,
                              int numberMessages) {
            this.max = max;
            this.numberMessages = numberMessages;
            this.unzipper = unzipper;
        }

        public void run() {
            for (int i = 0; i < numberMessages; ++i) {
                unzipper.doit(max);
            }
        }
    }

    class Demuxed implements Runnable {
        private final int numberThreads;
        private final int numberMessages;
        private final int max;

        Demuxed(int numberThreads, int max, int numberMessages) {
            this.numberThreads = numberThreads;
            this.numberMessages = numberMessages;
            this.max = max;
        }

        public void run() {
            ExecutorService pool = Executors.newCachedThreadPool();
            try {
                Window window = new Window(1000);
                Dispatcher dispatcher = new Dispatcher(new SourceImpl(numberMessages),
                        window, pool, numberThreads,
                        new Processor() {
                            private final UnitWork unzipper = new UnitWork();
                            public Object process(Object input) {
                                unzipper.doit(max);
                                return 1;
                            }
                        });
                pool.submit(dispatcher);
                int expected = 0;
                while (true) {
                    window.consume();
                    if (++expected == numberMessages) {
                        logger.debug("end");
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                pool.shutdownNow();
            }
        }
    }

    interface TestFactory {
        Runnable create(int numberThreads, int max);
        String getName();
    }

    class DemuxedTestFactory implements TestFactory {
        public Runnable create(int numberThreads, int max) {
            return new Demuxed(numberThreads, max, NUMBER_MESSAGES);
        }

        public String getName() {
            return "demux";
        }
    }

    class SingleThreadTestFactory implements TestFactory {
        public Runnable create(int numberThreads,
                               int max) {
            return new SingleThreaded(max, new UnitWork(), NUMBER_MESSAGES);
        }

        public String getName() {
            return "single";
        }
    }

    int numberCores() {
        Runtime r = Runtime.getRuntime();
        return r.availableProcessors();
    }

    PeriodFormatter periodFormatter = new PeriodFormatterBuilder()
            .appendSeconds()
            .appendSuffix("s ")
            .appendMillis()
            .appendSuffix("ms ")
            .toFormatter();

    @Test
    public void one() {
        int cores = numberCores();
        logger.info("available CPU cores {}", cores);
        logger.info("compare with strait single threaded sequential processing");
        List<ChannelBuffer> buffers = new ArrayList<ChannelBuffer>();
        List<TestFactory> factories = Arrays.asList(
                new SingleThreadTestFactory(),
                new DemuxedTestFactory());

        for (int max : Arrays.asList(1000, 2000, 5000, 10000, 20000, 40000, 60000, 1 << 20)) {
            for (int numThreads = 1; numThreads < cores; numThreads += 1) {
                for (TestFactory testFactory : factories) {
                    iteration(max, testFactory, numThreads);
                }
            }
        }
    }

    private void iteration(int max,
                           TestFactory testFactory,
                           int numThreads)
    {
        StopWatch time = new StopWatch();
        testFactory.create(numThreads, max).run();
        time.stop();
        logger.info("{}",
                String.format(
                        "threads %3d; unit duration: %4.4f msb; max %8d; impl %10s; time %s",
                        numThreads,
                        (double) time.getElapsedTime() / (double) NUMBER_MESSAGES,
                        max,
                        testFactory.getName(),
                        periodFormatter.print(new Period(time.getElapsedTime()))));
    }
}
