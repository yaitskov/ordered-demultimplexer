package com.github.dyaitskov.demux.queue1;

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class WindowTest {

    public static final int SIZE = 3;
    Window w = new Window(SIZE);

    @Test
    public void insertPassAhead() {
        Assert.assertEquals(0, w.used());
        Assert.assertEquals(SIZE, w.size);
        w.newMessage();
        Assert.assertEquals(1, w.used());
        w.insert(0, 0);
        Assert.assertEquals(1, w.used());
        w.newMessage();
        Assert.assertEquals(2, w.used());
        w.newMessage();
        Assert.assertEquals(3, w.used());
        w.insert(2, 2);
        Assert.assertEquals(3, w.used());
        w.insert(1, 1);
        Assert.assertEquals(3, w.used());
        Assert.assertEquals(0, w.consume());
        Assert.assertEquals(2, w.used());
        Assert.assertEquals(1, w.consume());
        Assert.assertEquals(1, w.used());
        Assert.assertEquals(2, w.consume());
        Assert.assertEquals(0, w.used());
    }

    @Test
    public void consumeNull() {
        w.newMessage();
        w.newMessage();
        Assert.assertEquals(2, w.used());
        Assert.assertNull(w.consume());
        Assert.assertNull(w.consume());
        Assert.assertNull(w.consume());
        Assert.assertEquals(2, w.used());
        w.insert(0, 0);
        Assert.assertEquals(0, w.consume());
        Assert.assertNull(w.consume());
        w.insert(1, 1);
        Assert.assertEquals(1, w.consume());
        Assert.assertNull(w.consume());
        Assert.assertNull(w.consume());
        Assert.assertNull(w.consume());
        Assert.assertNull(w.consume());
        Assert.assertEquals(0, w.used());
    }

    @Test
    public void changeBase() {
        w.newMessage();
        w.newMessage();
        w.insert(1, 1);
        w.insert(0, 0);
        w.newMessage();
        w.insert(2, 2);
        Assert.assertEquals(0, w.consume());
        Assert.assertEquals(1, w.consume());
        w.newMessage();
        w.insert(3, 3);
        w.newMessage();
        w.insert(4, 4);
        Assert.assertEquals(2, w.consume());
        Assert.assertEquals(3, w.consume());
        Assert.assertEquals(4, w.consume());
        Assert.assertEquals(0, w.used());
    }
}
