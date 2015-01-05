package org.demo.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * Created by vx00418 on 11/27/2014.
 */
public class NioTcpProxy {

    int localPort = 7000;
    int remotePort = 80;
    String remoteHost = "repo.she.pwj.com";

    //Allocate direct, because will do many IO operations  writes/reads
    ByteBuffer remoteClientBuffer = ByteBuffer.allocateDirect(2 * 1024);
    ByteBuffer localServerBuffer = ByteBuffer.allocateDirect(2 * 1024);



    SocketChannel remoteClientChanel;
    ServerSocketChannel localServerChanel;


    //  1.Create a selector instance.
    //  2.Register it with various channels, specifying I/O operations of interest for each channel.
    //  3.Repeatedly:
    //     * Call one of the select methods.
    //     * Get the list of selected keys.
    //     * For each key in the selected-keys set,
    //        -- Fetch the channel and (if applicable) attachment from the key
    //        -- Determine which operations are ready and perform them. If an accept operation, set the accepted channel to nonblocking and register it with the selector
    //        -- Modify the key’s operation interest set if needed
    //        -- Remove the key from the selected-keys set
    public void startServer() throws Exception {

        this.localServerChanel = ServerSocketChannel.open();
        ServerSocket serverSocket = localServerChanel.socket();
        Selector selector = Selector.open();
        localServerChanel.bind(new InetSocketAddress(localPort));
        localServerChanel.configureBlocking(false);

        // Register the ServerSocketChannel with the Selector
        localServerChanel.register(selector, SelectionKey.OP_ACCEPT);


        while (true) {
            int n = selector.select();

            if (n == 0) {
                continue;    // nothing to do
            }
            Iterator it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();
                // Is a new connection coming in?
                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel channel = server.accept();
                    registerChannel(selector, channel, SelectionKey.OP_READ);
                    sayHello(channel);
                }

                // Is there data to read on this channel?
                if (key.isReadable()) {
                    readDataFromSocket(key);
                }

                // Remove key from selected set; it's been handled
                it.remove();
            }
        }

    }

    public static void main(String[] args) throws Exception {
        new NioTcpProxy().startServer();

    }

    //Writing Data to a Buffer
    public void writeToBuffer(ByteBuffer byteBuffer, SocketChannel channel) throws IOException {
        channel.read(byteBuffer);
    }

    //Reading Data from a Buffer
    public void readFromBuffer(ByteBuffer byteBuffer, SocketChannel channel) throws IOException {
        channel.write(byteBuffer);
    }

    protected void registerChannel(Selector selector,  SelectableChannel channel, int ops) throws Exception {
        if (channel == null) {
            return;        // could happen
        }
        // Set the new channel nonblocking
        channel.configureBlocking(false);
        // Register it with the selector
        channel.register(selector, ops);
    }

    private void sayHello(SocketChannel channel) throws Exception {
        localServerBuffer.clear();
        String resp = "HTTP/1.1 200 OK\n" +
                "Content-Type: text/xml; charset=utf-8\n" +
                "Content-Length: length\n" +
                "\n" +
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "  <soap:Body>\n" +
                "  Hello word from server!\n" +
                "  </soap:Body>\n" +
                "</soap:Envelope>";

        localServerBuffer.put(resp.getBytes());
        localServerBuffer.flip();

        // channel.write(localServerBuffer);
        readFromBuffer(localServerBuffer,channel);

    }

    protected void readDataFromSocket(SelectionKey key) throws Exception {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int count;
        localServerBuffer.clear();            // Empty buffer

        // Loop while data is available; channel is nonblocking
        while ((count = socketChannel.read(localServerBuffer)) > 0) {
            localServerBuffer.flip();        // Make buffer readable
            // Send the data; don't assume it goes all at once
            while (localServerBuffer.hasRemaining()) {
                writeToBuffer(localServerBuffer,socketChannel);

            }
            // WARNING: the above loop is evil.  Because
            // it's writing back to the same nonblocking
            // channel it read the data from, this code can
            // potentially spin in a busy loop.  In real life
            // you'd do something more useful than this.
            localServerBuffer.clear();        // Empty buffer
        }

        if (count < 0) {
            // Close channel on EOF, invalidates the key
            socketChannel.close();
        }
    }

}
