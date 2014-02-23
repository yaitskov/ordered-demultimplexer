package com.github.dyaitskov.demux.queue1s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class SourceImpl implements Source {
    private static final Logger logger = LoggerFactory.getLogger(SourceImpl.class);
    private int n;
    private int i;

    public SourceImpl(int n) {
        this.n = n;
    }

    public Object next() throws InterruptedException {
        if (i < n) {
            logger.debug("source return {}", i);
            return i++;
        }
        throw new InterruptedException("stop");
    }
}
