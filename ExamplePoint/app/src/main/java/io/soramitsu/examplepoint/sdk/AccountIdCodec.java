package io.soramitsu.examplepoint.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.hyperledger.iroha.android.address.AccountAddress;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Encodes and validates canonical I105 account identifiers for the current Torii stack.
 */
public final class AccountIdCodec {

    private static final byte[] I105_CHECKSUM_PREFIX = "I105PRE".getBytes(StandardCharsets.US_ASCII);
    private static final char[] BASE58_ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final int[] BASE58_INDEXES = new int[128];
    private static final BigInteger BASE58_RADIX = BigInteger.valueOf(58L);

    static {
        Arrays.fill(BASE58_INDEXES, -1);
        for (int i = 0; i < BASE58_ALPHABET.length; i++) {
            BASE58_INDEXES[BASE58_ALPHABET[i]] = i;
        }
    }

    private AccountIdCodec() {
    }

    @NonNull
    public static String encodeAddress(@NonNull AccountAddress address, int i105Discriminant)
            throws AccountAddress.AccountAddressException {
        return encodeCanonicalBytes(address.canonicalBytes(), i105Discriminant);
    }

    @NonNull
    public static String encodeDomainlessAccount(@NonNull byte[] publicKey,
                                                 @NonNull String algorithm,
                                                 int i105Discriminant)
            throws AccountAddress.AccountAddressException {
        AccountAddress address = AccountAddress.fromAccount(
                AccountAddress.DEFAULT_DOMAIN_NAME,
                publicKey,
                algorithm
        );
        return encodeAddress(address, i105Discriminant);
    }

    @NonNull
    public static String migrateLegacyCanonicalHex(@NonNull String legacyCanonicalHex,
                                                   int i105Discriminant)
            throws AccountAddress.AccountAddressException {
        AccountAddress legacyAddress = AccountAddress.fromCanonicalHex(legacyCanonicalHex);
        return encodeAddress(toDomainlessAddress(legacyAddress), i105Discriminant);
    }

