package bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception;

public class IllegalUsernameException extends Exception {

    public IllegalUsernameException(String message) {
        super(message);
    }

    public IllegalUsernameException(String message, Throwable cause) {
        super(message, cause);
    }

}