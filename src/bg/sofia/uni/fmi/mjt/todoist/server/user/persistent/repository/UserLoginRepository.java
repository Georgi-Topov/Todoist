package bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository;

import bg.sofia.uni.fmi.mjt.todoist.server.file.FileCreator;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserAlreadyLoggedInException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserDoesNotExistException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static bg.sofia.uni.fmi.mjt.todoist.server.file.FileCreator.USERS_READ_WRITE_LOCK;

public class UserLoginRepository {

    private static final Path USERS_PATH;
    static {
        USERS_PATH = FileCreator.createFIle(Path.of("users.txt"));
    }

    private static final String USER_ATTRIBUTE_DELIMITER = ";";

    private final Set<String> loggedInUsers;

    public UserLoginRepository() {
        this.loggedInUsers = new HashSet<>();
    }

    private boolean existsUser(String username, String password) throws UserDoesNotExistException {
        USERS_READ_WRITE_LOCK.readLock().lock();
        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(USERS_PATH)))) {

            String currentUser;
            while ((currentUser = in.readLine()) != null) {
                String currentUsername = currentUser.split(USER_ATTRIBUTE_DELIMITER)[0];
                String currentPassword = currentUser.split(USER_ATTRIBUTE_DELIMITER)[1];

                if (Objects.equals(currentUsername, username)) {
                    if (Objects.equals(currentPassword, password)) {
                        return true;
                    }
                    throw new UserDoesNotExistException("Provided password is not correct!\n");
                }
            }
            return false;

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while reading from a file", e);
        } finally {
            USERS_READ_WRITE_LOCK.readLock().unlock();
        }
    }

    public boolean userInSession(String username) {
        return this.loggedInUsers.contains(username);
    }

    public void createUserSession(String username, String password)
            throws UserDoesNotExistException, UserAlreadyLoggedInException {
        if (userInSession(username)) {
            throw new UserAlreadyLoggedInException("You are already logged in! " +
                    "Type \"logout\" and then login with other credentials\n");
        }

        if (!existsUser(username, password)) {
            throw new UserDoesNotExistException("User with the provided username does not exist! " +
                    "First, user needs to register! " +
                    "To see the actual command, type \"help\"\n");
        }

        this.loggedInUsers.add(username);
    }

    public void endUserSession(String username) {
        this.loggedInUsers.remove(username);
    }

}