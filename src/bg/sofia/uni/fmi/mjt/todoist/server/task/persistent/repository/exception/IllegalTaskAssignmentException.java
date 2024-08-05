package bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception;

public class IllegalTaskAssignmentException extends Exception {

    public IllegalTaskAssignmentException(String message) {
        super(message);
    }

    public IllegalTaskAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }

}