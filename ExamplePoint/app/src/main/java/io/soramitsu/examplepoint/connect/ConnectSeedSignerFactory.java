package io.soramitsu.examplepoint.connect;

import androidx.annotation.NonNull;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.hyperledger.iroha.android.address.AccountAddress;

import java.util.Locale;

import io.soramitsu.examplepoint.BuildConfig;
import io.soramitsu.examplepoint.sdk.AccountIdCodec;
import io.soramitsu.examplepoint.sdk.Signers;

/** Helpers for deriving Connect signing identities from deterministic Ed25519 seed hex. */
public final class ConnectSeedSignerFactory {

    private ConnectSeedSignerFactory() {
    }

    @NonNull
    public static ConnectSigningIdentity fromSeedHex(@NonNull String seedHex,
                                                     @NonNull String accountDomain) {
        byte[] seed = parseSeedHex(seedHex);
        normalizeDomain(accountDomain);

        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(seed, 0);
        byte[] publicKey = privateKey.generatePublicKey().getEncoded();

        String accountId = deriveAccountId(publicKey);
        String displayName = "Connect " + accountId.substring(0, Math.min(accountId.length(), 16)) + "...";
        return new ConnectSigningIdentity(accountId, displayName, Signers.ed25519(seed));
    }

    @NonNull
    private static String deriveAccountId(@NonNull byte[] publicKey) {
        try {
            return AccountIdCodec.encodeDomainlessAccount(
                    publicKey,
                    "ed25519",
                    BuildConfig.TORII_I105_DISCRIMINANT
            );
        } catch (AccountAddress.AccountAddressException error) {
            throw new IllegalArgumentException("Failed to derive connect account id", error);
        }
    }

    @NonNull
    static byte[] parseSeedHex(@NonNull String seedHex) {
        String normalized = seedHex.trim();
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        if (normalized.length() != 64) {
            throw new IllegalArgumentException(
                    String.format(Locale.US,
                            "Connect seed hex must be 32 bytes (64 hex chars), got %d",
                            normalized.length()));
        }

        byte[] out = new byte[32];
        for (int i = 0; i < normalized.length(); i += 2) {
            int hi = Character.digit(normalized.charAt(i), 16);
            int lo = Character.digit(normalized.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Connect seed hex is not valid hexadecimal");
            }
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    @NonNull
    static String normalizeDomain(@NonNull String domain) {
        String trimmed = domain.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Connect signer domain must not be empty");
        }
        return trimmed;
    }

    @NonNull
    static String toHexLower(@NonNull byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(String.format(Locale.ROOT, "%02x", item & 0xFF));
        }
        return builder.toString();
    }
}
