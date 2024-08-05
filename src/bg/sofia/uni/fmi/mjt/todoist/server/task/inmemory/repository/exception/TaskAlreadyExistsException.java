package bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception;

public class TaskAlreadyExistsException extends IllegalTaskOperationException {

    public TaskAlreadyExistsException(String message) {
        super(message);
    }

    public TaskAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}