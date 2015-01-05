import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.Set;


public class PropertiesReaderTest {

    @Test
    public void shouldReadProperties() throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        final int DEFAULT_PORT = 5555;
        final String IP = "127.0.0.1";
        serverSocketChannel.bind(new InetSocketAddress(IP, DEFAULT_PORT));
        //serverSocketChannel.configureBlocking(false);
        SocketChannel socketChannel=serverSocketChannel.accept();
        System.out.println("Incoming connection from: " + socketChannel.getRemoteAddress());

        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        while(socketChannel.read(buffer) !=-1){
            buffer.flip();
            socketChannel.write(buffer);
            if(buffer.hasRemaining()){
                buffer.compact();
            }else{
                buffer.clear();
            }


        }
    }

    @Test
    public void shouldShowAvailableProcessors(){
        System.out.println(Runtime.getRuntime().availableProcessors());
    }
}
