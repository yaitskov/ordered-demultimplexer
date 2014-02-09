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
    private final AtomicIntegerArray lastIds;
    private final AtomicReferenceArray<Object> locks;
    private final AtomicReferenceArray<PriorityQueue<Message>> queues;
    private final int emptyValue;

    public SyncBar(int numQueues, int queueCapacity, int emptyValue) {
        queues = new AtomicReferenceArray<PriorityQueue<Message>>(numQueues);
        lastIds = new AtomicIntegerArray(queues.length());
        locks = new AtomicReferenceArray<Object>(lastIds.length());
        this.emptyValue = emptyValue;
        for (int i = 0; i < lastIds.length(); ++i) {
            queues.set(i, new PriorityQueue<Message>(queueCapacity));
            locks.set(i, new Object());
            lastIds.set(i, emptyValue);
        }
    }

    public int length() {
        return lastIds.length();
    }

    public void waitNotLess(int index, int id) {
        if (lastIds.get(index) < id) {
            Object lock = locks.get(index);
            synchronized (lock) {
                int cid;
                while ((cid = lastIds.get(index)) < id && cid != emptyValue) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        logger.info("interruption ignored; index {}; id {}",
                                index, id);
                    }
                }
            }
        }
    }

    public void put(Message message, int index) {
        if (message.getId() == emptyValue) {
            logger.error("index {} got emptyValue {}", index, emptyValue);
        }
        Object lock = locks.get(index);
        synchronized (lock) {
            if (queues.get(index).offer(message)) {
                lastIds.set(index, message.getId());
                lock.notifyAll();
            } else {
                logger.warn("lost message {} on queue {}",
                        message.getId(), index);
            }
        }
    }

    public Message take(int index) throws InterruptedException {
        Object lock = locks.get(index);
        synchronized (lock) {
            PriorityQueue<Message> queue = queues.get(index);
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

    public Message poll(int index) {
        Object lock = locks.get(index);
        synchronized (lock) {
            return queues.get(index).poll();
        }
    }
}
