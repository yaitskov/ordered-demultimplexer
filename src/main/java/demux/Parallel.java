package demux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*/
public abstract class Parallel<T> implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Parallel.class);
    private final WaitPutQueue<T> in;
    private final SyncBar bar;
    private final int threadNum;

    public Parallel(WaitPutQueue<T> in, SyncBar bar, int threadNum) {
        this.in = in;
        this.bar = bar;
        this.threadNum = threadNum;
    }

    public void run() {
        bar.setThreadFlag(threadNum, ParallelThreadStatus.RUNNING);
        logger.info("thread {} running", threadNum);
        try {
            while (true) {
                createAndEmitMessage();
            }
        } catch (Throwable e) {
            logger.info("thread {} interrupted", threadNum, e);
        } finally {
            bar.setThreadFlag(threadNum, ParallelThreadStatus.SLEEP);
            logger.info("thread {} ending", threadNum);
        }
    }

    protected abstract void createAndEmitMessage() throws InterruptedException;

    protected T take() throws InterruptedException {
        while (true) {
            T message = in.poll();
            if (message == null) {
                logger.info("not message for thread {}", threadNum);
                bar.setThreadFlag(threadNum, ParallelThreadStatus.SLEEP);
                in.waitPut();
                logger.info("thread {} check for new message", threadNum);
                bar.setThreadFlag(threadNum, ParallelThreadStatus.RUNNING);
            } else {
                logger.info("message {} polled in thread {}", message, threadNum);
                return message;
            }
        }
    }

    protected void put(Message message, int queueId) {
        logger.info("put {} to bar at thread {}", message, threadNum);
        bar.put(message, threadNum, queueId);
    }
}
