package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * @author Hedon Wang
 * @create 2021-09-18 10:10 PM
 */
public class ChatHandler implements Runnable{

    private ChatServer chatServer;          // 服务端
    private Socket socket;                  // 客户端

    public ChatHandler(ChatServer chatServer, Socket socket) {
        this.chatServer = chatServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 存储新上线的客户端
            chatServer.addClient(socket);
            System.out.println("添加客户端 [" + socket.getPort() + "] 成功！");

            // 读取用户发送的信息
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            String msg = null;
            while ((msg = reader.readLine()) != null) {
                // 检查用户是否是退出命令
                if (chatServer.readyToQuit(msg.trim())) {
                    break;
                }

                // 转发消息到其他在线的客户端
                chatServer.forwardMessage(socket, "客户端 [" + socket.getPort() + "]： " + msg + "\n");

            }
        } catch (IOException e) {
            System.out.println("添加客户端 [" + socket.getPort() + "] 失败...");
            e.printStackTrace();
        } finally {
            try {
                // 移除客户端
                chatServer.removeClient(socket);
            } catch (IOException e) {
                System.out.println("客户端 [" + socket.getPort() + "] 下线异常...");
                e.printStackTrace();
            }
        }
    }

}
