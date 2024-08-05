package bg.sofia.uni.fmi.mjt.todoist.client.command;

import bg.sofia.uni.fmi.mjt.todoist.client.DateParser;
import bg.sofia.uni.fmi.mjt.todoist.client.command.exception.InvalidCommandException;
import bg.sofia.uni.fmi.mjt.todoist.client.command.exception.UserNotAuthenticatedException;
import bg.sofia.uni.fmi.mjt.todoist.server.LoadIndividualTasks;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.TaskRepositoryAPI;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.CollaborationAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.CollaborationDoesNotExistException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskDatesException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.IllegalTaskOperationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.inmemory.repository.exception.TaskDoesNotExistException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.Task;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.CollaborationFileRepositoryAPI;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.TaskFileRepositoryAPI;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.FailedAuthorizationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.IllegalCollaborationNameException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.IllegalTaskAssignmentException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.UserAlreadyInCollaborationException;
import bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.exception.UserNotInCollaborationException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.UserRegistrationRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.UserRegistrationFileRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.UserLoginRepository;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.IllegalUsernameException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserAlreadyLoggedInException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserDoesNotExistException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static bg.sofia.uni.fmi.mjt.todoist.server.task.persistent.repository.TaskFileRepository.INDIVIDUAL_TASKS_PATH;

public class CommandExecutor {

    private static final int COMMAND_ARGUMENTS_MAX_COUNT = 4;
    private static final String COMMAND_ARGUMENT_DELIMITER = "=";
    private static final String SYMBOL_TO_REPLACE_FOR_COMMAND_TYPE = "-";
    private static final String REPLACEMENT_SYMBOL_FOR_COMMAND_TYPE = "_";

    private String loggedUser = null;
    private final UserLoginRepository userLoginRepository;
    private TaskRepositoryAPI individualTasks;
    private final TaskFileRepositoryAPI taskFileRepository;
    private final CollaborationFileRepositoryAPI collaborationFileRepository;

    public CommandExecutor(UserLoginRepository userLoginRepository, TaskRepositoryAPI individualTasks,
                           TaskFileRepositoryAPI taskFileRepository,
                           CollaborationFileRepositoryAPI collaborationFileRepository) {
        this.userLoginRepository = userLoginRepository;
        this.individualTasks = individualTasks;
        this.taskFileRepository = taskFileRepository;
        this.collaborationFileRepository = collaborationFileRepository;
    }

    public String execute(Command command)
            throws UserAlreadyExistsException, UserAlreadyLoggedInException, UserDoesNotExistException,
            IllegalTaskOperationException, FailedAuthorizationException,
            IllegalTaskAssignmentException, UserNotAuthenticatedException, InvalidCommandException,
            IllegalTaskDatesException, IllegalCollaborationNameException, IllegalUsernameException,
            CollaborationAlreadyExistsException, UserNotInCollaborationException, UserAlreadyInCollaborationException,
            CollaborationDoesNotExistException {

        validateCommand(command);

        CommandType commandType = CommandType.valueOf(command.command()
                .toUpperCase()
                .replace(SYMBOL_TO_REPLACE_FOR_COMMAND_TYPE, REPLACEMENT_SYMBOL_FOR_COMMAND_TYPE));

        return switch (commandType) {
            case CommandType.REGISTER -> register(command.arguments());
            case CommandType.LOGIN -> login(command.arguments());
            case CommandType.LOGOUT -> logout(command.arguments());
            case CommandType.ADD_TASK -> addTask(command.arguments());
            case CommandType.UPDATE_TASK -> updateTask(command.arguments());
            case CommandType.DELETE_TASK -> deleteTask(command.arguments());
            case CommandType.GET_TASK -> getTask(command.arguments());
            case CommandType.FINISH_TASK -> finishTask(command.arguments());
            case CommandType.LIST_TASKS -> listTasks(command.arguments());
            case CommandType.LIST_DASHBOARD -> listDashboard(command.arguments());
            case CommandType.ADD_COLLABORATION -> addCollaboration(command.arguments());
            case CommandType.DELETE_COLLABORATION -> deleteCollaboration(command.arguments());
            case CommandType.LIST_COLLABORATIONS -> listCollaborations(command.arguments());
            case CommandType.ADD_USER -> addUser(command.arguments());
            case CommandType.ASSIGN_TASK -> assignTask(command.arguments());
            case CommandType.LIST_COLLABORATION_TASKS -> listCollaborationTasks(command.arguments());
            case CommandType.LIST_COLLABORATION_USERS -> listCollaborationUsers(command.arguments());
            case CommandType.ADD_COLLABORATION_TASK -> addCollaborationTask(command.arguments());
            case CommandType.UPDATE_COLLABORATION_TASK -> updateCollaborationTask(command.arguments());
            case CommandType.DELETE_COLLABORATION_TASK -> deleteCollaborationTask(command.arguments());
            case CommandType.FINISH_COLLABORATION_TASK -> finishCollaborationTask(command.arguments());
            case CommandType.HELP -> printHelpMenu();
        };
    }

