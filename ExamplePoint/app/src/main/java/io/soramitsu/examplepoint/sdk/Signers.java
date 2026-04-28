package io.soramitsu.examplepoint.sdk;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.util.Arrays;

import io.soramitsu.examplepoint.util.CrashReporter;
import org.hyperledger.iroha.android.crypto.Signer;

/**
 * Helpers for creating {@link Signer} implementations backed by raw key material.
 */
public final class Signers {

    private Signers() {
    }

    public static Signer ed25519(byte[] privateKey) {
        if (privateKey == null || privateKey.length != 32) {
            throw new IllegalArgumentException("Ed25519 private key must be 32 bytes");
        }
        final byte[] keyCopy = Arrays.copyOf(privateKey, privateKey.length);
        final Ed25519PrivateKeyParameters privateKeyParameters = new Ed25519PrivateKeyParameters(keyCopy, 0);
        final byte[] publicKey = privateKeyParameters.generatePublicKey().getEncoded();

        return new Signer() {
            @Override
            public byte[] sign(byte[] message) {
                try {
                    final Ed25519Signer signer = new Ed25519Signer();
                    signer.init(true, privateKeyParameters);
                    signer.update(message, 0, message.length);
                    return signer.generateSignature();
                } catch (Exception ex) {
                    CrashReporter.logError("Signers", "Failed to sign payload", ex);
                    throw new IllegalStateException("Failed to sign payload", ex);
                }
            }

            @Override
            public byte[] publicKey() {
                return Arrays.copyOf(publicKey, publicKey.length);
            }

            @Override
            public String algorithm() {
                return "Ed25519";
            }
        };
    }
}
