package client;

import server.ChatServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Hedon Wang
 * @create 2021-09-23 6:59 PM
 */
public class UserInputHandler implements Runnable {

    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatClient){
        this.chatClient = chatClient;
    }

    @Override
    public void run() {

        try {
            // 等待用户输入消息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = consoleReader.readLine();
                // 像服务器发送消息
                chatClient.send(input);

                // 检查用户是否退出
                if (chatClient.readyToQuit(input)) {
                    break;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
