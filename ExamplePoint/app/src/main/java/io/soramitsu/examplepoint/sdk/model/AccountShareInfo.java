package io.soramitsu.examplepoint.sdk.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable snapshot describing how the current account should be rendered when sharing addresses.
 */
public final class AccountShareInfo {

    private final String displayName;
    private final String accountId;
    private final String domain;
    @Nullable
    private final String preferredAssetId;
    @Nullable
    private final String identityStatement;

    public AccountShareInfo(
            @NonNull String displayName,
            @NonNull String accountId,
            @NonNull String domain,
            @Nullable String preferredAssetId,
            @Nullable String identityStatement
    ) {
        this.displayName = displayName;
        this.accountId = accountId;
        this.domain = domain;
        this.preferredAssetId = preferredAssetId;
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
    public String getDomain() {
        return domain;
    }

    @Nullable
    public String getPreferredAssetId() {
        return preferredAssetId;
    }

    @Nullable
    public String getIdentityStatement() {
        return identityStatement;
    }
}
