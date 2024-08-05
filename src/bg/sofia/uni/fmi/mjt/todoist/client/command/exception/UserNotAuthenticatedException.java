package bg.sofia.uni.fmi.mjt.todoist.client.command.exception;

public class UserNotAuthenticatedException extends Exception {

    public UserNotAuthenticatedException(String message) {
        super(message);
    }

    public UserNotAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }

}