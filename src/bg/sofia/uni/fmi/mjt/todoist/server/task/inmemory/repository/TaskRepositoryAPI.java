package bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository;

import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskOperationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.TaskDoesNotExistException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.Task;

import java.time.LocalDate;

public interface TaskRepositoryAPI {

    /**
     * Adds the given task to the task repository of the currently logged user
     *
     * @param taskToAdd the task to be added
     * @throws IllegalTaskOperationException if the task to be added already exists for the current user
     * where two tasks are considered equal iff both tasks are inbox tasks
     * (do not have a date in which we want to finish them) and their names are equal or both tasks are not inbox and
     * have the same name and date in which we want to finish them
     */
    void addTaskToTaskRepository(Task taskToAdd) throws IllegalTaskOperationException;

    /**
     * Updates a task with another given task with the same name. So names can not be updated
     *
     * @param taskToUpdate the task to be modified
     * @param updatedTask the new modified version of the old task
     * @throws IllegalTaskOperationException if the new task has different name or the modification makes it such that
     * there is another task with the same name
     */
    void updateTaskFromTaskRepository(Task taskToUpdate, Task updatedTask)
            throws IllegalTaskOperationException;

    /**
     * Deletes a task from the task repository of the currently logged user
     * Even if the task does not exist, deletion count as successful
     *
     * @param taskToDelete the task to be deleted
     */
    void deleteTaskFromTaskRepository(Task taskToDelete);

    /**
     * Makes a task from the task repository of the currently logged user finished
     *
     * @param taskToFinish the task to be finished
     */
    void finishTaskFromTaskRepository(Task taskToFinish);

    /**
     * Returns a given task from the task repository of the currently logged user
     *
     * @param taskToGet the task to be returned
     * @throws TaskDoesNotExistException if the given task is not present in task repository of
     * the currently logged user
     */
    String getTaskFromTaskRepository(Task taskToGet) throws TaskDoesNotExistException;

    /**
     * Returns all tasks from the task repository of the currently logged user with the provided completion status
     * If there are no tasks, an empty collection is returned
     *
     * @param date optional attribute which is used to specify the date of the tasks to be returned
     * @param completed a flag used to indicate the completion status of the tasks to be returned
     */
    String listTasksFromRepository(LocalDate date, boolean completed);

    /**
     * Returns all tasks which are marked as desired to be finished today (the date attribute)
     */
    String listDashboard();

}