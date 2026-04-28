package io.soramitsu.examplepoint.sdk.identity;

import androidx.annotation.NonNull;

import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.util.encoders.Hex;

/**
 * Deterministically derives UAID literals from Nexus identity manifests.
 */
public final class NexusUaidFactory {

    private NexusUaidFactory() {}

    @NonNull
    public static String derive(@NonNull NexusIdentityManifest manifest) {
        byte[] input = manifest.toCanonicalBytes();
        Blake2bDigest digest = new Blake2bDigest(256);
        digest.update(input, 0, input.length);
        byte[] out = new byte[32];
        digest.doFinal(out, 0);
        return "uaid:" + Hex.toHexString(out);
    }
}
