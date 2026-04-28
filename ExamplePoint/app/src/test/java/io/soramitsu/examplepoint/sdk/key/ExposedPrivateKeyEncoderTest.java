package io.soramitsu.examplepoint.sdk.key;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ExposedPrivateKeyEncoderTest {

    @Test
    public void encodesCanonicalEd25519Multihash() {
        byte[] privateKey = new byte[32];
        for (int i = 0; i < privateKey.length; i++) {
            privateKey[i] = (byte) i;
        }

        String encoded = ExposedPrivateKeyEncoder.encodeEd25519(privateKey);

        assertTrue(encoded.startsWith("802620"));
        assertEquals(
                "802620000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F",
                encoded
        );
    }

    @Test
    public void rejectsInvalidKeyLength() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ExposedPrivateKeyEncoder.encodeEd25519(new byte[31])
        );

        assertEquals("Ed25519 private key must be 32 bytes", error.getMessage());
    }
}
