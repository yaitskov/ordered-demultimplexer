package com.github.dyaitskov.demux.queue1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 */
public class Window {
    private static final Logger logger = LoggerFactory.getLogger(Window.class);
    private final AtomicReferenceArray buffer;
    private int consumed;
    private int tail;
    private final int last;
    public final int size;
    private final AtomicInteger used = new AtomicInteger();
    private volatile int base;

    public Window(int size) {
        buffer = new AtomicReferenceArray(size);
        last = size - 1;
        this.size = size;
    }

    public int used() {
        return used.get();
    }

    public void insert(int id, Object result) {
        int index = id - base;
        if (index < 0) {
            logger.debug("negative id {} for base {}", index, base);
            index += size;
            if (index < 0) {
                logger.error("insert id out of window");
                return;
            }
        }
        buffer.set(index, result);
    }

    public void newMessage() {
        while (used.get() == size) {
            Thread.yield();
        }
        used.incrementAndGet();
        if (tail == last) {
            base += size;
            logger.debug("new base {}", base);
            tail = 0;
        } else {
            ++tail;
        }
    }

    public Object consume() {
        int index = consumed - base;
        if (index < 0) {
            logger.debug("negative id {} for base {}", index, base);
            index += size;
            if (index < 0) {
                logger.error("remove id out of window");
                return null;
            }
        }
        Object result = buffer.get(index);
        if (result == null) {
            return null;
        }
        buffer.set(index, null);
        ++consumed;
        used.decrementAndGet();
        return result;
    }
}
