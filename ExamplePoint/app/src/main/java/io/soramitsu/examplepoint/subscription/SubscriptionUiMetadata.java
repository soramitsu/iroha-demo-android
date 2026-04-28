package io.soramitsu.examplepoint.subscription;

import java.math.BigDecimal;

/**
 * Persisted UI-only metadata not represented directly in Torii subscription records.
 */
public final class SubscriptionUiMetadata {
    public final String subscriptionId;
    public final String merchantName;
    public final SubscriptionCadence cadence;
    public final BigDecimal usageCap;
    public final String note;

    public SubscriptionUiMetadata(
            String subscriptionId,
            String merchantName,
            SubscriptionCadence cadence,
            BigDecimal usageCap,
            String note
    ) {
        this.subscriptionId = subscriptionId;
        this.merchantName = merchantName;
        this.cadence = cadence;
        this.usageCap = usageCap;
        this.note = note;
    }
}
