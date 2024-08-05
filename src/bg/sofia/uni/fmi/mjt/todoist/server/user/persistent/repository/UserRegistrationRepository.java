package bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository;

import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.IllegalUsernameException;
import bg.sofia.uni.fmi.mjt.todoist.server.user.persistent.repository.exception.UserAlreadyExistsException;

import java.io.IOException;

public abstract class UserRegistrationRepository {

    abstract void validateUsername(String username) throws IllegalUsernameException;

    abstract boolean existsUser(String username) throws IOException;

    public abstract void registerUser(String username, String password) throws UserAlreadyExistsException,
            IllegalUsernameException;

}