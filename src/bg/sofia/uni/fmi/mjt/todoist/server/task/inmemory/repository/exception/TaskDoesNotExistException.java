package bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception;

public class TaskDoesNotExistException extends IllegalTaskOperationException {

    public TaskDoesNotExistException(String message) {
        super(message);
    }

    public TaskDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

}