package com.github.dyaitskov.demux.queue1;

/**
 */
public class Job {
    public final Object input;
    public final int id;

    public Job(Object input, int id) {
        this.input = input;
        this.id = id;
    }
}
