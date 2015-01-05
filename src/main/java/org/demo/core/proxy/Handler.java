package org.demo.core.proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by vx00418 on 12/8/2014.
 */
public class Handler implements Runnable {
    final SocketChannel channel;
    final SelectionKey key;
    ByteBuffer inputBuffer = ByteBuffer.allocate(1024);
    ByteBuffer outputBuffer = ByteBuffer.allocate(1024);

    static enum State {READING, SENDING}

    ;

    State handlerState = State.READING;

    public Handler(Selector selector, SocketChannel channel) throws IOException {
        this.channel = channel;
        channel.configureBlocking(false);
        key = channel.register(selector, SelectionKey.OP_READ, this);

    }

    public boolean inputIsCompleted() {
        return true;
    }

    public boolean outputIsCompleted() {
        return true;
    }

    void process() {

    }

    public void run() {
        try {
            if (handlerState == State.READING) read();
            else if (handlerState == State.SENDING) send();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void read() throws IOException {
        channel.read(inputBuffer);
        if (inputIsCompleted()) {
            process();
            handlerState = State.SENDING;
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    public void send() throws IOException {
        channel.write(outputBuffer);
        if (outputIsCompleted()) {
            key.cancel();
        }
    }
}
