package io.soramitsu.examplepoint.subscription;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SharedPreferences-backed storage for UI metadata keyed by subscription id.
 */
public final class SubscriptionUiMetadataStore {

    private static final String PREFS_NAME = "subscription_ui_metadata";
    private static final String KEY_ENTRIES = "entries_v1";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private final Type mapType = new TypeToken<Map<String, SubscriptionUiMetadata>>() {
    }.getType();

    public SubscriptionUiMetadataStore(@NonNull Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized Map<String, SubscriptionUiMetadata> loadAll() {
        String raw = preferences.getString(KEY_ENTRIES, null);
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, SubscriptionUiMetadata> parsed = gson.fromJson(raw, mapType);
        if (parsed == null || parsed.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(parsed));
    }

    public synchronized SubscriptionUiMetadata load(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.trim().isEmpty()) {
            return null;
        }
        return loadAll().get(subscriptionId);
    }

    public synchronized void upsert(SubscriptionUiMetadata metadata) {
        if (metadata == null || metadata.subscriptionId == null || metadata.subscriptionId.trim().isEmpty()) {
            return;
        }
        Map<String, SubscriptionUiMetadata> mutable = new LinkedHashMap<>(loadAll());
        mutable.put(metadata.subscriptionId, metadata);
        preferences.edit().putString(KEY_ENTRIES, gson.toJson(mutable)).apply();
    }
}
