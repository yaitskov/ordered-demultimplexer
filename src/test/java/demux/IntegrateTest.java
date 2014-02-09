package demux;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class IntegrateTest {
    private static final Logger logger = LoggerFactory.getLogger(IntegrateTest.class);

    public static final int QUEUE_CAPACITY = 1000;
    public static final int N_MESSAGES = 10;
    public static final int NUM_QUEUES = 2;
    public static final int NUM_THREADS = 1;
    public static final int IN_QUEUE_CAPACITY = 1000;

    private static class IdMessage implements Message {
        private final int id;

        private IdMessage(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    private static class IdHandler implements Handler {
        private final int maxMessages;
        private final CountDownLatch latch;
        private int gotMessages;
        private final int queueId;
        private Message previous;
        private volatile int numIllOrderedMessages;

        private IdHandler(CountDownLatch latch, int maxMessages,
                          int queueId, int numQueues)
        {
            this.latch = latch;
            this.maxMessages = maxMessages / numQueues
                    + maxMessages % numQueues > queueId ? 1 : 0;
            this.queueId = queueId;
        }

        public void pass(Message message) {
            if (previous == null) {
                previous = message;
            } else if (previous.getId() >= message.getId()) {
                logger.error("queue id {} bad order");
                ++numIllOrderedMessages;
            } else {
                previous = message;
            }
            if (++gotMessages == maxMessages) {
                logger.info("end queue id {}", queueId);
                latch.countDown();
            } else if (gotMessages > maxMessages) {
                logger.error("overflow");
            }
        }

        public int getNumIllOrderedMessages() {
            return numIllOrderedMessages;
        }
    }

    private static class Parallel implements Runnable {
        private final BlockingQueue<Integer> in;
        private final SyncBar bar;
        private final int numQueues;

        public Parallel(BlockingQueue<Integer> in, SyncBar bar, int numQueues) {
            this.in = in;
            this.bar = bar;
            this.numQueues = numQueues;
        }

        public void run() {
            try {
                while (true) {
                    int id = in.take();
                    for (int i = 0; i < Integer.MAX_VALUE; ++i);
                    bar.put(new IdMessage(id), id % numQueues);
                }
            } catch (InterruptedException e) {
                logger.info("parallel interrupted");
            }
        }
    }

    private static class InFiller implements Runnable {
        private final BlockingQueue<Integer> in;
        private final int nMessages;

        public InFiller(BlockingQueue<Integer> in, int nMessages) {
            this.in = in;
            this.nMessages = nMessages;
        }

        public void run() {
            logger.info("in filter started");
            for (int i = 0; i < nMessages; ++i) {
                if (!in.offer(i)) {
                    logger.info("in filter lost message {}", i);
                }
            }
            logger.info("in filter ended");
        }
    }

    @Test
    public void doIt() throws InterruptedException {
        final SyncBar bar = new SyncBar(NUM_QUEUES, QUEUE_CAPACITY, Integer.MIN_VALUE);
        ExecutorService pool = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(NUM_QUEUES);
        List<IdHandler> handlers = new ArrayList<IdHandler>();
        for (int i = 0; i < NUM_QUEUES; ++i) {
            IdHandler handler = new IdHandler(latch, N_MESSAGES, i, NUM_QUEUES);
            handlers.add(handler);
            pool.submit(new OrderMultiplexer(handler, i, bar));
        }
        BlockingQueue<Integer> in = new ArrayBlockingQueue<Integer>(IN_QUEUE_CAPACITY);
        for (int i = 0; i < NUM_THREADS; ++i) {
            pool.submit(new Parallel(in, bar, NUM_QUEUES));
        }
        pool.submit(new InFiller(in, N_MESSAGES));
        latch.await();
        logger.info("All messages arrived. Checking...");
        for (IdHandler handler : handlers) {
            Assert.assertEquals(0, handler.getNumIllOrderedMessages());
        }
        pool.shutdown();
    }
}
