package demux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
*/
public class Parallel implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Parallel.class);
    private final BlockingQueue<Integer> in;
    private final SyncBar bar;
    private final int numQueues;
    private final int threadNum;

    public Parallel(BlockingQueue<Integer> in, SyncBar bar,
                    int numQueues, int threadNum)
    {
        this.in = in;
        this.bar = bar;
        this.numQueues = numQueues;
        this.threadNum = threadNum;
    }

    public void run() {
        try {
            while (true) {
                int id = in.take();
                for (int i = 0; i < Integer.MAX_VALUE; ++i);
                int queueId = id % numQueues;
                logger.info("put msg {} to queue {}", id, queueId);
                bar.put(new IdMessage(id), threadNum, queueId);
            }
        } catch (InterruptedException e) {
            logger.info("parallel interrupted");
        }
    }
}
