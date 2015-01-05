package org.demo.core.exp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by vx00418 on 11/29/2014.
 */
public class SampleTcpServer {
    private static String clientChannel = "clientChannel";
    private static String serverChannel = "serverChannel";
    private static String channelType = "channelType";

    public static void main(String[] args) throws IOException {
        int port = 88;
        String localhost = "localhost";
        ServerSocketChannel channel = ServerSocketChannel.open();

        channel.bind(new InetSocketAddress(localhost, port));
        channel.configureBlocking(false);

        Selector selector = Selector.open();
        SelectionKey socketServerSelectionKey = channel.register(selector, SelectionKey.OP_ACCEPT);

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(channelType, serverChannel);
        socketServerSelectionKey.attach(properties);

        while (true) {
            if (selector.select() == 0)
                continue;
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (((Map<?, ?>) key.attachment()).get(channelType).equals(serverChannel)) {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                    SocketChannel clientSocketChannel = serverSocketChannel.accept();
                    if (clientSocketChannel != null) {
                        clientSocketChannel.configureBlocking(false);
                        SelectionKey clientKey = clientSocketChannel.register(
                                selector, SelectionKey.OP_READ,
                                SelectionKey.OP_WRITE);
                        Map<String, String> clientproperties = new HashMap<String, String>();
                        clientproperties.put(channelType, clientChannel);
                        clientKey.attach(clientproperties);

                        // write something to the new created client
                        CharBuffer buffer = CharBuffer.wrap("Hello client");
                        while (buffer.hasRemaining()) {
                            clientSocketChannel.write(Charset.defaultCharset()
                                    .encode(buffer));
                        }
                        buffer.clear();
                    }
                } else {
                    // data is available for read
                    // buffer for reading
                    ByteBuffer buffer = ByteBuffer.allocate(20);
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    int bytesRead = 0;
                    if (key.isReadable()) {
                        // the channel is non blocking so keep it open till the
                        // count is >=0
                        if ((bytesRead = clientChannel.read(buffer)) > 0) {
                            buffer.flip();
                            System.out.println(Charset.defaultCharset().decode(
                                    buffer));
                            buffer.clear();
                        }
                        if (bytesRead < 0) {
                            // the key is automatically invalidated once the
                            // channel is closed
                            clientChannel.close();
                        }
                    }
                }
                iterator.remove();
            }
        }


    }
}
