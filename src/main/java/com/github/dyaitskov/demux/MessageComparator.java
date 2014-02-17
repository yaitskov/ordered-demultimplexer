package com.github.dyaitskov.demux;

import java.util.Comparator;

/**
 */
public class MessageComparator implements Comparator<Message> {
    @Override
    public int compare(Message o1, Message o2) {
        return Integer.compare(o1.getId(), o2.getId());
    }
}
