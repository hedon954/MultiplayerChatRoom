package client;

/**
 * @author Hedon Wang
 * @create 2021-09-23 7:13 PM
 */
public class ChatClientStarter {
    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient("127.0.0.1", 7777);
        chatClient.start();
    }
}
