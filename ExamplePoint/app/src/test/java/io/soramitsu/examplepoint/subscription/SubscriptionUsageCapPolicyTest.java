package io.soramitsu.examplepoint.subscription;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubscriptionUsageCapPolicyTest {

    @Test
    public void projectedChargeSupportsDecimalInputs() {
        BigDecimal projected = SubscriptionUsageCapPolicy.projectedCharge(
                new BigDecimal("12.5"),
                new BigDecimal("3.25"),
                new BigDecimal("0.075")
        );

        assertEquals(new BigDecimal("1.181250000000"), projected);
    }

    @Test
    public void detectsWhenProjectedChargeExceedsCap() {
        boolean exceeds = SubscriptionUsageCapPolicy.exceedsCap(
                new BigDecimal("10"),
                new BigDecimal("2.5"),
                new BigDecimal("1.20"),
                new BigDecimal("14.90")
        );

        assertTrue(exceeds);
    }

    @Test
    public void allowsUsageWhenProjectedChargeIsWithinCap() {
        boolean exceeds = SubscriptionUsageCapPolicy.exceedsCap(
                new BigDecimal("10"),
                new BigDecimal("2"),
                new BigDecimal("1.20"),
                new BigDecimal("14.40")
        );

        assertFalse(exceeds);
    }
}
