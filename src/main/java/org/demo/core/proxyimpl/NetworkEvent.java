package org.demo.core.proxyimpl;


import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * This is the interface used by the network event handlers i.e.
 * @see ServerAcceptor and @see ServerHandler.
 *
 */
public interface NetworkEvent {
    public void register(Selector selector);
    public void processEvent(SelectionKey key);
    public void destroy();

}
