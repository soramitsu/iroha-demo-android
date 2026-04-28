package io.soramitsu.examplepoint.sdk.backup;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Generates 12 or 24 word passphrases using the supplied word list and BIP-39 derivation rules.
 */
public final class MnemonicGenerator {

    private final MnemonicWordList wordList;
    private final SecureRandom secureRandom;

    public MnemonicGenerator(@NonNull MnemonicWordList wordList, @NonNull SecureRandom secureRandom) {
        this.wordList = wordList;
        this.secureRandom = secureRandom;
    }

    public enum Length {
        WORDS_12(12, 128),
        WORDS_24(24, 256);

        private final int wordCount;
        private final int entropyBits;

        Length(int wordCount, int entropyBits) {
            this.wordCount = wordCount;
            this.entropyBits = entropyBits;
        }

        public int wordCount() {
            return wordCount;
        }

        public int entropyBits() {
            return entropyBits;
        }
    }

    public List<String> generate(@NonNull Length length) {
        final byte[] entropy = new byte[length.entropyBits() / 8];
        secureRandom.nextBytes(entropy);
        return Collections.unmodifiableList(generateFromEntropy(entropy));
    }

    List<String> generateFromEntropy(@NonNull byte[] entropy) {
        if (entropy.length * 8 % 32 != 0) {
            throw new IllegalArgumentException("Entropy length must be divisible by 32");
        }
        final int entropyBits = entropy.length * 8;
        final int checksumBits = entropyBits / 32;
        final int totalBits = entropyBits + checksumBits;
        final boolean[] bits = new boolean[totalBits];
        for (int i = 0; i < entropyBits; i++) {
            bits[i] = ((entropy[i / 8] >> (7 - (i % 8))) & 0x01) == 0x01;
        }
        final byte[] checksum = sha256(entropy);
        for (int i = 0; i < checksumBits; i++) {
            bits[entropyBits + i] = ((checksum[0] >> (7 - i)) & 0x01) == 0x01;
        }
        final int wordCount = totalBits / 11;
        final List<String> mnemonic = new ArrayList<>(wordCount);
        for (int i = 0; i < wordCount; i++) {
            int index = 0;
            for (int j = 0; j < 11; j++) {
                index <<= 1;
                if (bits[i * 11 + j]) {
                    index |= 1;
                }
            }
            mnemonic.add(wordList.wordAt(index));
        }
        Arrays.fill(checksum, (byte) 0);
        Arrays.fill(entropy, (byte) 0);
        return mnemonic;
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing", e);
        }
    }
}
