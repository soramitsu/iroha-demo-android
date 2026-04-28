package io.soramitsu.examplepoint.subscription;

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class SubscriptionScheduleTest {

    @Test
    public void advancesMonthlyByOneMonth() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long start = calendar.getTimeInMillis();

        long advanced = SubscriptionSchedule.advance(start, SubscriptionCadence.MONTHLY);
        Calendar result = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        result.setTimeInMillis(advanced);

        assertEquals(Calendar.FEBRUARY, result.get(Calendar.MONTH));
        assertEquals(1, result.get(Calendar.DAY_OF_MONTH));
    }
}
