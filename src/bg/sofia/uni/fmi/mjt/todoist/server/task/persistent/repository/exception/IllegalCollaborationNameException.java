package bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception;

public class IllegalCollaborationNameException extends Exception {

    public IllegalCollaborationNameException(String message) {
        super(message);
    }

    public IllegalCollaborationNameException(String message, Throwable cause) {
        super(message, cause);
    }

}