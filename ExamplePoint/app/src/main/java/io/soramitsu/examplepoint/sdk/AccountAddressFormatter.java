package io.soramitsu.examplepoint.sdk;

import androidx.annotation.NonNull;

import java.util.Locale;

import io.soramitsu.examplepoint.data.ToriiConfig;

import org.hyperledger.iroha.android.address.AccountAddress;

/**
 * Utility helpers for parsing and normalizing user supplied account literals.
 */
public final class AccountAddressFormatter {

    private AccountAddressFormatter() {
    }

    /**
     * Normalizes user supplied account literals (IH58/compressed/canonical hex) into a Torii-ready
     * {@code <literal>@<domain>} string. When the literal omits a domain, the Torii default domain is
     * used.
     */
    @NonNull
    public static String normalizeAccountId(
            @NonNull String raw,
            @NonNull ToriiConfig config
    ) throws AccountAddress.AccountAddressException {
        final String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Receiver is required");
        }
        String literal = trimmed;
        String domainOverride = null;
        final int atIndex = trimmed.indexOf('@');
        if (atIndex >= 0) {
            literal = trimmed.substring(0, atIndex);
            if (atIndex + 1 < trimmed.length()) {
                domainOverride = trimmed.substring(atIndex + 1);
            }
        }

        final String domain = normalizeDomain(domainOverride, config.domain());
        final AccountAddress.ParseResult parseResult = AccountAddress.parseAny(
                literal,
                config.ih58Prefix()
        );
        final String normalizedLiteral = parseResult.address.toIH58(config.ih58Prefix());
        return normalizedLiteral + "@" + domain;
    }

    @NonNull
    private static String normalizeDomain(String candidate, String fallback) {
        final String base = (candidate == null || candidate.trim().isEmpty())
                ? fallback
                : candidate.trim();
        return base.toLowerCase(Locale.US);
    }
}
