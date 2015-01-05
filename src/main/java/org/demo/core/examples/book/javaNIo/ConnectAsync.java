package org.demo.core.examples.book.javaNIo;

/**
 * Created by vx00418 on 12/1/2014.
 */

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Demonstrate asynchronous connection of a SocketChannel.
 *
 * @author Ron Hitchens (ron@ronsoft.com)
 */
public class ConnectAsync {
    public static void main(String[] argv) throws Exception {
        String host = "repo.she.pwj.com";
        int port = 8080;

        if (argv.length == 2) {
            host = argv[0];
            port = Integer.parseInt(argv[1]);
        }

        InetSocketAddress addr = new InetSocketAddress(host, port);
        SocketChannel sc = SocketChannel.open();
        Selector selector= Selector.open();
        sc.register(selector, SelectionKey.OP_CONNECT);
        sc.configureBlocking(false);

        System.out.println("initiating connection");

        sc.connect(addr);

        while (!sc.finishConnect()) {
            doSomethingUseful();
        }

        System.out.println("connection established");

        // Do something with the connected socket
        // The SocketChannel is still nonblocking

        sc.close();
    }

    private static void doSomethingUseful() {
        System.out.println("doing something useless");
    }
}

