package server;

/**
 * @author Hedon Wang
 * @create 2021-09-24 4:39 PM
 */
public class ChatServerStarter {
    public static void main(String[] args) {
        new ChatServer(9999).start();
    }
}
