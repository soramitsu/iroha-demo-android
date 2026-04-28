package io.soramitsu.examplepoint.sdk.backup;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.hyperledger.iroha.android.IrohaKeyManager;
import org.hyperledger.iroha.android.KeyManagementException;
import org.hyperledger.iroha.android.crypto.export.KeyExportBundle;
import org.hyperledger.iroha.android.crypto.export.KeyExportException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Coordinates mnemonic generation, deterministic export, and persistence to secure storage.
 */
public final class KeyBackupManager {

    public enum BackupDestination {
        GOOGLE_SECURE_STORAGE,
        ICLOUD_SECURE_STORAGE
    }

    private final Context context;
    private final IrohaKeyManager keyManager;
    private final Executor executor;
    private final MnemonicGenerator mnemonicGenerator;
    private SharedPreferences googleSecurePrefs;

    public KeyBackupManager(
            @NonNull Context context,
            @NonNull IrohaKeyManager keyManager,
            @NonNull Executor executor
    ) {
        this.context = context.getApplicationContext();
        this.keyManager = keyManager;
        this.executor = executor;
        MnemonicWordList wordList = MnemonicWordList.fromRawResource(this.context);
        this.mnemonicGenerator = new MnemonicGenerator(wordList, new SecureRandom());
    }

    public CompletableFuture<KeyBackupPlan> generateBackupPlan(@NonNull MnemonicGenerator.Length length) {
        return CompletableFuture.supplyAsync(() -> generatePlanInternal(length), executor);
    }

    public CompletableFuture<Void> persistPlan(
            @NonNull KeyBackupPlan plan,
            @NonNull EnumSet<BackupDestination> destinations
    ) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(destinations, "destinations");
        if (destinations.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> persistInternal(plan, destinations), executor);
    }

    private KeyBackupPlan generatePlanInternal(MnemonicGenerator.Length length) {
        final String keyAlias = "acct-" + UUID.randomUUID();
        final List<String> mnemonicWords = mnemonicGenerator.generate(length);
        final char[] passphrase = toPassphraseChars(mnemonicWords);
        try {
            keyManager.generateOrLoad(keyAlias, IrohaKeyManager.KeySecurityPreference.SOFTWARE_ONLY);
            final KeyExportBundle bundle = keyManager.exportDeterministicKey(keyAlias, passphrase);
            return new KeyBackupPlan(
                    keyAlias,
                    mnemonicWords,
                    bundle.encodeBase64(),
                    System.currentTimeMillis()
            );
        } catch (KeyManagementException | KeyExportException ex) {
            throw new CompletionException(ex);
        } finally {
            Arrays.fill(passphrase, '\0');
        }
    }

    private void persistInternal(KeyBackupPlan plan, EnumSet<BackupDestination> destinations) {
        Objects.requireNonNull(plan, "plan");
        try {
            if (destinations.contains(BackupDestination.GOOGLE_SECURE_STORAGE)) {
                storeInGoogle(plan);
            }
            if (destinations.contains(BackupDestination.ICLOUD_SECURE_STORAGE)) {
                storeInICloud(plan);
            }
        } catch (IOException | GeneralSecurityException | JSONException ex) {
            throw new CompletionException(ex);
        }
    }

    private void storeInGoogle(KeyBackupPlan plan) throws GeneralSecurityException, IOException {
        SharedPreferences prefs = googlePrefs();
        final String baseKey = "backup_" + plan.getKeyAlias();
        prefs.edit()
                .putString(baseKey + "_bundle", plan.getExportBundleBase64())
                .putString(baseKey + "_passphrase", plan.getPassphrase())
                .putLong(baseKey + "_timestamp", plan.getCreatedAtMs())
                .apply();
    }

    private void storeInICloud(KeyBackupPlan plan) throws IOException, JSONException {
        File directory = new File(context.getFilesDir(), "icloud_backups");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to prepare iCloud staging directory");
        }
        File file = new File(directory, plan.getKeyAlias() + ".json");
        JSONObject payload = new JSONObject();
        payload.put("alias", plan.getKeyAlias());
        payload.put("bundle", plan.getExportBundleBase64());
        payload.put("passphrase", plan.getPassphrase());
        payload.put("created_at_ms", plan.getCreatedAtMs());
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(payload.toString());
        }
    }

    private SharedPreferences googlePrefs() throws GeneralSecurityException, IOException {
        if (googleSecurePrefs == null) {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            googleSecurePrefs = EncryptedSharedPreferences.create(
                    context,
                    "iroha_secure_keys",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        }
        return googleSecurePrefs;
    }

    private static char[] toPassphraseChars(@NonNull java.util.List<String> words) {
        final String joined = String.join(" ", words);
        return joined.toCharArray();
    }
}