    private String printHelpMenu() {
        return """
                add-task name="<string>" {date="<dd/mm/yyyy>" due-date="<dd/mm/yyyy>" description="<string>"}
                update-task name="<string>" {date="<dd/mm/yyyy>"}
                            name="<string>" {date="<dd/mm/yyyy>" due-date="<dd/mm/yyyy>" description="<string>"}
                delete-task name="<string>" {date="<dd/mm/yyyy>"}
                get-task name="<string>" {date="<dd/mm/yyyy>"}
                finish-task name="<string>" {date="<dd/mm/yyyy>"}
                list-tasks completed="true"||"false" {date="<dd/mm/yyyy>"}
                list-dashboard
                
                add-collaboration collaboration="<string>"
                delete-collaboration collaboration="<string>"
                list-collaborations
                add-user collaboration="<string>" username="<string>"
                assign-task collaboration="<string>" username="<string>" name="<string>" {date="<dd/mm/yyyy>"}
                list-collaboration-tasks collaboration="<string>"
                list-collaboration-users collaboration="<string>"
                add-collaboration-task collaboration="<string>"
                      name="<string>" {date="<dd/mm/yyyy>" due-date="<dd/mm/yyyy>" description="<string>"}
                update-collaboration-task collaboration="<string>" name="<string>" {date="<dd/mm/yyyy>"}
                                   name="<string>" {date="<dd/mm/yyyy>" due-date="<dd/mm/yyyy>" description="<string>"}
                delete-collaboration-task collaboration="<string>" name="<string>" {date="<dd/mm/yyyy>"}
                finish-collaboration-task collaboration="<string>" name="<string>" {date="<dd/mm/yyyy>"}
                
                register username="<string>" password="<string>"
                login username="<string>" password="<string>"
                logout
                help
                """;
    }

    private String register(String[] args) throws UserAlreadyExistsException, InvalidCommandException,
            IllegalUsernameException {
        validateUserArgumentsSetUp(args);
        UserRegistrationRepository registration = new UserRegistrationFileRepository();

        registration.registerUser(
                args[0].split(COMMAND_ARGUMENT_DELIMITER)[1].replace("\"", ""),
                args[1].split(COMMAND_ARGUMENT_DELIMITER)[1].replace("\"", "")
        );

        return "Registration successful\n";
    }

    private String login(String[] args) throws UserAlreadyLoggedInException, UserDoesNotExistException,
            InvalidCommandException, IllegalTaskDatesException {
        validateUserArgumentsSetUp(args);
        if (this.loggedUser != null) {
            throw new UserAlreadyLoggedInException("You are already logged in as: " + this.loggedUser
                    + " Type \"logout\" and then login with other credentials");
        }

        this.loggedUser = args[0].split(COMMAND_ARGUMENT_DELIMITER)[1].replace("\"", "");
        try {
            this.userLoginRepository.createUserSession(
                    this.loggedUser,
                    args[1].split(COMMAND_ARGUMENT_DELIMITER)[1].replace("\"", ""));
        } catch (UserDoesNotExistException e) {
            this.loggedUser = null;
            throw new UserDoesNotExistException(e.getMessage());
        }

        this.individualTasks = LoadIndividualTasks.loadIndividualTaskData(this.loggedUser, INDIVIDUAL_TASKS_PATH);

        return "Login successful\n";
    }

