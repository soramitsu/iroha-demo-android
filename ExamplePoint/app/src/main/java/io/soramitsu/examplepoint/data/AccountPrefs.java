package io.soramitsu.examplepoint.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import io.soramitsu.examplepoint.sdk.identity.NexusIdentityManifest;

/**
 * SharedPreferences-backed store for one or more locally registered accounts.
 */
public final class AccountPrefs {

    private static final String PREFS_NAME = "iroha_account_profile";
    private static final String KEY_ADDRESS = "account_address";
    private static final String KEY_DOMAIN = "domain";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_KEY_ALIAS = "key_alias";
    private static final String KEY_ASSET_ID = "preferred_asset";
    private static final String KEY_IDENTITY_MANIFEST = "identity_manifest";
    private static final String KEY_ACCOUNTS = "account_profiles";
    private static final String KEY_ACTIVE_ACCOUNT = "active_account_id";

    private final SharedPreferences prefs;

    public AccountPrefs(Context context) {
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    AccountPrefs(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public synchronized Optional<AccountProfile> load() {
        ensureMigrated();
        List<AccountProfile> profiles = loadAllInternal();
        String activeAccountId = prefs.getString(KEY_ACTIVE_ACCOUNT, null);
        if ((activeAccountId == null || activeAccountId.trim().isEmpty()) && !profiles.isEmpty()) {
            activeAccountId = profiles.get(0).getAccountId();
            persist(profiles, activeAccountId);
        }
        if (activeAccountId == null) {
            return Optional.empty();
        }
        for (AccountProfile profile : profiles) {
            if (profile.getAccountId().equals(activeAccountId)) {
                return Optional.of(profile);
            }
        }
        if (!profiles.isEmpty()) {
            String recoveredId = profiles.get(0).getAccountId();
            persist(profiles, recoveredId);
            return Optional.of(profiles.get(0));
        }
        return Optional.empty();
    }

    public synchronized List<AccountProfile> loadAll() {
        ensureMigrated();
        return loadAllInternal();
    }

    public synchronized void save(@NonNull AccountProfile profile) {
        ensureMigrated();
        List<AccountProfile> profiles = new ArrayList<>(loadAllInternal());
        removeAccountInternal(profiles, profile.getAccountId());
        profiles.add(0, profile);
        persist(profiles, profile.getAccountId());
    }

    public synchronized boolean setActiveAccount(@NonNull String accountId) {
        ensureMigrated();
        List<AccountProfile> profiles = new ArrayList<>(loadAllInternal());
        int index = findAccountIndex(profiles, accountId);
        if (index < 0) {
            return false;
        }
        AccountProfile account = profiles.remove(index);
        profiles.add(0, account);
        persist(profiles, account.getAccountId());
        return true;
    }

    public synchronized boolean removeAccount(@NonNull String accountId) {
        ensureMigrated();
        List<AccountProfile> profiles = new ArrayList<>(loadAllInternal());
        if (!removeAccountInternal(profiles, accountId)) {
            return false;
        }
        String activeAccountId = prefs.getString(KEY_ACTIVE_ACCOUNT, null);
        String nextActive = activeAccountId;
        if (activeAccountId != null && activeAccountId.equals(accountId)) {
            nextActive = profiles.isEmpty() ? null : profiles.get(0).getAccountId();
        }
        persist(profiles, nextActive);
        return true;
    }

    public synchronized void clear() {
        prefs.edit().clear().apply();
    }

    private void ensureMigrated() {
        if (prefs.contains(KEY_ACCOUNTS)) {
            return;
        }
        final String address = prefs.getString(KEY_ADDRESS, null);
        final String domain = prefs.getString(KEY_DOMAIN, null);
        final String displayName = prefs.getString(KEY_DISPLAY_NAME, null);
        final String keyAlias = prefs.getString(KEY_KEY_ALIAS, null);
        if (address != null && domain != null && displayName != null && keyAlias != null) {
            final String assetId = prefs.getString(KEY_ASSET_ID, null);
            final String manifestJson = prefs.getString(KEY_IDENTITY_MANIFEST, null);
            final NexusIdentityManifest manifest = NexusIdentityManifest.fromStorageJson(manifestJson);
            AccountProfile legacyProfile = new AccountProfile(
                    address,
                    domain,
                    displayName,
                    keyAlias,
                    assetId,
                    manifest
            );
            persist(Collections.singletonList(legacyProfile), legacyProfile.getAccountId());
        } else {
            persist(Collections.emptyList(), null);
        }
        prefs.edit()
                .remove(KEY_ADDRESS)
                .remove(KEY_DOMAIN)
                .remove(KEY_DISPLAY_NAME)
                .remove(KEY_KEY_ALIAS)
                .remove(KEY_ASSET_ID)
                .remove(KEY_IDENTITY_MANIFEST)
                .apply();
    }

    private List<AccountProfile> loadAllInternal() {
        String json = prefs.getString(KEY_ACCOUNTS, null);
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = new JSONArray(json);
            List<AccountProfile> profiles = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                profiles.add(parseAccount(obj));
            }
            return Collections.unmodifiableList(profiles);
        } catch (JSONException e) {
            return Collections.emptyList();
        }
    }

    private void persist(List<AccountProfile> profiles, @Nullable String activeAccountId) {
        JSONArray array = new JSONArray();
        for (AccountProfile profile : profiles) {
            array.put(toJson(profile));
        }
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_ACCOUNTS, array.toString())
                .putString(KEY_ACTIVE_ACCOUNT, activeAccountId);
        editor.apply();
    }

    private static JSONObject toJson(AccountProfile profile) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(KEY_ADDRESS, profile.getAccountAddressHex());
            obj.put(KEY_DOMAIN, profile.getDomain());
            obj.put(KEY_DISPLAY_NAME, profile.getDisplayName());
            obj.put(KEY_KEY_ALIAS, profile.getKeyAlias());
            obj.put(KEY_ASSET_ID, profile.getPreferredAssetId());
            obj.put(KEY_IDENTITY_MANIFEST,
                    profile.getIdentityManifest() != null
                            ? profile.getIdentityManifest().toStorageJson()
                            : JSONObject.NULL);
        } catch (JSONException ignored) {
            // In practice this should not happen because keys and values are well-formed.
        }
        return obj;
    }

    private static AccountProfile parseAccount(JSONObject obj) throws JSONException {
        final String address = obj.getString(KEY_ADDRESS);
        final String domain = obj.getString(KEY_DOMAIN);
        final String displayName = obj.getString(KEY_DISPLAY_NAME);
        final String keyAlias = obj.getString(KEY_KEY_ALIAS);
        final String assetId = obj.isNull(KEY_ASSET_ID) ? null : obj.optString(KEY_ASSET_ID, null);
        final String manifestJson = obj.isNull(KEY_IDENTITY_MANIFEST)
                ? null
                : obj.optString(KEY_IDENTITY_MANIFEST, null);
        final NexusIdentityManifest manifest = NexusIdentityManifest.fromStorageJson(manifestJson);
        return new AccountProfile(
                address,
                domain,
                displayName,
                keyAlias,
                assetId,
                manifest
        );
    }

    private static boolean removeAccountInternal(List<AccountProfile> profiles, String accountId) {
        Iterator<AccountProfile> iterator = profiles.iterator();
        while (iterator.hasNext()) {
            AccountProfile profile = iterator.next();
            if (profile.getAccountId().equals(accountId)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    private static int findAccountIndex(List<AccountProfile> profiles, String accountId) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getAccountId().equals(accountId)) {
                return i;
            }
        }
        return -1;
    }
}
