package io.soramitsu.examplepoint.offline;

import androidx.annotation.NonNull;

public interface OfflineStateStoreGateway {
    OfflineState load();

    void save(@NonNull OfflineState state);
}
