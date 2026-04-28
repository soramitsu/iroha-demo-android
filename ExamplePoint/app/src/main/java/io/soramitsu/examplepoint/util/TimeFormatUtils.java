package io.soramitsu.examplepoint.util;

import java.text.DateFormat;
import java.util.Date;

public final class TimeFormatUtils {
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    private TimeFormatUtils() {}

    public static String format(long epochMs) {
        if (epochMs <= 0) {
            return null;
        }
        return DATE_FORMAT.format(new Date(epochMs));
    }
}
