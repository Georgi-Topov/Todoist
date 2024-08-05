package bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository;

import bg.sofia.uni.fmi.mjt.todoist.server.file.FileCreator;
import bg.sofia.uni.fmi.mjt.todoist.server.user.User;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.IllegalUsernameException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserDoesNotExistException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;

import static bg.sofia.uni.fmi.mjt.todoist.server.file.FileCreator.USERS_READ_WRITE_LOCK;

public class UserRegistrationFileRepository extends UserRegistrationRepository {

    private static final String USER_ATTRIBUTE_DELIMITER = ";";
    private static final String ILLEGAL_USERNAME_SYMBOL1 = "\"";
    private static final String ILLEGAL_USERNAME_SYMBOL2 = "'";
    private static final String ILLEGAL_USERNAME_SYMBOL3 = "/";
    private static final String ILLEGAL_USERNAME_SYMBOL4 = "\\";


    private static final Path REGISTERED_USERS_PATH;
    static {
        REGISTERED_USERS_PATH = FileCreator.createFIle(Path.of("users.txt"));
    }

    @Override
    void validateUsername(String username) throws IllegalUsernameException {
        if (username.contains(ILLEGAL_USERNAME_SYMBOL1) || username.contains(ILLEGAL_USERNAME_SYMBOL2)
                || username.contains(ILLEGAL_USERNAME_SYMBOL3) || username.contains(ILLEGAL_USERNAME_SYMBOL4)) {
            throw new IllegalUsernameException("Usernames cannot contain the following symbols: \\, \", ' and / \n");
        }
    }

    @Override
    boolean existsUser(String username) {
        USERS_READ_WRITE_LOCK.readLock().lock();
        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(REGISTERED_USERS_PATH)))) {

            String currentUser;
            while ((currentUser = in.readLine()) != null) {
                String currentUsername = currentUser.split(USER_ATTRIBUTE_DELIMITER)[0];

                if (Objects.equals(currentUsername, username)) {
                    return true;
                }
            }
            return false;

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while reading from a file", e);
        } finally {
            USERS_READ_WRITE_LOCK.readLock().unlock();
        }
    }

    @Override
    public void registerUser(String username, String password) throws UserAlreadyExistsException,
            IllegalUsernameException {
        validateUsername(username);

        if (existsUser(username)) {
            throw new UserAlreadyExistsException("User with the provided username already exists! " +
                    "Use another username!\n");
        }

        User newUser = new User(username, password);

        USERS_READ_WRITE_LOCK.writeLock().lock();
        try (PrintWriter out = new PrintWriter(
                new FileWriter(String.valueOf(REGISTERED_USERS_PATH), true), true)) {
            out.println(newUser.username() + USER_ATTRIBUTE_DELIMITER + newUser.password());
        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            USERS_READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    public static void checkUserRegistration(String username) throws UserDoesNotExistException {
        USERS_READ_WRITE_LOCK.readLock().lock();
        try (BufferedReader in = new BufferedReader(new FileReader(String.valueOf(REGISTERED_USERS_PATH)))) {

            String line;
            while ((line = in.readLine()) != null) {
                String readUser = line.split(USER_ATTRIBUTE_DELIMITER)[0]
                        .replace(ILLEGAL_USERNAME_SYMBOL1, "");
                if (Objects.equals(readUser, username)) {
                    return;
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("A problem occurred while writing to a file", e);
        } finally {
            USERS_READ_WRITE_LOCK.readLock().unlock();
        }

        throw new UserDoesNotExistException("User with the provided username does not exist! " +
                "First, user needs to register! " +
                "To see the actual command, type \"help\"\n");
    }

}