package io.soramitsu.examplepoint.offline;

import org.hyperledger.iroha.android.offline.OfflineAllowanceList;
import org.junit.Test;

import java.util.List;

import io.soramitsu.examplepoint.sdk.AccountIdCodec;

import static org.junit.Assert.assertEquals;

public class OfflineAllowanceSummarizerTest {

    @Test
    public void extractsPolicyAndDeadlineKind() throws Exception {
        String controllerId = controllerId();
        String recordJson = "{\"certificate\":{\"metadata\":{\"android.integrity.policy\":\"play_integrity\"}}}";
        OfflineAllowanceList.OfflineAllowanceItem allowance = allowance(
                "c1",
                controllerId,
                1L,
                500L,
                200L,
                150L,
                "verdict1",
                "nonce1",
                "10",
                recordJson);
        OfflineAllowanceSummary summary = OfflineAllowanceSummarizer.summarize(List.of(allowance));
        assertEquals("play_integrity", summary.getPrimaryPolicy());
        assertEquals("verdict1", summary.getPrimaryVerdictIdHex());
        assertEquals("nonce1", summary.getPrimaryAttestationNonceHex());
        assertEquals(Long.valueOf(150L), summary.getEarliestDeadlineMs());
        assertEquals(DeadlineKind.REFRESH, summary.getDeadlineKind());
        assertEquals(500L, summary.getEarliestCertificateExpiryMs());
    }

    @Test
    public void prefersEarliestDeadlineAcrossAllowances() throws Exception {
        String controllerId = controllerId();
        String recordJson = "{\"certificate\":{\"metadata\":{\"android.integrity.policy\":\"hms_safety_detect\"}}}";
        OfflineAllowanceList.OfflineAllowanceItem first = allowance(
                "c1",
                controllerId,
                1L,
                500L,
                100L,
                null,
                "verdict1",
                "nonce1",
                "5",
                "{}");
        OfflineAllowanceList.OfflineAllowanceItem second = allowance(
                "c2",
                controllerId,
                1L,
                180L,
                80L,
                70L,
                "verdict2",
                null,
                "3",
                recordJson);
        OfflineAllowanceSummary summary = OfflineAllowanceSummarizer.summarize(List.of(first, second));
        assertEquals(Long.valueOf(70L), summary.getEarliestDeadlineMs());
        assertEquals(DeadlineKind.REFRESH, summary.getDeadlineKind());
        assertEquals("verdict1", summary.getPrimaryVerdictIdHex());
        assertEquals("nonce1", summary.getPrimaryAttestationNonceHex());
        assertEquals("hms_safety_detect", summary.getPrimaryPolicy());
    }

    private static String controllerId() throws Exception {
        byte[] publicKey = new byte[32];
        for (int i = 0; i < publicKey.length; i++) {
            publicKey[i] = (byte) (0x11 + i);
        }
        return AccountIdCodec.encodeDomainlessAccount(publicKey, "ed25519", 753);
    }

    private static OfflineAllowanceList.OfflineAllowanceItem allowance(
            String certificateIdHex,
            String controllerId,
            long registeredAtMs,
            long certificateExpiresAtMs,
            long policyExpiresAtMs,
            Long refreshAtMs,
            String verdictIdHex,
            String attestationNonceHex,
            String remainingAmount,
            String recordJson
    ) {
        return new OfflineAllowanceList.OfflineAllowanceItem(
                certificateIdHex,
                controllerId,
                "Ctrl",
                "asset#wonderland",
                "asset",
                "Asset",
                "asset_alias",
                registeredAtMs,
                certificateExpiresAtMs,
                policyExpiresAtMs,
                refreshAtMs,
                verdictIdHex,
                attestationNonceHex,
                remainingAmount,
                recordJson
        );
    }
}
