package io.soramitsu.examplepoint.sdk.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;

import org.junit.Test;

public class UaidManifestInventoryTest {

    @Test
    public void manifestRecordRetainsManifestJson() {
        UaidManifestInventory.Revocation revocation = new UaidManifestInventory.Revocation(10L, "expired");
        UaidManifestInventory.Lifecycle lifecycle = new UaidManifestInventory.Lifecycle(1L, 2L, revocation);
        UaidManifestInventory.ManifestRecord record = new UaidManifestInventory.ManifestRecord(
                42L,
                "dataspace-x",
                "manifest-hash",
                "active",
                lifecycle,
                Arrays.asList("acct1", "acct2"),
                "{\"manifest\":\"payload\"}"
        );

        assertEquals("manifest-hash", record.getManifestHash());
        assertEquals("active", record.getStatus());
        assertSame(lifecycle, record.getLifecycle());
        assertEquals("{\"manifest\":\"payload\"}", record.getManifestJson());
    }
}
