package com.orbitz.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>
 * This class is the main worker for the HTTP connections and
 * NIO work. This uses a single NIO selector to manage and multi-
 * plex the NIO work.
 * </p>
 *
 * <p>
 * Notifications of errors are handled via log4j, however in a
 * production environment other solutions such as JMX are preferred.
 * Likewise, the maximum number of connections that this class
 * handles in the NIO selector is hard-coded to 200. This could
 * be configurable using any configuration mechanism.
 * </p>
 *
 * <p>Copyright (c) 2000-2005, Orbitz LLC, All Rights Reserved</p>
 */
public class NIOWorker extends Thread {
    private static final Logger log = Logger.getLogger(NIOWorker.class.getName());
    private static final int MAX = 200;

    private volatile boolean running = false;
    private ByteBuffer inBound = ByteBuffer.allocate(4096);
    private CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();
    private Selector selector;

    /**
     * Constructs a new <code>NIOWorker</code> that can be used to process
     * multiple requests at the same time. This is a runnable thread that is
     * started in this constructor. During construction a selector is opened for
     * use.
     */
    public NIOWorker() throws IOException {
        selector = Selector.open();

        // Start ourselves up
        setDaemon(true);
        start();
    }

    /**
     * The run loop. Inside this loop all the work is done. The selector is called
     * to see if anything is ready for processing. This loop also does a wait/notify
     * on the work queue so that when there is no work it doesn't spin its wheels.
     */
    public void run() {
        running = true;

        // Run until stopped
        while (running) {
            try {
                int num = selector.selectNow();
                if (num > 0) {
                    processKeys();
                } else {
                    Thread.yield();
                }

                // If there is no work, wait until these is something to do
                Set keys = selector.keys();
                synchronized (keys) {
                    if (keys.size() == 0) {
                        keys.wait();
                    } else {
                        Iterator iter = keys.iterator();
                        while (iter.hasNext()) {
                            SelectionKey key = (SelectionKey) iter.next();
                            WorkState state = (WorkState) key.attachment();
                            if (state.isTimedOut()) {
                                SocketChannel channel = (SocketChannel) key.channel();
                                finished(channel, key, state);
                            }
                        }
                    }
                }
            } catch (IOException ioe) {
                log.log(Level.SEVERE, ioe.getMessage(), ioe);
            } catch (InterruptedException ie) {
                log.log(Level.SEVERE, ie.getMessage(), ie);
            }
        }

        cleanup();
    }

    /**
     * Called when this runnable is shutdown so that the selector can be cleaned
     * up.
     */
    private void cleanup() {
        Set keys = selector.keys();
        synchronized (keys) {
            for (Iterator iter = keys.iterator(); iter.hasNext();) {
                SelectionKey key = (SelectionKey) iter.next();
                SocketChannel channel = (SocketChannel) key.channel();

                key.cancel();

                try {
                    channel.close();
                } catch (IOException ioe) {
                    log.log(Level.SEVERE, "Unable to shutdown an NIO socket channel", ioe);
                }
            }

            keys.notify();
        }

        try {
            selector.close();
        } catch (IOException ioe) {
            log.log(Level.SEVERE, "Unable to shutdown NIO", ioe);
        }
    }

    /**
     * Can be called to shutdown the NIOWorker thread and stop it from handling
     * additional requests. Requests in progress and not finished, but the sockets
     * are forcibly closed.
     */
    public void shutdown() {
        running = false;

        Set keys = selector.keys();
        synchronized (keys) {
            keys.notify();
        }
    }

    /**
     * <p>
     * Adds an HTTPMethod that the NIOWorker should process. When a method is
     * added, this method forces the client to wait on by syncronizing and then
     * waiting on an arbitrary Object (for this implementation, it waits on the
     * Work Object created in this method). The timeout given will be roughly the
     * maximum duration that the client will wait for the NIOWorker to handle the
     * HTTPMethod given.
     * </p>
     *
     * <p>
     * The response, if the request was successful, is set into the given HTTPMethod
     * Object.
     * </p>
     *
     * @param   method The method to add to the work queue of this NIOWorker.
     * @param   timeout The timeout for each method.
     */
    public void execute(HTTPMethod method, long timeout) {
        Work work = new Work();
        work.method = method;

        synchronized (work) {
            if (add(work, timeout)) {
                try {
                    work.wait(timeout);
                } catch (InterruptedException e) {
                    log.log(Level.SEVERE, "NIO operation interrupted");
                }
            } else {
                log.log(Level.SEVERE, "Unable to add work to the NIOWorker");
            }
        }
    }

