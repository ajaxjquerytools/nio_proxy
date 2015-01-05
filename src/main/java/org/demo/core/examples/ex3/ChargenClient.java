package org.demo.core.examples.ex3;

import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.io.IOException;

public class ChargenClient {

    public static int DEFAULT_PORT = 19;

    public static void main(String[] args) {

//        if (args.length == 0) {
//            System.out.println("Usage: java ChargenClient host [port]");
//            return;
//        }

        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException ex) {
            port = DEFAULT_PORT;
        }

        try {
            SocketAddress address = new InetSocketAddress("localhost", port);
            SocketChannel client = SocketChannel.open(address);

            ByteBuffer buffer = ByteBuffer.allocate(74);
           // WritableByteChannel out = Channels.newChannel(System.out);


            while (true) {
                // Put whatever code here you want to run every pass through the loop
                // whether anything is read or not
                int n = client.read(buffer);
                if (n > 0) {
                    buffer.flip();
                    System.out.println(buffer.getLong());
                    //out.write(buffer.getLong());
                    buffer.clear();
                } else if (n == -1) {
                    // This shouldn't happen unless the server is misbehaving.
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}