package io.soramitsu.examplepoint.offline;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SharedPreferences-backed persistence for offline wallet state.
 */
public final class OfflineStateStore implements OfflineStateStoreGateway {

    private static final String PREFS_NAME = "offline_wallet_state";
    private static final String KEY_STATE = "state_v1";

    private final SharedPreferences prefs;

    public OfflineStateStore(@NonNull Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public OfflineState load() {
        final String raw = prefs.getString(KEY_STATE, null);
        if (raw == null || raw.trim().isEmpty()) {
            return OfflineState.empty();
        }
        try {
            JSONObject obj = new JSONObject(raw);
            BigDecimal balance = new BigDecimal(obj.optString("balance", "0"));
            long nextCounter = obj.optLong("next_counter", 0L);
            Set<String> replay = new LinkedHashSet<>();
            JSONArray replayArray = obj.optJSONArray("replay_log");
            if (replayArray != null) {
                for (int i = 0; i < replayArray.length(); i++) {
                    String txId = replayArray.optString(i, null);
                    if (txId != null && !txId.isEmpty()) {
                        replay.add(txId);
                    }
                }
            }
            List<OfflineTransferRecord> history = new ArrayList<>();
            JSONArray historyArray = obj.optJSONArray("history");
            if (historyArray != null) {
                for (int i = 0; i < historyArray.length(); i++) {
                    Object entry = historyArray.get(i);
                    if (entry instanceof JSONObject) {
                        history.add(OfflineTransferRecord.fromJson((JSONObject) entry));
                    }
                }
            }
            return new OfflineState(balance, nextCounter, replay, history);
        } catch (JSONException | NumberFormatException e) {
            return OfflineState.empty();
        }
    }

    @Override
    public void save(@NonNull OfflineState state) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("balance", state.getBalance().toPlainString());
            obj.put("next_counter", state.getNextCounter());

            JSONArray replayArray = new JSONArray();
            for (String txId : state.getReplayLog()) {
                replayArray.put(txId);
            }
            obj.put("replay_log", replayArray);

            JSONArray historyArray = new JSONArray();
            for (OfflineTransferRecord record : state.getHistory()) {
                historyArray.put(record.toJson());
            }
            obj.put("history", historyArray);

            prefs.edit().putString(KEY_STATE, obj.toString()).apply();
        } catch (JSONException e) {
            // Persist nothing if encoding fails.
        }
    }

    public void reset() {
        prefs.edit().remove(KEY_STATE).apply();
    }
}
