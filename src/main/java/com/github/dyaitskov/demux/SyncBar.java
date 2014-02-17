package com.github.dyaitskov.demux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 */
public class SyncBar {
    private static final Logger logger = LoggerFactory.getLogger(SyncBar.class);
    /**
     * numbers of last message ids threads put into their queues.
     */
    private final AtomicIntegerArray lastMsgIds;
    private final AtomicReferenceArray<ParallelThreadStatus> threadStatuses;
    private final AtomicReferenceArray<Object> queueLocks;
    private final AtomicReferenceArray<Object> threadLocks;
    private final AtomicReferenceArray<PriorityQueue<Message>> queues;

    public SyncBar(int numQueues, int numThreads, int queueCapacity) {
        queues = new AtomicReferenceArray<PriorityQueue<Message>>(numQueues);
        queueLocks = new AtomicReferenceArray<Object>(numQueues);
        Comparator<Message> comparator = new MessageComparator();
        for (int i = 0; i < numQueues; ++i) {
            queueLocks.set(i, new Object());
            queues.set(i, new PriorityQueue<Message>(queueCapacity, comparator));
        }
        lastMsgIds = new AtomicIntegerArray(numThreads);
        threadLocks = new AtomicReferenceArray<Object>(numThreads);
        threadStatuses = new AtomicReferenceArray<ParallelThreadStatus>(numThreads);
        for (int i = 0; i < numThreads; ++i) {
            threadLocks.set(i, new Object());
            threadStatuses.set(i, ParallelThreadStatus.SLEEP);
        }
    }

    public void put(Message message, int threadIndex, int queueIndex) {
        logger.debug("put {} before queue lock {}", message.getId(), queueIndex);
        Object queueLock = queueLocks.get(queueIndex);
        synchronized (queueLock) {
            if (queues.get(queueIndex).offer(message)) {
                queueLock.notifyAll();
            } else {
                logger.warn("lost message {} on queue {}",
                        message.getId(), queueIndex);
            }
        }
        logger.debug("put {} after queue lock {}", message.getId(), queueIndex);
        Object threadLock = threadLocks.get(threadIndex);
        logger.debug("put {} before thread lock {}", message.getId(), threadIndex);
        synchronized (threadLock) {
            lastMsgIds.set(threadIndex, message.getId());
            threadLock.notifyAll();
        }
        logger.debug("put {} after thread lock {}", message.getId(), threadIndex);
    }

    public void ensureIdIsMax(int msgId) throws InterruptedException {
        for (int i = 0; i < lastMsgIds.length(); ++i) {
            int id = lastMsgIds.get(i);
            if (id < msgId - 1
                    && threadStatuses.get(i) == ParallelThreadStatus.RUNNING) {
                Object lock = threadLocks.get(i);
                logger.debug("{} before thread lock {}", msgId, i);
                synchronized (lock) {
                    while (lastMsgIds.get(i) < msgId - 1
                            && threadStatuses.get(i) == ParallelThreadStatus.RUNNING) {
                        logger.debug("wait thread lock {} cause {} < {}",
                                lastMsgIds.get(i), msgId, i);
                        lock.wait();
                    }
                }
                logger.debug("{} after thread lock {}", msgId, i);
            }
        }
    }

    public Message takeOrWait(int queueIndex) throws InterruptedException {
        Object lock = queueLocks.get(queueIndex);
        logger.debug("takeOrWait before queue lock {}", queueIndex);
        Message result = null;
        try {
            synchronized (lock) {
                PriorityQueue<Message> queue = queues.get(queueIndex);
                while (true) {
                    result = queue.poll();
                    if (result == null) {
                        logger.debug("takeOrWait wait queue lock {}", queueIndex);
                        lock.wait();
                    } else {
                        return result;
                    }
                }
            }
        } finally {
            logger.debug("takeOrWait {} after queue lock {}",
                    result.getId(), queueIndex);
        }
    }

    public Message poll(int queueIndex) {
        Object lock = queueLocks.get(queueIndex);
        Message result = null;
        logger.debug("poll before queue lock {}", queueIndex);
        try {
            synchronized (lock) {
                result = queues.get(queueIndex).poll();
                return result;
            }
        } finally {
            logger.debug("poll {} after queue lock {}",
                    result == null ? "null" : result.getId(), queueIndex);
        }
    }
    
    public void setThreadFlag(int threadIndex, ParallelThreadStatus status) {
        Object lock = threadLocks.get(threadIndex);
        synchronized (lock) {
            threadStatuses.set(threadIndex, status);
            switch (status) {
                case RUNNING:
                    logger.debug("thread {} is in woken",
                            threadIndex);
                    break;
                case SLEEP:
                    lock.notifyAll();
                    logger.debug("thread {} is in slept; {}",
                            threadIndex, lastMsgIds.get(threadIndex));
                    break;
                default:
                    throw new IllegalStateException("unsupported " + status);
            }
        }
    }
}
