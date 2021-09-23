package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * @author Hedon Wang
 * @create 2021-09-23 6:40 PM
 */
public class ChatServer {

    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Charset charset = Charset.forName("UTF-8");

    private int port;

    public ChatServer(){
        this(DEFAULT_PORT);
    }

    public ChatServer(int port){
        this.port = port;
    }

    /**
     * 启动服务端
     */
    public void start(){
        try {
            // 获得服务端的通道
            serverSocketChannel = ServerSocketChannel.open();
            // 修改为非阻塞模式
            serverSocketChannel.configureBlocking(false);
            // 绑定端口
            serverSocketChannel.bind(new InetSocketAddress(this.port));

            // 获得 Channel 控制器 Selector 对象
            selector = Selector.open();
            // 将服务端 Channel 注册到 Selector 中，注册 ACCEPT 事件
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口：" + this.port + "...");

            // Selector 监听事件
            while (true) {
                selector.select();
                // 处理所有被触发的事件
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey selectionKey : selectionKeys){
                    handles(selectionKey);
                }
                // 清空之前的事件集
                selectionKeys.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeResource(selector);
        }
    }

    /**
     * 处理被触发的事件
     */
    private void handles(SelectionKey selectionKey) throws IOException {
        // ACCEPT 事件 —— 即和客户端建立连接
        if (selectionKey.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel)selectionKey.channel();
            // 获取客户端 channel
            SocketChannel client = server.accept();
            // 将客户端 channel 转为非阻塞模式
            client.configureBlocking(false);
            // 为客户端 channel 注册 READ 事件
            // 当 READ 事件触发时，表示有客户端写东西了，channel 有可以读的东西
            client.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(client) + "已连接");
        }
        // READ 事件 —— 即客户端发来信息，需要转发给其他客户端
        else if (selectionKey.isReadable()) {
            SocketChannel client = (SocketChannel)selectionKey.channel();
            // 获取客户端发来的信息
            String fwdMsg = receive(client);
            if (fwdMsg.isEmpty()) {
                // 空信息 -> 客户端异常 -> 退出客户端
                selectionKey.cancel();
                selector.wakeup();
            } else {
                System.out.println(getClientName(client) + ": " + fwdMsg);

                // 转发信息
                forwardMessage(client, fwdMsg);

                // 判断用户是否准备退出
                if (readyToQuit(fwdMsg)){
                    selectionKey.cancel();
                    selector.wakeup();
                    System.out.println(getClientName(client) + "已断开");
                }
            }
        }
    }

    /**
     * 将 client 发来的信息转发给其他客户端
     */
    private void forwardMessage(SocketChannel client, String fwdMsg) throws IOException {
        for (SelectionKey key : selector.keys()) {
            Channel connectedChannel = key.channel();
            // 不转发给服务端
            if (connectedChannel instanceof ServerSocketChannel) {
                continue;
            }
            // 不转发给自身
            if (key.isValid() && connectedChannel instanceof SocketChannel && !client.equals(connectedChannel)) {
                wBuffer.clear();
                // 先将数据写到 wBuffer 中
                wBuffer.put(charset.encode(getClientName(client) + ": " + fwdMsg));
                wBuffer.flip();
                // 将从 rBuffer 将数据送到 channel
                while (wBuffer.hasRemaining()){
                    ((SocketChannel)connectedChannel).write(wBuffer);
                }
            }
        }
    }

    /**
     * 接收客户端发来的信息
     */
    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        // 将 channel 数据读到 rBuffer
        while (client.read(rBuffer) > 0) {}
        // 将 rBuffer 的写模式转为读模式
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    /**
     * 构建客户端名称
     */
    private String getClientName(SocketChannel client) {
        return "客户端 [" + client.socket().getPort() + "] ";
    }

    /**
     * 判断客户端是否准备退出
     */
    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 释放资源
     */
    private void closeResource(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
