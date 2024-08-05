package bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception;

public class UserNotInCollaborationException extends Exception {

    public UserNotInCollaborationException(String message) {
        super(message);
    }

    public UserNotInCollaborationException(String message, Throwable cause) {
        super(message, cause);
    }

}