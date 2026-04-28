package io.soramitsu.examplepoint.offline;

import androidx.annotation.Nullable;

import java.math.BigDecimal;

/**
 * Aggregated view over allowance list for UI.
 */
public final class OfflineAllowanceSummary {
    private final BigDecimal totalRemaining;
    private final long earliestPolicyExpiryMs;
    private final Long earliestRefreshMs;
    private final long earliestCertificateExpiryMs;
    private final Long earliestDeadlineMs;
    private final DeadlineKind deadlineKind;
    private final int allowanceCount;
    private final String primaryVerdictIdHex;
    private final String primaryAttestationNonceHex;
    private final String primaryPolicy;
    private final String deadlineState;
    private final Long deadlineMsRemaining;

    public OfflineAllowanceSummary(
            BigDecimal totalRemaining,
            long earliestPolicyExpiryMs,
            Long earliestRefreshMs,
            long earliestCertificateExpiryMs,
            Long earliestDeadlineMs,
            DeadlineKind deadlineKind,
            int allowanceCount,
            @Nullable String primaryVerdictIdHex,
            @Nullable String primaryAttestationNonceHex,
            @Nullable String primaryPolicy,
            @Nullable String deadlineState,
            @Nullable Long deadlineMsRemaining
    ) {
        this.totalRemaining = totalRemaining;
        this.earliestPolicyExpiryMs = earliestPolicyExpiryMs;
        this.earliestRefreshMs = earliestRefreshMs;
        this.earliestCertificateExpiryMs = earliestCertificateExpiryMs;
        this.earliestDeadlineMs = earliestDeadlineMs;
        this.deadlineKind = deadlineKind;
        this.allowanceCount = allowanceCount;
        this.primaryVerdictIdHex = primaryVerdictIdHex;
        this.primaryAttestationNonceHex = primaryAttestationNonceHex;
        this.primaryPolicy = primaryPolicy;
        this.deadlineState = deadlineState;
        this.deadlineMsRemaining = deadlineMsRemaining;
    }

    public BigDecimal getTotalRemaining() {
        return totalRemaining;
    }

    public long getEarliestPolicyExpiryMs() {
        return earliestPolicyExpiryMs;
    }

    public Long getEarliestRefreshMs() {
        return earliestRefreshMs;
    }

    public long getEarliestCertificateExpiryMs() {
        return earliestCertificateExpiryMs;
    }

    @Nullable
    public Long getEarliestDeadlineMs() {
        return earliestDeadlineMs;
    }

    @Nullable
    public DeadlineKind getDeadlineKind() {
        return deadlineKind;
    }

    public int getAllowanceCount() {
        return allowanceCount;
    }

    @Nullable
    public String getPrimaryVerdictIdHex() {
        return primaryVerdictIdHex;
    }

    @Nullable
    public String getPrimaryAttestationNonceHex() {
        return primaryAttestationNonceHex;
    }

    @Nullable
    public String getPrimaryPolicy() {
        return primaryPolicy;
    }

    @Nullable
    public String getDeadlineState() {
        return deadlineState;
    }

    @Nullable
    public Long getDeadlineMsRemaining() {
        return deadlineMsRemaining;
    }
}
