package com.github.dyaitskov.demux.queue1s;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class SyncBitMap {
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
        if (map == 0) {
            synchronized (lock) {
                while ((map = bits.get()) == 0) {
                    lock.wait();
                }
            }
        }
        return combinations[map];
    }

    public void clearBit(int wid) {
        while (true) {
            int map = bits.get();
            if (bits.compareAndSet(map, map & ~(1 << wid))) {
                break;
            }
        }
    }

    public void setBit(int index) {
        while (true) {
            int map = bits.get();
            if (bits.compareAndSet(map, map | (1 << index))) {
                if (map == 0) {
                    synchronized (lock) {
                        lock.notify();
                    }
                }
                break;
            }
        }
    }
}
