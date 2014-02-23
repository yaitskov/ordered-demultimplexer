package com.github.dyaitskov.demux.queue1s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*/
public class ProcessorImpl implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(SourceImpl.class);
    public Object process(Object input) {
        logger.debug("processor {}", input);
        return - ((Integer) input);
    }
}
