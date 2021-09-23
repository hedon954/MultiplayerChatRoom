package server;

import server.ChatServer;

/**
 * @author Hedon Wang
 * @create 2021-09-18 10:38 PM
 */
public class ChatServerStarter {

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}
