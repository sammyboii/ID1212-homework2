package client;

import java.io.IOException;

public class Client {

    public static void main(String[] args) throws ClassNotFoundException {
        try {
            new CLI().start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
