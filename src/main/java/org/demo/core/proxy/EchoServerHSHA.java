package org.demo.core.proxy;

/**
 * Created by vx00418 on 12/8/2014.
 */
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is a EchoServerHSHA which uses a Reactor pattern to accept new
 * connections and to handle read/write operations with the clients connected.
 * If it has a new incoming connection, it uses the Acceptor of the
 * Acceptor-Connector pattern to create the client connection. It also creates
 * an Wrapper pattern object of type EchoServerHandler which handles all future
 * read/write operations on the connection.
 *
 * The Server uses Half-Sync/Half-ASync pattern. Accepting client connection,
 * and socket read/write operations are done in the Half-ASync part. The input
 * message is put into a queue for the Half-Sync part to process and put the
 * result back in a queue for writing response back to client.
 *
 * Usage "java EchoServerHSHA portNumber". Where portNumber is an integer less
 * than 65536.
 *
 * Implementation ideas picked from links provided in the forums.
 * 1. https://today.java.net/article/2007/02/08/architecture-highly-scalable-nio-based-server
 * 2. http://jeewanthad.blogspot.ca/2013/02/reactor-pattern-explained-part-1.html
 * 3. nio.pdf from http://gee.cs.oswego.edu
 *
 * @author SomeOne
 */
public class EchoServerHSHA {
    public static void main(String[] args) throws IOException {
        int port = 8080;

        // Default port for the server is 8080 but user can pass the port number
        // as a command line argument.
        if (args.length < 1) {
            System.out.println("Usage : java EchoServerHSHA portNumber");
            System.out.println("Using 8080 as the port number if available");
        } else {
            try {
                port = Integer.parseInt(args[0]);
                if (port >= 65536)
                    throw new NumberFormatException();
            } catch (NumberFormatException numExcep) {
                System.out.println("Usage : java EchoServerHSHA portNumber");
                System.out.println("The portNumber has to be an integer less than 65536");
                System.out.println("Using 8080 as the port number if available");
                port = 8080;
            }
        }

        EchoServerHSHA server = new EchoServerHSHA();
        server.startServer(port);
    }

    /**
     * This method starts off the Reactor thread.
     *
     * @param port
     * @throws IOException
     */
    private void startServer(int port) throws IOException {
        new Thread(new EchoReactor(port)).start();
    }

    /**
     * This is the Reactor class of the Reactor pattern.
     *
     * @author SomeOne
     */
    private class EchoReactor implements Runnable {
        private int port;

        /**
         * While creating the Reactor, we setup the port that the server will
         * use.
         *
         * @param port
         * @throws IOException
         */
        private EchoReactor(int port) throws IOException {
            this.port = port;
        }

        /**
         * The Reactor first sets up the socket channel for the server. The
         * Acceptor of the Acceptor-Connector pattern object is setup to accept
         * new connections on the server socket at the port number passed in as
         * parameter.
         *
         * The Reactor blocks on select for I/O operations. This could be from
         * new clients requesting a connection or other clients that has
         * read/write I/O operations pending. Handler for the events are called
         * when an event occurs. The event occurs when selector.select()
         * returns.
         */
        @Override
        public void run() {
            try {
                Selector selector = Selector.open();
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.bind(new InetSocketAddress(port));
                SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                selectionKey.attach(new EchoAcceptor(selector, serverSocketChannel));

                while (!Thread.interrupted()) {
                    int selCount = selector.select();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();

                    /*
                     * If selCount is 0 and selectedKeys.size() is more than 1
                     * then we have disconnected clients for whom handlers needs
                     * to be removed. This is done by the Handler code. When the
                     * handler attempts a read operation and the operation
                     * returns -1, it indicates a closed connection. At which
                     * the selectionKey is detached by calling its cancel()
                     * method.
                     */
                    Iterator<SelectionKey> iteratorKey = selectedKeys.iterator();
                    while (iteratorKey.hasNext()) {
                        dispatchEvent(iteratorKey.next());
                    }
                }
            } catch (IOException ioException) {
                System.err.println("Either Server was intterupted or the Echo Reactor has other IOExceptions");
                ioException.printStackTrace();
            }
        }

        /**
         * When an event is triggered appropriate handlers are called. This
         * could either be the Acceptor which accepts new connection or the
         * EchoServerHandler which processes read/write operations on a socket
         * connection. The handler code is executed in the Half-ASync part of
         * the Half-Sync/Half-ASync pattern i.e. this initial connection and
         * read/write are done in the same thread.
         *
         * The EchoServerHandler uses a pool of Executors to process the
         * incoming data from the client using a Queue to pass data. After
         * processing the Executors puts back the processed data back into
         * another queue. This part runs in the Half-Sync part of the
         * Half-Sync/Half-ASync pattern.
         *
         * @param eventKey
         */
        private void dispatchEvent(SelectionKey eventKey) {
            NetworkEvent eventHandler = (NetworkEvent) eventKey.attachment();
            if (eventHandler != null) {
                eventHandler.processEvent();
            }
        }
    }

    /**
     * This is the interface used by the network event handlers i.e.
     * EchoAcceptor and EchoServerHandler.
     *
     * @author SomeOne
     */
    public interface NetworkEvent {
        public void processEvent();
    }

    /**
     * This is the Acceptor class of the Acceptor-Connector pattern.
     *
     * @author SomeOne
     */
    private class EchoAcceptor implements NetworkEvent {
        ServerSocketChannel serverSocketChannel;
        Selector selector;