    /**
     * Adds the work to the NIOWorker thread (via the Selector) by opening the
     * SocketChannel to the remote server. This handles cases were the connect
     * fails initially an also handles notification of the NIOWorker thread when
     * work has been successfully added.
     *
     * @param   work The work to add.
     * @param   timeout The timeout for this unit of work.
     * @return  True if the work was added, false if it wasn't.
     */
    protected boolean add(Work work, long timeout) {
        boolean added = false;

        // If the queue or keys are full, no more requests should be handled.
        // This synchronizes on the keys around the loop because the register
        // method also synchronizes and if we already hold the monitor, it will
        // reduce overhead. This also speeds up the notify call below.
        Set keys = selector.keys();
        synchronized (keys) {
            if (keys.size() >= MAX) {
                // This could use JMX notifications or some other mechanism to give
                // visibility into the NIOWorker at runtime
                log.log(Level.SEVERE, "NIOWorker is full");
                return false;
            }

            SocketChannel channel = null;

            try {
                URL url = work.method.getURL();
                int port = url.getPort() > 0 ? url.getPort() : 80;
                InetAddress ia = InetAddress.getByName(url.getHost());
                InetSocketAddress isa = new InetSocketAddress(ia, port);

                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(isa);
                System.out.println("REQUST STRING = ["+work.method.toHTTP()+"]");
                work.outBound = ByteBuffer.wrap(work.method.toHTTP().getBytes());
                WorkState state = new WorkState(timeout, work);
                channel.register(selector, SelectionKey.OP_CONNECT, state);
                added = true;
            } catch (IOException ioe) {
                log.log(Level.SEVERE, "Problem adding work to NIOWorker thread", ioe);

                if (channel != null) {
                    // Just in case is was created and IOException happened after!!!
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        key.cancel();
                    }

                    try {
                        channel.close();
                    } catch (IOException ioe2) {
                        // Smother
                        log.log(Level.SEVERE, "Error closing channel", ioe2);
                    }
                }
            }

            // If we added the work, notify the NIOWorker thread
            if (added) {
                keys.notify();
            }
        }

        selector.wakeup();

