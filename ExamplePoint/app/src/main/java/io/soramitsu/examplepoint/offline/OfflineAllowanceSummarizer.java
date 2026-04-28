package io.soramitsu.examplepoint.offline;

import java.math.BigDecimal;
import java.util.List;

import org.hyperledger.iroha.android.offline.OfflineAllowanceList;

final class OfflineAllowanceSummarizer {

    private OfflineAllowanceSummarizer() {}

    static OfflineAllowanceSummary summarize(List<OfflineAllowanceList.OfflineAllowanceItem> allowances) {
        BigDecimal total = BigDecimal.ZERO;
        long minPolicy = Long.MAX_VALUE;
        Long minRefresh = null;
        long minCertificate = Long.MAX_VALUE;
        String primaryVerdict = null;
        String primaryNonce = null;
        String primaryPolicy = null;
        String deadlineState = null;
        Long deadlineMsRemaining = null;
        for (OfflineAllowanceList.OfflineAllowanceItem item : allowances) {
            if (item.remainingAmount() != null) {
                try {
                    total = total.add(new BigDecimal(item.remainingAmount()));
                } catch (NumberFormatException ignored) {
                    // ignore malformed entries but continue aggregation
                }
            }
            if (item.policyExpiresAtMs() > 0 && item.policyExpiresAtMs() < minPolicy) {
                minPolicy = item.policyExpiresAtMs();
            }
            Long refreshAt = item.refreshAtMs();
            if (refreshAt != null && refreshAt > 0 && (minRefresh == null || refreshAt < minRefresh)) {
                minRefresh = refreshAt;
            }
            if (item.certificateExpiresAtMs() > 0 && item.certificateExpiresAtMs() < minCertificate) {
                minCertificate = item.certificateExpiresAtMs();
            }
            if (primaryVerdict == null && item.verdictIdHex() != null && !item.verdictIdHex().isBlank()) {
                primaryVerdict = item.verdictIdHex();
            }
            if (primaryNonce == null && item.attestationNonceHex() != null && !item.attestationNonceHex().isBlank()) {
                primaryNonce = item.attestationNonceHex();
            }
            if (primaryPolicy == null) {
                primaryPolicy = extractPolicy(item);
            }
            if (deadlineState == null || deadlineMsRemaining == null) {
                DeadlineFields fields = extractDeadlineFields(item);
                if (fields != null) {
                    deadlineState = fields.state;
                    deadlineMsRemaining = fields.msRemaining;
                }
            }
        }
        long earliestCertificate = minCertificate == Long.MAX_VALUE ? -1L : minCertificate;
        DeadlineKind kind = null;
        Long earliestDeadline = null;
        if (minRefresh != null && minRefresh > 0) {
            earliestDeadline = minRefresh;
            kind = DeadlineKind.REFRESH;
        }
        if (minPolicy != Long.MAX_VALUE && (earliestDeadline == null || minPolicy < earliestDeadline)) {
            earliestDeadline = minPolicy;
            kind = DeadlineKind.POLICY;
        }
        if (earliestCertificate > 0 && (earliestDeadline == null || earliestCertificate < earliestDeadline)) {
            earliestDeadline = earliestCertificate;
            kind = DeadlineKind.CERTIFICATE;
        }
        return new OfflineAllowanceSummary(
                total,
                minPolicy == Long.MAX_VALUE ? -1L : minPolicy,
                minRefresh,
                earliestCertificate,
                earliestDeadline,
                kind,
                allowances.size(),
                primaryVerdict,
                primaryNonce,
                primaryPolicy,
                deadlineState,
                deadlineMsRemaining
        );
    }

    private static String extractPolicy(OfflineAllowanceList.OfflineAllowanceItem allowance) {
        try {
            Object certificate = allowance.recordAsMap().get("certificate");
            if (!(certificate instanceof java.util.Map<?, ?> certificateMap)) {
                return null;
            }
            Object metadata = certificateMap.get("metadata");
            if (!(metadata instanceof java.util.Map<?, ?> metadataMap)) {
                return null;
            }
            Object policy = metadataMap.get("android.integrity.policy");
            if (policy == null) {
                return null;
            }
            String normalized = String.valueOf(policy).trim();
            return normalized.isEmpty() ? null : normalized.toLowerCase(java.util.Locale.ROOT);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static DeadlineFields extractDeadlineFields(OfflineAllowanceList.OfflineAllowanceItem allowance) {
        try {
            Object deadlineKind = allowance.recordAsMap().get("deadline_kind");
            Object deadlineState = allowance.recordAsMap().get("deadline_state");
            Object remaining = allowance.recordAsMap().get("deadline_ms_remaining");
            if (deadlineKind == null && deadlineState == null && remaining == null) {
                return null;
            }
            Long remainingVal = null;
            if (remaining instanceof Number n) {
                remainingVal = n.longValue();
            } else if (remaining != null) {
                try {
                    remainingVal = Long.parseLong(String.valueOf(remaining));
                } catch (NumberFormatException ignored) {
                    remainingVal = null;
                }
            }
            return new DeadlineFields(
                    deadlineState == null ? null : String.valueOf(deadlineState),
                    remainingVal);
        } catch (Exception ignored) {
            return null;
        }
    }

    private record DeadlineFields(String state, Long msRemaining) {}
}
