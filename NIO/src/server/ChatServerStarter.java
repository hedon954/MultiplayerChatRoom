package server;

/**
 * @author Hedon Wang
 * @create 2021-09-23 6:58 PM
 */
public class ChatServerStarter {
    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }
}
