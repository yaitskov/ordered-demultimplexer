package com.github.dyaitskov.demux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
*/
public class IdHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(IdHandler.class);
    private final int maxMessages;
    private final CountDownLatch latch;
    private int gotMessages;
    private final int queueId;
    private Message previous;
    private volatile int numProblems;

    IdHandler(CountDownLatch latch, int maxMessages,
              int queueId, int numQueues)
    {
        this.latch = latch;
        this.maxMessages = maxMessages / numQueues
                + (((maxMessages % numQueues) > queueId) ? 1 : 0);
        this.queueId = queueId;
        logger.info("max messages {} for queue {}", this.maxMessages, queueId);
    }

    public void pass(Message message) {
        logger.info("pass message {} to queueId {}",
                message.getId(), queueId);
        if (previous == null) {
            previous = message;
        } else if (previous.getId() >= message.getId()) {
            logger.error("queue id {} bad order {} >= {}",
                    new Object[] {queueId, previous.getId(), message.getId()});
            ++numProblems;
        } else {
            previous = message;
        }
        if (++gotMessages == maxMessages) {
            logger.info("end queue id {}", queueId);
            latch.countDown();
        } else if (gotMessages > maxMessages) {
            logger.error("overflow of queue {} with {}",
                    queueId, message.getId());
            ++numProblems;
        }
    }

    public int getNumProblems() {
        return numProblems;
    }
}
