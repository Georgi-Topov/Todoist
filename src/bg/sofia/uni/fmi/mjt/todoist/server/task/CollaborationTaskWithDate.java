package bg.sofia.uni.fmi.mjt.todoist.server.task;

import bg.sofia.uni.fmi.mjt.todoist.client.DateParser;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;

public class CollaborationTaskWithDate extends TaskWithDate {

    public CollaborationTaskWithDate(TaskBuilder name) throws IllegalTaskDatesException {
        super(name);
    }

    public CollaborationTaskWithDate(Task task) {
        super(task);
    }

    @Override
    public String print() {
        return "CollaborationTaskWithDate[name=" + this.getName()
                + ", date=" + this.getDate()
                + ", dueDate=" + this.getDueDate()
                + ", description=" + this.getDescription()
                + ", assignee= " + this.getAssignee()
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
                + "\"\"" + this.getAssignee()
                + "\"";
    }

}