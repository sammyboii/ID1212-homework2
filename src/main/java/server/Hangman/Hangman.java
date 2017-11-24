package server.Hangman;

import com.sun.tools.internal.xjc.BadCommandLineException;
import server.ErroneousInputException;
import shared.GameSnapshot;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.*;

public class Hangman {
    private String[] word;
    private String wordString;
    private int score = 0;
    private boolean started = false;
    private final WordLibrary library = WordLibrary.getInstance();
    private String[] correctGuesses;
    private int attempts;
    private int maxAttempts;
    private boolean firstRun;
    private Set<String> guesses;

    public int getWordLength () {
        return word.length;
    }

    private String[] getProgress () {
        return this.correctGuesses;
    }

    public GameSnapshot start () throws InternalError {
        try {
            this.wordString = library.getRandomWord();
            this.word = wordString.split("");
            this.maxAttempts = word.length;
            this.guesses = new HashSet<>();
            this.correctGuesses = new String[word.length];
            this.attempts = 0;
            this.firstRun = true;
            this.started = true;
        } catch (Exception ex) {
            throw new InternalError("Could not construct Hangman");
        }
        GameSnapshot initialSnapshot = createSnapshot("Game started");
        firstRun = false;
        return initialSnapshot;
    }

    private GameSnapshot createSnapshot(String message) {
        return new GameSnapshot(firstRun, message, isFinished(), getProgress(), attempts, maxAttempts, score);
    }

    private GameSnapshot createSnapshot() {
        return createSnapshot(null);
    }

    public GameSnapshot endGame (boolean win) {
        score = win ? score + 1 : score - 1;
        this.started = false;
        return createSnapshot("Game ended");
    }

    public GameSnapshot guess(String s) throws ErroneousInputException {
        validate(s);
        guesses.add(s);

        if (isWordGuess(s)) {
            if (s.equals(wordString)) {
                correctGuesses = word;
                return endGame(true);
            }
            else {
                if (++attempts == maxAttempts) {
                    return endGame(false);
                }

                return createSnapshot("The word " + s + " is incorrect");
            }
        } else {
            boolean matched = false;
            for (int i = 0; i < word.length; i++) {
                if (word[i].equals(s)) {
                    matched = true;
                    correctGuesses[i] = s;
                }
            }

            if (!matched) {
                ++attempts;
                if (attempts == maxAttempts) {
                    return endGame(false);
                }
            }

            return createSnapshot();
        }
    }

    private boolean isFinished() {
        return attempts == maxAttempts;
    }

    private void validate(String s) throws ErroneousInputException {
        if (!started) {
            throw new ErroneousInputException("Game must be started before you can make a guess");
        }

        if (s.length() < 1) {
            throw new ErroneousInputException("No guess provided!");
        }

        if (s.length() > word.length) {
            throw new ErroneousInputException("Guess too long!");
        }

        if (s.length() > 1 && !isWordGuess(s)) {
            throw new ErroneousInputException("Guess must be of 1 letter or entire word");
        }

        if (guesses.contains(s)) {
            throw new ErroneousInputException("You already guessed '" + s + "'");
        }
    }

    private boolean isWordGuess(String s) {
       return (s.length() == word.length);
    }
}
