package io.soramitsu.examplepoint.offline;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.data.ToriiConfig;
import io.soramitsu.examplepoint.sdk.AccountIdCodec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import io.soramitsu.examplepoint.offline.DeadlineKind;
import io.soramitsu.examplepoint.offline.OfflinePlatformSnapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OfflineRepositoryNetworkTest {

    private MockWebServer server;

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void syncAllowancesHitsExpectedPath() throws Exception {
        server = new MockWebServer();
        server.start();
        String accountId = accountId(0x31);
        server.enqueue(new MockResponse()
                .setBody("{\"total\":1,\"items\":[{\"certificate_id_hex\":\"c1\",\"controller_id\":\"" + accountId + "\",\"controller_display\":\"Alice\",\"asset_id\":\"asset#wonderland\",\"asset_definition_id\":\"asset\",\"asset_definition_name\":\"Asset\",\"asset_definition_alias\":null,\"registered_at_ms\":1,\"expires_at_ms\":2,\"policy_expires_at_ms\":3,\"refresh_at_ms\":4,\"verdict_id_hex\":\"deadbeef\",\"attestation_nonce_hex\":\"cafebabe\",\"remaining_amount\":\"5\",\"record\":{}}]}")
                .setHeader("Content-Type", "application/json"));

        HttpUrl base = server.url("/");
        ToriiConfig config = new ToriiConfig(
                base,
                "chain",
                "wonderland",
                "asset#wonderland",
                753,
                null,
                null);
        InMemoryStateStore store = new InMemoryStateStore();
        OfflineRepository repo = new OfflineRepository(config, new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .build(), store);
        AccountProfile profile = new AccountProfile(
                accountId,
                "wonderland",
                "Alice",
                "alias",
                "asset#wonderland",
                null);

        OfflineAllowanceSnapshot snapshot = repo.syncAllowances(profile).get(5, TimeUnit.SECONDS);
        assertNotNull(snapshot);
        assertEquals("5", snapshot.getTotalRemaining().toPlainString());
        assertEquals(3L, snapshot.getNextPolicyExpiryMs());
        assertEquals(Long.valueOf(4L), snapshot.getNextRefreshMs());
        assertEquals(2L, snapshot.getNextCertificateExpiryMs());
        assertEquals(Long.valueOf(2L), snapshot.getEarliestDeadlineMs());
        assertEquals(DeadlineKind.CERTIFICATE, snapshot.getDeadlineKind());
        assertNull(snapshot.getDeadlineState());
        assertNull(snapshot.getDeadlineMsRemaining());
        assertEquals("deadbeef", snapshot.getPrimaryVerdictIdHex());
        assertEquals("cafebabe", snapshot.getPrimaryAttestationNonceHex());
        String path = server.takeRequest().getPath();
        assertNotNull(path);
        assertTrue(path.startsWith("/v1/offline/allowances"));
        assertFalse(path.contains("address_format="));
        assertTrue(path.contains("filter=controller%3D%3D" + accountId));
    }

    @Test
    public void fetchesLatestPlatformSnapshot() throws Exception {
        server = new MockWebServer();
        server.start();
        String accountId = accountId(0x41);
        String receiverId = accountId(0x51);
        server.enqueue(new MockResponse()
                .setBody("{\"total\":1,\"items\":[{\"bundle_id_hex\":\"b1\",\"receiver_id\":\"" + receiverId + "\",\"receiver_display\":\"Bob\",\"deposit_account_id\":\"" + accountId + "\",\"deposit_account_display\":\"Alice\",\"asset_id\":\"asset#wonderland\",\"receipt_count\":1,\"total_amount\":\"10\",\"claimed_delta\":\"10\",\"platform_policy\":\"play_integrity\",\"platform_token_snapshot\":{\"policy\":\"play_integrity\",\"attestation_jws_b64\":\"token\"},\"transfer\":{}}]}")
                .setHeader("Content-Type", "application/json"));

        HttpUrl base = server.url("/");
        ToriiConfig config = new ToriiConfig(
                base,
                "chain",
                "wonderland",
                "asset#wonderland",
                753,
                null,
                null);
        OfflineRepository repo = new OfflineRepository(config, new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .build(), new InMemoryStateStore());
        AccountProfile profile = new AccountProfile(
                accountId,
                "wonderland",
                "Alice",
                "alias",
                "asset#wonderland",
                null);

        OfflinePlatformSnapshot snapshot = repo.fetchLatestPlatformSnapshot(profile).get(5, TimeUnit.SECONDS);
        assertNotNull(snapshot);
        assertEquals("play_integrity", snapshot.getPolicy());
        assertEquals("token", snapshot.getAttestationJwsB64());
        assertEquals("b1", snapshot.getBundleIdHex());
        String path = server.takeRequest().getPath();
        assertNotNull(path);
        assertTrue(path.startsWith("/v1/offline/transfers"));
        assertTrue(path.contains("filter=controller%3D%3D" + accountId));
        assertFalse(path.contains("controller_id="));
        assertTrue(path.contains("sort=recorded_at_ms%3Adesc"));
        assertTrue(path.contains("limit=1"));
    }

    private static String accountId(int startByte) throws Exception {
        byte[] publicKey = new byte[32];
        for (int i = 0; i < publicKey.length; i++) {
            publicKey[i] = (byte) (startByte + i);
        }
        return AccountIdCodec.encodeDomainlessAccount(publicKey, "ed25519", 753);
    }

    private static final class InMemoryStateStore implements OfflineStateStoreGateway {
        private OfflineState state = OfflineState.empty();

        @Override
        public OfflineState load() {
            return state;
        }

        @Override
        public void save(OfflineState state) {
            this.state = state;
        }
    }
}
