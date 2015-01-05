package org.demo.core.proxyimpl;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class ServerAcceptor implements NetworkEvent {


    @Override
    public void register(Selector selector) {
        try {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(NioProxyStarter.LOCAL_PORT));
            serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, 4 * 1024);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT, this);
        } catch (IOException ex) {
            //TODO: do with logger
            System.err.println(ex.getMessage());
        }
    }

    @Override
    public void processEvent(SelectionKey key) {
        if (key.isValid() && key.isAcceptable()) {
            try {
                ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                SocketChannel socketChannel = serverChannel.accept();
                new ServerHandler(key.selector(), socketChannel);
            } catch (IOException ex) {
                //TODO: do correct
                System.err.println(ex.getMessage());
            }
        }
    }

    @Override
    public void destroy() {

    }
}
