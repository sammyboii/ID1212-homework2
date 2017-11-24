package shared;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.StringJoiner;

public class Parser {
    private StringBuilder accumulator = new StringBuilder();
    private final Queue<String> contentQueue = new ArrayDeque<>();

    public static String extractFromBuffer (ByteBuffer buffer) {
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes);
    }

    public synchronized void add(String content) {
        accumulator.append(content);
        while (extractContent());
    }

    public synchronized String next() {
        return contentQueue.poll();
    }

    public synchronized boolean hasNext() {
        return !contentQueue.isEmpty();
    }

    public static String setLengthHeader(String content) {
        StringJoiner joiner = new StringJoiner(Constants.LENGTH_HEADER_DELIMITER);
        joiner.add(Integer.toString(content.length()));
        joiner.add(content);
        return joiner.toString();
    }

    public static ContentType typeOf (String content) {
        String[] parts = parts(content);
        String type = parts[Constants.CONTENT_TYPE_INDEX];
        ContentType value = ContentType.valueOf(type);
        return value;
    }

    public static String bodyOf (String content) {
        String[] parts = parts(content);
        return parts[Constants.CONTENT_BODY_INDEX];
    }

    public static boolean recognizesType (String content) {
        for (ContentType type : ContentType.values()) {
            if (typeOf(content) == type) {
                return true;
            }
        }
        return false;
    }

    private boolean extractContent () {
        String allContent = accumulator.toString();
        String[] headerSplit = allContent.split(Constants.LENGTH_HEADER_DELIMITER);
        if (headerSplit.length < 2) {
            return false;
        }
        int firstChunkLength = Integer.parseInt(headerSplit[0]);
        String content = headerSplit[1];
        if (content.length() >= firstChunkLength) {
            String completeContent = content.substring(0, firstChunkLength);
            contentQueue.add(completeContent);
            accumulator.delete(0, headerSplit[0].length() + Constants.LENGTH_HEADER_DELIMITER.length() + firstChunkLength);
            return true;
        }
        return false;
    }

    public static String getMessage (String content) {
        return content;
    }

    public static GameSnapshot gameSnapshot (String content) {
        String[] values = content.split(Constants.GAMESNAPSHOT_VALUE_DELIMITER);
        boolean firstRun = Boolean.getBoolean(values[Constants.GAMESNAPSHOT_FIRSTRUN_INDEX]);
        String message = replaceNullString(values[Constants.GAMESNAPSHOT_MESSAGE_INDEX]);
        boolean finished = Boolean.getBoolean(values[Constants.GAMESNAPSHOT_FINISHED_INDEX]);
        String[] progress = values[Constants.GAMESNAPSHOT_PROGRESS_INDEX].split(Constants.GAMESNAPSHOT_PROGRESS_CHARACTER_DELIMITER);
        int attempts = Integer.parseInt(values[Constants.GAMESNAPSHOT_ATTEMPTS_INDEX]);
        int maxAttempts = Integer.parseInt(values[Constants.GAMESNAPSHOT_MAXATTEMPTS_INDEX]);
        int score = Integer.parseInt(values[Constants.GAMESNAPSHOT_SCORE_INDEX]);
        replaceNullStringArray(progress);

        return new GameSnapshot(firstRun, message, finished, progress, attempts, maxAttempts, score);
    }

    private static void replaceNullStringArray (String[] progress) {
        for (int i=0; i < progress.length; i++) {
            progress[i] = replaceNullString(progress[i]);
        }
    }

    private static String replaceNullString(String str) {
        if (str.equals("null")) {
            return null;
        }
        return str;
    }

    private static String[] parts(String content) {
        return content.split(Constants.CONTENT_TYPE_DELIMITER);
    }

    public enum ContentType {
        CONNECTED,
        STARTED,
        INFO,
        SNAPSHOT
    }
}
