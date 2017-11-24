package server.Hangman;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class WordLibrary {
    private static WordLibrary instance = null;
    private static ArrayList<String> words;

    private WordLibrary() {
        words = loadWords("/Users/smartin/Downloads/words.txt");
    }

    public static String getRandomWord () {
        Random rand = new Random();
        int randomIndex = rand.nextInt(words.size());
        return words.get(randomIndex).toLowerCase();
    }

    private ArrayList<String> loadWords (String path) {
        ArrayList<String> words = new ArrayList<>();

        try {
            FileInputStream fs = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fs));
            String line;

            try {
                while ((line = br.readLine()) != null) {
                    words.add(line);
                }
            } catch (IOException ioex) {
                System.err.println("Something is wrong with words.txt");
            }
            return words;
        } catch (FileNotFoundException ex) {
            System.err.println("Could not read words.txt");
        }
        return words;
    }

    public static WordLibrary getInstance() {
        if(instance == null) {
            instance = new WordLibrary();
        }
        return instance;
    }
}
