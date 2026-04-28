package io.soramitsu.examplepoint.offline;

import androidx.annotation.NonNull;

import org.hyperledger.iroha.android.offline.OfflineAllowanceList;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable view of the last allowance sync.
 */
public final class OfflineAllowanceSnapshot {

    private final List<OfflineAllowanceList.OfflineAllowanceItem> allowances;
    private final BigDecimal totalRemaining;
    private final long syncedAtMs;
    private final long nextPolicyExpiryMs;
    private final Long nextRefreshMs;
    private final long nextCertificateExpiryMs;
    private final Long earliestDeadlineMs;
    private final DeadlineKind deadlineKind;
    private final int allowanceCount;
    private final String primaryVerdictIdHex;
    private final String primaryAttestationNonceHex;
    private final String primaryPolicy;
    private final String deadlineState;
    private final Long deadlineMsRemaining;

    public OfflineAllowanceSnapshot(
            @NonNull List<OfflineAllowanceList.OfflineAllowanceItem> allowances,
            @NonNull BigDecimal totalRemaining,
            long syncedAtMs,
            long nextPolicyExpiryMs,
            Long nextRefreshMs,
            int allowanceCount,
            String primaryVerdictIdHex,
            String primaryAttestationNonceHex,
            String primaryPolicy,
            long nextCertificateExpiryMs,
            Long earliestDeadlineMs,
            DeadlineKind deadlineKind,
            String deadlineState,
            Long deadlineMsRemaining
    ) {
        this.allowances = Collections.unmodifiableList(List.copyOf(Objects.requireNonNull(allowances, "allowances")));
        this.totalRemaining = Objects.requireNonNull(totalRemaining, "totalRemaining");
        this.syncedAtMs = syncedAtMs;
        this.nextPolicyExpiryMs = nextPolicyExpiryMs;
        this.nextRefreshMs = nextRefreshMs;
        this.nextCertificateExpiryMs = nextCertificateExpiryMs;
        this.earliestDeadlineMs = earliestDeadlineMs;
        this.deadlineKind = deadlineKind;
        this.allowanceCount = allowanceCount;
        this.primaryVerdictIdHex = primaryVerdictIdHex;
        this.primaryAttestationNonceHex = primaryAttestationNonceHex;
        this.primaryPolicy = primaryPolicy;
        this.deadlineState = deadlineState;
        this.deadlineMsRemaining = deadlineMsRemaining;
    }

    public List<OfflineAllowanceList.OfflineAllowanceItem> getAllowances() {
        return allowances;
    }

    public BigDecimal getTotalRemaining() {
        return totalRemaining;
    }

    public long getSyncedAtMs() {
        return syncedAtMs;
    }

    public long getNextPolicyExpiryMs() {
        return nextPolicyExpiryMs;
    }

    public Long getNextRefreshMs() {
        return nextRefreshMs;
    }

    public long getNextCertificateExpiryMs() {
        return nextCertificateExpiryMs;
    }

    public Long getEarliestDeadlineMs() {
        return earliestDeadlineMs;
    }

    public DeadlineKind getDeadlineKind() {
        return deadlineKind;
    }

    public int getAllowanceCount() {
        return allowanceCount;
    }

    public String getPrimaryVerdictIdHex() {
        return primaryVerdictIdHex;
    }

    public String getPrimaryAttestationNonceHex() {
        return primaryAttestationNonceHex;
    }

    public String getPrimaryPolicy() {
        return primaryPolicy;
    }

    public String getDeadlineState() {
        return deadlineState;
    }

    public Long getDeadlineMsRemaining() {
        return deadlineMsRemaining;
    }
}
