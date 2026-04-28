package io.soramitsu.examplepoint.sdk;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import java.security.PublicKey;

/**
 * Utilities for extracting raw Ed25519 key material from JCA key instances.
 */
public final class KeyEncodingUtils {
    private KeyEncodingUtils() {
    }

    /**
     * Extracts the 32-byte Ed25519 public key from a {@link PublicKey}. The key is assumed to be
     * encoded using the standard RFC 8410 SubjectPublicKeyInfo structure.
     */
    public static byte[] extractEd25519PublicKey(PublicKey publicKey) {
        try {
            byte[] encoded = publicKey.getEncoded();
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(encoded);
            return info.getPublicKeyData().getOctets();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to extract Ed25519 public key bytes", ex);
        }
    }
}
