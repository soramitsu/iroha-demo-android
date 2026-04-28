package io.soramitsu.examplepoint.offline;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;

import io.soramitsu.examplepoint.sdk.AccountIdCodec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class OfflineWithdrawTest {

    @Test
    public void withdrawLocallyDebitsBalanceAndAdvancesCounter() throws Exception {
        String accountId = accountId();
        OfflineState start = new OfflineState(
                new BigDecimal("5"),
                2L,
                new LinkedHashSet<>(),
                Collections.emptyList());
        OfflineState updated = OfflineRepository.withdrawLocally(
                start,
                accountId,
                accountId,
                new BigDecimal("3"),
                "memo",
                10L);
        assertEquals(new BigDecimal("2"), updated.getBalance());
        assertEquals(3L, updated.getNextCounter());
        assertEquals(1, updated.getHistory().size());
    }

    @Test
    public void withdrawLocallyRejectsInsufficient() throws Exception {
        String accountId = accountId();
        OfflineState start = new OfflineState(
                new BigDecimal("1"),
                0L,
                new LinkedHashSet<>(),
                Collections.emptyList());
        assertThrows(IllegalStateException.class, () -> OfflineRepository.withdrawLocally(
                start,
                accountId,
                accountId,
                new BigDecimal("2"),
                null,
                1L));
    }

    private static String accountId() throws Exception {
        byte[] publicKey = new byte[32];
        for (int i = 0; i < publicKey.length; i++) {
            publicKey[i] = (byte) (0x41 + i);
        }
        return AccountIdCodec.encodeDomainlessAccount(publicKey, "ed25519", 753);
    }
}
