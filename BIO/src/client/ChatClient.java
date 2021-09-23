package client;

import java.io.*;
import java.net.Socket;

/**
 * @author Hedon Wang
 * @create 2021-09-18 10:11 PM
 */
public class ChatClient {

    private final String DEFAULT_SERVER_HOST = "127.0.0.1";     // 服务端主机
    private final int DEFAULT_SERVER_PORT = 8888;               // 服务端端口
    private final String QUIT = "quit";                         // 客户端退出命令

    private Socket socket;  // 客户端 socket
    private BufferedReader reader;
    private BufferedWriter writer;

    /**
     * 发送消息
     */
    public void sendMessage(String message) throws IOException {
        if (socket != null && !socket.isOutputShutdown()) {
            if (writer != null) {
                writer.write(message + "\n");
                writer.flush();
            }
        }
    }

    /**
     * 接收消息
     */
    public String receiveMessage() throws IOException {
        if (socket != null && !socket.isInputShutdown()) {
            if (reader != null) {
                return reader.readLine();
            }
        }
        return null;
    }

    /**
     * 检查用户是否准备退出
     */
    public boolean readyToQuit(String message) {
        return QUIT.equals(message);
    }

    /**
     * 启动客户端
     */
    public void start() {
        try {
            // 创建 socket，绑定服务端
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);

            // 创建 IO 流
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            // 处理用户输入
            new Thread(new UserInputHandler(this)).start();

            // 读取服务器转发的其他客户端的信息
            String message = null;
            while ((message = receiveMessage()) != null) {
                System.out.println(message);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    /**
     * 关闭客户端
     */
    private void close() {

        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
