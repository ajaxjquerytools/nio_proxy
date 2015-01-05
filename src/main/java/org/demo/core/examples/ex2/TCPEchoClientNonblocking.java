package org.demo.core.examples.ex2;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TCPEchoClientNonblocking {

    public static void main(String args[]) throws Exception {

        //1.Get and convert arguments
       // if ((args.length < 2) || (args.length > 3)) // Test for correct # of args
       //     throw new IllegalArgumentException("Parameter(s): <Server> <Word> [<Port>]");

        String server = "repo.she.pwj.com"; // Server name or IP address
        // Convert input String to bytes using the default charset
        String request="GET / HTTP/1.1\n" +
                "Host: repo.she.pwj.com:8080\n" +
                "Connection: keep-alive\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36\n" +
                "Accept-Encoding: gzip,deflate,sdch\n" +
                "Accept-Language: en-US,en;q=0.8";
        byte[] argument = request.getBytes();

       // int servPort = (args.length == 3) ? Integer.parseInt(args[2]) : 7;
        int servPort = 8080;

        // Create channel and set to nonblocking
        SocketChannel clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(false);

        // Initiate connection to server and repeatedly poll until complete


        if (!clientChannel.connect(new InetSocketAddress(server, servPort))) {
            while (!clientChannel.finishConnect()) {
                System.out.print("*");   // Do something else
            }
        }
        ByteBuffer writeBuf = ByteBuffer.wrap(argument);
        ByteBuffer readBuf = ByteBuffer.allocateDirect(1024);
        int totalBytesRcvd = 0; // Total bytes received so far
        int bytesRcvd; // Bytes received in last read
        while (totalBytesRcvd < argument.length) {
            if (writeBuf.hasRemaining()) {
                clientChannel.write(writeBuf);
            }
            if ((bytesRcvd = clientChannel.read(readBuf)) == -1) {
                throw new SocketException("Connection closed prematurely");
            }
            totalBytesRcvd += bytesRcvd;
           // System.out.print(".");     // Do something else
        }
        System.out.println("============");
        System.out.println("Received: " +   // convert to String per default charset
                new String(readBuf.array(), 0, totalBytesRcvd));
        clientChannel.close();
    }
}