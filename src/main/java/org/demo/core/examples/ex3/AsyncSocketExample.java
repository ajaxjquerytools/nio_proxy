package org.demo.core.examples.ex3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AsyncSocketExample {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        SocketAddress address = new InetSocketAddress("repo.she.pwj.com", 8080);
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
        Future<Void> connected = client.connect(address);

        ByteBuffer buffer = ByteBuffer.allocate(74);

// wait for the connection to finish
        connected.get();

// read from the connection
        Future<Integer> future = client.read(buffer);

// do other things...

// wait for the read to finish...
        future.get();

// flip and drain the buffer
        buffer.flip();
        WritableByteChannel out = Channels.newChannel(System.out);
        out.write(buffer);
    }
}