    public static boolean isCanonicalAccountId(@Nullable String literal, int i105Discriminant) {
        if (literal == null) {
            return false;
        }
        try {
            byte[] canonical = decodeCanonicalBytes(literal, i105Discriminant);
            return literal.equals(encodeCanonicalBytes(canonical, i105Discriminant));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @NonNull
    public static byte[] decodeCanonicalBytes(@NonNull String literal, int i105Discriminant) {
        String trimmed = literal.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Account ID must not be empty");
        }
        if (!trimmed.equals(literal)) {
            throw new IllegalArgumentException("Account ID must not contain leading or trailing whitespace");
        }
        byte[] payload = decodeBase58(trimmed);
        if (payload.length < 3) {
            throw new IllegalArgumentException("Account ID is too short");
        }

        int splitAt = payload.length - 2;
        byte[] body = Arrays.copyOf(payload, splitAt);
        byte[] checksum = Arrays.copyOfRange(payload, splitAt, payload.length);
        byte[] expectedChecksum = checksum(body);
        if (!Arrays.equals(checksum, expectedChecksum)) {
            throw new IllegalArgumentException("Account ID checksum mismatch");
        }

        PrefixDecodeResult prefix = decodeI105Prefix(body);
        if (prefix.discriminant != i105Discriminant) {
            throw new IllegalArgumentException("Account ID belongs to a different network");
        }
        return Arrays.copyOfRange(body, prefix.prefixLength, body.length);
    }

    @NonNull
    public static String encodeCanonicalBytes(@NonNull byte[] canonicalBytes, int i105Discriminant) {
        if (canonicalBytes.length == 0) {
            throw new IllegalArgumentException("Canonical account payload must not be empty");
        }
        byte[] prefixBytes = encodeI105Prefix(i105Discriminant);
        byte[] body = new byte[prefixBytes.length + canonicalBytes.length];
        System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
        System.arraycopy(canonicalBytes, 0, body, prefixBytes.length, canonicalBytes.length);

        byte[] checksum = checksum(body);
        byte[] payload = new byte[body.length + checksum.length];
        System.arraycopy(body, 0, payload, 0, body.length);
        System.arraycopy(checksum, 0, payload, body.length, checksum.length);
        return encodeBase58(payload);
    }

    @NonNull
    private static AccountAddress toDomainlessAddress(@NonNull AccountAddress legacyAddress)
            throws AccountAddress.AccountAddressException {
        Optional<AccountAddress.SingleKeyPayload> singleKeyPayload = legacyAddress.singleKeyPayload();
        if (singleKeyPayload.isPresent()) {
            AccountAddress.SingleKeyPayload payload = singleKeyPayload.get();
            return AccountAddress.fromAccount(
                    AccountAddress.DEFAULT_DOMAIN_NAME,
                    payload.publicKey(),
                    curveIdToAlgorithm(payload.curveId())
            );
        }

        Optional<AccountAddress.MultisigPolicyPayload> multisig = legacyAddress.multisigPolicyPayload();
        if (multisig.isPresent()) {
            return AccountAddress.fromMultisigPolicy(AccountAddress.DEFAULT_DOMAIN_NAME, multisig.get());
        }
        return legacyAddress;
    }

    @NonNull
    private static String curveIdToAlgorithm(int curveId) {
        if (curveId == 0x01) {
            return "ed25519";
        }
        if (curveId == 0x04) {
            return "secp256k1";
        }
        throw new IllegalArgumentException("Unsupported curve id for I105 migration: " + curveId);
    }

    @NonNull
    private static byte[] encodeI105Prefix(int discriminant) {
        if (discriminant < 0 || discriminant > 0x3fff) {
            throw new IllegalArgumentException("I105 discriminant must fit in 14 bits");
        }
        if (discriminant <= 63) {
            return new byte[]{(byte) discriminant};
        }
        byte lower = (byte) ((discriminant & 0x3f) | 0x40);
        byte upper = (byte) (discriminant >> 6);
        return new byte[]{lower, upper};
    }

    @NonNull
    private static PrefixDecodeResult decodeI105Prefix(@NonNull byte[] body) {
        int first = body[0] & 0xFF;
        if (first <= 63) {
            return new PrefixDecodeResult(first, 1);
        }
        if ((first & 0x40) != 0) {
            if (body.length < 2) {
                throw new IllegalArgumentException("Account ID prefix is truncated");
            }
            int second = body[1] & 0xFF;
            int discriminant = (second << 6) | (first & 0x3F);
            return new PrefixDecodeResult(discriminant, 2);
        }
        throw new IllegalArgumentException("Account ID prefix encoding is invalid");
    }

    @NonNull
    private static byte[] checksum(@NonNull byte[] body) {
        Blake2bDigest digest = new Blake2bDigest(512);
        digest.update(I105_CHECKSUM_PREFIX, 0, I105_CHECKSUM_PREFIX.length);
        digest.update(body, 0, body.length);
        byte[] output = new byte[64];
        digest.doFinal(output, 0);
        return new byte[]{output[0], output[1]};
    }

    @NonNull
    private static String encodeBase58(@NonNull byte[] input) {
        if (input.length == 0) {
            return "";
        }

        BigInteger value = new BigInteger(1, input);
        StringBuilder builder = new StringBuilder();
        while (value.signum() > 0) {
            BigInteger[] divRem = value.divideAndRemainder(BASE58_RADIX);
            builder.append(BASE58_ALPHABET[divRem[1].intValue()]);
            value = divRem[0];
        }

        for (byte item : input) {
            if (item == 0) {
                builder.append(BASE58_ALPHABET[0]);
            } else {
                break;
            }
        }
        return builder.reverse().toString();
    }

    @NonNull
    private static byte[] decodeBase58(@NonNull String input) {
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current >= BASE58_INDEXES.length || BASE58_INDEXES[current] < 0) {
                throw new IllegalArgumentException("Account ID contains non-I105 characters");
            }
            value = value.multiply(BASE58_RADIX).add(BigInteger.valueOf(BASE58_INDEXES[current]));
        }

        byte[] decoded = value.toByteArray();
        if (decoded.length > 0 && decoded[0] == 0) {
            decoded = Arrays.copyOfRange(decoded, 1, decoded.length);
        }

        int leadingZeroes = 0;
        while (leadingZeroes < input.length() && input.charAt(leadingZeroes) == BASE58_ALPHABET[0]) {
            leadingZeroes++;
        }

        byte[] out = new byte[leadingZeroes + decoded.length];
        System.arraycopy(decoded, 0, out, leadingZeroes, decoded.length);
        return out;
    }

    private static final class PrefixDecodeResult {
        private final int discriminant;
        private final int prefixLength;

        private PrefixDecodeResult(int discriminant, int prefixLength) {
            this.discriminant = discriminant;
            this.prefixLength = prefixLength;
        }
    }
}
