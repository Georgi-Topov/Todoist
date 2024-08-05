package bg.sofia.uni.fmi.mjt.todoist.server.task;

import bg.sofia.uni.fmi.mjt.todoist.client.DateParser;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;

import java.util.Objects;

public class InboxTask extends Task {

    InboxTask(TaskBuilder name) throws IllegalTaskDatesException {
        super(name);
    }

    public InboxTask(Task task) {
        super(task);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof InboxTask inboxTask)) {
            return false;
        }

        return Objects.equals(this.getName(), inboxTask.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getName());
    }

    // can be used as a more human-readable way to show the task
    // the other way is used for the file format
    @Override
    public String print() {
        return "InboxTask[name=" + this.getName()
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