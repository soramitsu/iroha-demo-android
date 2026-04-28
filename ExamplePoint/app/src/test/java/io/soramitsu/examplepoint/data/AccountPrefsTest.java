package io.soramitsu.examplepoint.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.hyperledger.iroha.android.address.AccountAddress;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.soramitsu.examplepoint.sdk.AccountIdCodec;
import okhttp3.HttpUrl;

public class AccountPrefsTest {

    private static final int I105_DISCRIMINANT = 753;

    private InMemoryPreferences preferences;
    private AccountPrefs accountPrefs;

    @Before
    public void setUp() {
        preferences = new InMemoryPreferences();
        accountPrefs = new AccountPrefs(preferences, config());
    }

    @Test
    public void savePersistsActiveAccount() throws Exception {
        AccountProfile profile = profile(accountId(0x01), "wonderland", "alias1", "Alice");
        accountPrefs.save(profile);

        Optional<AccountProfile> loaded = accountPrefs.load();
        assertTrue(loaded.isPresent());
        assertEquals(profile.getAccountId(), loaded.get().getAccountId());
        assertEquals(1, accountPrefs.loadAll().size());
    }

    @Test
    public void setActiveAccountReordersProfileList() throws Exception {
        AccountProfile first = profile(accountId(0x11), "wonderland", "aliasA", "First");
        AccountProfile second = profile(accountId(0x21), "wonderland", "aliasB", "Second");

        accountPrefs.save(first);
        accountPrefs.save(second);

        boolean switched = accountPrefs.setActiveAccount(first.getAccountId());
        assertTrue(switched);
        assertEquals(first.getAccountId(), accountPrefs.load().get().getAccountId());
        assertEquals(first.getAccountId(), accountPrefs.loadAll().get(0).getAccountId());
    }

    @Test
    public void removeActiveAccountFallsBackToNextProfile() throws Exception {
        AccountProfile first = profile(accountId(0x31), "wonderland", "aliasA", "First");
        AccountProfile second = profile(accountId(0x41), "wonderland", "aliasB", "Second");

        accountPrefs.save(first);
        accountPrefs.save(second);
        accountPrefs.setActiveAccount(second.getAccountId());

        boolean removed = accountPrefs.removeAccount(second.getAccountId());
        assertTrue(removed);
        assertEquals(first.getAccountId(), accountPrefs.load().get().getAccountId());
        assertEquals(1, accountPrefs.loadAll().size());
    }

    @Test
    public void removingLastAccountClearsActiveSelection() throws Exception {
        AccountProfile profile = profile(accountId(0x51), "wonderland", "alias1", "Solo");
        accountPrefs.save(profile);

        boolean removed = accountPrefs.removeAccount(profile.getAccountId());
        assertTrue(removed);
        assertFalse(accountPrefs.load().isPresent());
        assertTrue(accountPrefs.loadAll().isEmpty());
    }

    @Test
    public void loadMigratesLegacyCanonicalHexToDomainlessI105AccountId() throws Exception {
        preferences.edit()
                .putString("account_address", legacyCanonicalHex("wonderland"))
                .putString("domain", "wonderland")
                .putString("display_name", "Alice")
                .putString("key_alias", "legacy-key")
                .apply();

        Optional<AccountProfile> loaded = accountPrefs.load();

        assertTrue(loaded.isPresent());
        assertEquals(expectedAccountId(), loaded.get().getAccountId());
        assertFalse(preferences.contains("account_address"));
        assertTrue(AccountIdCodec.isCanonicalAccountId(loaded.get().getAccountId(), I105_DISCRIMINANT));
    }

    private static AccountProfile profile(String address, String domain, String alias, String displayName) {
        return new AccountProfile(address, domain, displayName, alias, null, null);
    }

    private static ToriiConfig config() {
        return new ToriiConfig(
                HttpUrl.get("https://torii.test"),
                "chain",
                "wonderland",
                "asset#wonderland",
                I105_DISCRIMINANT,
                null,
                null
        );
    }

    private static String legacyCanonicalHex(String domain) throws Exception {
        return AccountAddress.fromAccount(domain, publicKey(), "ed25519").canonicalHex();
    }

    private static String expectedAccountId() throws Exception {
        return AccountIdCodec.encodeDomainlessAccount(publicKey(), "ed25519", I105_DISCRIMINANT);
    }

    private static String accountId(int startByte) throws Exception {
        byte[] out = new byte[32];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) (startByte + i);
        }
        return AccountIdCodec.encodeDomainlessAccount(out, "ed25519", I105_DISCRIMINANT);
    }

    private static byte[] publicKey() {
        byte[] out = new byte[32];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) (i + 1);
        }
        return out;
    }

    private static final class InMemoryPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(values);
        }

        @Override
        public String getString(String key, String defValue) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object value = values.get(key);
            if (value instanceof Set) {
                //noinspection unchecked
                return new HashSet<>((Set<String>) value);
            }
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object value = values.get(key);
            return value instanceof Integer ? (Integer) value : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object value = values.get(key);
            return value instanceof Long ? (Long) value : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object value = values.get(key);
            return value instanceof Float ? (Float) value : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : defValue;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new MemoryEditor(values);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            // no-op for tests
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
            // no-op for tests
        }

        private static final class MemoryEditor implements Editor {
            private final Map<String, Object> store;
            private final Map<String, Object> pending = new HashMap<>();
            private boolean clearRequested;
            private final Set<String> removals = new HashSet<>();

            MemoryEditor(Map<String, Object> store) {
                this.store = store;
            }

            @Override
            public Editor putString(String key, String value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) {
                pending.put(key, new HashSet<>(values));
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                removals.add(key);
                return this;
            }

            @Override
            public Editor clear() {
                clearRequested = true;
                return this;
            }

            @Override
            public boolean commit() {
                if (clearRequested) {
                    store.clear();
                }
                for (String key : removals) {
                    store.remove(key);
                }
                store.putAll(pending);
                pending.clear();
                removals.clear();
                clearRequested = false;
                return true;
            }

            @Override
            public void apply() {
                commit();
            }
        }
    }
}
