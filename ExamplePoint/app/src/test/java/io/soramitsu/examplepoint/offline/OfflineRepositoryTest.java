package io.soramitsu.examplepoint.offline;

import org.junit.Test;

import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.data.ToriiConfig;
import io.soramitsu.examplepoint.sdk.AccountIdCodec;
import okhttp3.HttpUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OfflineRepositoryTest {

    @Test
    public void buildAllowancesUrlBindsControllerAndAddressFormat() throws Exception {
        ToriiConfig config = new ToriiConfig(
                HttpUrl.parse("https://torii.test") ,
                "chain",
                "wonderland",
                "asset#wonderland",
                753,
                null,
                null);
        String accountId = accountId();
        AccountProfile profile = new AccountProfile(
                accountId,
                "wonderland",
                "Alice",
                "alias",
                "asset#wonderland",
                null);

        HttpUrl url = OfflineRepository.buildAllowancesUrl(config, profile);
        assertNotNull(url);
        assertEquals("/v1/offline/allowances", url.encodedPath());
        assertEquals("controller==" + accountId, url.queryParameter("filter"));
        assertEquals(null, url.queryParameter("address_format"));
    }

    @Test
    public void buildTransfersUrlUsesFilterBasedControllerQuery() throws Exception {
        ToriiConfig config = new ToriiConfig(
                HttpUrl.parse("https://torii.test"),
                "chain",
                "wonderland",
                "asset#wonderland",
                753,
                null,
                null);
        String accountId = accountId();
        AccountProfile profile = new AccountProfile(
                accountId,
                "wonderland",
                "Alice",
                "alias",
                "asset#wonderland",
                null);

        HttpUrl url = OfflineRepository.buildTransfersUrl(config, profile);
        assertNotNull(url);
        assertEquals("/v1/offline/transfers", url.encodedPath());
        assertEquals("controller==" + accountId, url.queryParameter("filter"));
        assertEquals("recorded_at_ms:desc", url.queryParameter("sort"));
        assertEquals("1", url.queryParameter("limit"));
        assertEquals(null, url.queryParameter("address_format"));
    }

    private static String accountId() throws Exception {
        byte[] publicKey = new byte[32];
        for (int i = 0; i < publicKey.length; i++) {
            publicKey[i] = (byte) (0x21 + i);
        }
        return AccountIdCodec.encodeDomainlessAccount(publicKey, "ed25519", 753);
    }
}
