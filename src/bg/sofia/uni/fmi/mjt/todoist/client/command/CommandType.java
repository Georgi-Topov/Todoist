package bg.sofia.uni.fmi.mjt.todoist.client.command;

public enum CommandType {

    REGISTER("register"),
    LOGIN("login"),
    LOGOUT("logout"),
    ADD_TASK("add-task"),
    UPDATE_TASK("update-task"),
    DELETE_TASK("delete-task"),
    GET_TASK("get-task"),
    FINISH_TASK("finish-task"),
    LIST_TASKS("list-tasks"),
    LIST_DASHBOARD("list-dashboard"),
    ADD_COLLABORATION("add-collaboration"),
    DELETE_COLLABORATION("delete-collaboration"),
    LIST_COLLABORATIONS("list-collaborations"),
    ADD_USER("add-user"),
    ASSIGN_TASK("assign-task"),
    LIST_COLLABORATION_TASKS("list-collaboration-tasks"),
    LIST_COLLABORATION_USERS("list-collaboration-users"),
    ADD_COLLABORATION_TASK("add-collaboration-task"),
    UPDATE_COLLABORATION_TASK("update-collaboration-task"),
    DELETE_COLLABORATION_TASK("delete-collaboration-task"),
    FINISH_COLLABORATION_TASK("finish-collaboration-task"),
    HELP("help");

    private final String command;

    CommandType(String command) {
        this.command = command;
    }

    public String getCommand() {
        return this.command;
    }

}