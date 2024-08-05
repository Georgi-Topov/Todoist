package bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository;

import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskOperationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.TaskAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.TaskDoesNotExistException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.Task;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskRepository implements TaskRepositoryAPI {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(
                    new TypeToken<LocalDate>() { }.getType(), new LocalDateConverter()).create();
    private final Set<Task> unfinishedTasks;
    private final Set<Task> finishedTasks;

    public TaskRepository() {
        this.unfinishedTasks = new HashSet<>();
        this.finishedTasks = new HashSet<>();
    }

    public TaskRepository(Set<Task> unfinishedTasks, Set<Task> finishedTasks) {
        this.unfinishedTasks = unfinishedTasks;
        this.finishedTasks = finishedTasks;
    }

    @Override
    public void addTaskToTaskRepository(Task taskToAdd) throws IllegalTaskOperationException {
        if (this.unfinishedTasks.contains(taskToAdd)) {
            throw new TaskAlreadyExistsException("Cannot add a task which is already present\n");
        }
        this.unfinishedTasks.add(taskToAdd);
    }

    @Override
    public void updateTaskFromTaskRepository(Task taskToUpdate, Task updatedTask)
            throws IllegalTaskOperationException {

        if (!Objects.equals(taskToUpdate, updatedTask)) {
            throw new IllegalTaskOperationException("Cannot update a task with a task with a different name\n");
        }

        this.unfinishedTasks.remove(taskToUpdate);
        if (this.unfinishedTasks.contains(updatedTask)) {
            throw new TaskAlreadyExistsException("Cannot update a task with a task which is already present\n");
        }

        this.unfinishedTasks.add(updatedTask);
    }

    @Override
    public void deleteTaskFromTaskRepository(Task taskToDelete) {
        // made in such a way so that it is not needed to write checks
        // since tasks identify solely by their name there is no risk of undesired behavior
        this.finishedTasks.remove(taskToDelete);
        this.unfinishedTasks.remove(taskToDelete);
    }

    @Override
    public void finishTaskFromTaskRepository(Task taskToFinish) {
        if (this.unfinishedTasks.contains(taskToFinish)) {
            this.unfinishedTasks.remove(taskToFinish);
            this.finishedTasks.add(taskToFinish);
        }
    }

    @Override
    public String getTaskFromTaskRepository(Task taskToGet) throws TaskDoesNotExistException {
        if (this.unfinishedTasks.contains(taskToGet)) {
            Task taskToGetFromRepository = new HashSet<>(this.unfinishedTasks).stream()
                    .filter(task -> task.equals(taskToGet))
                    .collect(Collectors.toSet())
                    .iterator()
                    .next();

            return gson.toJson(taskToGetFromRepository);
        }
        throw new TaskDoesNotExistException("Cannot get a task which is not present\n");
    }

    // the first implementation considered absence of date as an indication that only inbox tasks are desired
    // later it was changed so that its absence means all tasks with the corresponding completion status
    // no matter what the date is
    @Override
    public String listTasksFromRepository(LocalDate date, boolean completed) {
        Set<Task> tasksWithDate = getTasksWithDate(completed);
        Set<Task> tasksWithTheGivenDate = tasksWithDate.stream()
                .filter(task -> task.getDate().equals(date))
                .collect(Collectors.toSet());

        Set<Task> tasks;
        if (completed) {
            tasks = new HashSet<>(this.finishedTasks);
        } else {
            tasks = new HashSet<>(this.unfinishedTasks);
        }

        if (date == null) {
            return gson.toJson(tasks);
        } else {
            return gson.toJson(tasksWithTheGivenDate);
        }
    }

    @Override
    public String listDashboard() {
        Set<Task> tasksDesiredToBeFinishedToday = this.unfinishedTasks.stream()
                .filter(task -> task.getDate() != null)
                .filter(task -> task.getDate().equals(LocalDate.now()))
                .collect(Collectors.toSet());

        return gson.toJson(tasksDesiredToBeFinishedToday);
    }

    private Set<Task> getTasksWithDate(boolean completed) {
        Set<Task> tasksWithDate = new HashSet<>();

        if (completed) {
            for (Task task : this.finishedTasks) {
                if (task.getDate() != null) {
                    tasksWithDate.add(task);
                }
            }
        } else {
            for (Task task : this.unfinishedTasks) {
                if (task.getDate() != null) {
                    tasksWithDate.add(task);
                }
            }
        }

        return tasksWithDate;
    }

    // because of problems with LocalDate serialization with gson
    // the following class implementation and the gson initialization were directly copied from StackOverflow
    // https://stackoverflow.com/questions/66716526/gson-does-not-correctly-serialize-localdate
    public static class LocalDateConverter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(DateTimeFormatter.ISO_LOCAL_DATE.format(src));
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return DateTimeFormatter.ISO_LOCAL_DATE.parse(json.getAsString(), LocalDate::from);
        }

    }

}