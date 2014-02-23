package com.github.dyaitskov.demux.queue1s;

/**
 */
public interface Source {
    Object next() throws InterruptedException;
}
