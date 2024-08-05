package bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository;

import bg.sofia.uni.fmi.mjt.todoist.server.task.Task;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.CollaborationAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.CollaborationDoesNotExistException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskOperationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.FailedAuthorizationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.IllegalCollaborationNameException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.IllegalTaskAssignmentException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.UserAlreadyInCollaborationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.UserNotInCollaborationException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserDoesNotExistException;

import java.util.Set;

public interface CollaborationFileRepositoryAPI {

    void addCollaboration(String username, String collaboration) throws CollaborationAlreadyExistsException,
            IllegalCollaborationNameException;

    void deleteCollaboration(String username, String collaboration) throws FailedAuthorizationException;

    void addUserToCollaboration(String username, String collaboration, String usernameToAdd)
            throws CollaborationAlreadyExistsException, UserDoesNotExistException, UserNotInCollaborationException,
            UserAlreadyInCollaborationException, CollaborationDoesNotExistException;

    void assignTaskToUser(String username, String collaboration, String usernameAssignee, Task taskToAssign)
            throws IllegalTaskAssignmentException, IllegalTaskDatesException, CollaborationAlreadyExistsException,
            CollaborationDoesNotExistException;

    void addTaskToCollaborationRepository(String username,
                                          String collaboration, Task taskToAdd) throws IllegalTaskOperationException,
            IllegalTaskDatesException, CollaborationAlreadyExistsException, UserNotInCollaborationException;

    void updateTaskFromCollaborationRepository(String username, String collaboration,
                                               Task taskToUpdate, Task updatedTask)
            throws IllegalTaskOperationException, IllegalTaskDatesException,
            CollaborationAlreadyExistsException, UserNotInCollaborationException, CollaborationDoesNotExistException;

    void deleteTaskFromCollaborationRepository(String username,
                                               String collaboration, Task taskToDelete)
            throws IllegalTaskDatesException, CollaborationAlreadyExistsException, UserNotInCollaborationException,
            CollaborationDoesNotExistException;

    void finishTaskFromCollaborationRepository(String username, String collaboration, Task taskToFinish)
            throws IllegalTaskDatesException, CollaborationAlreadyExistsException, UserNotInCollaborationException;

    Set<String> listCollaborations(String username);

    String listCollaborationTasks(String username, String collaboration) throws IllegalTaskDatesException,
            UserNotInCollaborationException;

    Set<String> listCollaborationUsers(String username, String collaboration);

}