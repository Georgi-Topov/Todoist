package bg.sofia.uni.fmi.mjt.todoist.client.command;

import java.util.ArrayList;
import java.util.List;

public class CommandCreator {

    private static String getCommandArgumentsBeginning(String input, List<String> tokens, StringBuilder sb) {
        int index = 0;
        boolean insideQuote = false;

        // first check for listDashboard command which is supposed to have no arguments
        while (index < input.length() && input.charAt(index) != ' ' || insideQuote) {
            if (input.charAt(index) == '"') {
                insideQuote = !insideQuote;
            }

            sb.append(input.charAt(index));
            index++;
        }
        index++;

        tokens.add(sb.toString().replace("\"", ""));
        sb.delete(0, sb.length());

        return index < input.length() ? input.substring(index) : "";
    }

    private static List<String> getCommandArguments(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        String inputArguments = getCommandArgumentsBeginning(input, tokens, sb);

        boolean insideQuote = false;
        for (char c : inputArguments.toCharArray()) {
            if (c == '"') {
                insideQuote = !insideQuote;
            }

            if (c == ' ' && !insideQuote) {
                tokens.add(sb.toString().replace("\"", ""));
                sb.delete(0, sb.length());
            } else {
                sb.append(c);
            }
        }

        tokens.add(sb.toString().replace("\"", ""));
        return tokens;
    }

    public static Command newCommand(String clientInput) {
        List<String> tokens = CommandCreator.getCommandArguments(clientInput);
        String[] args = tokens.subList(1, tokens.size()).toArray(new String[0]);

        return new Command(tokens.getFirst(), args);
    }

}