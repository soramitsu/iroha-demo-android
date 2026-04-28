package io.soramitsu.examplepoint.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.soramitsu.examplepoint.sdk.identity.NexusIdentityManifest;

/**
 * Immutable snapshot describing the locally registered account.
 */
public final class AccountProfile {

    private final String accountId;
    private final String domain;
    private final String displayName;
    private final String keyAlias;
    private final String preferredAssetId;
    @Nullable
    private final NexusIdentityManifest identityManifest;

    public AccountProfile(
            @NonNull String accountId,
            @NonNull String domain,
            @NonNull String displayName,
            @NonNull String keyAlias,
            @Nullable String preferredAssetId,
            @Nullable NexusIdentityManifest identityManifest
    ) {
        this.accountId = accountId;
        this.domain = domain;
        this.displayName = displayName;
        this.keyAlias = keyAlias;
        this.preferredAssetId = preferredAssetId;
        this.identityManifest = identityManifest;
    }

    @NonNull
    public String getDomain() {
        return domain;
    }

    @NonNull
    public String getAccountId() {
        return accountId;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getKeyAlias() {
        return keyAlias;
    }

    @Nullable
    public String getPreferredAssetId() {
        return preferredAssetId;
    }

    @Nullable
    public NexusIdentityManifest getIdentityManifest() {
        return identityManifest;
    }

    public AccountProfile withPreferredAsset(@Nullable String assetId) {
        return new AccountProfile(
                accountId,
                domain,
                displayName,
                keyAlias,
                assetId,
                identityManifest
        );
    }
}
