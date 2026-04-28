package io.soramitsu.examplepoint.sdk.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable representation of the UAID portfolio response returned by Torii.
 */
public final class UaidPortfolio {

    private final String uaid;
    private final Totals totals;
    private final List<Dataspace> dataspaces;

    public UaidPortfolio(@NonNull String uaid,
                         @NonNull Totals totals,
                         @NonNull List<Dataspace> dataspaces) {
        this.uaid = uaid;
        this.totals = totals;
        this.dataspaces = Collections.unmodifiableList(new ArrayList<>(dataspaces));
    }

    @NonNull
    public String getUaid() {
        return uaid;
    }

    @NonNull
    public Totals getTotals() {
        return totals;
    }

    @NonNull
    public List<Dataspace> getDataspaces() {
        return dataspaces;
    }

    public static final class Totals {
        private final int accounts;
        private final int positions;

        public Totals(int accounts, int positions) {
            this.accounts = accounts;
            this.positions = positions;
        }

        public int getAccounts() {
            return accounts;
        }

        public int getPositions() {
            return positions;
        }
    }

    public static final class Dataspace {
        private final long dataspaceId;
        @Nullable
        private final String dataspaceAlias;
        private final List<Account> accounts;

        public Dataspace(long dataspaceId,
                         @Nullable String dataspaceAlias,
                         @NonNull List<Account> accounts) {
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
        public List<Account> getAccounts() {
            return accounts;
        }
    }

    public static final class Account {
        private final String accountId;
        @Nullable
        private final String label;
        private final List<Asset> assets;

        public Account(@NonNull String accountId,
                       @Nullable String label,
                       @NonNull List<Asset> assets) {
            this.accountId = accountId;
            this.label = label;
            this.assets = Collections.unmodifiableList(new ArrayList<>(assets));
        }

        @NonNull
        public String getAccountId() {
            return accountId;
        }

        @Nullable
        public String getLabel() {
            return label;
        }

        @NonNull
        public List<Asset> getAssets() {
            return assets;
        }
    }

    public static final class Asset {
        private final String assetId;
        private final String assetDefinitionId;
        private final String quantity;

        public Asset(@NonNull String assetId,
                     @NonNull String assetDefinitionId,
                     @NonNull String quantity) {
            this.assetId = assetId;
            this.assetDefinitionId = assetDefinitionId;
            this.quantity = quantity;
        }

        @NonNull
        public String getAssetId() {
            return assetId;
        }

        @NonNull
        public String getAssetDefinitionId() {
            return assetDefinitionId;
        }

        @NonNull
        public String getQuantity() {
            return quantity;
        }
    }
}
