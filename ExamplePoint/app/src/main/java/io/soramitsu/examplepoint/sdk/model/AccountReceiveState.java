package io.soramitsu.examplepoint.sdk.model;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import io.soramitsu.examplepoint.network.ToriiClient;

/**
 * Aggregates all information required to render the receive screen.
 */
public final class AccountReceiveState {

    private final AccountShareInfo shareInfo;
    private final List<AccountAsset> assets;
    private final ToriiClient.ExplorerAccountQrSnapshot qrSnapshot;

    public AccountReceiveState(
            @NonNull AccountShareInfo shareInfo,
            @NonNull List<AccountAsset> assets,
            @NonNull ToriiClient.ExplorerAccountQrSnapshot qrSnapshot
    ) {
        this.shareInfo = shareInfo;
        this.assets = Collections.unmodifiableList(assets);
        this.qrSnapshot = qrSnapshot;
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
    public ToriiClient.ExplorerAccountQrSnapshot getQrSnapshot() {
        return qrSnapshot;
    }
}
