package client;

/**
 * @author Hedon Wang
 * @create 2021-09-24 4:39 PM
 */
public class ChatClientStarter {
    public static void main(String[] args) {
        new ChatClient("localhost", 9999).start();
    }
}
