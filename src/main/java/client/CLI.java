package client;

import shared.GameSnapshot;
import shared.Listener;
import shared.Parser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class CLI implements Runnable {
    private Net net;
    private boolean takingInput = false;

     void start () throws IOException, ClassNotFoundException {
        if (takingInput) {
            return;
        }
        takingInput = true;
        Listener observer = new ResponseListener();
        net = new Net();
        net.connect("localhost", 1337, observer);
        new Thread(this).start();
    }

    private static void showInputSign() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        System.out.print("> ");
    }

    @Override
    public void run () {
        while (takingInput) {
            Scanner scanner = new Scanner(System.in);
            String command = scanner.nextLine();
            showInputSign();
            try {
                this.net.sendCommand(command);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                showInputSign();
            }
        }
        System.out.println("Disconnected");
    }

    private static void handleResponse (String response) {
        switch (Parser.typeOf(response)) {
            case STARTED:
                System.out.println("Game started! Enter 'guess' followed by a letter or a word to start guessing");
                renderSnapshot(Parser.gameSnapshot(Parser.bodyOf(response)));
                break;
            case INFO:
                System.out.println(Parser.bodyOf(response));
                break;
            case SNAPSHOT:
                renderSnapshot(Parser.gameSnapshot(Parser.bodyOf(response)));
                break;
            default:
                System.out.println("Unknown response from server");
        }
        showInputSign();
    }

    private static void renderSnapshot(GameSnapshot game) {
        String formatted;
        if (!game.isFinished()) {
            formatted = "Progress: " + game.getProgress() + " | Attempts remaining: " + (game.getMaxAttempts() - game.getAttempts() + " | Score: " + game.getScore());
        } else {
            formatted = "You've run out of attempts :( Enter 'start' to begin a new game, or 'quit' to exit";
            if (game.isWon())
                formatted = "Congratulations! You guessed it. Enter 'start' to begin a new game, or 'quit' to exit";
        }
        System.out.println(formatted);
    }

    private class ResponseListener implements Listener {
        @Override
        public void connected(InetSocketAddress address) {
            System.out.println("Welcome to HangMan! Enter 'start' to begin a new game, or 'quit' to exit.");
            showInputSign();
        }

        @Override
        public void disconnected() {
            System.out.println("Disconnected from server");
        }

        @Override
        public void receivedContent(String content) {
            handleResponse(content);
        }
    }
}
