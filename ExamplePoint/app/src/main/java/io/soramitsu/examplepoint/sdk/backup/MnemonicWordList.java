package io.soramitsu.examplepoint.sdk.backup;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.soramitsu.examplepoint.R;

/**
 * Immutable container for mnemonic words used when generating BIP-39 style passphrases.
 */
public final class MnemonicWordList {

    private final List<String> words;

    public MnemonicWordList(@NonNull List<String> words) {
        if (words.size() < 2048) {
            throw new IllegalArgumentException("Mnemonic word list must contain at least 2048 entries");
        }
        this.words = Collections.unmodifiableList(new ArrayList<>(words));
    }

    public int size() {
        return words.size();
    }

    @NonNull
    public String wordAt(int index) {
        return words.get(index);
    }

    static MnemonicWordList fromStream(@NonNull InputStream inputStream) throws IOException {
        final List<String> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    entries.add(trimmed);
                }
            }
        }
        return new MnemonicWordList(entries);
    }

    public static MnemonicWordList fromRawResource(@NonNull Context context) {
        final Resources resources = context.getResources();
        try (InputStream stream = resources.openRawResource(R.raw.mnemonic_words)) {
            return fromStream(stream);
        } catch (Resources.NotFoundException | IOException e) {
            throw new IllegalStateException("Unable to load mnemonic word list", e);
        }
    }
}