        private EchoAcceptor(Selector selector, ServerSocketChannel serverSocketChannel) {
            this.serverSocketChannel = serverSocketChannel;
            this.selector = selector;
        }

        public void processEvent() {
            try {
                /*
                 * When an event occurs and if it is due to a new connection
                 * from a client then a new socket channel corresponding to the
                 * new client is available. A new handler object for the client
                 * is created which then manages all read/write operations on
                 * the client socket.
                 */
                SocketChannel clientSocketChannel = serverSocketChannel.accept();
                if (clientSocketChannel != null) {
                    clientSocketChannel.configureBlocking(false);
                    SelectionKey eventSelectionKey = clientSocketChannel.register(selector, SelectionKey.OP_READ);
                    new EchoServerHandler(clientSocketChannel, eventSelectionKey);
                    selector.wakeup();
                }
            } catch (IOException exception) {
                System.err.println("Echo Acceptor was not able to accept the client connection and create the handler");
                exception.printStackTrace();
            }
        }
    }

    private ExecutorService poolEchoHandler = Executors.newFixedThreadPool(3);

    /**
     * This is the Wrapper facade pattern. It wraps the low level socket I/O
     * operations in the server.
     *
     * The Wrapper runs in the same thread as the Reactor thread. The data
     * processing is done in Executor threads and runs as Half-Sync part of the
     * Half-Sync/Half-ASync pattern.
     *
     * @author SomeOne
     */
    private class EchoServerHandler implements NetworkEvent {
        private SocketChannel clientSocketChannel;
        private ByteBuffer outputBuf = null;
        private final SelectionKey eventSelectionKey;
        private final int READ = 1, WRITE = 2;
        private int curState = READ;
        private Queue<ByteBuffer> inQueue = new LinkedList<ByteBuffer>();
        private Queue<ByteBuffer> outQueue = new LinkedList<ByteBuffer>();

        /**
         * The new EchoServerHandler created attaches itself to handle read
         * operations. During the actual read/write operation it toggles to
         * alternate between read and write operations responding to client
         * request by just echoing back the string received from the client.
         *
         * @param clientChannel
         * @param key
         * @throws IOException
         */
        private EchoServerHandler(SocketChannel clientChannel, SelectionKey key) throws IOException {
            clientSocketChannel = clientChannel;
            eventSelectionKey = key;
            eventSelectionKey.attach(this);
        }

        public void processEvent() {
            try {
                if (curState == READ) {
                    read();
                } else if (curState == WRITE) {
                    write();
                }
            } catch (ClientDisconnectedException ioExcep) {
                /*
                 * Detach the server when read fails indicating a disconnected
                 * server.
                 */
                eventSelectionKey.cancel();
            } catch (IOException ioExcep) {
                ;
            }
        }

        /**
         * Read from the client socket.
         *
         * @throws ClientDisconnectedException
         * @throws IOException
         */
        private void read() throws IOException {
            ByteBuffer input = ByteBuffer.allocate(1024);
            int countBytes = clientSocketChannel.read(input);
            if (countBytes > 0) {
                input.flip();
                synchronized (inQueue) {
                    inQueue.add(input);
                    poolEchoHandler.execute(new ProcessEchoData(inQueue, outQueue, countBytes));
                }
                curState = WRITE;
                eventSelectionKey.interestOps(SelectionKey.OP_WRITE);
            } else if (countBytes < 0) {
                throw new ClientDisconnectedException();
            }
        }

        /**
         * Write to the client socket.
         *
         * @throws IOException
         */
        private void write() throws IOException {
            outputBuf = null;
            if (!outQueue.isEmpty()) {
                synchronized (outQueue) {
                    if (!outQueue.isEmpty()) {
                        outputBuf = outQueue.poll();
                    }
                }
            }

            if (outputBuf != null) {
                clientSocketChannel.write(outputBuf);
                outputBuf.clear();
                outputBuf = null;
                curState = READ;
                eventSelectionKey.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    /**
     * We throw a ClientDisconnectedException if client disconnects and read()
     * fails.
     *
     * @author SomeOne
     */
    private class ClientDisconnectedException extends RuntimeException {
    }

    /**
     * ProcessEchoData plays the part of the Half-Sync in the
     * Half-Sync/Half-ASync pattern. While the EchoReactor, EchoAcceptor and
     * EchoServerHandler methods runs in the Half-ASync part of the pattern.
     *
     * @author SomeOne
     */
    private class ProcessEchoData implements Runnable {
        private Queue<ByteBuffer> inQueue, outQueue;
        private int countBytes;

        public ProcessEchoData(Queue<ByteBuffer> inQ, Queue<ByteBuffer> outQ, int count) {
            inQueue = inQ;
            outQueue = outQ;
            countBytes = count;
        }

        public void run() {
            ByteBuffer input = null;
            synchronized (inQueue) {
                if (!inQueue.isEmpty()) {
                    input = inQueue.poll();
                }
            }

            if (input != null) {
                byte[] buffer = new byte[countBytes];
                System.arraycopy(input.array(), 0, buffer, 0, countBytes);
                ByteBuffer outputBuf = ByteBuffer.wrap(buffer);
                synchronized (outQueue) {
                    outQueue.add(outputBuf);
                }
            }
        }
    }
}
