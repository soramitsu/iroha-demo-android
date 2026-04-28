package io.soramitsu.examplepoint.sdk.key;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.Locale;

/**
 * Encodes Ed25519 private keys to canonical ExposedPrivateKey multihash hex.
 */
public final class ExposedPrivateKeyEncoder {

    private static final String ED25519_OID = "1.3.101.112";
    private static final long ED25519_PRIVATE_MULTIHASH_CODE = 0x1300L;
    private static final int ED25519_PRIVATE_KEY_BYTES = 32;

    private ExposedPrivateKeyEncoder() {
    }

    public static String encodePrivateKey(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("privateKey must not be null");
        }
        return encodeEd25519(extractEd25519PrivateKey(privateKey));
    }

    public static String encodeEd25519(byte[] privateKeyBytes) {
        if (privateKeyBytes == null || privateKeyBytes.length != ED25519_PRIVATE_KEY_BYTES) {
            throw new IllegalArgumentException("Ed25519 private key must be 32 bytes");
        }

        StringBuilder builder = new StringBuilder((2 + 1 + privateKeyBytes.length) * 2);
        appendVarintHexLower(builder, ED25519_PRIVATE_MULTIHASH_CODE);
        appendVarintHexLower(builder, privateKeyBytes.length);
        appendHexUpper(builder, privateKeyBytes);
        return builder.toString();
    }

    public static byte[] extractEd25519PrivateKey(PrivateKey privateKey) {
        byte[] encoded = privateKey.getEncoded();
        if (encoded == null || encoded.length == 0) {
            throw new IllegalArgumentException("Private key must be encodable");
        }
        try {
            PrivateKeyInfo keyInfo = PrivateKeyInfo.getInstance(encoded);
            String algorithmOid = keyInfo.getPrivateKeyAlgorithm().getAlgorithm().getId();
            if (!ED25519_OID.equals(algorithmOid)) {
                throw new IllegalArgumentException("Only Ed25519 private keys are supported");
            }

            ASN1Encodable parsed = keyInfo.parsePrivateKey();
            if (!(parsed instanceof ASN1OctetString)) {
                throw new IllegalArgumentException("Unexpected Ed25519 private key encoding");
            }
            byte[] octets = ((ASN1OctetString) parsed).getOctets();
            if (octets.length == ED25519_PRIVATE_KEY_BYTES) {
                return octets;
            }

            ASN1Primitive nested = ASN1Primitive.fromByteArray(octets);
            if (nested instanceof ASN1OctetString) {
                byte[] nestedOctets = ((ASN1OctetString) nested).getOctets();
                if (nestedOctets.length == ED25519_PRIVATE_KEY_BYTES) {
                    return nestedOctets;
                }
            }
            throw new IllegalArgumentException("Ed25519 private key must be 32 bytes");
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse Ed25519 private key", e);
        }
    }

    private static void appendVarintHexLower(StringBuilder builder, long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Varint value must be non-negative");
        }
        do {
            int lower7 = (int) (value & 0x7FL);
            value >>>= 7;
            int out = value == 0 ? lower7 : (lower7 | 0x80);
            builder.append(String.format(Locale.ROOT, "%02x", out));
        } while (value != 0);
    }

    private static void appendHexUpper(StringBuilder builder, byte[] bytes) {
        for (byte b : bytes) {
            builder.append(String.format(Locale.ROOT, "%02X", b & 0xFF));
        }
    }
}
