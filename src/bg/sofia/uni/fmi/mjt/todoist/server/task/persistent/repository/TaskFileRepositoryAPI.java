package bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository;

import bg.sofia.uni.fmi.mjt.todoist.server.task.Task;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskOperationException;

public interface TaskFileRepositoryAPI {

    /**
     * Persists the current task in the permanent individual tasks repository
     *
     * @param username the user performing the operation
     * @param taskToAdd the task to be added
     * @throws IllegalTaskOperationException if the task to be added already exists for the current user
     */
    void addTaskToTaskRepository(String username, Task taskToAdd) throws IllegalTaskOperationException;

    /**
     * Replaces the old task in the permanent individual tasks repository
     *
     * @param taskToUpdate the task to be modified
     * @param updatedTask the new modified version of the old task
     * @throws IllegalTaskOperationException if the new task has different name or the modification makes it such that
     * there is another task with the same name
     * @throws IllegalTaskDatesException if the date we want to finish the task or
     * the due date are before the current date or the due date is before the date we want to finish the task
     */
    void updateTaskFromTaskRepository(Task taskToUpdate, Task updatedTask)
            throws IllegalTaskOperationException, IllegalTaskDatesException;

    /**
     * Deletes the task from the permanent individual tasks repository
     * Even if the task does not exist, deletion count as successful
     *
     * @param taskToDelete the task to be deleted
     * @throws IllegalTaskDatesException if the date we want to finish the task or
     * the due date are before the current date or the due date is before the date we want to finish the task
     */
    void deleteTaskFromTaskRepository(Task taskToDelete) throws IllegalTaskDatesException;

    /**
     * Makes the task from the permanent individual tasks repository finished
     *
     * @param taskToFinish the task to be finished
     * @throws IllegalTaskDatesException if the date we want to finish the task or
     * the due date are before the current date or the due date is before the date we want to finish the task
     */
    void finishTaskFromTaskRepository(Task taskToFinish) throws IllegalTaskDatesException;

}