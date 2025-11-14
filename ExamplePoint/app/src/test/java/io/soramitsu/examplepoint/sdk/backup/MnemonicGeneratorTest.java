package io.soramitsu.examplepoint.sdk.backup;

import static org.junit.Assert.assertEquals;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class MnemonicGeneratorTest {

    private MnemonicWordList wordList;

    @Before
    public void setUp() {
        final List<String> entries = new ArrayList<>(2048);
        for (int i = 0; i < 2048; i++) {
            entries.add(String.format("word%04d", i));
        }
        wordList = new MnemonicWordList(entries);
    }

    @Test
    public void generateProducesRequestedWordCount() {
        MnemonicGenerator generator = new MnemonicGenerator(wordList, new DeterministicSecureRandom());
        List<String> twelveWords = generator.generate(MnemonicGenerator.Length.WORDS_12);
        assertEquals(12, twelveWords.size());
        List<String> twentyFourWords = generator.generate(MnemonicGenerator.Length.WORDS_24);
        assertEquals(24, twentyFourWords.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateFromEntropyRejectsInvalidLength() {
        MnemonicGenerator generator = new MnemonicGenerator(wordList, new DeterministicSecureRandom());
        byte[] entropy = new byte[10];
        generator.generateFromEntropy(entropy);
    }

    @Test
    public void generateFromEntropyUsesChecksumBits() {
        MnemonicGenerator generator = new MnemonicGenerator(wordList, new DeterministicSecureRandom());
        byte[] entropy = new byte[16]; // 128-bit zero entropy.
        List<String> mnemonic = generator.generateFromEntropy(entropy.clone());
        assertEquals(12, mnemonic.size());
        for (int i = 0; i < 11; i++) {
            assertEquals("word0000", mnemonic.get(i));
        }
        assertEquals("word0003", mnemonic.get(11));
    }

    private static final class DeterministicSecureRandom extends SecureRandom {
        private byte value;

        @Override
        public void nextBytes(byte[] bytes) {
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = value++;
            }
        }
    }
}
