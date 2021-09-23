package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Hedon Wang
 * @create 2021-09-18 10:10 PM
 */
public class ChatServer {

    private int DEFAULT_PORT = 8888;                                // 默认端口
    private final String QUIT = "quit";                             // 客户端退出命令

    private ServerSocket serverSocket;                              // socket

    private ExecutorService executorService;                        // 线程池
    private HashMap<Integer, Writer> connectedClients;              // 端口：写对象

    public ChatServer() {
        connectedClients = new HashMap<>();
        executorService = Executors.newFixedThreadPool(10);
    }

    /**
     * 客户端上线
     * @param socket        accept 到的客户端 socket
     * @throws IOException  获取 socket 的 outputStream 时可能抛出 IOException
     */
    public synchronized void addClient(Socket socket) throws IOException{
        if (socket != null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
            // 添加
            connectedClients.put(port, writer);
            // 日志
            System.out.println("客户端 [" + port + "] 已连接到服务器");
        }
    }

    /**
     * 客户端下线
     * @param socket        accept 到的客户端 socket
     * @throws IOException  关闭 socket 的 outputStream 时可能抛出 IOException
     */
    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null){
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                // 关闭 writer 对象
                connectedClients.get(port).close();
                // 移除
                connectedClients.remove(port);
                // 日志
                System.out.println("客户端 [" + socket.getPort() + "] 已下线");
            }
        }
    }

    /**
     * 服务端转发信息给除发送者之外的所有客户端
     * @param socket        发送者
     * @param message       消息
     * @throws IOException  向 socket 的 outputStream 进行写操作时可能抛出 IOException
     */
    public synchronized void forwardMessage(Socket socket, String message) throws IOException {
        if (socket != null && !message.isEmpty()) {
            for (Integer port: connectedClients.keySet()) {
                if (!port.equals(socket.getPort())) {
                    Writer writer = connectedClients.get(port);
                    writer.write(message);
                    writer.flush();
                }
            }
        }
    }

    /**
     * 检查用户是否准备退出
     */
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 服务端主线程
     * 1. 监听客户端，等待客户端连接
     * 2. 接收客户端信息，并进行转发
     * 3. 维护在线的客户端列表
     */
    public void start(){
        try {
            // 绑定监听端口
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口：" + DEFAULT_PORT + "...");
            // 监听客户端请求
            Socket accept;
            while (true) {
                // 等待客户端连接
                accept = serverSocket.accept();
                // 创建 ChatHandler 线程
                executorService.execute(new ChatHandler(this, accept));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }

    }

    /**
     * 关闭服务器
     */
    private synchronized void close(){
        if (serverSocket != null){
            try {
                serverSocket.close();
                System.out.println("服务器正常退出...");
            }catch (IOException e) {
                System.out.println("服务器退出异常...");
                e.printStackTrace();
            }
        }
    }
}
