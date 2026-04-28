package io.soramitsu.examplepoint.sdk;

import org.hyperledger.iroha.android.address.AccountAddress;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccountIdCodecTest {

    private static final int I105_DISCRIMINANT = 753;

    @Test
    public void encodeDomainlessAccountRoundTripsCanonicalBytes() throws Exception {
        byte[] publicKey = publicKey(0x01);
        String accountId = AccountIdCodec.encodeDomainlessAccount(publicKey, "ed25519", I105_DISCRIMINANT);

        assertTrue(AccountIdCodec.isCanonicalAccountId(accountId, I105_DISCRIMINANT));
        AccountAddress address = AccountAddress.fromAccount(
                AccountAddress.DEFAULT_DOMAIN_NAME,
                publicKey,
                "ed25519");
        assertArrayEquals(address.canonicalBytes(), AccountIdCodec.decodeCanonicalBytes(accountId, I105_DISCRIMINANT));
    }

    @Test
    public void migrateLegacyCanonicalHexRemovesEmbeddedDomainContext() throws Exception {
        byte[] publicKey = publicKey(0x11);
        String expected = AccountIdCodec.encodeDomainlessAccount(publicKey, "ed25519", I105_DISCRIMINANT);

        String wonderlandHex = AccountAddress.fromAccount("wonderland", publicKey, "ed25519").canonicalHex();
        String retailHex = AccountAddress.fromAccount("retail", publicKey, "ed25519").canonicalHex();

        assertEquals(expected, AccountIdCodec.migrateLegacyCanonicalHex(wonderlandHex, I105_DISCRIMINANT));
        assertEquals(expected, AccountIdCodec.migrateLegacyCanonicalHex(retailHex, I105_DISCRIMINANT));
    }

    private static byte[] publicKey(int startByte) {
        byte[] out = new byte[32];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) (startByte + i);
        }
        return out;
    }
}
