package io.soramitsu.examplepoint.subscription;

import androidx.annotation.NonNull;

import org.hyperledger.iroha.android.client.SubscriptionToriiClient;
import org.hyperledger.iroha.android.subscriptions.SubscriptionActionRequest;
import org.hyperledger.iroha.android.subscriptions.SubscriptionCreateRequest;
import org.hyperledger.iroha.android.subscriptions.SubscriptionListParams;
import org.hyperledger.iroha.android.subscriptions.SubscriptionListResponse;
import org.hyperledger.iroha.android.subscriptions.SubscriptionPlanCreateRequest;
import org.hyperledger.iroha.android.subscriptions.SubscriptionUsageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Backend-facing subscription repository powered by {@link SubscriptionToriiClient}.
 */
public final class SubscriptionBackendRepository {

    private static final long DEFAULT_RETRY_BACKOFF_MS = 86_400_000L;
    private static final long DEFAULT_GRACE_MS = 604_800_000L;
    private static final int DEFAULT_MAX_FAILURES = 3;
    private static final int MAX_CREATE_ATTEMPTS = 3;

    private final SubscriptionToriiClient client;
    private final String defaultAssetDefinition;
    private final String defaultDomain;
    private final SubscriptionUiMetadataStore metadataStore;

    public SubscriptionBackendRepository(
            @NonNull SubscriptionToriiClient client,
            @NonNull String defaultAssetDefinition,
            @NonNull String defaultDomain,
            @NonNull SubscriptionUiMetadataStore metadataStore
    ) {
        this.client = client;
        this.defaultAssetDefinition = defaultAssetDefinition;
        this.defaultDomain = defaultDomain;
        this.metadataStore = metadataStore;
    }

    public List<SubscriptionRecord> fetchSubscriptions(@NonNull String ownerAccountId) {
        SubscriptionListResponse response = client.listSubscriptions(
                SubscriptionListParams.builder()
                        .ownedBy(ownerAccountId)
                        .build()
        ).join();

        Map<String, SubscriptionUiMetadata> metadata = metadataStore.loadAll();
        List<SubscriptionRecord> mapped = new ArrayList<>();
        for (SubscriptionListResponse.SubscriptionRecord record : response.items()) {
            mapped.add(SubscriptionMapper.toRecord(record, metadata.get(record.subscriptionId())));
        }
        mapped.sort(Comparator.comparingLong(item -> item.nextChargeAtMs));
        return mapped;
    }

    public SubscriptionRecord createSubscription(
            @NonNull String authority,
            @NonNull String privateKey,
            @NonNull SubscriptionCreateInput input
    ) {
        Throwable lastCollision = null;
        for (int attempt = 1; attempt <= MAX_CREATE_ATTEMPTS; attempt++) {
            CreateIds ids = CreateIds.generate(defaultDomain);
            try {
                createPlan(authority, privateKey, ids.planId, input);
                client.createSubscription(
                        SubscriptionCreateRequest.builder()
                                .authority(authority)
                                .subscriptionId(ids.subscriptionId)
                                .planId(ids.planId)
                                .grantUsageToProvider(input.amountType == SubscriptionAmountType.VARIABLE)
                                .build()
                ).join();

                metadataStore.upsert(new SubscriptionUiMetadata(
                        ids.subscriptionId,
                        input.merchantName,
                        input.cadence,
                        input.usageCap,
                        input.note
                ));

                SubscriptionListResponse.SubscriptionRecord created = client.getSubscription(ids.subscriptionId).join();
                if (created != null) {
                    SubscriptionUiMetadata metadata = metadataStore.load(ids.subscriptionId);
                    return SubscriptionMapper.toRecord(created, metadata);
                }
                List<SubscriptionRecord> subscriptions = fetchSubscriptions(authority);
                for (SubscriptionRecord record : subscriptions) {
                    if (ids.subscriptionId.equals(record.id)) {
                        return record;
                    }
                }
                throw new IllegalStateException("Created subscription was not returned by backend");
            } catch (RuntimeException ex) {
                if (isCollision(ex) && attempt < MAX_CREATE_ATTEMPTS) {
                    lastCollision = ex;
                    continue;
                }
                throw ex;
            }
        }
        throw new IllegalStateException(
                "Subscription plan/subscription id collision after " + MAX_CREATE_ATTEMPTS + " attempts"
        );
    }

    public void pauseSubscription(String subscriptionId, String authority, String privateKey) {
        client.pauseSubscription(subscriptionId, actionRequest(authority, privateKey)).join();
    }

    public void resumeSubscription(String subscriptionId, String authority, String privateKey) {
        client.resumeSubscription(subscriptionId, actionRequest(authority, privateKey)).join();
    }

