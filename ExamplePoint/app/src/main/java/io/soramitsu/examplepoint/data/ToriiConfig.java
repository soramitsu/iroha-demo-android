package io.soramitsu.examplepoint.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import okhttp3.HttpUrl;

import io.soramitsu.examplepoint.BuildConfig;

/**
 * Aggregates compile-time Torii configuration values.
 */
public final class ToriiConfig {

    private final HttpUrl baseUrl;
    private final String chainId;
    private final String domain;
    private final String defaultAssetId;
    private final int ih58Prefix;
    @Nullable
    private final String adminAccountId;
    @Nullable
    private final byte[] adminPrivateKey;

    public ToriiConfig(
            @NonNull HttpUrl baseUrl,
            @NonNull String chainId,
            @NonNull String domain,
            @NonNull String defaultAssetId,
            int ih58Prefix,
            @Nullable String adminAccountId,
            @Nullable byte[] adminPrivateKey
    ) {
        this.baseUrl = baseUrl;
        this.chainId = chainId;
        this.domain = domain;
        this.defaultAssetId = defaultAssetId;
        this.ih58Prefix = ih58Prefix;
        this.adminAccountId = adminAccountId;
        this.adminPrivateKey = adminPrivateKey;
    }

    @NonNull
    public HttpUrl baseUrl() {
        return baseUrl;
    }

    @NonNull
    public String chainId() {
        return chainId;
    }

    @NonNull
    public String domain() {
        return domain;
    }

    @NonNull
    public String defaultAssetId() {
        return defaultAssetId;
    }

    public int ih58Prefix() {
        return ih58Prefix;
    }

    public boolean hasAdminCredentials() {
        return adminAccountId != null
                && !adminAccountId.isEmpty()
                && adminPrivateKey != null
                && adminPrivateKey.length == 32;
    }

    @Nullable
    public String adminAccountId() {
        return adminAccountId;
    }

    @Nullable
    public byte[] adminPrivateKey() {
        return adminPrivateKey == null ? null : adminPrivateKey.clone();
    }

    public static ToriiConfig fromBuildConfig() {
        HttpUrl url = HttpUrl.parse(BuildConfig.TORII_BASE_URL);
        if (url == null) {
            throw new IllegalStateException("Invalid TORII_BASE_URL: " + BuildConfig.TORII_BASE_URL);
        }
        return new ToriiConfig(
                url,
                BuildConfig.TORII_CHAIN_ID,
                BuildConfig.TORII_DOMAIN,
                BuildConfig.TORII_DEFAULT_ASSET_ID,
                BuildConfig.TORII_IH58_PREFIX,
                emptyToNull(BuildConfig.TORII_ADMIN_ACCOUNT_ID),
                parsePrivateKey(BuildConfig.TORII_ADMIN_PRIVATE_KEY)
        );
    }

    @Nullable
    private static byte[] parsePrivateKey(String hex) {
        if (hex == null) {
            return null;
        }
        String trimmed = hex.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            trimmed = trimmed.substring(2);
        }
        if (trimmed.length() != 64) {
            throw new IllegalStateException(
                    String.format(
                            Locale.US,
                            "TORII_ADMIN_PRIVATE_KEY must be 32 bytes (64 hex chars), found %d",
                            trimmed.length()));
        }
        byte[] bytes = new byte[32];
        try {
            for (int i = 0; i < trimmed.length(); i += 2) {
                bytes[i / 2] = (byte) Integer.parseInt(trimmed.substring(i, i + 2), 16);
            }
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("TORII_ADMIN_PRIVATE_KEY is not valid hex", ex);
        }
        return bytes;
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
