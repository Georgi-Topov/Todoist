package bg.sofia.uni.fmi.mjt.todoist.server.logger.vo;

public enum LoggerLevel {

    ERROR(0, "ERROR"),
    INFO(1, "INFO"),
    DEBUG(2, "DEBUG"),
    TRACE(3, "TRACE");

    private final Integer code;
    private final String level;

    LoggerLevel(Integer code, String level) {
        this.code = code;
        this.level = level;
    }

    public Integer getCode() {
        return code;
    }

    public String getLevel() {
        return level;
    }

}