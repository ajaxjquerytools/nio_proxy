package org.demo.core.proxyimpl;

import java.io.IOException;
import java.nio.channels.Selector;

/**
 * Created by vx00418 on 12/9/2014.
 */
public class NioProxyStarter {
    public static final int LOCAL_PORT = 9090;
    public static final int REMOTE_PORT = 8081;
    public static final String REMOTE_HOST = "xldn3513vdap.ldn.swissbank.com";

    public static void main(String[] args) {

        int availableProcessors = Runtime.getRuntime().availableProcessors();


        ServerAcceptor acceptor = new ServerAcceptor();
        try {
            Selector selector = Selector.open();
            acceptor.register(selector);

            //workers
            Thread proxyReactor = new Thread(new NioProxyReactor(selector));
            proxyReactor.start();

            try {
                proxyReactor.join();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

        } catch (IOException ex) {
            System.err.println("Couldnt open selector" + ex.getMessage());
        }

    }
}
