package com.github.dyaitskov.demux.queue1s;


import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import org.junit.Assert;
import org.junit.Test;

/**
 */
public class SyncBitMapTest {

    @Test
    public void constructor() {
        SyncBitMap map = new SyncBitMap(8);
        Assert.assertNotNull(map);
    }

    @Test
    public void setClear() throws InterruptedException {
        SyncBitMap map = new SyncBitMap(8);
        map.setBit(0);
        map.setBit(1);
        Assert.assertEquals(
                Sets.newHashSet(Ints.asList(map.findSetBits())),
                Sets.newHashSet(0, 1));
        map.clearBit(0);
        Assert.assertEquals(
                Sets.newHashSet(Ints.asList(map.findSetBits())),
                Sets.newHashSet(1));
    }
}
