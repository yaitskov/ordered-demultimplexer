import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.Queue;

/**
 */
public class IntChannelHandler extends SimpleChannelHandler {

    private final Queue<Integer> queue;

    public IntChannelHandler(Queue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception
    {
        queue.add((Integer) e.getMessage());
    }
}
