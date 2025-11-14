package io.soramitsu.examplepoint.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Optional;

import io.soramitsu.examplepoint.sdk.identity.NexusIdentityManifest;

/**
 * Simple SharedPreferences-backed store for the active account profile.
 */
public final class AccountPrefs {

    private static final String PREFS_NAME = "iroha_account_profile";
    private static final String KEY_ADDRESS = "account_address";
    private static final String KEY_DOMAIN = "domain";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_KEY_ALIAS = "key_alias";
    private static final String KEY_ASSET_ID = "preferred_asset";
    private static final String KEY_UAID = "uaid";
    private static final String KEY_IDENTITY_MANIFEST = "identity_manifest";

    private final SharedPreferences prefs;

    public AccountPrefs(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Optional<AccountProfile> load() {
        final String address = prefs.getString(KEY_ADDRESS, null);
        final String domain = prefs.getString(KEY_DOMAIN, null);
        final String displayName = prefs.getString(KEY_DISPLAY_NAME, null);
        final String keyAlias = prefs.getString(KEY_KEY_ALIAS, null);
        if (address == null || domain == null || displayName == null || keyAlias == null) {
            return Optional.empty();
        }
        final String assetId = prefs.getString(KEY_ASSET_ID, null);
        final String uaid = prefs.getString(KEY_UAID, null);
        final String manifestJson = prefs.getString(KEY_IDENTITY_MANIFEST, null);
        final NexusIdentityManifest manifest = NexusIdentityManifest.fromStorageJson(manifestJson);
        return Optional.of(new AccountProfile(
                address,
                domain,
                displayName,
                keyAlias,
                assetId,
                uaid,
                manifest));
    }

    public void save(AccountProfile profile) {
        prefs.edit()
                .putString(KEY_ADDRESS, profile.getAccountAddressHex())
                .putString(KEY_DOMAIN, profile.getDomain())
                .putString(KEY_DISPLAY_NAME, profile.getDisplayName())
                .putString(KEY_KEY_ALIAS, profile.getKeyAlias())
                .putString(KEY_ASSET_ID, profile.getPreferredAssetId())
                .putString(KEY_UAID, profile.getUaid())
                .putString(KEY_IDENTITY_MANIFEST,
                        profile.getIdentityManifest() != null
                                ? profile.getIdentityManifest().toStorageJson()
                                : null)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
