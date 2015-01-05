package org.demo.core.proxyimpl;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ServerHandler implements NetworkEvent {

    private SocketChannel localSocketChannel;
    private SocketChannel remoteSocketChannel;
    private ProxyByteBuffer remoteByteBuffer;
    private ProxyByteBuffer localByteBuffer;


    public ServerHandler(Selector selector, SocketChannel localSocketChannel) {
        this.localSocketChannel = localSocketChannel;
        try {
            this.localSocketChannel.configureBlocking(false);
            localSocketChannel.register(selector, SelectionKey.OP_READ, this);
            this.register(selector);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void register(Selector selector) {
        try {

            InetSocketAddress inetSocketAddress = new InetSocketAddress(NioProxyStarter.REMOTE_HOST, NioProxyStarter.REMOTE_PORT);

            remoteSocketChannel = SocketChannel.open();
            remoteSocketChannel.connect(inetSocketAddress);
            remoteSocketChannel.configureBlocking(false);
            remoteSocketChannel.register(selector, SelectionKey.OP_READ, this);

        } catch (IOException ex) {
            //TODO: write correct logging
            System.err.println(ex.getMessage());
        }
    }

    @Override
    public void processEvent(SelectionKey key) {
        //process local
        if (key.channel() == localSocketChannel) {
            if (key.isValid() && key.isReadable()) {
                System.out.println("LOCAL SOCKET READABLE");
            }
            if (key.isValid() && key.isWritable()) {
                System.out.println("LOCAL SOCKET WRITEBLE");
            }
        }
        //process remote
        if (key.channel() == remoteSocketChannel) {
            if (key.isValid() && key.isReadable()) {
                System.out.println("REMOTE SOCKET READABLE");
            }
            if (key.isValid() && key.isWritable()) {
                System.out.println("REMOTE SOCKET READABLE");
            }
        }
    }


    @Override
    public void destroy() {

    }
}
