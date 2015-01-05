package org.demo.core.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by vx00418 on 12/4/2014.
 */
public class Reactor implements Runnable {
    final Selector selector;
    final ServerSocketChannel serverSocketChannel;


    Reactor(int port) throws IOException{
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector,SelectionKey.OP_ACCEPT,new Acceptor());
    }

    @Override
    public void run() {
        try{
            while (!Thread.interrupted()){
                selector.select();
                Set<SelectionKey> selectionKeys= selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                while (it.hasNext()){
                    dispatch(it.next());
                    selectionKeys.clear();
                }
            }
        }catch (IOException ex){

        }
    }

    public void dispatch(SelectionKey k){
         Runnable r = (Runnable)(k.attachment());
        if(r != null){
            new Thread(r).start();
        }
    }

    public class Acceptor implements Runnable{

        @Override
        public void run(){
            try{
                SocketChannel c = serverSocketChannel.accept();
                if(c!=null)
                    new Thread(new Handler(selector,c)).start();
            }
            catch(IOException ex){
                throw new RuntimeException(ex);
            }
        }
    }
}
