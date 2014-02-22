package com.github.dyaitskov.demux.queuen;

/**
 */
public interface WaitPutQueue<T> {
    void waitPut() throws InterruptedException;
    boolean put(T t);
    T poll();
}
