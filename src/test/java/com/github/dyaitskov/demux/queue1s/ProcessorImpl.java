package com.github.dyaitskov.demux.queue1s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
*/
public class ProcessorImpl implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(SourceImpl.class);
    public Object process(Object input) {
        Random r = new Random();
        long l = 0;
        int m = r.nextInt((Integer) input * 100 + 10);
        for (int i = 0; i < m; ++i) {
            l += r.nextInt(10);
        }
        logger.debug("processor {}, {}", input, l);
        return - ((Integer) input);
    }
}
