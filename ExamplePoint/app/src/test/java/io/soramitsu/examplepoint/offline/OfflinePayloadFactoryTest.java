package io.soramitsu.examplepoint.offline;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class OfflinePayloadFactoryTest {

    @Test
    public void computeTxIdIsDeterministic() {
        String id1 = OfflinePayloadFactory.computeTxId("alice@test", "inv-1", "10.5", 3);
        String id2 = OfflinePayloadFactory.computeTxId("alice@test", "inv-1", "10.5", 3);
        assertEquals(id1, id2);
    }

    @Test
    public void computeTxIdChangesWithCounter() {
        String id1 = OfflinePayloadFactory.computeTxId("alice@test", "inv-1", "10.5", 3);
        String id2 = OfflinePayloadFactory.computeTxId("alice@test", "inv-1", "10.5", 4);
        assertNotEquals(id1, id2);
    }
}
