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
        } catch (InterruptedException e) {
            logger.info("thread {} interrupted {}", threadNum, e);
        } finally {
            logger.info("thread {} ending", threadNum);
        }
    }

    protected abstract void createAndEmitMessage() throws InterruptedException;

    protected T take() throws InterruptedException {
        while (true) {
            T message = in.poll();
            if (message == null) {
                bar.setThreadFlag(threadNum, ParallelThreadStatus.SLEEP);
                in.waitPut();
                bar.setThreadFlag(threadNum, ParallelThreadStatus.RUNNING);
            } else {
                return message;
            }
        }
    }

    protected void put(Message message, int queueId) {
        bar.put(message, threadNum, queueId);
    }
}
