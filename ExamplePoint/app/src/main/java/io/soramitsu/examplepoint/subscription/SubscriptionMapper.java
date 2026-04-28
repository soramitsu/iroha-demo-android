package io.soramitsu.examplepoint.subscription;

import androidx.annotation.Nullable;

import org.hyperledger.iroha.android.subscriptions.SubscriptionListResponse;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * Maps SDK subscription DTOs into app UI records.
 */
public final class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    public static SubscriptionRecord toRecord(
            SubscriptionListResponse.SubscriptionRecord source,
            @Nullable SubscriptionUiMetadata metadata
    ) {
        Map<String, Object> subscription = source.subscription();
        Map<String, Object> plan = source.plan();
        Map<String, Object> invoice = source.invoice();

        Map<String, Object> pricing = asMap(plan == null ? null : plan.get("pricing"));
        Map<String, Object> pricingDetail = asMap(pricing == null ? null : pricing.get("detail"));
        String pricingKind = asString(pricing == null ? null : pricing.get("kind"));

        SubscriptionAmountType amountType = "usage".equalsIgnoreCase(pricingKind)
                ? SubscriptionAmountType.VARIABLE
                : SubscriptionAmountType.FIXED;

        BigDecimal fixedAmount = amountType == SubscriptionAmountType.FIXED
                ? asDecimal(pricingDetail == null ? null : pricingDetail.get("amount"))
                : null;

        BigDecimal unitPrice = amountType == SubscriptionAmountType.VARIABLE
                ? asDecimal(pricingDetail == null ? null : pricingDetail.get("unit_price"))
                : null;

        String unitKey = amountType == SubscriptionAmountType.VARIABLE
                ? asString(pricingDetail == null ? null : pricingDetail.get("unit_key"))
                : null;

        BigDecimal accumulatedUsage = null;
        if (amountType == SubscriptionAmountType.VARIABLE) {
            Map<String, Object> usageMap = asMap(subscription.get("usage_accumulated"));
            if (usageMap != null && !usageMap.isEmpty()) {
                Object usage = unitKey != null ? usageMap.get(unitKey) : null;
                if (usage == null) {
                    usage = usageMap.values().iterator().next();
                }
                accumulatedUsage = asDecimal(usage);
            }
        }

        SubscriptionCadence cadence = resolveCadence(plan, metadata);
        String merchantName = metadata != null && metadata.merchantName != null
                ? metadata.merchantName
                : fallbackMerchantName(source.subscriptionId(), subscription, plan);

        SubscriptionRecord record = new SubscriptionRecord(
                source.subscriptionId(),
                merchantName,
                fixedAmount,
                metadata != null ? metadata.usageCap : null,
                amountType,
                cadence,
                asLong(subscription.get("next_charge_ms"), 0L),
                resolveStatus(subscription.get("status")),
                asBoolean(subscription.get("cancel_at_period_end"), false),
                asLongOrNull(invoice == null ? null : invoice.get("attempted_at_ms")),
                asDecimal(invoice == null ? null : invoice.get("amount")),
                metadata != null ? metadata.note : null
        );
        record.planId = asString(subscription.get("plan_id"));
        record.providerAccountId = asString(subscription.get("provider"));
        record.unitPrice = unitPrice;
        record.unitKey = unitKey;
        record.usageAccumulated = accumulatedUsage;
        record.billingTriggerId = asString(subscription.get("billing_trigger_id"));
        record.billingPeriod = resolveBillingPeriod(plan);
        return record;
    }

    public static SubscriptionStatus resolveStatus(Object statusNode) {
        String raw;
        if (statusNode instanceof Map) {
            raw = asString(((Map<?, ?>) statusNode).get("status"));
        } else {
            raw = asString(statusNode);
        }
        if (raw == null) {
            return SubscriptionStatus.ACTIVE;
        }
        String normalized = raw.trim().toLowerCase(Locale.US);
        if ("paused".equals(normalized)) {
            return SubscriptionStatus.PAUSED;
        }
        if ("past_due".equals(normalized)) {
            return SubscriptionStatus.PAST_DUE;
        }
        if ("canceled".equals(normalized)) {
            return SubscriptionStatus.CANCELED;
        }
        if ("suspended".equals(normalized)) {
            return SubscriptionStatus.SUSPENDED;
        }
        return SubscriptionStatus.ACTIVE;
    }

    private static SubscriptionCadence resolveCadence(
            Map<String, Object> plan,
            @Nullable SubscriptionUiMetadata metadata
    ) {
        if (metadata != null && metadata.cadence != null) {
            return metadata.cadence;
        }
        Map<String, Object> billing = asMap(plan == null ? null : plan.get("billing"));
        Map<String, Object> cadence = asMap(billing == null ? null : billing.get("cadence"));
        String kind = asString(cadence == null ? null : cadence.get("kind"));
        if ("fixed_period".equalsIgnoreCase(kind)) {
            Map<String, Object> detail = asMap(cadence.get("detail"));
            long periodMs = asLong(detail == null ? null : detail.get("period_ms"), 0L);
            if (periodMs >= 31_536_000_000L) {
                return SubscriptionCadence.YEARLY;
            }
            if (periodMs >= 7_776_000_000L) {
                return SubscriptionCadence.QUARTERLY;
            }
            return SubscriptionCadence.MONTHLY;
        }
        return SubscriptionCadence.MONTHLY;
    }

    private static String resolveBillingPeriod(Map<String, Object> plan) {
        Map<String, Object> billing = asMap(plan == null ? null : plan.get("billing"));
        Map<String, Object> billFor = asMap(billing == null ? null : billing.get("bill_for"));
        return asString(billFor == null ? null : billFor.get("period"));
    }

    private static String fallbackMerchantName(
            String subscriptionId,
            Map<String, Object> subscription,
            Map<String, Object> plan
    ) {
        String display = asString(plan == null ? null : plan.get("display_name"));
        if (display != null && !display.trim().isEmpty()) {
            return display;
        }
        String planId = asString(subscription.get("plan_id"));
        if (planId != null && !planId.trim().isEmpty()) {
            return planId;
        }
        return subscriptionId;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) value;
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return String.valueOf(value);
    }

    private static long asLong(Object value, long fallback) {
        Long parsed = asLongOrNull(value);
        return parsed == null ? fallback : parsed;
    }

    private static Long asLongOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static BigDecimal asDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