        return added;
    }

    /**
     * If the selector has keys (i.e. work), this method examines each key and
     * handles each one accordingly. If the key has successfully connected a
     * channel, this registers that key for a write operation. If the key is
     * writable, the HTTPMethod for the key is written out to the channel. If the
     * entire HTTPMethod has been written, the key is registered for a read
     * operation. If the key is readable, the {@link #doRead(java.nio.channels.SocketChannel, com.orbitz.nio.NIOWorker.WorkState)}
     * method is called to handle the read.
     */
    protected void processKeys() {
        Set keys = selector.selectedKeys();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            SelectionKey key = (SelectionKey) iter.next();
            iter.remove();

            WorkState state = (WorkState) key.attachment();
            SocketChannel channel = (SocketChannel) key.channel();

            if (state.isTimedOut()) {
                finished(channel, key, state);
                continue;
            }

            // Handle those just connected
            try {
                if (key.isConnectable()) {
                    if (channel.finishConnect()) {
                        channel.register(selector, SelectionKey.OP_WRITE, state);
                    } else {
                        channel.register(selector, SelectionKey.OP_CONNECT, state);
                    }
                } else if (key.isWritable()) {
                    if (doWrite(channel, state)) {
                        channel.register(selector, SelectionKey.OP_READ, state);
                    } else {
                        channel.register(selector, SelectionKey.OP_WRITE, state);
                    }
                } else if (key.isReadable()) {
                    if (doRead(channel, state)) {
                        finished(channel, key, state);
                    } else {
                        channel.register(selector, SelectionKey.OP_READ, state);
                    }
                } else {
                    throw new IOException("INVALID NIO SelectionKey STATE!");
                }
            } catch (IOException ioe) {
                log.log(Level.SEVERE, "Error encountered while processing keys", ioe);
                finished(channel, key, state);
            }
        }
    }

    /**
     * Handles a write operation. The remaining bytes in the outBound buffer (on
     * the {@link com.orbitz.nio.NIOWorker.Work}) is saved, the outBound buffer is passed to the write method
     * on the Channel, and if the number of bytes written is the same as the number
     * remaining, this returns true. Otherwise false is returned.
     *
     * @param   state The WorkState to use while reading. The channel, buffer,
     *          decoder, and parser are fetched from this state.
     * @return  True if th write is complete, false otherwise.
     * @throws  java.io.IOException If anything went wrong during the read, decode, parse
     *          or cancel.
     */
    protected boolean doWrite(SocketChannel channel, WorkState state)
    throws IOException {
        Work work = state.work;
        int rem = work.outBound.remaining();
        int num = channel.write(work.outBound);
        return (num == rem);
    }

    /**
     * <p>
     * Handles a read operation. First the static inBound buffer is cleared and the
     * decoder is reset. Than the channel is read from. If the channel signals
     * that it is complete, this method returns true. Otherwise, false is returned.
     * </p>
     *
     * <p>
     * If the channel signals that it read some bytes, those bytes are decoded
     * and then feed to the HTTPParser.
     * </p>
     *
     * @param   channel The Channel to read from.
     * @param   state The WorkState to use while reading. The channel, buffer,
     *          decoder, and parser are fetched from this state.
     * @throws  java.io.IOException If anything went wrong during the read, decode, parse
     *          or cancel.
     */
    protected boolean doRead(SocketChannel channel, WorkState state)
    throws IOException {
        inBound.clear();
        decoder.reset();

        boolean done = false;
        int num = channel.read(inBound);
        if (num == -1) {
            done = true;
            state.success = true;
        } else if (num > 0) {
            inBound.flip();
            CharBuffer buf = decoder.decode(inBound);
            state.parser.feed(buf.toString());
        }

        return done;
    }

    /**
     * <p>
     * Signals that this request is finished. The channel of the SelectionKey is
     * closed, the endTime is set and the response time is calculated and set onto
     * the response (if there is one).
     * </p>
     */
    protected void finished(SocketChannel channel, SelectionKey key, WorkState state) {
        // First, cancel the key
        key.cancel();

        // Failed to register, close the channel and wake up thread
        try {
            channel.close();
        } catch (IOException ioe) {
            // smother, can't close the channel
            log.log(Level.SEVERE, ioe.getMessage(), ioe);
        } finally {
            Work work = state.work;

            // Notify about timeout
            if (state.isTimedOut()) {
                // This could use JMX notifications or some other mechanism to give
                // visibility into the NIOWorker at runtime
                log.log(Level.SEVERE, "NIO work timed out before it was completed");
                synchronized (work) {
                    work.notify();
                }
            } else {
                synchronized (work) {
                    if (state.success) {
                        work.method.setResponse(state.parser.getResponse());
                    }

                    HTTPResponse response = work.method.getResponse();
                    if (response != null) {
                        response.setRunningTime(System.currentTimeMillis() - state.startTime);
                    }

                    work.notify();
                }
            }
        }
    }

    /**
     * <p>
     * A class that stores the state of a single unit of work that is being
     * processed by the NIOWorker thread. This maintains the channel, Work,
     * running time and parser that will be used to process the HTTP request.
     * </p>
     */
    public static class WorkState {
        public final StringBuffer buffer = new StringBuffer();
        public final long startTime;
        public final long timeoutPoint;
        public boolean success = false;
        public Work work;
        public final HTTPParser parser;

        public WorkState(long timeout, Work work) {
            this.startTime = System.currentTimeMillis();
            this.timeoutPoint = startTime + timeout;
            this.work = work;
            this.parser = new HTTPParser(work.method);
        }

        /**
         * Returns true if this request timed out meaning the start time plus the
         * timeout is before now.
         */
        public boolean isTimedOut() {
            long now = System.currentTimeMillis();
            return (now > timeoutPoint);
        }
    }

    /**
     * This class stores the pieces of a single unit of work including the
     * HTTPMethod and the HTTP request message in a ByteBuffer.
     */
    public static class Work {
        public HTTPMethod method;
        public ByteBuffer outBound;
    }
}