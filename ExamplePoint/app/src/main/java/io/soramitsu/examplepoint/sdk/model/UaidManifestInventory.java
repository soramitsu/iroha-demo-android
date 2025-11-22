package io.soramitsu.examplepoint.sdk.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable representation of the UAID manifest inventory returned by Torii.
 */
public final class UaidManifestInventory {

    private final String uaid;
    private final List<ManifestRecord> manifests;

    public UaidManifestInventory(@NonNull String uaid,
                                 @NonNull List<ManifestRecord> manifests) {
        this.uaid = uaid;
        this.manifests = Collections.unmodifiableList(new ArrayList<>(manifests));
    }

    @NonNull
    public String getUaid() {
        return uaid;
    }

    @NonNull
    public List<ManifestRecord> getManifests() {
        return manifests;
    }

    public static final class ManifestRecord {
        private final long dataspaceId;
        @Nullable
        private final String dataspaceAlias;
        private final String manifestHash;
        private final String status;
        private final Lifecycle lifecycle;
        private final List<String> accounts;
        private final String manifestJson;

        public ManifestRecord(long dataspaceId,
                              @Nullable String dataspaceAlias,
                              @NonNull String manifestHash,
                              @NonNull String status,
                              @NonNull Lifecycle lifecycle,
                              @NonNull List<String> accounts,
                              @NonNull String manifestJson) {
            this.dataspaceId = dataspaceId;
            this.dataspaceAlias = dataspaceAlias;
            this.manifestHash = manifestHash;
            this.status = status;
            this.lifecycle = lifecycle;
            this.accounts = Collections.unmodifiableList(new ArrayList<>(accounts));
            this.manifestJson = manifestJson;
        }

        public long getDataspaceId() {
            return dataspaceId;
        }

        @Nullable
        public String getDataspaceAlias() {
            return dataspaceAlias;
        }

        @NonNull
        public String getManifestHash() {
            return manifestHash;
        }

        @NonNull
        public String getStatus() {
            return status;
        }

        @NonNull
        public Lifecycle getLifecycle() {
            return lifecycle;
        }

        @NonNull
        public List<String> getAccounts() {
            return accounts;
        }

        @NonNull
        public String getManifestJson() {
            return manifestJson;
        }
    }

    public static final class Lifecycle {
        private final Long activatedEpoch;
        private final Long expiredEpoch;
        private final Revocation revocation;

        public Lifecycle(@Nullable Long activatedEpoch,
                         @Nullable Long expiredEpoch,
                         @Nullable Revocation revocation) {
            this.activatedEpoch = activatedEpoch;
            this.expiredEpoch = expiredEpoch;
            this.revocation = revocation;
        }

        @Nullable
        public Long getActivatedEpoch() {
            return activatedEpoch;
        }

        @Nullable
        public Long getExpiredEpoch() {
            return expiredEpoch;
        }

        @Nullable
        public Revocation getRevocation() {
            return revocation;
        }
    }

    public static final class Revocation {
        private final Long revokedEpoch;
        private final String reason;

        public Revocation(@Nullable Long revokedEpoch, @Nullable String reason) {
            this.revokedEpoch = revokedEpoch;
            this.reason = reason;
        }

        @Nullable
        public Long getRevokedEpoch() {
            return revokedEpoch;
        }

        @Nullable
        public String getReason() {
            return reason;
        }
    }
}
