package bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository;

import bg.sofia.uni.fmi.mjt.todoist.server.file.FileCreator;
import bg.sofia.uni.fmi.mjt.todoist.server.task.Task;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.TaskRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.CollaborationAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.CollaborationDoesNotExistException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.TaskAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.FailedAuthorizationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.IllegalCollaborationNameException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.IllegalTaskAssignmentException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.UserAlreadyInCollaborationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.UserNotInCollaborationException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.UserRegistrationFileRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserDoesNotExistException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class CollaborationFileRepository implements CollaborationFileRepositoryAPI {

    private static final String USER_ATTRIBUTE_DELIMITER = ";";
    private static final String ILLEGAL_USERNAME_SYMBOL1 = "\"";
    private static final String ILLEGAL_USERNAME_SYMBOL2 = "/";
    private static final String ILLEGAL_USERNAME_SYMBOL3 = "\\";
    private static final String UNFINISHED_TASK = "U";
    private static final String FINISHED_TASK = "F";
    private static final String ATTRIBUTE_DELIMITER = ";";
    private static final String TASK_VALUE_DELIMITER = "\"";
    private static final Path COLLABORATIONS_PATH;
    private static final Path COLLABORATION_REGULAR_USERS_PATH;
    private static final Path COLLABORATION_TASKS_PATH;

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(
                    new TypeToken<LocalDate>() { }.getType(), new TaskRepository.LocalDateConverter()).create();


    static {
        COLLABORATIONS_PATH = FileCreator.createFIle(Path.of("collaborations.txt"));
        COLLABORATION_REGULAR_USERS_PATH = FileCreator.createFIle(Path.of("collaboration regular users.txt"));
        COLLABORATION_TASKS_PATH = FileCreator.createFIle(Path.of("collaboration tasks.txt"));
    }

    private void validateCollaborationName(String collaboration) throws IllegalCollaborationNameException {
        if (collaboration.contains(ILLEGAL_USERNAME_SYMBOL1) || collaboration.contains(ILLEGAL_USERNAME_SYMBOL2)
                || collaboration.contains(ILLEGAL_USERNAME_SYMBOL3)) {
            throw new IllegalCollaborationNameException("Collaborations cannot contain " +
                    "the following symbols: \\, \" and /\n");
        }
    }

    @Override
    public void addCollaboration(String username, String collaboration) throws IllegalCollaborationNameException,
            CollaborationAlreadyExistsException {
        validateCollaborationName(collaboration);

        if (checkCollaborationExistence(collaboration)) {
            throw new CollaborationAlreadyExistsException("Collaboration with the given name already exists! " +
                    "Use another name for your collaboration\n");
        }

        FileCreator.COLLABORATIONS_READ_WRITE_LOCK.writeLock().lock();
        try (PrintWriter out = new PrintWriter(new FileWriter(
                String.valueOf(COLLABORATIONS_PATH), true), true)) {

            out.println(TASK_VALUE_DELIMITER + collaboration + TASK_VALUE_DELIMITER
                    + ATTRIBUTE_DELIMITER + username);

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.COLLABORATIONS_READ_WRITE_LOCK.writeLock().unlock();
        }

        addCollaborationUser(collaboration, username);
    }

    @Override
    public void deleteCollaboration(String username, String collaboration) throws FailedAuthorizationException {
        if (!removeCollaborationInformationFromFile(username, collaboration)) {
            throw new FailedAuthorizationException("Not authorized to delete collaboration "
                    + collaboration + ". Only the owner can delete it\n");
        }

        removeCollaborationInformationFromFile(collaboration, COLLABORATION_REGULAR_USERS_PATH);
        removeCollaborationInformationFromFile(collaboration, COLLABORATION_TASKS_PATH);
    }

    @Override
    public void addUserToCollaboration(String username, String collaboration, String usernameToAdd)
            throws UserDoesNotExistException, UserNotInCollaborationException, UserAlreadyInCollaborationException,
            CollaborationDoesNotExistException {

        if (!checkCollaborationExistence(collaboration)) {
            throw new CollaborationDoesNotExistException("There is no collaboration with the given name! " +
                    "First, create the collaboration! Type \"help\" to see the manual\n");
        }

        UserRegistrationFileRepository.checkUserRegistration(usernameToAdd);

        if (!checkUserPresenceInCollaboration(username, collaboration)) {
            throw new UserNotInCollaborationException("You need to be a member of the collaboration " +
                    "to add other members\n");
        }

        if (checkUserPresenceInCollaboration(usernameToAdd, collaboration)) {
            throw new UserAlreadyInCollaborationException("Given user is already a member of the collaboration\n");
        }

        if (checkUserPresenceInCollaboration(username, collaboration)
                && !checkUserPresenceInCollaboration(usernameToAdd, collaboration)) {
            addCollaborationUser(collaboration, usernameToAdd);
        }
    }

    @Override
    public void assignTaskToUser(String username, String collaboration, String usernameAssignee, Task taskToAssign)
            throws IllegalTaskDatesException, IllegalTaskAssignmentException, CollaborationDoesNotExistException {

        if (!checkCollaborationExistence(collaboration)) {
            throw new CollaborationDoesNotExistException("There is no collaboration with the given name! " +
                    "First, create the collaboration! Type \"help\" to see the manual\n");
        }

        if (checkUserPresenceInCollaboration(username, collaboration)
                && checkUserPresenceInCollaboration(usernameAssignee, collaboration)) {

            FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().lock();
            Path tempPath = FileCreator.createFIle(Path.of("tmp.txt"));

            try (BufferedReader in = new BufferedReader(
                    new FileReader(String.valueOf(COLLABORATION_TASKS_PATH)));
                 PrintWriter out = new PrintWriter(new FileWriter(String.valueOf(tempPath)), true)) {

                String line;
                while ((line = in.readLine()) != null) {
                    String currentCollaboration = line.split(ATTRIBUTE_DELIMITER)[0]
                            .replaceAll(TASK_VALUE_DELIMITER, "");

                    Task readTask = Task.builder().build(line);

                    if (Objects.equals(collaboration, currentCollaboration) && taskToAssign.equals(readTask)) {
                        Task taskWithSetAssignee = readTask.setAssignee(usernameAssignee);

                        out.println(TASK_VALUE_DELIMITER + collaboration + TASK_VALUE_DELIMITER +
                                ATTRIBUTE_DELIMITER + preserveTaskCompletionStatus(line) + taskWithSetAssignee);
                        continue;
                    }

                    out.println(line);
                }

            } catch (IOException e) {
                throw new UncheckedIOException("A problem occurred while writing to a file", e);
            } finally {
                FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().unlock();
            }

            moveTemporaryFileIntoExistingFile(tempPath, COLLABORATION_TASKS_PATH);
        } else {
            throw new IllegalTaskAssignmentException("Both the user who assigns the task and the assignee " +
                    "must be part of the collaboration\n");
        }
    }

    @Override
    public void addTaskToCollaborationRepository(String username, String collaboration, Task taskToAdd)
            throws IllegalTaskDatesException, UserNotInCollaborationException, TaskAlreadyExistsException {
        checkCollaborationExistence(collaboration);

        if (checkUserPresenceInCollaboration(username, collaboration)) {
            FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().lock();
            try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(COLLABORATION_TASKS_PATH)));
                 PrintWriter out = new PrintWriter(new FileWriter(
                         String.valueOf(COLLABORATION_TASKS_PATH), true), true)) {

                String line;
                while ((line = in.readLine()) != null) {
                    String readCollaboration = line.split(USER_ATTRIBUTE_DELIMITER)[0]
                            .replace(ILLEGAL_USERNAME_SYMBOL1, "");
                    Task readTask = Task.builder().build(line);
                    if (readTask.equals(taskToAdd) && Objects.equals(readCollaboration, collaboration)) {
                        throw new TaskAlreadyExistsException("Provided task already exists\n");
                    }
                }

                out.println(TASK_VALUE_DELIMITER + collaboration + TASK_VALUE_DELIMITER
                        + ATTRIBUTE_DELIMITER + UNFINISHED_TASK + taskToAdd);

            } catch (IOException e) {
                throw new UncheckedIOException("A problem occurred while writing to a file", e);
            } finally {
                FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().unlock();
            }
        } else {
            throw new UserNotInCollaborationException("To add a task to a collaboration " +
                    "you need to be a member of the collaboration\n");
        }

    }

    @Override
    public void updateTaskFromCollaborationRepository(String username, String collaboration, Task taskToUpdate,
                                                      Task updatedTask) throws IllegalTaskDatesException,
            UserNotInCollaborationException, CollaborationDoesNotExistException {

        if (!checkCollaborationExistence(collaboration)) {
            throw new CollaborationDoesNotExistException("There is no collaboration with the given name! " +
                    "First, create the collaboration! Type \"help\" to see the manual\n");
        }

        if (checkUserPresenceInCollaboration(username, collaboration)) {

            FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().lock();
            Path tempPath = FileCreator.createFIle(Path.of("tmp.txt"));
            try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(COLLABORATION_TASKS_PATH)));
                 PrintWriter out = new PrintWriter(new FileWriter(String.valueOf(tempPath)), true)) {

                String line;
                while ((line = in.readLine()) != null) {
                    Task readTask = Task.builder().build(line);

                    String assignee;
                    if (taskToUpdate.equals(readTask)) {

                        if (readTask.getAssignee() != null) {
                            assignee = readTask.getAssignee();

                            out.println(line.split(ATTRIBUTE_DELIMITER)[0] + ATTRIBUTE_DELIMITER
                                    + preserveTaskCompletionStatus(line) + updatedTask
                                    + assignee + ILLEGAL_USERNAME_SYMBOL1);
                        } else {
                            out.println(line.split(ATTRIBUTE_DELIMITER)[0] + ATTRIBUTE_DELIMITER
                                    + preserveTaskCompletionStatus(line) + updatedTask);
                        }
                        continue;

                    }

                    out.println(line);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("A problem occurred while writing to a file", e);
            } finally {
                FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().unlock();
            }

            moveTemporaryFileIntoExistingFile(tempPath, COLLABORATION_TASKS_PATH);
        } else {
            throw new UserNotInCollaborationException("To update a task in a collaboration " +
                    "you need to be a member of the collaboration\n");
        }
    }

    @Override
    public void deleteTaskFromCollaborationRepository(String username, String collaboration, Task taskToDelete)
            throws IllegalTaskDatesException, UserNotInCollaborationException, CollaborationDoesNotExistException {

        if (!checkCollaborationExistence(collaboration)) {
            throw new CollaborationDoesNotExistException("There is no collaboration with the given name! " +
                    "First, create the collaboration! Type \"help\" to see the manual\n");
        }

        if (checkUserPresenceInCollaboration(username, collaboration)) {

            FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().lock();
            Path tempPath = FileCreator.createFIle(Path.of("tmp.txt"));
            try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(COLLABORATION_TASKS_PATH)));
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
                FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().unlock();
            }

            moveTemporaryFileIntoExistingFile(tempPath, COLLABORATION_TASKS_PATH);
        } else {
            throw new UserNotInCollaborationException("To delete a task from a collaboration " +
                    "you need to be a member of it\n");
        }
    }

    @Override
    public void finishTaskFromCollaborationRepository(String username, String collaboration, Task taskToFinish)
            throws IllegalTaskDatesException, UserNotInCollaborationException {
        checkCollaborationExistence(collaboration);

        if (checkUserPresenceInCollaboration(username, collaboration)) {

            FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().lock();
            Path tempPath = FileCreator.createFIle(Path.of("tmp.txt"));
            try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(COLLABORATION_TASKS_PATH)));
                 PrintWriter out = new PrintWriter(new FileWriter(String.valueOf(tempPath)), true)) {

                String line;
                while ((line = in.readLine()) != null) {
                    Task readTask = Task.builder().build(line);
                    if (readTask.equals(taskToFinish)) {
                        String assignee;

                        if (readTask.getAssignee() != null) {
                            assignee = readTask.getAssignee();
                            out.println(line.split(ATTRIBUTE_DELIMITER)[0] + ATTRIBUTE_DELIMITER
                                    + FINISHED_TASK + taskToFinish
                                    + assignee + ILLEGAL_USERNAME_SYMBOL1);
                        } else {
                            out.println(line.split(ATTRIBUTE_DELIMITER)[0] + ATTRIBUTE_DELIMITER
                                    + FINISHED_TASK + taskToFinish);
                        }

                        continue;
                    }

                    out.println(line);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("A problem occurred while writing to a file", e);
            } finally {
                FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().unlock();
            }

            moveTemporaryFileIntoExistingFile(tempPath, COLLABORATION_TASKS_PATH);
        } else {
            throw new UserNotInCollaborationException("To mark a task as finished " +
                    "you need to be a member of the collaboration\n");
        }
    }

    @Override
    public Set<String> listCollaborations(String username) {
        Set<String> collaborations = new HashSet<>();

        FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.readLock().lock();
        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(COLLABORATION_REGULAR_USERS_PATH)))) {

            String line;
            while ((line = in.readLine()) != null) {
                String currentCollaboration = line.split(ATTRIBUTE_DELIMITER)[0]
                        .replaceAll(TASK_VALUE_DELIMITER, "");
                String user = line.split(ATTRIBUTE_DELIMITER)[1];

                if (Objects.equals(username, user)) {
                    collaborations.add(currentCollaboration);
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.readLock().unlock();
        }

        return collaborations;
    }

    @Override
    public String listCollaborationTasks(String username, String collaboration) throws IllegalTaskDatesException,
            UserNotInCollaborationException {
        Set<Task> collaborationTasks = new HashSet<>();

        if (checkUserPresenceInCollaboration(username, collaboration)) {

            FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.readLock().lock();
            try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(COLLABORATION_TASKS_PATH)))) {

                String line;
                while ((line = in.readLine()) != null) {
                    String currentCollaboration = line.split(ATTRIBUTE_DELIMITER)[0]
                            .replaceAll(TASK_VALUE_DELIMITER, "");
                    Task currentTask = Task.builder().build(line);

                    if (Objects.equals(currentCollaboration, collaboration)) {
                        collaborationTasks.add(currentTask);
                    }
                }

            } catch (IOException e) {
                throw new UncheckedIOException("A problem occurred while writing to a file", e);
            } finally {
                FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.readLock().unlock();
            }

            return gson.toJson(collaborationTasks);
        } else {
            throw new UserNotInCollaborationException("To see the tasks in a collaboration " +
                    "you need to be a member of it\n");
        }
    }

    @Override
    public Set<String> listCollaborationUsers(String username, String collaboration) {
        Set<String> collaborationUsers = new HashSet<>();

        FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.readLock().lock();
        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(COLLABORATION_REGULAR_USERS_PATH)))) {

            String line;
            while ((line = in.readLine()) != null) {
                String currentCollaboration = line.split(ATTRIBUTE_DELIMITER)[0]
                        .replaceAll(TASK_VALUE_DELIMITER, "");
                String user = line.split(ATTRIBUTE_DELIMITER)[1];

                if (Objects.equals(collaboration, currentCollaboration)) {
                    collaborationUsers.add(user);
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.readLock().unlock();
        }

        // on average more optimal than always checking user's presence in the given collaboration
        if (!collaborationUsers.contains(username)) {
            return new HashSet<>();
        }

        return collaborationUsers;
    }

    private boolean removeCollaborationInformationFromFile(String username, String collaborationToDelete) {
        boolean isCollaborationDeleted = false;
        Path tempPath = FileCreator.createFIle(Path.of("tmp.txt"));

        FileCreator.COLLABORATIONS_READ_WRITE_LOCK.writeLock().lock();
        try (BufferedReader in = new BufferedReader(new FileReader(
                String.valueOf(CollaborationFileRepository.COLLABORATIONS_PATH)));
             PrintWriter out = new PrintWriter(new FileWriter(String.valueOf(tempPath)), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String currentCollaboration = line.split(ATTRIBUTE_DELIMITER)[0]
                        .replaceAll(TASK_VALUE_DELIMITER, "");

                String userOwner = line.split(ATTRIBUTE_DELIMITER)[1];

                if (Objects.equals(username, userOwner)
                        && Objects.equals(collaborationToDelete, currentCollaboration)) {
                    isCollaborationDeleted = true;
                    continue;
                }

                out.println(line);
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.COLLABORATIONS_READ_WRITE_LOCK.writeLock().unlock();
        }

        moveTemporaryFileIntoExistingFile(tempPath, CollaborationFileRepository.COLLABORATIONS_PATH);
        return isCollaborationDeleted;
    }

    private void removeCollaborationInformationFromFile(String collaborationToDelete,
                                                        Path affectedFile) {
        boolean isForTasks = false;
        if (affectedFile.equals(COLLABORATION_TASKS_PATH)) {
            isForTasks = true;
            FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().lock();
        } else {
            FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.writeLock().lock();
        }

        Path tempPath = FileCreator.createFIle(Path.of("tmp.txt"));

        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(affectedFile)));
             PrintWriter out = new PrintWriter(new FileWriter(String.valueOf(tempPath)), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String currentCollaboration = line.split(ATTRIBUTE_DELIMITER)[0]
                        .replaceAll(TASK_VALUE_DELIMITER, "");

                if (Objects.equals(collaborationToDelete, currentCollaboration)) {
                    continue;
                }

                out.println(line);
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            if (isForTasks) {
                FileCreator.COLLABORATION_TASKS_READ_WRITE_LOCK.writeLock().unlock();
            } else {
                FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.writeLock().unlock();
            }
        }

        moveTemporaryFileIntoExistingFile(tempPath, affectedFile);
    }

    private void addCollaborationUser(String collaboration, String usernameToAdd) {
        FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.writeLock().lock();
        try (PrintWriter out = new PrintWriter(new FileWriter(
                String.valueOf(CollaborationFileRepository.COLLABORATION_REGULAR_USERS_PATH), true), true)) {

            out.println(TASK_VALUE_DELIMITER + collaboration + TASK_VALUE_DELIMITER
                    + ATTRIBUTE_DELIMITER + usernameToAdd);

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    private boolean checkUserPresenceInCollaboration(String username, String collaboration) {
        FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.readLock().lock();
        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(COLLABORATION_REGULAR_USERS_PATH)))) {

            String line;
            while ((line = in.readLine()) != null) {
                String currentCollaboration = line.split(ATTRIBUTE_DELIMITER)[0]
                        .replaceAll(TASK_VALUE_DELIMITER, "");
                String currentUser = line.split(ATTRIBUTE_DELIMITER)[1];

                if (Objects.equals(collaboration, currentCollaboration) && Objects.equals(username, currentUser)) {
                    return true;
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.COLLABORATION_USERS_READ_WRITE_LOCK.readLock().unlock();
        }

        return false;
    }

    private boolean checkCollaborationExistence(String collaboration) {
        FileCreator.COLLABORATIONS_READ_WRITE_LOCK.readLock().lock();
        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(COLLABORATIONS_PATH)))) {
            String line;

            while ((line = in.readLine()) != null) {
                String currentCollaboration = line.split(ATTRIBUTE_DELIMITER)[0]
                        .replaceAll(TASK_VALUE_DELIMITER, "");

                if (Objects.equals(collaboration, currentCollaboration)) {
                    return true;
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            FileCreator.COLLABORATIONS_READ_WRITE_LOCK.readLock().unlock();
        }

        return false;
    }

    private String preserveTaskCompletionStatus(String line) {
        String prefix = line.split(ATTRIBUTE_DELIMITER)[1].split(TASK_VALUE_DELIMITER)[0];

        if (prefix.endsWith(UNFINISHED_TASK)) {
            return UNFINISHED_TASK;
        } else {
            return FINISHED_TASK;
        }
    }

    private void moveTemporaryFileIntoExistingFile(Path src, Path dst) {
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        }
    }

}