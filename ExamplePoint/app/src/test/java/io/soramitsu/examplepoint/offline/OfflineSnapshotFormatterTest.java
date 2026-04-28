package io.soramitsu.examplepoint.offline;

import org.json.JSONObject;
import org.junit.Test;

import io.soramitsu.examplepoint.sdk.AccountIdCodec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OfflineSnapshotFormatterTest {

    @Test
    public void formatsSnapshotPayload() throws Exception {
        String accountId = accountId();
        OfflinePlatformSnapshot snapshot = new OfflinePlatformSnapshot(
                "bundle1",
                "play_integrity",
                "token");
        String json = OfflineSnapshotFormatter.formatSharePayload(snapshot, accountId);
        try {
            JSONObject parsed = new JSONObject(json);
            assertEquals("bundle1", parsed.getString("bundle_id_hex"));
            assertEquals("play_integrity", parsed.getString("policy"));
            assertEquals("token", parsed.getString("attestation_jws_b64"));
            assertEquals(accountId, parsed.getString("account_id"));
            assertTrue(json.contains("bundle_id_hex"));
        } catch (Exception e) {
            throw new AssertionError("JSON parse failed", e);
        }
    }

    private static String accountId() throws Exception {
        byte[] publicKey = new byte[32];
        for (int i = 0; i < publicKey.length; i++) {
            publicKey[i] = (byte) (0x31 + i);
        }
        return AccountIdCodec.encodeDomainlessAccount(publicKey, "ed25519", 753);
    }
}
