package bg.sofia.uni.fmi.mjt.todoist.server.user;

import java.util.Objects;

public record User(String username, String password) {

    private static final String USER_ATTRIBUTE_DELIMITER = ";";

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || this.getClass() != object.getClass()) {
            return false;
        }

        User user = (User) object;
        return Objects.equals(this.username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.username);
    }

    public static User of(String line) {
        String[] userAttributes = line.split(USER_ATTRIBUTE_DELIMITER);
        return new User(userAttributes[0], userAttributes[1]);
    }

    // Since passwords must not be deserialized
    // and gson does not support transient keyword for record members
    // JSON format is written explicitly
    @Override
    public String toString() {
        return "{\n" +
                "   \"username\": " + "\"" + this.username + "\"" +
                "\n}";
    }

}