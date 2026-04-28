package io.soramitsu.examplepoint.sdk;

import org.hyperledger.iroha.android.address.AccountAddress;
import org.junit.Test;

import io.soramitsu.examplepoint.data.ToriiConfig;
import okhttp3.HttpUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountLiteralFormatterTest {

    private static final int I105_DISCRIMINANT = 753;
    private static final ToriiConfig CONFIG = new ToriiConfig(
            HttpUrl.get("https://torii.test"),
            "chain",
            "wonderland",
            "asset#wonderland",
            I105_DISCRIMINANT,
            null,
            null
    );

    @Test
    public void normalizeAcceptsCanonicalI105AccountIds() throws Exception {
        String accountId = AccountIdCodec.encodeDomainlessAccount(publicKey(0x21), "ed25519", I105_DISCRIMINANT);

        AccountLiteralFormatter.ParsedAccountLiteral parsed =
                AccountLiteralFormatter.normalize(accountId, CONFIG);

        assertFalse(parsed.isAlias());
        assertEquals(accountId, parsed.literal());
    }

    @Test
    public void normalizeAliasCanonicalizesSupportedAliasShapes() {
        assertEquals("alice@retail.dataspace", AccountLiteralFormatter.normalizeAlias("Alice@Retail.Dataspace"));
        assertEquals("alice@dataspace", AccountLiteralFormatter.normalizeAlias("ALICE@DATASPACE"));
        assertTrue(AccountLiteralFormatter.normalize(" Alice@Retail.Dataspace ", CONFIG).isAlias());
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeRejectsLegacyCanonicalHexAccountLiteral() throws Exception {
        String legacyHex = AccountAddress.fromAccount("wonderland", publicKey(0x31), "ed25519").canonicalHex();
        AccountLiteralFormatter.normalize(legacyHex, CONFIG);
    }

    private static byte[] publicKey(int startByte) {
        byte[] out = new byte[32];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) (startByte + i);
        }
        return out;
    }
}
