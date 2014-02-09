package demux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class OrderMultiplexer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(OrderMultiplexer.class);
    private final Handler handler;
    private final int myIndex;
    private final SyncBar bar;

    public OrderMultiplexer(Handler handler,
                            int myIndex,
                            SyncBar bar)
    {
        this.handler = handler;
        this.myIndex = myIndex;
        this.bar = bar;
    }

    @Override
    public void run() {
        Message msg;
        s1:
        while (true) {
            try {
                msg = bar.take(myIndex);
            } catch (InterruptedException e) {
                logger.info("interrupted index {}", myIndex);
                break;
            }
            s2:
            while (true) {
                int myId = msg.getId();
                for (int i = 0; i < bar.length(); ++i) {
                    bar.waitNotLess(i, myId);
                }
                Message next = bar.poll(myIndex);
                while (next != null) {
                    if (next.getId() < myId) {
                        handler.pass(next);
                    } else {
                        handler.pass(msg);
                        msg = next;
                        continue s2;
                    }
                }
                handler.pass(msg);
                continue s1;
            }
        }
    }
}
