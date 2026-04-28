package io.soramitsu.examplepoint.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.Objects;

import io.soramitsu.examplepoint.sdk.model.UaidBindings;
import io.soramitsu.examplepoint.sdk.model.UaidManifestInventory;
import io.soramitsu.examplepoint.sdk.model.UaidPortfolio;

/**
 * Simple SharedPreferences-backed cache for UAID portfolio/bindings/manifests so the wallet can
 * display the last known state when Torii is unavailable.
 */
final class UaidCache {

    private static final String PREFS_NAME = "uaid_cache";
    private static final String KEY_PORTFOLIO = "portfolio_json";
    private static final String KEY_PORTFOLIO_TS = "portfolio_ts";
    private static final String KEY_BINDINGS = "bindings_json";
    private static final String KEY_BINDINGS_TS = "bindings_ts";
    private static final String KEY_MANIFESTS = "manifests_json";
    private static final String KEY_MANIFESTS_TS = "manifests_ts";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    UaidCache(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    void savePortfolio(UaidPortfolio portfolio) {
        prefs.edit()
                .putString(KEY_PORTFOLIO, gson.toJson(portfolio))
                .putLong(KEY_PORTFOLIO_TS, System.currentTimeMillis())
                .apply();
    }

    void saveBindings(UaidBindings bindings) {
        prefs.edit()
                .putString(KEY_BINDINGS, gson.toJson(bindings))
                .putLong(KEY_BINDINGS_TS, System.currentTimeMillis())
                .apply();
    }

    void saveManifests(UaidManifestInventory inventory) {
        prefs.edit()
                .putString(KEY_MANIFESTS, gson.toJson(inventory))
                .putLong(KEY_MANIFESTS_TS, System.currentTimeMillis())
                .apply();
    }

    @Nullable
    CachedValue<UaidPortfolio> loadPortfolio() {
        return load(KEY_PORTFOLIO, KEY_PORTFOLIO_TS, UaidPortfolio.class);
    }

    @Nullable
    CachedValue<UaidBindings> loadBindings() {
        return load(KEY_BINDINGS, KEY_BINDINGS_TS, UaidBindings.class);
    }

    @Nullable
    CachedValue<UaidManifestInventory> loadManifests() {
        return load(KEY_MANIFESTS, KEY_MANIFESTS_TS, UaidManifestInventory.class);
    }

    private <T> CachedValue<T> load(String valueKey, String tsKey, Class<T> clazz) {
        final String json = prefs.getString(valueKey, null);
        if (json == null) {
            return null;
        }
        final long timestamp = prefs.getLong(tsKey, 0L);
        try {
            T value = gson.fromJson(json, clazz);
            if (value == null) {
                return null;
            }
            return new CachedValue<>(value, timestamp);
        } catch (Exception ignored) {
            return null;
        }
    }

    static final class CachedValue<T> {
        private final T value;
        private final long timestampMs;

        CachedValue(T value, long timestampMs) {
            this.value = Objects.requireNonNull(value, "value");
            this.timestampMs = timestampMs;
        }

        T getValue() {
            return value;
        }

        long getTimestampMs() {
            return timestampMs;
        }
    }
}
