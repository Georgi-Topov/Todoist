package bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception;

public class FailedAuthorizationException extends Exception {

    public FailedAuthorizationException(String message) {
        super(message);
    }

    public FailedAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

}