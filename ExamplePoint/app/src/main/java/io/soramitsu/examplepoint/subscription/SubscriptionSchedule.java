package io.soramitsu.examplepoint.subscription;

import java.util.Calendar;

public final class SubscriptionSchedule {

    private SubscriptionSchedule() {
    }

    public static long advance(long fromMs, SubscriptionCadence cadence) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(fromMs);
        calendar.add(Calendar.MONTH, cadence.months);
        return calendar.getTimeInMillis();
    }
}
