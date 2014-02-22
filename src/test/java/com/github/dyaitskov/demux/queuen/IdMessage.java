package com.github.dyaitskov.demux.queuen;

import com.github.dyaitskov.demux.queuen.Message;

/**
*/
public class IdMessage implements Message {
    private final int id;

    IdMessage(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
