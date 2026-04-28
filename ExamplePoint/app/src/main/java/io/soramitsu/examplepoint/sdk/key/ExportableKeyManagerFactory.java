package io.soramitsu.examplepoint.sdk.key;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.security.Provider;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.iroha.android.IrohaKeyManager;
import org.hyperledger.iroha.android.crypto.export.FileKeyExportStore;

/**
 * Builds an {@link IrohaKeyManager} configured with persistent exportable software keys.
 */
public final class ExportableKeyManagerFactory {

    private static final String KEY_EXPORTS_DIR = "iroha_keys";
    private static final String KEY_EXPORTS_FILE = "exports.properties";

    private ExportableKeyManagerFactory() {
    }

    public static IrohaKeyManager create(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        Provider existing = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (!(existing instanceof BouncyCastleProvider)) {
            if (existing != null) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            }
            Security.addProvider(new BouncyCastleProvider());
        }
        File rootDir = new File(appContext.getFilesDir(), KEY_EXPORTS_DIR);
        File exportFile = new File(rootDir, KEY_EXPORTS_FILE);

        FileKeyExportStore exportStore = new FileKeyExportStore(exportFile);
        KeyExportPassphraseStore passphraseStore = new KeyExportPassphraseStore(appContext);
        return IrohaKeyManager.withExportableSoftwareKeys(exportStore, passphraseStore::loadOrCreate);
    }
}
