package bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository;

import bg.sofia.uni.fmi.mjt.todoist.server.file.FileCreator;
import bg.sofia.uni.fmi.mjt.todoist.server.task.Task;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskOperationException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class TaskFileRepository implements TaskFileRepositoryAPI {

    private static final String UNFINISHED_TASK = "U";
    private static final String FINISHED_TASK = "F";
    private static final String ATTRIBUTE_DELIMITER = ";";
    private static final String TASK_VALUE_DELIMITER = "\"";

    public static final Path INDIVIDUAL_TASKS_PATH;

    static {
        INDIVIDUAL_TASKS_PATH = FileCreator.createFIle(Path.of("individual tasks.txt"));
    }

    // since the following add, update, delete and finish implementations are meant to be invoked
    // only after the corresponding operations are invoked in TaskRepository
    // it is guaranteed that they are correct even without explicit check for the presence/absence of a given task
    // this is done for performance purposes

    @Override
    public void addTaskToTaskRepository(String username, Task taskToAdd) {
        FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.writeLock().lock();
        try (PrintWriter out = new PrintWriter(new FileWriter(
                String.valueOf(INDIVIDUAL_TASKS_PATH), true), true)) {

            out.println(TASK_VALUE_DELIMITER + username + TASK_VALUE_DELIMITER
                    + ATTRIBUTE_DELIMITER + UNFINISHED_TASK + taskToAdd);

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    @Override
    public void updateTaskFromTaskRepository(Task taskToUpdate, Task updatedTask)
            throws IllegalTaskOperationException, IllegalTaskDatesException {
        if (!Objects.equals(taskToUpdate.getName(), updatedTask.getName())) {
            throw new IllegalTaskOperationException("Cannot update a task with a task with a different name\n");
        }

        FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.writeLock().lock();
        Path tempPath = FileCreator.createFIle(Path.of("tmp.txt"));

        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(INDIVIDUAL_TASKS_PATH)));
             PrintWriter out = new PrintWriter(new FileWriter(String.valueOf(tempPath)), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                Task readTask = Task.builder().build(line);
                if (readTask.equals(taskToUpdate)) {
                    out.println(line.split(ATTRIBUTE_DELIMITER)[0] + ATTRIBUTE_DELIMITER
                            + preserveTaskCompletionStatus(line) + updatedTask);
                    continue;
                }

                out.println(line);
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.writeLock().unlock();
        }

        moveTemporaryFileIntoExistingFile(tempPath);
    }

    @Override
    public void deleteTaskFromTaskRepository(Task taskToDelete) throws IllegalTaskDatesException {
        FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.writeLock().lock();
        Path tempPath = FileCreator.createFIle(Path.of("tmp.txt"));

        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(INDIVIDUAL_TASKS_PATH)));
             PrintWriter out = new PrintWriter(new FileWriter(String.valueOf(tempPath)), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                Task readTask = Task.builder().build(line);
                if (readTask.equals(taskToDelete)) {
                    continue;
                }

                out.println(line);
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.writeLock().unlock();
        }

        moveTemporaryFileIntoExistingFile(tempPath);
    }

    @Override
    public void finishTaskFromTaskRepository(Task taskToFinish) throws IllegalTaskDatesException {
        FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.writeLock().lock();
        Path tempPath = FileCreator.createFIle(Path.of("tmp.txt"));

        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(INDIVIDUAL_TASKS_PATH)));
             PrintWriter out = new PrintWriter(new FileWriter(String.valueOf(tempPath)), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                Task readTask = Task.builder().build(line);

                if (readTask.equals(taskToFinish)) {
                    out.println(line.split(ATTRIBUTE_DELIMITER)[0] + ATTRIBUTE_DELIMITER
                            + FINISHED_TASK + readTask);
                    continue;
                }

                out.println(line);
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.INDIVIDUAL_TASKS_READ_WRITE_LOCK.writeLock().unlock();
        }

        moveTemporaryFileIntoExistingFile(tempPath);
    }

    private String preserveTaskCompletionStatus(String line) {
        String prefix = line.split(ATTRIBUTE_DELIMITER)[1].split(TASK_VALUE_DELIMITER)[0];

        if (prefix.endsWith(UNFINISHED_TASK)) {
            return UNFINISHED_TASK;
        } else {
            return FINISHED_TASK;
        }
    }

    private void moveTemporaryFileIntoExistingFile(Path tempPath) {
        try {
            Files.move(tempPath, INDIVIDUAL_TASKS_PATH, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        }
    }

}