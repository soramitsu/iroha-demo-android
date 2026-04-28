package io.soramitsu.examplepoint.sdk.model;

import androidx.annotation.NonNull;

/**
 * Simple value object describing an account asset entry returned by Torii.
 */
public final class AccountAsset {
    private final String assetId;
    private final String quantity;

    public AccountAsset(@NonNull String assetId, @NonNull String quantity) {
        this.assetId = assetId;
        this.quantity = quantity;
    }

    @NonNull
    public String getAssetId() {
        return assetId;
    }

    @NonNull
    public String getQuantity() {
        return quantity;
    }
}
