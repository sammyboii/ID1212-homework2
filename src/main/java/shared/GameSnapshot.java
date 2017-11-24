package shared;

import java.io.Serializable;
import java.util.StringJoiner;


public class GameSnapshot {
    private final boolean firstRun;
    private final boolean finished;
    private final String message;
    private String[] progress;
    private final int attempts;
    private final int maxAttempts;
    private final int score;

    public GameSnapshot(boolean firstRun, String message, boolean finished, String[] progress, int attempts, int maxAttempts, int score) {
        this.firstRun = firstRun;
        this.message = message;
        this.finished = finished;
        this.progress = progress;
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.score = score;
    }

    public boolean isFirstRun() {
        return firstRun;
    }

    public boolean isFinished() {
        return attempts == maxAttempts;
    }

    public int getScore() {
        return score;
    }

    public boolean isWon() {
        for (String c : this.progress) {
            if (c == null) return false;
        }
        return true;
    }

    public String getProgress () { return formatProgress(); };
    public int getAttempts () { return attempts; };
    public int getMaxAttempts () { return maxAttempts; };
    public String getMessage () { return message; }

    private String formatProgress () {
        if (this.progress == null) return "";

        String formatted = "";
        for (int i = 0; i < this.progress.length; i++) {
            if (this.progress[i] != null)
                formatted += " " + this.progress[i];
            else
                formatted += " _";
        }
        return formatted;
    }

    private void delimit(StringBuilder sb) {
        sb.append(Constants.GAMESNAPSHOT_VALUE_DELIMITER);
    }

    private String delimit(String[] progress) {
        StringJoiner joiner = new StringJoiner(Constants.GAMESNAPSHOT_PROGRESS_CHARACTER_DELIMITER);
        for (String c : progress) {
            joiner.add(c);
        }
        return joiner.toString();
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(Constants.GAMESNAPSHOT_VALUE_DELIMITER);
        String[] values = {
                Boolean.toString(firstRun),
                message,
                Boolean.toString(finished),
                delimit(progress),
                Integer.toString(attempts),
                Integer.toString(maxAttempts),
                Integer.toString(score)
        };

        for (String value : values) {
            joiner.add(value);
        }
        return joiner.toString();
    }
}
