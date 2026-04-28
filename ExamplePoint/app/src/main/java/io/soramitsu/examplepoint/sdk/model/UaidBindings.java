package io.soramitsu.examplepoint.sdk.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable representation of the UAID bindings snapshot returned by Torii.
 */
public final class UaidBindings {

    private final String uaid;
    private final List<DataspaceBinding> dataspaces;

    public UaidBindings(@NonNull String uaid, @NonNull List<DataspaceBinding> dataspaces) {
        this.uaid = uaid;
        this.dataspaces = Collections.unmodifiableList(new ArrayList<>(dataspaces));
    }

    @NonNull
    public String getUaid() {
        return uaid;
    }

    @NonNull
    public List<DataspaceBinding> getDataspaces() {
        return dataspaces;
    }

    public static final class DataspaceBinding {
        private final long dataspaceId;
        @Nullable
        private final String dataspaceAlias;
        private final List<String> accounts;

        public DataspaceBinding(long dataspaceId,
                                @Nullable String dataspaceAlias,
                                @NonNull List<String> accounts) {
            this.dataspaceId = dataspaceId;
            this.dataspaceAlias = dataspaceAlias;
            this.accounts = Collections.unmodifiableList(new ArrayList<>(accounts));
        }

        public long getDataspaceId() {
            return dataspaceId;
        }

        @Nullable
        public String getDataspaceAlias() {
            return dataspaceAlias;
        }

        @NonNull
        public List<String> getAccounts() {
            return accounts;
        }
    }
}
