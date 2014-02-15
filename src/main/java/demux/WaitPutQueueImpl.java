package demux;

import java.util.Queue;

/**
 */
public class WaitPutQueueImpl<T> implements WaitPutQueue<T> {
    private final Object lock;
    private final Queue<T> queue;

    public WaitPutQueueImpl(Queue<T> queue) {
        lock = new Object();
        this.queue = queue;
    }

    @Override
    public void waitPut() throws InterruptedException {
        synchronized (lock) {
            lock.wait();
        }
    }

    @Override
    public boolean put(T t) {
        synchronized (lock) {
            if (queue.offer(t)) {
                lock.notify();
                return true;
            }
            return false;
        }
    }

    @Override
    public T poll() {
        synchronized (lock) {
            return queue.poll();
        }
    }
}
