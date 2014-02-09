package demux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 */
public class PollExecute implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PollExecute.class);
    private final Handler handler;
    private final BlockingQueue<Message> queue;

    public PollExecute(Handler handler, BlockingQueue<Message> queue) {
        this.handler = handler;
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                handler.pass(queue.take());
            }
        } catch (InterruptedException e) {
            logger.debug("interrupted");
        }
    }
}
