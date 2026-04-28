package io.soramitsu.examplepoint.subscription;

import java.math.BigDecimal;

public final class SubscriptionRecord {
    public String id;
    public String planId;
    public String merchantName;
    public String providerAccountId;
    public BigDecimal amount;
    public BigDecimal maxAmount;
    public SubscriptionAmountType amountType;
    public SubscriptionCadence cadence;
    public long nextChargeAtMs;
    public SubscriptionStatus status;
    public boolean cancelAtPeriodEnd;
    public Long lastChargeAtMs;
    public BigDecimal lastChargeAmount;
    public String note;
    public String unitKey;
    public BigDecimal unitPrice;
    public BigDecimal usageAccumulated;
    public String billingTriggerId;
    public String billingPeriod;

    public SubscriptionRecord() {
        // Required for Gson.
    }

    public SubscriptionRecord(
            String id,
            String merchantName,
            BigDecimal amount,
            BigDecimal maxAmount,
            SubscriptionAmountType amountType,
            SubscriptionCadence cadence,
            long nextChargeAtMs,
            SubscriptionStatus status,
            boolean cancelAtPeriodEnd,
            Long lastChargeAtMs,
            BigDecimal lastChargeAmount,
            String note
    ) {
        this.id = id;
        this.merchantName = merchantName;
        this.amount = amount;
        this.maxAmount = maxAmount;
        this.amountType = amountType;
        this.cadence = cadence;
        this.nextChargeAtMs = nextChargeAtMs;
        this.status = status;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.lastChargeAtMs = lastChargeAtMs;
        this.lastChargeAmount = lastChargeAmount;
        this.note = note;
    }

    public SubscriptionRecord(
            String id,
            String planId,
            String merchantName,
            String providerAccountId,
            BigDecimal amount,
            BigDecimal maxAmount,
            SubscriptionAmountType amountType,
            SubscriptionCadence cadence,
            long nextChargeAtMs,
            SubscriptionStatus status,
            boolean cancelAtPeriodEnd,
            Long lastChargeAtMs,
            BigDecimal lastChargeAmount,
            String note,
            String unitKey,
            BigDecimal unitPrice,
            BigDecimal usageAccumulated,
            String billingTriggerId,
            String billingPeriod
    ) {
        this(
                id,
                merchantName,
                amount,
                maxAmount,
                amountType,
                cadence,
                nextChargeAtMs,
                status,
                cancelAtPeriodEnd,
                lastChargeAtMs,
                lastChargeAmount,
                note
        );
        this.planId = planId;
        this.providerAccountId = providerAccountId;
        this.unitKey = unitKey;
        this.unitPrice = unitPrice;
        this.usageAccumulated = usageAccumulated;
        this.billingTriggerId = billingTriggerId;
        this.billingPeriod = billingPeriod;
    }
}
