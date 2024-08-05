package bg.sofia.uni.fmi.mjt.todoist.client;

import java.time.LocalDate;

public class DateParser {

    private static final String DATE_DELIMITER = "/";

    public static String dateValue(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.getDayOfMonth()
                + DATE_DELIMITER + date.getMonth().getValue()
                + DATE_DELIMITER + date.getYear();
    }

    public static LocalDate parseDate(String date)  {
        String[] dateValues = date.split(DATE_DELIMITER);
        return LocalDate.of(Integer.parseInt(dateValues[2]),
                Integer.parseInt(dateValues[1]), Integer.parseInt(dateValues[0]));
    }

}