package io.soramitsu.examplepoint.network;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.security.KeyPair;

import io.soramitsu.examplepoint.data.ToriiConfig;
import io.soramitsu.examplepoint.sdk.AccountIdCodec;
import io.soramitsu.examplepoint.sdk.KeyEncodingUtils;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hyperledger.iroha.android.IrohaKeyManager;
import org.hyperledger.iroha.android.client.CanonicalRequestSigner;
import org.hyperledger.iroha.android.crypto.Signer;
import org.hyperledger.iroha.android.model.TransactionPayload;
import org.hyperledger.iroha.android.norito.NoritoJavaCodecAdapter;
import org.hyperledger.iroha.android.tx.SignedTransaction;
import org.hyperledger.iroha.android.tx.TransactionBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ToriiClientSubmitTest {

    private MockWebServer server;

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void submitTransactionUsesLatestTransactionEndpointAndContentType() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(new MockResponse().setResponseCode(202));

        ToriiClient client = new ToriiClient(config(server.url("/")));
        client.submitTransaction(sampleTransaction());

        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/transaction", request.getPath());
        assertEquals("application/x-norito", request.getHeader("Content-Type"));
        assertFalse(request.getPath().contains("/v1/pipeline/transactions"));
        assertNotNull(request.getBody());
        assertFalse(request.getBody().readByteString().size() == 0);
    }

    @Test
    public void resolveAccountAliasUsesSignedAliasEndpoint() throws Exception {
        server = new MockWebServer();
        server.start();

        String resolvedAccountId = accountIdFromSeed(0x21);
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"alias\":\"alice@retail.dataspace\",\"account_id\":\"" + resolvedAccountId + "\"}"));

        ToriiClient client = new ToriiClient(config(server.url("/")));
        KeyPair keyPair = keyPair("torii-alias-test");
        String authorityAccountId = accountId(keyPair);

        String resolved = client.resolveAccountAlias(
                "alice@retail.dataspace",
                authorityAccountId,
                keyPair.getPrivate());

        assertEquals(resolvedAccountId, resolved);
        RecordedRequest request = server.takeRequest();
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/v1/aliases/resolve", request.getPath());
        assertEquals(authorityAccountId, request.getHeader(CanonicalRequestSigner.HEADER_ACCOUNT));
        assertNotNull(request.getHeader(CanonicalRequestSigner.HEADER_SIGNATURE));
        assertNotNull(request.getHeader(CanonicalRequestSigner.HEADER_TIMESTAMP_MS));
        assertNotNull(request.getHeader(CanonicalRequestSigner.HEADER_NONCE));
        assertEquals("{\"alias\":\"alice@retail.dataspace\"}", request.getBody().readUtf8());
    }

    private static ToriiConfig config(HttpUrl base) {
        return new ToriiConfig(
                base,
                "00000000-0000-0000-0000-000000000000",
                "wonderland",
                "point#wonderland",
                753,
                null,
                null
        );
    }

    private static SignedTransaction sampleTransaction() throws Exception {
        IrohaKeyManager keyManager = IrohaKeyManager.withSoftwareFallback();
        KeyPair keyPair = keyManager.generateOrLoad("torii-test", IrohaKeyManager.KeySecurityPreference.SOFTWARE_ONLY);
        Signer signer = keyManager.signerForAlias("torii-test", IrohaKeyManager.KeySecurityPreference.SOFTWARE_ONLY);

        TransactionPayload payload = TransactionPayload.builder()
                .setChainId("00000000-0000-0000-0000-000000000000")
                .setAuthority(accountId(keyPair))
                .setCreationTimeMs(System.currentTimeMillis())
                .setTimeToLiveMs(60_000L)
                .setNonce(1)
                .setInstructions(Collections.emptyList())
                .build();

        TransactionBuilder builder = new TransactionBuilder(new NoritoJavaCodecAdapter(), keyManager);
        return builder.encodeAndSign(payload, signer);
    }

    private static KeyPair keyPair(String alias) throws Exception {
        IrohaKeyManager keyManager = IrohaKeyManager.withSoftwareFallback();
        return keyManager.generateOrLoad(alias, IrohaKeyManager.KeySecurityPreference.SOFTWARE_ONLY);
    }

    private static String accountId(KeyPair keyPair) throws Exception {
        return AccountIdCodec.encodeDomainlessAccount(
                KeyEncodingUtils.extractEd25519PublicKey(keyPair.getPublic()),
                "ed25519",
                753
        );
    }

    private static String accountIdFromSeed(int startByte) throws Exception {
        byte[] publicKey = new byte[32];
        for (int i = 0; i < publicKey.length; i++) {
            publicKey[i] = (byte) (startByte + i);
        }
        return AccountIdCodec.encodeDomainlessAccount(publicKey, "ed25519", 753);
    }
}
