package org.demo.core.examples.rsdn;

/**
 * Created by vx00418 on 11/30/2014.
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class JmxRmiMapper {

    static Map<Integer, Integer> portMap = new HashMap<Integer, Integer>();
    static Map<SocketChannel, SocketChannel> proxy2JmxMap = new HashMap<SocketChannel, SocketChannel>();
    static Map<SocketChannel, SocketChannel> jmx2ProxyMap = new HashMap<SocketChannel, SocketChannel>();
    static Map<SocketChannel, List<byte[]>> writeOrder = new HashMap<SocketChannel, List<byte[]>>();

    private static void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        Integer jmxPort = portMap.get(serverChannel.socket().getLocalPort());
        if (jmxPort == null) {
            serverChannel.close();
            key.cancel();
            return;
        }

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress("repo.she.pwj.com", jmxPort));
        socketChannel.register(key.selector(), SelectionKey.OP_CONNECT);

        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);

        jmx2ProxyMap.put(socketChannel, channel);
        proxy2JmxMap.put(channel, socketChannel);

        channel.register(key.selector(), SelectionKey.OP_READ);
    }

    private static void connect(SelectionKey key) {
        SocketChannel sChannel = (SocketChannel) key.channel();

        boolean success = false;
        try {
            success = sChannel.finishConnect();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        if (!success) {
            // An error occurred; handle it

            // Unregister the channel with this selector
            key.cancel();
        }
        int ops = key.interestOps();

        if ((ops & SelectionKey.OP_WRITE) != 0) {
            key.interestOps(SelectionKey.OP_WRITE);
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private static void read(SelectionKey key) throws IOException {

        SocketChannel source = (SocketChannel) key.channel();
        SocketChannel destination = jmx2ProxyMap.get(source);

        if (destination == null) {
            destination = proxy2JmxMap.get(source);
        }
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = -1;

        try {
            numRead = source.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (numRead == -1) {
            source.close();
            key.cancel();
            return;
        }

        List<byte[]> order = writeOrder.get(destination);
        if (order == null) {
            order = new ArrayList<byte[]>();
            writeOrder.put(destination, order);
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);

        order.add(data);

        SelectionKey destkey = destination.keyFor(key.selector());

        destkey.interestOps(SelectionKey.OP_WRITE);
    }

    private static void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        List<byte[]> pendingData = writeOrder.get(channel);
        Iterator<byte[]> items = pendingData.iterator();
        while (items.hasNext()) {
            byte[] item = items.next();
            items.remove();
            channel.write(ByteBuffer.wrap(item));
        }
        key.interestOps(SelectionKey.OP_READ);
    }


    public static void main(String[] args) throws Exception {
        Selector proxySelector = Selector.open();

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        // bind to port
        InetSocketAddress listenAddr = new InetSocketAddress((InetAddress) null, 9000);
        serverChannel.socket().bind(listenAddr);
        serverChannel.register(proxySelector, SelectionKey.OP_ACCEPT);

        ServerSocketChannel serverChannel2 = ServerSocketChannel.open();
        serverChannel2.configureBlocking(false);

        InetSocketAddress listenAddr2 = new InetSocketAddress((InetAddress) null, 9001);
        serverChannel2.socket().bind(listenAddr2);
        serverChannel2.register(proxySelector, SelectionKey.OP_ACCEPT);

        portMap.put(9000, 8080);
        portMap.put(9001, 8080);
        try {
            // processing
            while (true) {
                // wait for events
                proxySelector.select();

                // wakeup to work on selected keys
                Iterator keys = proxySelector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = (SelectionKey) keys.next();

                    if (!key.isValid()) {
                        continue;
                    }

                    // this is necessary to prevent the same key from coming up
                    // again the next time around.
                    keys.remove();

                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isConnectable()) {
                        connect(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}