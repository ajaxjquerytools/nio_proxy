package org.demo.core.proxyimpl;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * TODO: add javadoc
 */
public class NioProxyReactor implements Runnable {
    private final Selector selector;


    public NioProxyReactor(Selector selector) {
        this.selector = selector;
    }

    @Override
    public void run() {
        //event loop
        while (!Thread.interrupted()) {
            try {
                int selCount = selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();

                Iterator<SelectionKey> iteratorKey = selectedKeys.iterator();
                while (iteratorKey.hasNext()) {
                    SelectionKey key=iteratorKey.next();
                    iteratorKey.remove();
                    NetworkEvent networkEvent=(NetworkEvent)key.attachment();
                    networkEvent.processEvent(key);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
