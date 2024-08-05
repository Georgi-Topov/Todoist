package bg.sofia.uni.fmi.mjt.todoist.client.command.exception;

public class InvalidCommandException extends Exception {

    public InvalidCommandException(String message) {
        super(message);
    }

    public InvalidCommandException(String message, Throwable cause) {
        super(message, cause);
    }

}