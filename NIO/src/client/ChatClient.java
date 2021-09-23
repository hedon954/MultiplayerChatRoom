package client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * @author Hedon Wang
 * @create 2021-09-23 6:59 PM
 */
public class ChatClient {

    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private String host;
    private int port;
    private SocketChannel clientSocketChannel;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);
    private Selector selector;
    private Charset charset = Charset.forName("UTF-8");


    public ChatClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
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
            // 获取客户端 channel
            clientSocketChannel = SocketChannel.open();
            // 设置为非阻塞模式
            clientSocketChannel.configureBlocking(false);

            // 创建一个通道管理器 selector
            selector = Selector.open();
            // channel 注册 CONNECT 事件到 selector
            clientSocketChannel.register(selector, SelectionKey.OP_CONNECT);
            // 连接服务端
            clientSocketChannel.connect(new InetSocketAddress(this.host, this.port));

            // 监听事件
            while (true){
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    handles(key);
                }
                selectionKeys.clear();
            }
        }catch (IOException e){
            e.printStackTrace();
        } catch (ClosedSelectorException e) {
            // 正常退出，无需处理
        } finally {
            closeResource(selector);
        }
    }

    /**
     * 处理被触发的事件
     */
    private void handles(SelectionKey key) throws IOException {
        // CONNECT 事件 —— 客户端已经连接上服务端了
        if (key.isConnectable()) {
            SocketChannel client = (SocketChannel)key.channel();
            // 判断是否已经完成连接
            if (client.isConnectionPending()) {
                client.finishConnect();
                // 处理用户输入
                new Thread(new UserInputHandler(this)).start();
            }
            // 注册 READ 事件
            client.register(selector, SelectionKey.OP_READ);
        }
        // READ 事件 —— 服务端转发别的客户端的消息过来
        else if (key.isReadable()) {
            SocketChannel clientSocketChannel = (SocketChannel) key.channel();
            String msg = receive(clientSocketChannel);
            if (msg.isEmpty()) {
                // 空信息 -> 服务端异常 -> 客户端退出
                closeResource(selector);
            } else {
                System.out.println(msg);
            }
        }
    }

    /**
     * 从通道中读取信息
     */
    private String receive(SocketChannel clientSocketChannel) throws IOException {
        rBuffer.clear();
        while (clientSocketChannel.read(rBuffer) > 0){}
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    /**
     * 发送消息
     */
    public void send(String input) throws IOException {
        if (input.isEmpty()) {
            return;
        }

        // 先写入 wBuffer
        wBuffer.clear();
        wBuffer.put(charset.encode(input));
        wBuffer.flip();
        // 再转到 channel
        while (wBuffer.hasRemaining()){
            clientSocketChannel.write(wBuffer);
        }

        // 监控用户是否退出
        if (readyToQuit(input)) {
            closeResource(selector);
        }
    }

    /**
     * 判断客户端是否准备退出
     */
    public boolean readyToQuit(String msg) {
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
