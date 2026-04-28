package io.soramitsu.examplepoint.sdk;

import androidx.annotation.NonNull;

import java.util.Locale;

import io.soramitsu.examplepoint.data.ToriiConfig;

/**
 * Shared formatter for canonical I105 account identifiers and on-chain account aliases.
 */
public final class AccountLiteralFormatter {

    private AccountLiteralFormatter() {
    }

    @NonNull
    public static ParsedAccountLiteral normalize(@NonNull String raw, @NonNull ToriiConfig config) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Receiver account ID or alias is required");
        }
        if (trimmed.contains("@")) {
            return ParsedAccountLiteral.alias(normalizeAlias(trimmed));
        }
        if (!AccountIdCodec.isCanonicalAccountId(trimmed, config.i105Discriminant())) {
            throw new IllegalArgumentException(
                    "Account ID must be canonical I105, or use an alias like name@domain.dataspace or name@dataspace");
        }
        return ParsedAccountLiteral.accountId(trimmed);
    }

    @NonNull
    public static String normalizeAlias(@NonNull String raw) {
        String trimmed = raw.trim();
        if (!trimmed.equals(raw)) {
            throw new IllegalArgumentException("Account alias must not contain leading or trailing whitespace");
        }
        if (!isAscii(trimmed)) {
            throw new IllegalArgumentException("Account alias must use ASCII characters only");
        }

        String canonical = trimmed.toLowerCase(Locale.US);
        String[] atParts = canonical.split("@", -1);
        if (atParts.length != 2) {
            throw new IllegalArgumentException(
                    "Account alias must use name@domain.dataspace or name@dataspace format");
        }
        if (!isAliasSegment(atParts[0])) {
            throw new IllegalArgumentException("Account alias name segment is invalid");
        }

        String[] rightParts = atParts[1].split("\\.", -1);
        if (rightParts.length == 1) {
            if (!isAliasSegment(rightParts[0])) {
                throw new IllegalArgumentException("Account alias dataspace segment is invalid");
            }
            return canonical;
        }
        if (rightParts.length == 2) {
            if (!isAliasSegment(rightParts[0])) {
                throw new IllegalArgumentException("Account alias domain segment is invalid");
            }
            if (!isAliasSegment(rightParts[1])) {
                throw new IllegalArgumentException("Account alias dataspace segment is invalid");
            }
            return canonical;
        }
        throw new IllegalArgumentException(
                "Account alias must use name@domain.dataspace or name@dataspace format");
    }

    private static boolean isAliasSegment(@NonNull String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            boolean allowed = (current >= 'a' && current <= 'z')
                    || (current >= '0' && current <= '9')
                    || current == '-'
                    || current == '_';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAscii(@NonNull String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    public static final class ParsedAccountLiteral {
        private final String literal;
        private final LiteralType type;

        private ParsedAccountLiteral(String literal, LiteralType type) {
            this.literal = literal;
            this.type = type;
        }

        @NonNull
        public static ParsedAccountLiteral accountId(@NonNull String literal) {
            return new ParsedAccountLiteral(literal, LiteralType.ACCOUNT_ID);
        }

        @NonNull
        public static ParsedAccountLiteral alias(@NonNull String literal) {
            return new ParsedAccountLiteral(literal, LiteralType.ACCOUNT_ALIAS);
        }

        @NonNull
        public String literal() {
            return literal;
        }

        public boolean isAlias() {
            return type == LiteralType.ACCOUNT_ALIAS;
        }
    }

    private enum LiteralType {
        ACCOUNT_ID,
        ACCOUNT_ALIAS
    }
}
