package io.soramitsu.examplepoint.subscription;

import org.hyperledger.iroha.android.subscriptions.SubscriptionListResponse;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SubscriptionMapperTest {

    @Test
    public void mapsBackendStatusesToUiStatuses() {
        assertEquals(SubscriptionStatus.ACTIVE, mapStatus("active"));
        assertEquals(SubscriptionStatus.PAUSED, mapStatus("paused"));
        assertEquals(SubscriptionStatus.PAST_DUE, mapStatus("past_due"));
        assertEquals(SubscriptionStatus.CANCELED, mapStatus("canceled"));
        assertEquals(SubscriptionStatus.SUSPENDED, mapStatus("suspended"));
    }

    private static SubscriptionStatus mapStatus(String status) {
        Map<String, Object> subscription = new LinkedHashMap<>();
        Map<String, Object> statusNode = new LinkedHashMap<>();
        statusNode.put("status", status);
        statusNode.put("value", null);
        subscription.put("status", statusNode);
        subscription.put("next_charge_ms", 1L);
        subscription.put("plan_id", "plan_test#wonderland");

        Map<String, Object> pricingDetail = new LinkedHashMap<>();
        pricingDetail.put("amount", "10");
        pricingDetail.put("asset_definition", "point#wonderland");

        Map<String, Object> pricing = new LinkedHashMap<>();
        pricing.put("kind", "fixed");
        pricing.put("detail", pricingDetail);

        Map<String, Object> cadenceDetail = new LinkedHashMap<>();
        cadenceDetail.put("anchor_day", 1L);
        cadenceDetail.put("anchor_time_ms", 0L);

        Map<String, Object> cadence = new LinkedHashMap<>();
        cadence.put("kind", "monthly_calendar");
        cadence.put("detail", cadenceDetail);

        Map<String, Object> billing = new LinkedHashMap<>();
        billing.put("cadence", cadence);

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("billing", billing);
        plan.put("pricing", pricing);

        SubscriptionListResponse.SubscriptionRecord source =
                new SubscriptionListResponse.SubscriptionRecord("sub_test$subscriptions", subscription, null, plan);

        return SubscriptionMapper.toRecord(source, null).status;
    }
}
