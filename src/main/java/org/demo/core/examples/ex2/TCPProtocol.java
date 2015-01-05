package org.demo.core.examples.ex2;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by vx00418 on 11/29/2014.
 */
public interface TCPProtocol {
    void handleAccept(SelectionKey key) throws IOException;

    void handleRead(SelectionKey key) throws IOException;

    void handleWrite(SelectionKey key) throws IOException;
}
