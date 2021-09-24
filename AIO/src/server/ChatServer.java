package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Hedon Wang
 * @create 2021-09-24 4:38 PM
 */
public class ChatServer {

    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final int THREAD_POOL_SIZE = 8;

    private AsynchronousChannelGroup channelGroup;                  // 自定义 asyncChannelGroup
    private AsynchronousServerSocketChannel serverSocketChannel;    // 服务端异步通道

    private List<ClientHandler> connectedClients;
    private int port;

    public ChatServer(){
        this(DEFAULT_PORT);
    }

    public ChatServer(int port){
        this.port = port;
        this.connectedClients = new ArrayList<>();
    }

    /**
     * 启动服务器
     */
    public void start() {
        // 定义一个线程池，给 channel group 用
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try {
            // 自定义 asyncChannelGroup
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            // 开一个服务端通道
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
            // 绑定端口
            serverSocketChannel.bind(new InetSocketAddress(LOCALHOST, this.port));
            System.out.println("启动服务端，监听端口：" + this.port + "...");

            // 监听客户端的连接请求
            while (true) {
                // 参数1：附带对象
                // 参数2：客户端连接后要进行的回调
                serverSocketChannel.accept(null, new AcceptHandler(this.serverSocketChannel, this.connectedClients));
                // 阻塞一下，避免一直循环。
                // accept 后，read 前可以做其他一些操作，因为是异步非阻塞的
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeResource(serverSocketChannel);
            if (channelGroup != null) {
                channelGroup.shutdown();
            }
        }
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