    private String logout(String[] args) throws InvalidCommandException {
        validateZeroArgumentInput(args);

        this.userLoginRepository.endUserSession(this.loggedUser);
        this.loggedUser = null;
        return "Logout successful\n";
    }

    private String addTask(String[] args) throws IllegalTaskOperationException, UserNotAuthenticatedException,
            InvalidCommandException, IllegalTaskDatesException {
        verifyUserLoginStatusForTaskOperations();

        validateTaskArgumentsSetUp(args);
        Task taskToAdd = taskBuilder(args);

        this.individualTasks.addTaskToTaskRepository(taskToAdd);
        this.taskFileRepository.addTaskToTaskRepository(this.loggedUser, taskToAdd);

        return "Task " + args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + " added successfully\n";
    }

    private String updateTask(String[] args) throws IllegalTaskOperationException,
            UserNotAuthenticatedException, InvalidCommandException, IllegalTaskDatesException {
        verifyUserLoginStatusForTaskOperations();

        validateTaskArgumentsSetUp(args);

        int positionToSplitTasks = updateTaskSplitter(args, 0);
        String[] updatedTaskArgs = getTaskArguments(args, positionToSplitTasks);
        validateTaskValue(args[0], updatedTaskArgs[0]);

        Task taskToUpdate = taskBuilder(getTaskArguments(positionToSplitTasks, args));
        Task updatedTask = taskBuilder(updatedTaskArgs);

        this.individualTasks.updateTaskFromTaskRepository(taskToUpdate, updatedTask);
        this.taskFileRepository.updateTaskFromTaskRepository(taskToUpdate, updatedTask);

        return "Task " + args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + " updated successfully\n";
    }

    private String deleteTask(String[] args) throws UserNotAuthenticatedException,
            InvalidCommandException, IllegalTaskDatesException {
        verifyUserLoginStatusForTaskOperations();

        validateTaskArgumentsSetUp(args);
        Task taskToDelete = taskBuilder(args);

        this.individualTasks.deleteTaskFromTaskRepository(taskToDelete);
        this.taskFileRepository.deleteTaskFromTaskRepository(taskToDelete);

        return "Task " + args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + " deleted successfully\n";
    }

    private String getTask(String[] args) throws TaskDoesNotExistException, UserNotAuthenticatedException,
            InvalidCommandException, IllegalTaskDatesException {
        verifyUserLoginStatusForTaskOperations();

        validateTaskArgumentsSetUp(args);

        return this.individualTasks.getTaskFromTaskRepository(taskBuilder(args));
    }

    private String finishTask(String[] args) throws UserNotAuthenticatedException, InvalidCommandException,
            IllegalTaskDatesException {
        verifyUserLoginStatusForTaskOperations();

        validateTaskArgumentsSetUp(args);

        Task taskToFinish = taskBuilder(args);
        this.individualTasks.finishTaskFromTaskRepository(taskToFinish);
        this.taskFileRepository.finishTaskFromTaskRepository(taskToFinish);

        return "Task " + args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + " finished\n";
    }

    private String listTasks(String[] args) throws UserNotAuthenticatedException, InvalidCommandException {
        verifyUserLoginStatusForTaskOperations();

        if (args.length < 1 || args.length > 2 || !args[0].startsWith("completed=")) {
            throw new InvalidCommandException("Given command is not valid");
        }

        if (args.length == 2 && args[1].startsWith("date=")) {

            return this.individualTasks.listTasksFromRepository(DateParser.parseDate(args[1]
                            .split(COMMAND_ARGUMENT_DELIMITER)[1]
                            .replace("\"", "")),
                    Boolean.parseBoolean(args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                            .replace("\"", "")));

        } else if (args.length == 1) {

            return this.individualTasks.listTasksFromRepository(null,
                    Boolean.parseBoolean(args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                            .replace("\"", "")));

        } else {
            throw new InvalidCommandException("Given command is not valid");
        }
    }

    private String listDashboard(String[] args) throws UserNotAuthenticatedException, InvalidCommandException {
        verifyUserLoginStatusForTaskOperations();

        validateZeroArgumentInput(args);
        return this.individualTasks.listDashboard();
    }

    private String addCollaboration(String[] args) throws CollaborationAlreadyExistsException,
            UserNotAuthenticatedException, InvalidCommandException, IllegalCollaborationNameException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(args);

