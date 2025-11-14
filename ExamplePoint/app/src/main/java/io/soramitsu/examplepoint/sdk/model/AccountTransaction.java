package io.soramitsu.examplepoint.sdk.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents a single entry returned by Torii's account transaction history endpoints.
 */
public final class AccountTransaction {

    private final String entrypointHash;
    private final boolean success;
    @Nullable
    private final String authority;
    private final long timestampMs;
    @Nullable
    private final String errorMessage;

    public AccountTransaction(
            @NonNull String entrypointHash,
            boolean success,
            @Nullable String authority,
            long timestampMs,
            @Nullable String errorMessage
    ) {
        this.entrypointHash = entrypointHash;
        this.success = success;
        this.authority = authority;
        this.timestampMs = timestampMs;
        this.errorMessage = errorMessage;
    }

    @NonNull
    public String getEntrypointHash() {
        return entrypointHash;
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getAuthority() {
        return authority;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
}
