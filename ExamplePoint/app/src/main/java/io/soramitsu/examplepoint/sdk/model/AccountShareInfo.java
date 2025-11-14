package io.soramitsu.examplepoint.sdk.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable snapshot describing how the current account should be rendered when sharing addresses.
 */
public final class AccountShareInfo {

    private final String displayName;
    private final String accountId;
    private final String accountAddressHex;
    private final String ih58Literal;
    private final String compressedLiteral;
    private final String compressedWarning;
    private final int networkPrefix;
    private final String domain;
    @Nullable
    private final String preferredAssetId;
    @Nullable
    private final String uaid;
    @Nullable
    private final String identityStatement;

    public AccountShareInfo(
            @NonNull String displayName,
            @NonNull String accountId,
            @NonNull String accountAddressHex,
            @NonNull String ih58Literal,
            @NonNull String compressedLiteral,
            @NonNull String compressedWarning,
            int networkPrefix,
            @NonNull String domain,
            @Nullable String preferredAssetId,
            @Nullable String uaid,
            @Nullable String identityStatement
    ) {
        this.displayName = displayName;
        this.accountId = accountId;
        this.accountAddressHex = accountAddressHex;
        this.ih58Literal = ih58Literal;
        this.compressedLiteral = compressedLiteral;
        this.compressedWarning = compressedWarning;
        this.networkPrefix = networkPrefix;
        this.domain = domain;
        this.preferredAssetId = preferredAssetId;
        this.uaid = uaid;
        this.identityStatement = identityStatement;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getAccountId() {
        return accountId;
    }

    @NonNull
    public String getAccountAddressHex() {
        return accountAddressHex;
    }

    @NonNull
    public String getIh58Literal() {
        return ih58Literal;
    }

    @NonNull
    public String getCompressedLiteral() {
        return compressedLiteral;
    }

    @NonNull
    public String getCompressedWarning() {
        return compressedWarning;
    }

    public int getNetworkPrefix() {
        return networkPrefix;
    }

    @NonNull
    public String getDomain() {
        return domain;
    }

    @Nullable
    public String getPreferredAssetId() {
        return preferredAssetId;
    }

    @Nullable
    public String getUaid() {
        return uaid;
    }

    @Nullable
    public String getIdentityStatement() {
        return identityStatement;
    }
}
