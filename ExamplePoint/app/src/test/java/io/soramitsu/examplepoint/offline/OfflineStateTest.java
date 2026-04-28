package io.soramitsu.examplepoint.offline;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OfflineStateTest {

    @Test
    public void marksReplayAndCreditsBalance() {
        OfflineState initial = new OfflineState(
                new BigDecimal("5.0"),
                1L,
                new LinkedHashSet<>(),
                Collections.emptyList());
        OfflineTransferRecord record = new OfflineTransferRecord(
                "tx123",
                OfflineTransferRecord.Direction.INCOMING,
                "#1",
                "2",
                "peer@test",
                1000L,
                null);
        OfflineState updated = initial.markSeen("tx123").credit(new BigDecimal("2"), record);
        assertTrue(updated.hasSeen("tx123"));
        assertEquals(new BigDecimal("7.0"), updated.getBalance());
        assertEquals(1, updated.getHistory().size());
    }

    @Test
    public void debitsBalanceAndAdvancesCounter() {
        OfflineState initial = new OfflineState(
                new BigDecimal("10"),
                4L,
                new LinkedHashSet<>(),
                List.of());
        OfflineTransferRecord record = new OfflineTransferRecord(
                "tx-out",
                OfflineTransferRecord.Direction.OUTGOING,
                "#4",
                "3",
                "bob@test",
                2000L,
                null);
        OfflineState withCounter = initial.nextCounterState();
        OfflineState updated = withCounter.debit(new BigDecimal("3"), record);
        assertEquals(new BigDecimal("7"), updated.getBalance());
        assertEquals(5L, updated.getNextCounter());
        assertEquals(1, updated.getHistory().size());
        assertFalse(updated.hasSeen("tx-out"));
    }
}
