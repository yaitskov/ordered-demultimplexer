package com.github.dyaitskov.demux.queue1s;


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
}
