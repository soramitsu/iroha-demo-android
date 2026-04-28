package io.soramitsu.examplepoint.subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Computes projected usage charges for client-side cap checks.
 */
public final class SubscriptionUsageCapPolicy {

    private static final int MONEY_SCALE = 12;

    private SubscriptionUsageCapPolicy() {
    }

    public static BigDecimal projectedCharge(
            BigDecimal accumulatedUnits,
            BigDecimal additionalUnits,
            BigDecimal unitPrice
    ) {
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice is required");
        }
        BigDecimal current = accumulatedUnits == null ? BigDecimal.ZERO : accumulatedUnits;
        BigDecimal delta = additionalUnits == null ? BigDecimal.ZERO : additionalUnits;
        if (delta.signum() < 0) {
            throw new IllegalArgumentException("additionalUnits must be non-negative");
        }
        BigDecimal totalUnits = current.add(delta);
        return totalUnits.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static boolean exceedsCap(
            BigDecimal accumulatedUnits,
            BigDecimal additionalUnits,
            BigDecimal unitPrice,
            BigDecimal cap
    ) {
        if (cap == null) {
            return false;
        }
        return projectedCharge(accumulatedUnits, additionalUnits, unitPrice).compareTo(cap) > 0;
    }
}
