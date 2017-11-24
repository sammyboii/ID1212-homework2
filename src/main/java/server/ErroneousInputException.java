package server;

public class ErroneousInputException extends Exception {
    private final String message;

    public ErroneousInputException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
