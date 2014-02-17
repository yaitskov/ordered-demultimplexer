package demux.speed;

import demux.Handler;
import demux.Message;
import demux.OrderMultiplexer;
import demux.Parallel;
import demux.SyncBar;
import demux.WaitPutQueue;
import demux.WaitPutQueueImpl;
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

    class MessageBuffer implements Message {
        public final int id;
        public final int max;

        MessageBuffer(int id, int max) {
            this.id = id;
            this.max = max;
        }

        public int getId() {
            return id;
        }
    }

    class Demuxed implements Runnable {
        public static final int QUEUE_ID = 0;
        private final int numberThreads;
        private final int numberMessages;
        private final int max;

        Demuxed(int numberThreads, int max, int numberMessages) {
            this.numberThreads = numberThreads;
            this.numberMessages = numberMessages;
            this.max = max;
        }

        public void run() {
            final SyncBar bar = new SyncBar(1, numberThreads, 1000);
            final WaitPutQueue<MessageBuffer> inQueue = new WaitPutQueueImpl<MessageBuffer>(
                    new ArrayDeque<MessageBuffer>(1000));
            ExecutorService pool = Executors.newCachedThreadPool();
            final CountDownLatch latch = new CountDownLatch(numberMessages);
            pool.submit(new OrderMultiplexer(
                    new Handler() {
                        public void pass(Message message) {
                            latch.countDown();
                        }
                    }, QUEUE_ID, bar));

            for (int iThread = 0; iThread < numberThreads; ++iThread) {
                pool.submit(new Parallel<MessageBuffer>(inQueue, bar, iThread) {
                    private final UnitWork unzipper = new UnitWork();
                    protected void createAndEmitMessage() throws InterruptedException {
                        MessageBuffer message = take();
                        unzipper.doit(message.max);
                        put(message, QUEUE_ID);
                    }
                });
            }

            pool.submit(new Runnable() {
                public void run() {
                    for (int i = 0; i < numberMessages; ++i) {
                        while (!inQueue.put(new MessageBuffer(i, max))) {
                            logger.warn("in queue is full");
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            pool.shutdownNow();
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
