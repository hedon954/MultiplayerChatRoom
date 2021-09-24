package client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Hedon Wang
 * @create 2021-09-24 4:39 PM
 */
public class ChatClient {
    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private Charset charset = Charset.forName("UTF-8");

    private String host;
    private int port;

    private AsynchronousSocketChannel clientSocketChannel;

    public ChatClient(){
        this(LOCALHOST, DEFAULT_PORT);
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 启动客户端
     */
    public void start(){

        try {
            // 获得一个客户端异步通道
            clientSocketChannel = AsynchronousSocketChannel.open();
            // 连接服务端
            Future<Void> connectFuture = clientSocketChannel.connect(new InetSocketAddress(this.host, this.port));

            // ======================================
            // 此处可以加一些其他的操作，因为是异步非阻塞的
            // ======================================

            // 等待连接结束
            connectFuture.get();

            // 处理用户输入
            new Thread(new UserInputHandler(this)).start();

            // 接收其他客户端的消息
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
            while (true) {
                Future<Integer> readFuture = clientSocketChannel.read(buffer);
                int result = readFuture.get();
                if (result <= 0){
                    System.out.println("服务器断开...");
                    break;
                }
                buffer.flip();
                System.out.println(charset.decode(buffer));
                buffer.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            closeResource(clientSocketChannel);
        }

    }


    /**
     * 向服务器发送信息
     */
    public void send(String msg) {
        if (msg.isEmpty()) {
            return;
        }
        Future<Integer> writeFuture = clientSocketChannel.write(charset.encode(msg));
        try {
            writeFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断客户端是否要下线
     */
    public boolean readyToQuit(String msg){
        return msg.equals(QUIT);
    }

    /**
     * 释放资源
     */
    private void closeResource(Closeable closeable){
        if (closeable != null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
