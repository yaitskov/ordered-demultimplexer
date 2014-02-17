package com.github.dyaitskov.demux;

/**
 */
public class ParallelImpl extends Parallel<Integer> {
    private final int numQueues;

    public ParallelImpl(WaitPutQueue<Integer> in, SyncBar bar,
                        int threadNum, int numQueues) {
        super(in, bar, threadNum);
        this.numQueues = numQueues;
    }

    @Override
    protected void createAndEmitMessage() throws InterruptedException {
        int message = take();
        int queueId = message % numQueues;
        put(new IdMessage(message), queueId);
    }
}
