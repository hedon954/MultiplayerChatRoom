package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 收到客户端发来的信息后，IO 完成后要做的回调
 *
 * @param   '<V>'     The result type of the I/O operation                      这里是从客户端读到了多少数据，所以是 Integer
 * @param   '<A>'     The type of the object attached to the I/O operation      附加对象的类型，这里是 ByteBuffer
 */
public class ClientHandler implements CompletionHandler<Integer, ByteBuffer> {

    private static final String QUIT = "quit";

    private Charset charset = Charset.forName("UTF-8");

    private AsynchronousSocketChannel clientChannel;
    private List<ClientHandler> connectedClients;

    public ClientHandler(AsynchronousSocketChannel clientChannel, List<ClientHandler> connectedClients) {
        this.clientChannel = clientChannel;
        this.connectedClients = connectedClients;
    }

    /**
     * IO 正常完成后要做的回调： 将客户端发来的消息转发给其他客户端
     */
    @Override
    public void completed(Integer result, ByteBuffer attachment) {

        ByteBuffer buffer = attachment;

        // 如果 attachment 为空，则表示之前服务器刚刚对 buffer 完成了读操作（即已经转发了），不需要额外的操作
        if (buffer == null) {

        }
        // 如果 attachment 不为空，则表示服务器刚刚对 buffer 完成了写操作（即还没转发），现在可以来读取它
        else {
            // 客户端异常
            if (result <= 0) {
                removeClient(this);
                return;
            }
            // 将 buffer 从写模式切换为读模式
            buffer.flip();
            // 获取客户端发来的消息
            String fwdMsg = receive(buffer);
            System.out.println(getClientName(this.clientChannel) + "：" + fwdMsg);
            // 转发
            forwardMessage(clientChannel, fwdMsg);
            buffer.clear();

            // 判断用户是否要退出
            if (readyToQuit(fwdMsg)) {
                // 退出，移除客户端
                removeClient(this);
            } else {
                // 不退出，继续监听客户端信息
                clientChannel.read(buffer, buffer, this);
            }
        }

    }

    /**
     * IO 异常结束后要做的回调
     */
    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
        System.out.println("ClientHandler 发生异常了，exception: " + exc + "，attachment: " + attachment);
    }

    /**
     * 获取客户端发来的消息
     */
    private String receive(ByteBuffer buffer) {
        return String.valueOf(charset.decode(buffer));
    }

    /**
     * 构建客户端名称
     */
    private String getClientName(AsynchronousSocketChannel socketChannel) {
        String port = "UNKNOWN_CLIENT";
        try {
            InetSocketAddress remoteAddress = (InetSocketAddress)clientChannel.getRemoteAddress();
            port = "" + remoteAddress.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "客户端 [" + port + "] ";
    }

    /**
     * 判断客户端是否要下线
     */
    private boolean readyToQuit(String msg){
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

    /**
     * 添加新客户端
     */
    public synchronized void addClient(ClientHandler clientHandler) {
        this.connectedClients.add(clientHandler);
        System.out.println(getClientName(clientHandler.clientChannel) + "上线");
    }

    /**
     * 移除异常客户端
     */
    public synchronized void removeClient(ClientHandler clientHandler) {
        this.connectedClients.remove(clientHandler);
        System.out.println(getClientName(clientHandler.clientChannel) + "下线");
        closeResource(clientHandler.clientChannel);
    }

    /**
     * 转发 self 的消息给 connectedClients 中其他的信息
     */
    private synchronized void forwardMessage(AsynchronousSocketChannel self, String fwdMsg) {
        for (ClientHandler clientHandler: this.connectedClients) {
            // 不转发给自身
            if (clientHandler.clientChannel.equals(self)) {
                continue;
            }
            // 转发给其他客户端
            try {
                clientHandler.clientChannel.write(charset.encode(getClientName(self) + fwdMsg), null, clientHandler);
            }catch (Exception e){
                // 捕获异常是为了避免某个客户端出意外而导致整个系统瘫痪
                e.printStackTrace();
            }
        }
    }
}