        this.collaborationFileRepository.addCollaboration(this.loggedUser,
                args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]);

        return "Collaboration " + args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + " added successfully\n";
    }

    private String deleteCollaboration(String[] args) throws FailedAuthorizationException,
            UserNotAuthenticatedException, InvalidCommandException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(args);

        this.collaborationFileRepository.deleteCollaboration(this.loggedUser,
                args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]);

        return "Collaboration " + args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + " deleted successfully\n";
    }

    private String listCollaborations(String[] args) throws UserNotAuthenticatedException, InvalidCommandException {
        verifyUserLoginStatusForTaskOperations();
        validateZeroArgumentInput(args);
        return this.collaborationFileRepository.listCollaborations(this.loggedUser).toString();
    }

    private String addUser(String[] args) throws UserNotAuthenticatedException, InvalidCommandException,
            CollaborationAlreadyExistsException, UserDoesNotExistException, UserNotInCollaborationException,
            UserAlreadyInCollaborationException, CollaborationDoesNotExistException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(new String[]{args[0]});
        if (args.length != 2) {
            throw new RuntimeException("Invalid task operation");
        }
        validateUsernameArgumentSetUp(new String[]{args[1]});

        this.collaborationFileRepository.addUserToCollaboration(this.loggedUser,
                args[0].split(COMMAND_ARGUMENT_DELIMITER)[1], args[1].split(COMMAND_ARGUMENT_DELIMITER)[1]);

        return "User " + args[1].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + " added successfully to collaboration "
                + args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + "\n";
    }

    private String assignTask(String[] args) throws UserNotAuthenticatedException,
            InvalidCommandException, IllegalTaskDatesException,
            CollaborationAlreadyExistsException, IllegalTaskAssignmentException, CollaborationDoesNotExistException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(new String[]{args[0]});
        if (args.length < COMMAND_ARGUMENTS_MAX_COUNT - 1 || args.length > COMMAND_ARGUMENTS_MAX_COUNT) {
            throw new InvalidCommandException("Given command is not valid");
        }

        validateUsernameArgumentSetUp(new String[]{args[1]});
        String[] taskAttributes = getTaskArguments(args, 2);
        validateTaskArgumentsSetUp(taskAttributes);
        Task taskToHaveAssignee = taskBuilder(taskAttributes);

        this.collaborationFileRepository.assignTaskToUser(this.loggedUser,
                args[0].split(COMMAND_ARGUMENT_DELIMITER)[1], args[1].split(COMMAND_ARGUMENT_DELIMITER)[1],
                taskToHaveAssignee);

        return "Task " + args[2].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + " from collaboration "
                + args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + " assigned successfully to user "
                + args[1].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", "") + "\n";
    }

    private String listCollaborationTasks(String[] args) throws UserNotAuthenticatedException,
            InvalidCommandException, IllegalTaskDatesException, UserNotInCollaborationException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(args);
        return this.collaborationFileRepository
                .listCollaborationTasks(this.loggedUser, args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]);
    }

    private String listCollaborationUsers(String[] args) throws UserNotAuthenticatedException,
            InvalidCommandException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(args);
        return this.collaborationFileRepository
                .listCollaborationUsers(this.loggedUser, args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]).toString();
    }


    public String addCollaborationTask(String[] args) throws IllegalTaskOperationException,
            UserNotAuthenticatedException, InvalidCommandException, IllegalTaskDatesException,
            CollaborationAlreadyExistsException, UserNotInCollaborationException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(new String[]{args[0]});
        String[] taskArguments = getTaskArguments(args, 1);
        validateTaskArgumentsSetUp(taskArguments);
        Task taskToAdd = taskBuilder(taskArguments);

        this.collaborationFileRepository.addTaskToCollaborationRepository(this.loggedUser,
                args[0].split(COMMAND_ARGUMENT_DELIMITER)[1], taskToAdd);

        return "Task added successfully to the collaboration\n";
    }

    public String updateCollaborationTask(String[] args) throws IllegalTaskOperationException,
            UserNotAuthenticatedException, InvalidCommandException, IllegalTaskDatesException,
            CollaborationAlreadyExistsException, UserNotInCollaborationException, CollaborationDoesNotExistException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(new String[]{args[0]});
        String[] taskArguments = getTaskArguments(args, 1);
        validateTaskArgumentsSetUp(taskArguments);

        int positionToSplitTasks = updateTaskSplitter(args, 1);
        String[] updatedTaskArgs = getTaskArguments(args, positionToSplitTasks);
        validateTaskValue(args[1], updatedTaskArgs[0]);

        Task taskToUpdate = taskBuilder(getTaskArguments(positionToSplitTasks - 1, taskArguments));
        Task updatedTask = taskBuilder(updatedTaskArgs);

        this.collaborationFileRepository
                .updateTaskFromCollaborationRepository(this.loggedUser,
                            args[0].split(COMMAND_ARGUMENT_DELIMITER)[1], taskToUpdate, updatedTask);

        return "Task updated successfully\n";
    }

    public String deleteCollaborationTask(String[] args) throws UserNotAuthenticatedException,
            InvalidCommandException, IllegalTaskDatesException, CollaborationAlreadyExistsException,
            UserNotInCollaborationException, CollaborationDoesNotExistException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(new String[]{args[0]});
        String[] taskArguments = getTaskArguments(args, 1);
        validateTaskArgumentsSetUp(taskArguments);

        Task taskToDelete = taskBuilder(taskArguments);

        this.collaborationFileRepository
                .deleteTaskFromCollaborationRepository(this.loggedUser,
                        args[0].split(COMMAND_ARGUMENT_DELIMITER)[1], taskToDelete);

        return "Task deleted successfully from the collaboration\n";
    }

    public String finishCollaborationTask(String[] args) throws UserNotAuthenticatedException,
            InvalidCommandException, IllegalTaskDatesException, CollaborationAlreadyExistsException,
            UserNotInCollaborationException {
        verifyUserLoginStatusForTaskOperations();

        validateCollaborationArguments(new String[]{args[0]});
        String[] taskArguments = getTaskArguments(args, 1);
        validateTaskArgumentsSetUp(taskArguments);

        Task taskToFinish = taskBuilder(taskArguments);

        this.collaborationFileRepository
                .finishTaskFromCollaborationRepository(this.loggedUser,
                        args[0].split(COMMAND_ARGUMENT_DELIMITER)[1], taskToFinish);

        return "Task finished\n";
    }

    private void validateCommand(Command command) throws InvalidCommandException {
        if (Arrays.stream(CommandType.values())
                .noneMatch(e -> e.getCommand().equals(command.command()))) {
            throw new InvalidCommandException("Not supported command");
        }
    }

    // needed so that a convenient way of writing input is provided
    // (no need to have keys for missing values) e.g. no need to write 'date=' if it has no value
    private void getAttributeKey(String[] prefixes, String[] args, Task.TaskBuilder taskBuilder)
            throws InvalidCommandException {
        Set<String> uniquePrefixes = new HashSet<>(Arrays.asList(prefixes));
        if (uniquePrefixes.size() != prefixes.length) {
            throw new InvalidCommandException("Given command is not valid");
        }

        for (int i = 0; i < prefixes.length; i++) {
            switch (prefixes[i]) {
                case "date=" -> taskBuilder.setDate(DateParser.parseDate(args[i + 1]
                        .split(COMMAND_ARGUMENT_DELIMITER)[1]
                        .replace("\"", "")));
                case "due-date=" -> taskBuilder.setDueDate(DateParser.parseDate(args[i + 1]
                        .split(COMMAND_ARGUMENT_DELIMITER)[1]
                        .replace("\"", "")));
                case "description=" -> taskBuilder.setDescription(args[i + 1]
                        .split(COMMAND_ARGUMENT_DELIMITER)[1]
                        .replace("\"", ""));
                default -> throw new InvalidCommandException("Given command is not valid");
            }
        }
    }

    private Task taskBuilder(String[] args) throws InvalidCommandException, IllegalTaskDatesException {
        Task.TaskBuilder taskBuilder = Task.builder(args[0].split(COMMAND_ARGUMENT_DELIMITER)[1]
                .replace("\"", ""));

        String[] prefixes;
        switch (args.length) {
            case 2 -> {
                prefixes = new String[1];
                prefixes[0] = args[1].split(COMMAND_ARGUMENT_DELIMITER)[0] + COMMAND_ARGUMENT_DELIMITER;
                getAttributeKey(prefixes, args, taskBuilder);
            }

            case COMMAND_ARGUMENTS_MAX_COUNT - 1 -> {
                prefixes = new String[2];
                prefixes[0] = args[1].split(COMMAND_ARGUMENT_DELIMITER)[0] + COMMAND_ARGUMENT_DELIMITER;
                prefixes[1] = args[2].split(COMMAND_ARGUMENT_DELIMITER)[0] + COMMAND_ARGUMENT_DELIMITER;
                getAttributeKey(prefixes, args, taskBuilder);
            }

            case COMMAND_ARGUMENTS_MAX_COUNT -> {
                prefixes = new String[COMMAND_ARGUMENTS_MAX_COUNT - 1];
                prefixes[0] = args[1].split(COMMAND_ARGUMENT_DELIMITER)[0] + COMMAND_ARGUMENT_DELIMITER;
                prefixes[1] = args[2].split(COMMAND_ARGUMENT_DELIMITER)[0] + COMMAND_ARGUMENT_DELIMITER;
                prefixes[2] = args[COMMAND_ARGUMENTS_MAX_COUNT - 1].split(COMMAND_ARGUMENT_DELIMITER)[0]
                        + COMMAND_ARGUMENT_DELIMITER;
                getAttributeKey(prefixes, args, taskBuilder);
            }
        }

        return taskBuilder.build();
    }

    private void verifyUserLoginStatusForTaskOperations() throws UserNotAuthenticatedException {
        if (!this.userLoginRepository.userInSession(this.loggedUser)) {
            throw new UserNotAuthenticatedException("Before performing any operation each user must login first\n");
        }
    }

    private void validateUserArgumentsSetUp(String[] args) throws InvalidCommandException {
        if (args.length != 2) {
            throw new InvalidCommandException("Given command is not valid");
        }

        validateUsernameArgumentSetUp(args);
        validateUserPasswordArgumentSetUp(args);
    }

    // created solely based on the need to invoke this fragment in register, addUser and assignTask methods
    // and to avoid code duplication; here a tradeoff is made between best name for the method and the need
    // think about the way it is organised now
    private void validateUsernameArgumentSetUp(String[] args) throws InvalidCommandException {
        if (!args[0].startsWith("username=")) {
            throw new InvalidCommandException("Given command is not valid");
        }
    }

    private void validateUserPasswordArgumentSetUp(String[] args) throws InvalidCommandException {
        if (!args[1].startsWith("password=")) {
            throw new InvalidCommandException("Given command is not valid");
        }
    }

    private void validateTaskArgumentsSetUp(String[] args) throws InvalidCommandException {
        if (args.length < 1 || !args[0].startsWith("name=")) {
            throw new InvalidCommandException("Given command is not valid");
        }
    }

    private void validateCollaborationArguments(String[] args) throws InvalidCommandException {
        if (args.length != 1 || !args[0].startsWith("collaboration=")) {
            throw new InvalidCommandException("Given command is not valid");
        }
    }

    private void validateTaskValue(String oldName, String newName) throws InvalidCommandException {
        if (!Objects.equals(oldName, newName)) {
            throw new InvalidCommandException("Given command is not valid");
        }
    }

    void validateZeroArgumentInput(String[] args) throws InvalidCommandException {
        if (args.length != 1 && !args[0].isEmpty()) {
            throw new InvalidCommandException("Given command is not valid");
        }
    }

    private String[] getTaskArguments(String[] args, int index) {
        String[] taskArguments = new String[args.length - index];
        System.arraycopy(args, index, taskArguments, 0, args.length - index);
        return taskArguments;
    }

    private String[] getTaskArguments(int index, String[] args) {
        String[] taskArguments = new String[index];
        System.arraycopy(args, 0, taskArguments, 0, index);
        return taskArguments;
    }

    private int updateTaskSplitter(String[] args, int positionToStart) {
        int positionToSplitTasks = positionToStart;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("name=")) {
                positionToSplitTasks = i;
            }
        }
        return positionToSplitTasks;
    }

}