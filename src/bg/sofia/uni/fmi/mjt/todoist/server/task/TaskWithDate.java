package bg.sofia.uni.fmi.mjt.todoist.server.task;

import bg.sofia.uni.fmi.mjt.todoist.client.DateParser;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;

import java.util.Objects;

public class TaskWithDate extends Task {

    TaskWithDate(TaskBuilder name) throws IllegalTaskDatesException {
        super(name);
    }

    TaskWithDate(Task task) {
        super(task);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof TaskWithDate taskWithDate)) {
            return false;
        }

        return Objects.equals(this.getName(), taskWithDate.getName())
                && this.getDate().equals(taskWithDate.getDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getName(), this.getDate());
    }

    // can be used as a more human-readable way to show the task
    // the other way is used for the file format
    @Override
    public String print() {
        return "TaskWithDate[name=" + this.getName()
                + ", date=" + this.getDate()
                + ", dueDate=" + this.getDueDate()
                + ", description=" + this.getDescription()
                + "]";
    }

    // U stands for unfinished and F - finished
    // used when storing in the file - individual tasks.txt
    @Override
    public String toString() {
        return "\"" + this.getName()
                + "\"\"" + DateParser.dateValue(this.getDate())
                + "\"\"" + DateParser.dateValue(this.getDueDate())
                + "\"\"" + this.getDescription()
                + "\"";
    }

}