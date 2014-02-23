package com.github.dyaitskov.demux.queue1s;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class SyncBitMap {
    private static final Logger logger = LoggerFactory.getLogger(SyncBitMap.class);
    private final int[][] combinations;
    private final AtomicInteger bits = new AtomicInteger();
    private final Object lock = new Object();
    public SyncBitMap(int threads) {
        Set<Set<Integer>> superSet = Sets.powerSet(
                Sets.newHashSet(
                        ContiguousSet.create(
                                Range.closedOpen(0, threads),
                                DiscreteDomain.integers()).asList()));
        combinations = new int[superSet.size()][];
        for (Set<Integer> set : superSet) {
            int key = 0;
            for (Integer threadId : set) {
                key |= (1 << threadId);
            }
            combinations[key] = new TIntHashSet(set).toArray();
        }
    }

    public int[] findSetBits() throws InterruptedException {
        int map = bits.get();
        logger.debug("get map {}", map);
        if (map == 0) {
            synchronized (lock) {
                while ((map = bits.get()) == 0) {
                    logger.debug("wait bits");
                    lock.wait();
                }
            }
        }
        logger.debug("return map {}", map);
        return combinations[map];
    }

    public void clearBit(int wid) {
        logger.debug("bit {}", wid);
        while (true) {
            int map = bits.get();
            if (bits.compareAndSet(map, map & ~(1 << wid))) {
                break;
            }
        }
    }

    public void setBit(int index) {
        logger.debug("bit {}", index);
        while (true) {
            int map = bits.get();
            if (bits.compareAndSet(map, map | (1 << index))) {
                if (map == 0) {
                    synchronized (lock) {
                        logger.debug("notify map not zero {}", index);
                        lock.notify();
                    }
                }
                break;
            }
        }
    }
}
