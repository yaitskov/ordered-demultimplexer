package com.github.dyaitskov.demux.queue1s;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class IntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
    @Test
    public void integrate() throws InterruptedException {
        logger.info("test");
        for (Integer threads : ContiguousSet.create(
                Range.closedOpen(1, 8),
                DiscreteDomain.integers())) {
            for (Integer windowSize : ContiguousSet.create(
                    Range.closedOpen(1, 16),
                    DiscreteDomain.integers())) {
                logger.info("------ threads {} ---- window {} ---",
                        threads, windowSize);
                new Integrate().useCase(threads, windowSize, 2000);
            }
        }
    }
}
