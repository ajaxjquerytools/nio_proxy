package org.demo.core.examples.ex5;

/**
 * Created by vx00418 on 12/1/2014.
 */

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;


public class SimpleClient {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String string1 = "GET / HTTP/1.0\n" +
                "\n";

        String string2 = "Second message";
        SocketTest test1 = new SocketTest(string1);
        Thread thread = new Thread(test1);
        thread.start();
        //thread2.start();
    }

    static class SocketTest implements Runnable {

        private String message = "";
        private Selector selector;
        WritableByteChannel soutChannel = Channels.newChannel(System.out);
        private ByteBuffer responseByteBuffer = ByteBuffer.allocateDirect(512);

        public SocketTest(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            SocketChannel channel;
            try {
                selector = Selector.open();

                InetSocketAddress ia = new InetSocketAddress("xldn4997vdap.ldn.swissbank.com", 8000);

                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(ia);


                channel.register(selector, SelectionKey.OP_CONNECT);


                while (!Thread.interrupted()) {
                    selector.selectNow();
//                    selector.select(1000);

                    Set<SelectionKey> keys = selector.selectedKeys();
                    for (Iterator iter = keys.iterator(); iter.hasNext(); ) {

                        SelectionKey key = (SelectionKey) iter.next();
                        iter.remove();
                        SocketChannel socketChannel = (SocketChannel) key.channel();

                        if (!key.isValid()) continue;

                        if (key.isConnectable()) {
                            System.out.println("I am connected to the server");
                            connect(key);
                        }

//                      if(key.isConnectable()){
//                          if(channel.finishConnect()){
//                               channel.register(selector,SelectionKey.OP_WRITE);
//                           }else{
//                               channel.register(selector,SelectionKey.OP_CONNECT);
//                           }
//                      }

                        if (key.isWritable()) {
                            System.out.println("Is writable == true");
                            write(key);
                            channel.write(ByteBuffer.wrap(message.getBytes()));
                            channel.register(selector, SelectionKey.OP_READ);
                        }
                        if (key.isReadable()) {
                            System.out.println("Is readeble == true");

                            if(readToChannelNotBlocking(key)){
                                //TODO: finish logic should be here
                                System.exit(0);
                            }
//                            else{
//                                channel.register(selector,SelectionKey.OP_READ);
//                            }
                            //System.exit(0);
                        }


                    }
                }
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } finally {
                close();
            }
        }

        private void close() {
            try {
                selector.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private void read(SelectionKey key) throws IOException {

            System.out.println("reading started");
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer readBuffer = ByteBuffer.allocate(1024);
            readBuffer.clear();
            int length;
            try {
                length = channel.read(readBuffer);
            } catch (IOException e) {
                System.out.println("Reading problem, closing connection");
                key.cancel();
                channel.close();
                return;
            }
            if (length == -1) {
                System.out.println("Nothing was read from server");
                channel.close();
                key.cancel();
                return;
            }
            readBuffer.flip();
            CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();
            //byte[] buff = new byte[1024];
            //readBuffer.get(buff, 0, length);
            // System.out.println("Server said: " + new String(buff));
            CharBuffer buf = decoder.decode(readBuffer);
            System.out.println(buf.toString());
            //Close connection after response from server
            key.cancel();
            channel.close();
        }

        private void readToChannelBlocking(SelectionKey key) throws IOException {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            while (clientChannel.read(buffer) != -1) {
                // Prepare the buffer to be drained
                buffer.flip();
                // Make sure that the buffer was fully drained
                while (buffer.hasRemaining()) {
                    soutChannel.write(buffer);
                }
                // Make the buffer empty, ready for filling
                buffer.clear();
            }
        }

        private boolean readToChannelNotBlocking(SelectionKey key) throws IOException {
            responseByteBuffer.clear();
            System.out.println("NEW CHANK======");
            SocketChannel clientChannel = (SocketChannel) key.channel();
            int remaining = clientChannel.read(responseByteBuffer);
            // Prepare the buffer to be drained
            responseByteBuffer.flip();

            soutChannel.write(responseByteBuffer);
            if (remaining > 0) return false;
            return true;

        }

        private boolean readToChannelPippedBlocked(SelectionKey key) throws IOException{

            SocketChannel clientChannel = (SocketChannel) key.channel();

            Pipe pipe = key.channel().provider().openPipe();
            pipe.sink();

            return false;
        }
        private boolean readToChannelPippedNotBlocked(SelectionKey key) throws IOException{
            return false;
        }

        private void write(SelectionKey key) throws IOException {
            System.out.println("writing started");
            SocketChannel channel = (SocketChannel) key.channel();
            channel.write(ByteBuffer.wrap(message.getBytes()));
            key.interestOps(SelectionKey.OP_READ);

            // key.interestOps(SelectionKey.OP_WRITE|SelectionKey.OP_READ);
            //channel.register(key.selector(),SelectionKey.OP_READ);

            //key.selector().wakeup();

            System.out.println("writing finished");
        }

        private void connect(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel) key.channel();
            if (channel.isConnectionPending()) {
                channel.finishConnect();
            }
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_WRITE);
        }
    }
}
