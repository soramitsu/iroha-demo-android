package io.soramitsu.examplepoint.subscription;

public enum SubscriptionCadence {
    MONTHLY(1),
    QUARTERLY(3),
    YEARLY(12);

    public final int months;

    SubscriptionCadence(int months) {
        this.months = months;
    }
}
