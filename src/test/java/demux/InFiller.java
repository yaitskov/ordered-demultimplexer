package demux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*/
public class InFiller implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(InFiller.class);
    private final WaitPutQueue<Integer> in;
    private final int nMessages;

    public InFiller(WaitPutQueue<Integer> in, int nMessages) {
        this.in = in;
        this.nMessages = nMessages;
    }

    public void run() {
        logger.info("in filter started");
        for (int i = 0; i < nMessages; ++i) {
            if (in.put(i)) {
                logger.info("put message to fill queue {}", i);
            } else {
                logger.info("in filter lost message {}", i);
            }
        }
        logger.info("in filter ended");
    }
}