    public void cancelSubscription(String subscriptionId, String authority, String privateKey) {
        client.cancelSubscription(
                subscriptionId,
                SubscriptionActionRequest.builder()
                        .authority(authority)
                        .cancelMode(SubscriptionActionRequest.CancelMode.PERIOD_END)
                        .build()
        ).join();
    }

    public void keepSubscription(String subscriptionId, String authority, String privateKey) {
        client.keepSubscription(subscriptionId, actionRequest(authority, privateKey)).join();
    }

    public void chargeNowSubscription(String subscriptionId, String authority, String privateKey) {
        client.chargeSubscriptionNow(subscriptionId, actionRequest(authority, privateKey)).join();
    }

    public void recordSubscriptionUsage(
            @NonNull String authority,
            @NonNull String privateKey,
            @NonNull SubscriptionUsageInput input
    ) {
        client.recordSubscriptionUsage(
                input.subscriptionId,
                SubscriptionUsageRequest.builder()
                        .authority(authority)
                        .unitKey(input.unitKey)
                        .delta(input.delta.stripTrailingZeros().toPlainString())
                        .build()
        ).join();
    }

    private void createPlan(String authority, String privateKey, String planId, SubscriptionCreateInput input) {
        client.createSubscriptionPlan(
                SubscriptionPlanCreateRequest.builder()
                        .authority(authority)
                        .planId(planId)
                        .plan(buildPlan(authority, input))
                        .build()
        ).join();
    }

    private Map<String, Object> buildPlan(String providerAccountId, SubscriptionCreateInput input) {
        Map<String, Object> billing = new LinkedHashMap<>();
        billing.put("cadence", buildCadence(input.cadence));

        Map<String, Object> billFor = new LinkedHashMap<>();
        billFor.put("period", input.amountType == SubscriptionAmountType.FIXED ? "next_period" : "previous_period");
        billFor.put("value", null);
        billing.put("bill_for", billFor);
        billing.put("retry_backoff_ms", DEFAULT_RETRY_BACKOFF_MS);
        billing.put("max_failures", DEFAULT_MAX_FAILURES);
        billing.put("grace_ms", DEFAULT_GRACE_MS);

        Map<String, Object> pricing = new LinkedHashMap<>();
        Map<String, Object> pricingDetail = new LinkedHashMap<>();
        pricingDetail.put("asset_definition", defaultAssetDefinition);
        if (input.amountType == SubscriptionAmountType.FIXED) {
            pricing.put("kind", "fixed");
            pricingDetail.put("amount", toNumeric(input.amount));
        } else {
            pricing.put("kind", "usage");
            pricingDetail.put("unit_price", toNumeric(input.unitPrice));
            pricingDetail.put("unit_key", input.unitKey);
        }
        pricing.put("detail", pricingDetail);

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("provider", providerAccountId);
        plan.put("billing", billing);
        plan.put("pricing", pricing);
        return plan;
    }

    private static Map<String, Object> buildCadence(SubscriptionCadence cadence) {
        Map<String, Object> cadenceValue = new LinkedHashMap<>();
        Map<String, Object> detail = new LinkedHashMap<>();
        if (cadence == SubscriptionCadence.MONTHLY) {
            cadenceValue.put("kind", "monthly_calendar");
            detail.put("anchor_day", 1L);
            detail.put("anchor_time_ms", 0L);
        } else {
            cadenceValue.put("kind", "fixed_period");
            detail.put("period_ms", cadencePeriodMs(cadence));
        }
        cadenceValue.put("detail", detail);
        return cadenceValue;
    }

    private static long cadencePeriodMs(SubscriptionCadence cadence) {
        if (cadence == SubscriptionCadence.YEARLY) {
            return 31_536_000_000L;
        }
        if (cadence == SubscriptionCadence.QUARTERLY) {
            return 7_776_000_000L;
        }
        return 2_592_000_000L;
    }

    private static String toNumeric(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Numeric plan field is required");
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static boolean isCollision(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.US);
                if (normalized.contains("status 409")
                        || normalized.contains("conflict")
                        || normalized.contains("already exists")
                        || normalized.contains("duplicate")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static SubscriptionActionRequest actionRequest(String authority, String privateKey) {
        return SubscriptionActionRequest.builder()
                .authority(authority)
                .build();
    }

    private static final class CreateIds {
        private final String planId;
        private final String subscriptionId;

        private CreateIds(String planId, String subscriptionId) {
            this.planId = planId;
            this.subscriptionId = subscriptionId;
        }

        private static CreateIds generate(String domain) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String planId = "plan_" + suffix + "#" + domain;
            String subscriptionId = "sub_" + suffix + "$subscriptions";
            return new CreateIds(planId, subscriptionId);
        }
    }
}
