package bg.sofia.uni.fmi.mjt.todoist.server;

import bg.sofia.uni.fmi.mjt.todoist.server.file.FileCreator;
import bg.sofia.uni.fmi.mjt.todoist.server.task.Task;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.TaskRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.TaskRepositoryAPI;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LoadIndividualTasks {

    private static final String VALUE_DELIMITER = ";";
    private static final String USERNAME_DELIMITER = "\"";
    private static final char UNFINISHED_TASK = 'U';

    public static TaskRepositoryAPI loadIndividualTaskData(String username, Path fileToReadFrom)
            throws IllegalTaskDatesException {
        if (fileToReadFrom == null) {
            return new TaskRepository();
        }

        Set<Task> finishedTasks = new HashSet<>();
        Set<Task> unfinishedTasks = new HashSet<>();

        FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.readLock().lock();
        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(fileToReadFrom)))) {

            String line;
            while ((line = in.readLine()) != null) {
                Task readTask = Task.builder().build(line);
                String readUser = line.split(VALUE_DELIMITER)[0].replace(USERNAME_DELIMITER, "");

                if (Objects.equals(readUser, username)) {
                    if (line.split(VALUE_DELIMITER)[1].charAt(0) == UNFINISHED_TASK) {
                        unfinishedTasks.add(readTask);
                    } else {
                        finishedTasks.add(readTask);
                    }
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("An error occurred while reading from a file", e);
        } finally {
            FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.readLock().unlock();
        }

        return new TaskRepository(unfinishedTasks, finishedTasks);
    }

}