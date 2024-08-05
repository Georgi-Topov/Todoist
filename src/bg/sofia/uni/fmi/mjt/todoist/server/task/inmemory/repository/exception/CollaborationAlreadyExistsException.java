package bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception;

public class CollaborationAlreadyExistsException extends Exception {

    public CollaborationAlreadyExistsException(String message) {
        super(message);
    }

    public CollaborationAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}