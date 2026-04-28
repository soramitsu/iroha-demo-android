package io.soramitsu.examplepoint.offline;

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Lightweight view of the platform token snapshot returned by Torii.
 */
public final class OfflinePlatformSnapshot {
    private final String bundleIdHex;
    private final String policy;
    private final String attestationJwsB64;

    public OfflinePlatformSnapshot(String bundleIdHex,
                                   String policy,
                                   @Nullable String attestationJwsB64) {
        this.bundleIdHex = Objects.requireNonNull(bundleIdHex, "bundleIdHex");
        this.policy = policy;
        this.attestationJwsB64 = attestationJwsB64;
    }

    public String getBundleIdHex() {
        return bundleIdHex;
    }

    @Nullable
    public String getPolicy() {
        return policy;
    }

    @Nullable
    public String getAttestationJwsB64() {
        return attestationJwsB64;
    }

    public boolean hasAttestation() {
        return attestationJwsB64 != null && !attestationJwsB64.isBlank();
    }
}
