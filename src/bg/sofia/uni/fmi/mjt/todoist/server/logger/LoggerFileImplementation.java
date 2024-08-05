package bg.sofia.uni.fmi.mjt.todoist.server.logger;

import bg.sofia.uni.fmi.mjt.todoist.server.file.FileCreator;
import bg.sofia.uni.fmi.mjt.todoist.server.logger.vo.LoggerLevel;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

public class LoggerFileImplementation implements Logger {

    private static final Path LOGGER_FILE_PATH;

    private final LoggerLevel loggerLevel;

    static {
        LOGGER_FILE_PATH = FileCreator.createFIle(Path.of("logger.txt"));
    }

    public LoggerFileImplementation(LoggerLevel loggerLevel) {
        this.loggerLevel = loggerLevel;
    }

    @Override
    public void error(Object toLog) {
        log(LoggerLevel.ERROR, toLog);
    }

    @Override
    public void info(Object toLog) {
        LoggerLevel currentLoggerLevel = LoggerLevel.INFO;
        if (isLoggingAllowed(currentLoggerLevel)) {
            log(currentLoggerLevel, toLog);
        }
    }

    @Override
    public void debug(Object toLog) {
        LoggerLevel currentLoggerLevel = LoggerLevel.DEBUG;
        if (isLoggingAllowed(currentLoggerLevel)) {
            log(currentLoggerLevel, toLog);
        }
    }

    @Override
    public void trace(Object toLog) {
        LoggerLevel currentLoggerLevel = LoggerLevel.TRACE;
        if (isLoggingAllowed(currentLoggerLevel)) {
            log(currentLoggerLevel, toLog);
        }
    }

    private boolean isLoggingAllowed(LoggerLevel currentLoggerLevel) {
        return this.loggerLevel.getCode().compareTo(currentLoggerLevel.getCode()) >= 0;
    }

    private synchronized void log(LoggerLevel currentLoggerLevel, Object toLog) {
        StringBuilder sb = new StringBuilder();
        try (PrintWriter out = new PrintWriter(new FileWriter(String.valueOf(LOGGER_FILE_PATH), true))) {
            sb.append(new Date()).append(" [").append(currentLoggerLevel.getLevel()).append("] - ")
                    .append(((Throwable) toLog).getClass().getSimpleName()).append(": \"")
                    .append(((Throwable) toLog).getMessage()).append("\" ")
                    .append(Arrays.toString(((Throwable) toLog).getStackTrace()));

            synchronized (LoggerFileImplementation.class) {
                out.println(sb);
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to log information", e);
        }
    }
}