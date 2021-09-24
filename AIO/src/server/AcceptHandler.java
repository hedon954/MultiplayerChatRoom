package server;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.List;

/**
 * 客户端连接完成后要做的回调
 *
 * @param   '<V>'     The result type of the I/O operation                      这里要返回的是客户端对应的异步通道
 * @param   '<A>'     The type of the object attached to the I/O operation      附加对象的类型
 */
public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

    private static final int BUFFER = 1024;

    private AsynchronousServerSocketChannel serverSocketChannel;
    private List<ClientHandler> connectedClients;

    public AcceptHandler(AsynchronousServerSocketChannel serverSocketChannel, List<ClientHandler> connectedClients) {
        this.serverSocketChannel = serverSocketChannel;
        this.connectedClients = connectedClients;
    }

    /**
     * IO 正常完成后要做的回调： 接收客户端发来的信息 -> 转发给其他客户端
     */
    @Override
    public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
        // 如果服务端还在线的话，要继续监听客户端的连接请求
        if (serverSocketChannel.isOpen()) {
            serverSocketChannel.accept(null, this);
        }

        if (clientChannel != null && clientChannel.isOpen()) {
            ClientHandler clientHandler = new ClientHandler(clientChannel, this.connectedClients);
            // 添加新客户端
            clientHandler.addClient(clientHandler);

            // 接收客户端发来的信息
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
            // 参数1：把客户端发来的信息读要 buffer 缓冲区中
            // 参数2：将 buffer 作为附加对象传给回调对象
            // 参数3：回调对象，每个客户端对应一个自己的 ClientHandler
            clientChannel.read(buffer, buffer, clientHandler);
        }
    }

    /**
     * IO 异常结束后要做的回调
     */
    @Override
    public void failed(Throwable exc, Object attachment) {
        System.out.println("AcceptHandler 发生异常了，exception: " + exc + "，attachment: " + attachment);
    }
}
