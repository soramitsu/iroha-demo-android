package io.soramitsu.examplepoint.sdk.model;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Aggregates all information required to render the receive screen.
 */
public final class AccountReceiveState {

    private final AccountShareInfo shareInfo;
    private final List<AccountAsset> assets;
    private final String qrLiteral;

    public AccountReceiveState(
            @NonNull AccountShareInfo shareInfo,
            @NonNull List<AccountAsset> assets,
            @NonNull String qrLiteral
    ) {
        this.shareInfo = shareInfo;
        this.assets = Collections.unmodifiableList(assets);
        this.qrLiteral = qrLiteral;
    }

    @NonNull
    public AccountShareInfo getShareInfo() {
        return shareInfo;
    }

    @NonNull
    public List<AccountAsset> getAssets() {
        return assets;
    }

    @NonNull
    public String getQrLiteral() {
        return qrLiteral;
    }
}
