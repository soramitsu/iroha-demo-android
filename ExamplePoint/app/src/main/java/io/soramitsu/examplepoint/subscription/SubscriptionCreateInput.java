package io.soramitsu.examplepoint.subscription;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * UI payload used when creating backend subscription plans/subscriptions.
 */
public final class SubscriptionCreateInput {
    public final String merchantName;
    public final SubscriptionAmountType amountType;
    public final BigDecimal amount;
    public final BigDecimal unitPrice;
    public final String unitKey;
    public final BigDecimal usageCap;
    public final SubscriptionCadence cadence;
    public final String note;

    public SubscriptionCreateInput(
            String merchantName,
            SubscriptionAmountType amountType,
            BigDecimal amount,
            BigDecimal unitPrice,
            String unitKey,
            BigDecimal usageCap,
            SubscriptionCadence cadence,
            String note
    ) {
        this.merchantName = requireNonBlank(merchantName, "merchantName");
        this.amountType = Objects.requireNonNull(amountType, "amountType");
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.unitKey = unitKey == null ? null : unitKey.trim();
        this.usageCap = usageCap;
        this.cadence = Objects.requireNonNull(cadence, "cadence");
        this.note = note == null ? null : note.trim();
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
