package bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception;

public class IllegalTaskDatesException extends Exception {

    public IllegalTaskDatesException(String message) {
        super(message);
    }

    public IllegalTaskDatesException(String message, Throwable cause) {
        super(message, cause);
    }

}