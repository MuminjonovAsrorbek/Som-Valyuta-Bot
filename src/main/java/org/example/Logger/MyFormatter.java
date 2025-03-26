package org.example.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class MyFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {

        String message = record.getMessage();

        Instant instant = record.getInstant();

        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        String dateTime = formatter.format(localDateTime);

        String sourceClassName = record.getSourceClassName();

        long longThreadID = record.getLongThreadID();

        Throwable thrown = record.getThrown();

        if (thrown != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            thrown.printStackTrace(pw);

            message = message + "\n" + sw.toString();
        }

        return String.format("%s %s %s%n%s: %s%n", dateTime, sourceClassName, longThreadID, record.getLevel(), message);

    }
}
