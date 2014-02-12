package demux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final AtomicReferenceArray<Object> queueLocks;
    private final AtomicReferenceArray<Object> threadLocks;
    private final AtomicReferenceArray<PriorityQueue<Message>> queues;
    private final int emptyValue;

    public SyncBar(int numQueues, int numThreads,
                   int queueCapacity, int emptyValue)
    {
        this.emptyValue = emptyValue;
        queues = new AtomicReferenceArray<PriorityQueue<Message>>(numQueues);
        queueLocks = new AtomicReferenceArray<Object>(numQueues);
        for (int i = 0; i < numQueues; ++i) {
            queueLocks.set(i, new Object());
            queues.set(i, new PriorityQueue<Message>(queueCapacity));
        }
        lastMsgIds = new AtomicIntegerArray(numThreads);
        threadLocks = new AtomicReferenceArray<Object>(numThreads);
        for (int i = 0; i < numThreads; ++i) {
            threadLocks.set(i, new Object());
            lastMsgIds.set(i, emptyValue);
        }
    }

    public void put(Message message, int threadIndex, int queueIndex) {
        if (message.getId() == emptyValue) {
            logger.error("queue {} thread {} put emptyValue",
                    queueIndex, threadIndex);
            return;
        }
        Object threadLock = threadLocks.get(threadIndex);
        synchronized (threadLock) {
            Object queueLock = queueLocks.get(queueIndex);
            synchronized (queueLock) {
                if (queues.get(queueIndex).offer(message)) {
                    queueLock.notifyAll();
                } else {
                    logger.warn("lost message {} on queue {}",
                            message.getId(), queueIndex);
                }
            }
            lastMsgIds.set(threadIndex, message.getId());
            threadLock.notifyAll();
        }
    }

    public void ensureIdIsMax(int msgId, int queueIndex) throws InterruptedException {
        for (int i = 0; i < lastMsgIds.length(); ++i) {
            int id = lastMsgIds.get(i);
            if (id < msgId || id == emptyValue) {
                Object lock = threadLocks.get(i);
                synchronized (lock) {
                    while (lastMsgIds.get(i) < msgId) {
                        lock.wait();
                    }
                }
            }
        }
    }

    public Message takeOrWait(int queueIndex) throws InterruptedException {
        Object lock = queueLocks.get(queueIndex);
        synchronized (lock) {
            PriorityQueue<Message> queue = queues.get(queueIndex);
            Message result;
            while (true) {
                result = queue.poll();
                if (result == null) {
                    lock.wait();
                } else {
                    return result;
                }
            }
        }
    }

    public Message poll(int queueIndex) {
        Object lock = queueLocks.get(queueIndex);
        synchronized (lock) {
            return queues.get(queueIndex).poll();
        }
    }
}
