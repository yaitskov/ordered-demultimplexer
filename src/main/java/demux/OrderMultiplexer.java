package demux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class OrderMultiplexer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(OrderMultiplexer.class);
    private final Handler handler;
    private final int queueIndex;
    private final SyncBar bar;

    public OrderMultiplexer(Handler handler,
                            int queueIndex,
                            SyncBar bar)
    {
        this.handler = handler;
        this.queueIndex = queueIndex;
        this.bar = bar;
    }

    @Override
    public void run() {
        Message msg;
        while (true) {
            try {
                msg = bar.takeOrWait(queueIndex);
            } catch (InterruptedException e) {
                logger.info("interrupted queue index {}", queueIndex);
                break;
            }
            int msgId = msg.getId();
            while (true) {
                try {
                    bar.ensureIdIsMax(msgId, queueIndex);
                } catch (InterruptedException e) {
                    logger.info("ignore interrupted queue index {}", queueIndex);
                    continue;
                }
                Message next;
                while ((next = bar.poll(queueIndex)) != null) {
                    if (next.getId() < msgId) {
                        handler.pass(next);
                    } else {
                        handler.pass(msg);
                        msg = next;
                        break;
                    }
                }
                handler.pass(msg);
                break;
            }
        }
    }
}
