package bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception;

public class IllegalTaskOperationException extends Exception {

    public IllegalTaskOperationException(String message) {
        super(message);
    }

    public IllegalTaskOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}