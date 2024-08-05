package bg.sofia.uni.fmi.mjt.todoist.server.task;

import bg.sofia.uni.fmi.mjt.todoist.client.DateParser;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;

import java.time.LocalDate;
import java.util.Objects;

public abstract class Task {

    private static final String TASK_DELIMITER = ";";
    private static final String TASK_ATTRIBUTE_DELIMITER = "\"";

    private final String name;
    private final LocalDate date;
    private final LocalDate dueDate;
    private final String description;
    private String assignee;

    public static TaskBuilder builder(String name) {
        return new TaskBuilder(name);
    }

    public static TaskBuilder builder() {
        return new TaskBuilder(null);
    }

    public String getName() {
        return this.name;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public String getDescription() {
        return this.description;
    }

    public String getAssignee() {
        return this.assignee;
    }

    public Task setAssignee(String assignee) {
        this.assignee = assignee;

        return this.date == null ?
                new CollaborationInboxTask(this) : new CollaborationTaskWithDate(this);
    }

    private void validateTaskDates(LocalDate date, LocalDate dueDate) throws IllegalTaskDatesException {
        if (date != null && dueDate != null) {
            if (date.isAfter(dueDate)) {
                throw new IllegalTaskDatesException(
                        "Due date cannot be before the date preferred to finish the task\n"
                );
            }
        }

        if (date != null && date.isBefore(LocalDate.now())) {
            throw new IllegalTaskDatesException("Preferred date cannot be before today\n");
        }

        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new IllegalTaskDatesException("Due date cannot be before today\n");
        }
    }

    public abstract String print();

    Task(TaskBuilder taskBuilder) throws IllegalTaskDatesException {
        this.name = taskBuilder.name;
        this.date = taskBuilder.date;
        this.dueDate = taskBuilder.dueDate;
        this.description = taskBuilder.description;
        this.assignee = null;
        validateTaskDates(this.date, this.dueDate);
    }

    Task(Task task) {
        this.name = task.name;
        this.date = task.date;
        this.dueDate = task.dueDate;
        this.description = task.description;
        this.assignee = task.assignee;
    }

    public static class TaskBuilder {

        private final String name;
        private LocalDate date;
        private LocalDate dueDate;
        private String description;

        private TaskBuilder(String name) {
            this.name = name;
        }

        public TaskBuilder setDate(LocalDate date) {
            this.date = date;
            return this;
        }

        public TaskBuilder setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public TaskBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Task build() throws IllegalTaskDatesException {
            return this.date == null ? new InboxTask(this) : new TaskWithDate(this);
        }

        // used to construct task from a line of a file
        // correct input is guaranteed
        public Task build(String line) throws IllegalTaskDatesException {
            String[] tokens = line.split(TASK_DELIMITER);
            tokens[1] = tokens[1].substring(1);
            String[] taskAttributes = tokens[1].split(TASK_ATTRIBUTE_DELIMITER);

            final int maxTaskAttributeTokens = taskAttributes.length;
            final int firstOptionalAttribute = 3;
            final int secondOptionalAttribute = 5;
            final int thirdOptionalAttribute = 7;
            final int forthOptionalAttribute = 9;

            TaskBuilder taskBuilder = new TaskBuilder(taskAttributes[1]);
            for (int i = 1; i < maxTaskAttributeTokens; i += 2) {
                if (Objects.equals(taskAttributes[i], "null")) {
                    switch (i) {
                        case firstOptionalAttribute -> taskBuilder.setDate(null);
                        case secondOptionalAttribute -> taskBuilder.setDueDate(null);
                        case thirdOptionalAttribute -> taskBuilder.setDescription(null);
                        case forthOptionalAttribute -> {
                            return taskBuilder.build().setAssignee(null);
                        }
                    }
                } else {
                    switch (i) {
                        case firstOptionalAttribute -> taskBuilder.setDate(DateParser.parseDate(taskAttributes[i]));
                        case secondOptionalAttribute -> taskBuilder.setDueDate(DateParser.parseDate(taskAttributes[i]));
                        case thirdOptionalAttribute -> taskBuilder.setDescription(taskAttributes[i]);
                        case forthOptionalAttribute -> {
                            return taskBuilder.build().setAssignee(taskAttributes[i]);
                        }
                    }
                }
            }

            return taskBuilder.build();
        }

    }

}