package bg.sofia.uni.fmi.mjt.todoist.server.logger;

public interface Logger {

    void error(Object toLog);

    void info(Object toLog);

    void debug(Object toLog);

    void trace(Object toLog);

}