package io.soramitsu.examplepoint.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable snapshot describing the locally registered account.
 */
public final class AccountProfile {

    private final String accountAddressHex;
    private final String domain;
    private final String displayName;
    private final String keyAlias;
    private final String preferredAssetId;

    public AccountProfile(
            @NonNull String accountAddressHex,
            @NonNull String domain,
            @NonNull String displayName,
            @NonNull String keyAlias,
            @Nullable String preferredAssetId
    ) {
        this.accountAddressHex = accountAddressHex;
        this.domain = domain;
        this.displayName = displayName;
        this.keyAlias = keyAlias;
        this.preferredAssetId = preferredAssetId;
    }

    @NonNull
    public String getAccountAddressHex() {
        return accountAddressHex;
    }

    @NonNull
    public String getDomain() {
        return domain;
    }

    @NonNull
    public String getAccountId() {
        return accountAddressHex + "@" + domain;
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

    public AccountProfile withPreferredAsset(@Nullable String assetId) {
        return new AccountProfile(accountAddressHex, domain, displayName, keyAlias, assetId);
    }
}
