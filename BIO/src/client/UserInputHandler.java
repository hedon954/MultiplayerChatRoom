package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Hedon Wang
 * @create 2021-09-18 10:11 PM
 */
public class UserInputHandler implements Runnable{

    private ChatClient chatClient;          // 对应的客户端

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        // 等待用户输入消息
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String message = null;

        try {
            while (true) {
                // 读取用户输入
                message = reader.readLine();

                // 向服务器发送消息
                chatClient.sendMessage(message);

                // 检查用户是否准备退出
                if (chatClient.readyToQuit(message)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
