package server;


import com.sun.tools.internal.xjc.BadCommandLineException;
import shared.Constants;
import shared.GameSnapshot;
import server.Hangman.Hangman;
import shared.Parser;

import java.util.ArrayList;
import java.util.Arrays;

class Controller {
    private Hangman hangman;
    private String[] commands = new String[]{"start","guess","quit"};

    Controller () {
        try {
            this.hangman = new Hangman();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Response parseCommand (String cmd) throws ErroneousInputException,InternalError {
        String[] command = cmd.split(" ");
        switch (command[0]) {
            case "start":
                return new Response(Parser.ContentType.STARTED, this.hangman.start().toString());
            case "quit":
                return new Response(Parser.ContentType.INFO, this.hangman.endGame(false).toString());
            case "guess":
                if (command.length < 2)
                    throw new ErroneousInputException("Guesses must be in the format 'guess <letter>' or 'guess <word>'.\nFor example: 'guess r' or 'guess pirate'");
                return new Response(Parser.ContentType.SNAPSHOT, this.hangman.guess(command[1]).toString());
            default:
                throw new ErroneousInputException("'" + command[0] + "' is not a recognized command. Available commands: " + Arrays.toString(commands));
        }
    }

    public String[] getCommands() {
        return commands;
    }

}
