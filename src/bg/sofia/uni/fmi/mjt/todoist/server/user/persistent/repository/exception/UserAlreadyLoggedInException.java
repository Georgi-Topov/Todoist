package bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception;

public class UserAlreadyLoggedInException extends Exception {

    public UserAlreadyLoggedInException(String message) {
        super(message);
    }

    public UserAlreadyLoggedInException(String message, Throwable cause) {
        super(message, cause);
    }

}