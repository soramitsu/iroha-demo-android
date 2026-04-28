package io.soramitsu.examplepoint.sdk.key;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * Stores a stable passphrase used to encrypt deterministic software key exports.
 */
public final class KeyExportPassphraseStore {

    private static final String PREFS_NAME = "iroha_key_export_passphrase";
    private static final String PREFS_FALLBACK_NAME = "iroha_key_export_passphrase_fallback";
    private static final String KEY_PASSPHRASE = "passphrase_v1";
    private static final int PASSPHRASE_BYTES = 32;

    private final Context appContext;
    private volatile SharedPreferences preferences;

    public KeyExportPassphraseStore(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    public synchronized char[] loadOrCreate() {
        SharedPreferences prefs = sharedPreferences();
        String value = prefs.getString(KEY_PASSPHRASE, null);
        if (value == null || value.trim().isEmpty()) {
            value = generatePassphrase();
            prefs.edit().putString(KEY_PASSPHRASE, value).apply();
        }
        return value.toCharArray();
    }

    private SharedPreferences sharedPreferences() {
        if (preferences != null) {
            return preferences;
        }
        synchronized (this) {
            if (preferences == null) {
                preferences = createEncryptedPreferences();
            }
            return preferences;
        }
    }

    private SharedPreferences createEncryptedPreferences() {
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            return appContext.getSharedPreferences(PREFS_FALLBACK_NAME, Context.MODE_PRIVATE);
        }
    }

    private static String generatePassphrase() {
        byte[] bytes = new byte[PASSPHRASE_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
}
