package bg.sofia.uni.fmi.mjt.todoist.server.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileCreator {

    public static final ReadWriteLock USERS_READ_WRITE_LOCK = new ReentrantReadWriteLock();
    public static final ReadWriteLock INDIVIDUAL_TASKS_READ_WRITE_LOCK = new ReentrantReadWriteLock();
    public static final ReadWriteLock COLLABORATIONS_READ_WRITE_LOCK = new ReentrantReadWriteLock();
    public static final ReadWriteLock COLLABORATION_TASKS_READ_WRITE_LOCK = new ReentrantReadWriteLock();
    public static final ReadWriteLock COLLABORATION_USERS_READ_WRITE_LOCK = new ReentrantReadWriteLock();

    public static Path createFIle(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Not valid");
        }

        if (Files.notExists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return path;
    }

}