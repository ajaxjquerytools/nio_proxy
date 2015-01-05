package org.demo.core.examples.ex4;

/**
 * Created by vx00418 on 11/27/2014.
 */

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class TcpClient {

    public static void main(String[] args) {

           final int DEFAULT_PORT = 5555;
          final String IP = "127.0.0.1";
       //// final int DEFAULT_PORT = 8080;
       // final String IP = "repo.she.pwj.com";

        ByteBuffer buffer = ByteBuffer.allocateDirect(2 * 1024);
        ByteBuffer randomBuffer;
        CharBuffer charBuffer;

        Charset charset = Charset.defaultCharset();
        CharsetDecoder decoder = charset.newDecoder();

        //open Selector and ServerSocketChannel by calling the open() method
        try (Selector selector = Selector.open();
             SocketChannel socketChannel = SocketChannel.open()) {

            //check that both of them were successfully opened
            if ((socketChannel.isOpen()) && (selector.isOpen())) {

                //configure non-blocking mode
                socketChannel.configureBlocking(false);
                //set some options
                socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 128 * 1024);
                socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 128 * 1024);
                socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

                //register the current channel with the given selector
                socketChannel.register(selector, SelectionKey.OP_CONNECT);

                //connect to remote host
                socketChannel.connect(new java.net.InetSocketAddress(IP, DEFAULT_PORT));

                System.out.println("Localhost: " + socketChannel.getLocalAddress());

                //waiting for the connection
                while (selector.select(1000) > 0) {
                    //get keys
                    Set keys = selector.selectedKeys();
                    Iterator its = keys.iterator();

                    //process each key
                    while (its.hasNext()) {
                        SelectionKey key = (SelectionKey) its.next();
                        //remove the current key
                        its.remove();
                        //get the socket channel for this key
                        try (SocketChannel keySocketChannel = (SocketChannel) key.channel()) {
                            //attempt a connection
                            if (key.isConnectable()) {
                                //signal connection success
                                System.out.println("I am connected!");
                                //close pending connections
                                if (keySocketChannel.isConnectionPending()) {
                                    keySocketChannel.finishConnect();
                                }
                                //read/write from/to server
                                while (keySocketChannel.read(buffer) != -1) {
                                    System.out.println("read/write from/to server");
                                    buffer.flip();
                                    charBuffer = decoder.decode(buffer);
                                    System.out.println("DATA FROM SERVER=[ "+charBuffer.toString()+" ]");
                                    if (buffer.hasRemaining()) {
                                        System.out.println("BUFFER HAS REMAINING");
                                        buffer.compact();
                                    } else {
                                        System.out.println("BUFFER CLEAR");
                                        buffer.clear();
                                    }
                                    int r = new Random().nextInt(100);
                                    if (r == 50) {
                                        System.out.println("50 was generated! Close the socket channel!");
                                        break;
                                    } else {
                                        System.out.println("WRITE DATA TO SERVER");
                                        String request="GET /artifactory/webapp/search/artifact/?1&q=xstream HTTP/1.1\n" +
                                                "Host: repo.she.pwj.com:8080\n" +
                                                "Connection: keep-alive\n" +
                                                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\n" +
                                                "User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.114 Safari/537.36\n" +
                                                "Accept-Encoding: gzip,deflate,sdch\n" +
                                                "Accept-Language: en-US,en;q=0.8\n" +
                                                "Cookie: JSESSIONID=57DAE21A656D016501D5CFED69DB3D88; art-page=\"/artifactory/webapp/search/artifact/?q=xstream\"";
                                        randomBuffer = ByteBuffer.wrap(request.getBytes("UTF-8"));
                                        keySocketChannel.write(randomBuffer);
                                        try {
                                            Thread.sleep(1500);
                                        } catch (InterruptedException ex) {
                                        }
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            System.err.println(ex);
                        }
                    }
                }
            } else {
                System.out.println("The socket channel or selector cannot be opened!");
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }


    }
}
