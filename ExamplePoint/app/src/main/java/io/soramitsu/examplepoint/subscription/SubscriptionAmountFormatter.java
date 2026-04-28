package io.soramitsu.examplepoint.subscription;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public final class SubscriptionAmountFormatter {
    private static final NumberFormat FORMATTER =
            new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private SubscriptionAmountFormatter() {
    }

    public static String label(
            SubscriptionAmountType type,
            BigDecimal amount,
            BigDecimal maxAmount,
            String unit
    ) {
        if (type == SubscriptionAmountType.VARIABLE) {
            if (maxAmount != null) {
                return "Up to " + unit + " " + format(maxAmount);
            }
            return "Usage based";
        }
        return unit + " " + format(amount);
    }

    private static String format(BigDecimal amount) {
        if (amount == null) {
            return "--";
        }
        FORMATTER.setGroupingUsed(true);
        return FORMATTER.format(amount);
    }
}
