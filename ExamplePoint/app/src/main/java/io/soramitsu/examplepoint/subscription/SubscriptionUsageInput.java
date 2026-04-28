package io.soramitsu.examplepoint.subscription;

import java.math.BigDecimal;

/**
 * Request payload for usage recording.
 */
public final class SubscriptionUsageInput {
    public final String subscriptionId;
    public final String unitKey;
    public final BigDecimal delta;

    public SubscriptionUsageInput(String subscriptionId, String unitKey, BigDecimal delta) {
        this.subscriptionId = requireNonBlank(subscriptionId, "subscriptionId");
        this.unitKey = requireNonBlank(unitKey, "unitKey");
        if (delta == null) {
            throw new IllegalArgumentException("delta is required");
        }
        if (delta.signum() <= 0) {
            throw new IllegalArgumentException("delta must be positive");
        }
        this.delta = delta;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
