package com.orbitz.nio;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>
 * This class is an example of how the NIOWorker can be used.
 * </p>
 *
 * <p>Copyright (c) 2000-2005, Orbitz LLC, All Rights Reserved</p>
 */
public class NIOWorkerExample {
    private NIOWorker worker;

    public NIOWorkerExample() throws IOException {
        worker = new NIOWorker();
    }

    public void callAmazon() throws IOException {
        URL url = new URL("http://xldn4997vdap.ldn.swissbank.com:8000");
        HTTPGetMethod get = new HTTPGetMethod(url);
        long timeout = 60 * 1000; // ten seconds

        worker.execute(get, timeout);
        System.out.println(get.toHTTP());
        if (get.getResponse() != null) {
            System.out.println(get.getRequestString());
            System.out.println(get.getResponse().getBody());
            System.out.println("Got a response from google");
        } else {
            System.out.println("Google is down!!");
        }
    }

    public static void main(String[] args) {
        try {
            NIOWorkerExample example = new NIOWorkerExample();
            example.callAmazon();
        } catch (IOException ioe) {
            System.err.println("NIOWorker failed");
            ioe.printStackTrace();
        }
    }
}