package io.soramitsu.examplepoint.connect;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import io.soramitsu.examplepoint.BuildConfig;
import io.soramitsu.examplepoint.sdk.AccountIdCodec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class ConnectSeedSignerFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void parseSeedHexRejectsInvalidLength() {
        ConnectSeedSignerFactory.parseSeedHex("abcd");
    }

    @Test
    public void fromSeedHexBuildsDeterministicAccountIdAndSigner() throws Exception {
        String seed = "000102030405060708090a0b0c0d0e0f"
                + "101112131415161718191a1b1c1d1e1f";

        ConnectSigningIdentity primary = ConnectSeedSignerFactory.fromSeedHex(seed, "sbp");
        ConnectSigningIdentity secondary = ConnectSeedSignerFactory.fromSeedHex(seed, "other-domain");
        assertNotNull(primary);
        assertEquals(primary.accountId(), secondary.accountId());
        assertFalse(primary.accountId().contains("@"));
        assertTrue(AccountIdCodec.isCanonicalAccountId(
                primary.accountId(),
                BuildConfig.TORII_I105_DISCRIMINANT));

        byte[] signature = primary.signer().sign("hello-connect".getBytes(StandardCharsets.UTF_8));
        assertEquals(64, signature.length);
    }
}
