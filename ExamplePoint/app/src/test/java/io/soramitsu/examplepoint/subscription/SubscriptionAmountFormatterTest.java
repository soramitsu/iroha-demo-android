package io.soramitsu.examplepoint.subscription;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class SubscriptionAmountFormatterTest {

    @Test
    public void formatsFixedAmount() {
        String label = SubscriptionAmountFormatter.label(
                SubscriptionAmountType.FIXED,
                new BigDecimal("1200"),
                null,
                "IRH"
        );
        assertEquals("IRH 1,200", label);
    }

    @Test
    public void formatsVariableAmountWithMax() {
        String label = SubscriptionAmountFormatter.label(
                SubscriptionAmountType.VARIABLE,
                null,
                new BigDecimal("9000"),
                "IRH"
        );
        assertEquals("Up to IRH 9,000", label);
    }
}
