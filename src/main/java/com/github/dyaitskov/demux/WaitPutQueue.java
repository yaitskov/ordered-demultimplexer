package com.github.dyaitskov.demux;

/**
 */
public interface WaitPutQueue<T> {
    void waitPut() throws InterruptedException;
    boolean put(T t);
    T poll();
}
