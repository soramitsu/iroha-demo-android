package io.soramitsu.examplepoint.sdk.backup;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot describing a generated key alias and its mnemonic backup payload.
 */
public final class KeyBackupPlan {

    private final String keyAlias;
    private final List<String> mnemonicWords;
    private final String exportBundleBase64;
    private final long createdAtMs;

    public KeyBackupPlan(
            @NonNull String keyAlias,
            @NonNull List<String> mnemonicWords,
            @NonNull String exportBundleBase64,
            long createdAtMs
    ) {
        if (mnemonicWords.isEmpty()) {
            throw new IllegalArgumentException("Mnemonic word list must not be empty");
        }
        this.keyAlias = keyAlias;
        this.mnemonicWords = Collections.unmodifiableList(new ArrayList<>(mnemonicWords));
        this.exportBundleBase64 = exportBundleBase64;
        this.createdAtMs = createdAtMs;
    }

    @NonNull
    public String getKeyAlias() {
        return keyAlias;
    }

    @NonNull
    public List<String> getMnemonicWords() {
        return mnemonicWords;
    }

    @NonNull
    public String getExportBundleBase64() {
        return exportBundleBase64;
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }

    @NonNull
    public String getPassphrase() {
        return String.join(" ", mnemonicWords);
    }
}
