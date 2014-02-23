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
        if (result == null) {
            logger.error("id {} is null", id);
        }
        int index = id - base;
        if (index < 0) {
            logger.debug("negative id {} for base {}.", index, base);
            index += size;
            if (index < 0) {
                logger.error("insert id out of window.");
                return;
            }
        } else {
            logger.debug("insert id {} base {}.", id, base);
        }
        if (buffer.compareAndSet(index, null, result)) {
            logger.debug("inserted {} at index {}.", result, index);
        } else {
            logger.error("index {} is not null.", index);
        }
    }

    public boolean reserveCell() {
        int n = used.get();
        logger.debug("reserve message. used {}.", n);
        while (n == size) {
            logger.trace("window is full.");
            return false;
        }
        used.incrementAndGet();
        if (buffer.get(tail) != null) {
            logger.error("tail {} is not null {}", tail, buffer.get(tail));
        }
        if (tail == last) {
            base += size;
            logger.debug("new base {}. cell {} is reserved.", base, tail);
            tail = 0;
        } else {
            logger.debug("cell {} is reserved.", tail);
            ++tail;
        }
        return true;
    }

    public Object consume() {
        int index = consumed - base;
        if (index < 0) {
            logger.debug("negative id {} for base {}.", index, base);
            index += size;
            if (index < 0) {
                logger.error("remove id out of window.");
                return null;
            }
        }
        Object result = buffer.get(index);
        if (result == null) {
            logger.debug("cell {} is empty. base {}.", index, base);
            return null;
        }
        if (!buffer.compareAndSet(index, result, null)) {
            logger.error("index {} changed  to {}", index, buffer.get(index));
        }
        ++consumed;
        used.decrementAndGet();
        logger.debug("global id {} of message {}.", consumed, result);
        return result;
    }
}
