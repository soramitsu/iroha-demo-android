package io.soramitsu.examplepoint.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Captures minimal offline transfer history for local audit and replay protection.
 */
public final class OfflineTransferRecord {

    public enum Direction {
        OUTGOING,
        INCOMING
    }

    private final String txId;
    private final Direction direction;
    private final String counterLabel;
    private final String amount;
    private final String peer;
    private final long timestampMs;
    private final String memo;

    public OfflineTransferRecord(
            @NonNull String txId,
            @NonNull Direction direction,
            @NonNull String counterLabel,
            @NonNull String amount,
            @NonNull String peer,
            long timestampMs,
            @Nullable String memo
    ) {
        this.txId = Objects.requireNonNull(txId, "txId");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.counterLabel = Objects.requireNonNull(counterLabel, "counterLabel");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.peer = Objects.requireNonNull(peer, "peer");
        this.timestampMs = timestampMs;
        this.memo = memo;
    }

    public String getTxId() {
        return txId;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getCounterLabel() {
        return counterLabel;
    }

    public String getAmount() {
        return amount;
    }

    public String getPeer() {
        return peer;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    @Nullable
    public String getMemo() {
        return memo;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("tx_id", txId);
        obj.put("direction", direction.name().toLowerCase());
        obj.put("counter_label", counterLabel);
        obj.put("amount", amount);
        obj.put("peer", peer);
        obj.put("timestamp_ms", timestampMs);
        if (memo != null && !memo.trim().isEmpty()) {
            obj.put("memo", memo);
        }
        return obj;
    }

    public static OfflineTransferRecord fromJson(JSONObject obj) throws JSONException {
        final String txId = obj.getString("tx_id");
        final String directionSlug = obj.optString("direction", "incoming");
        final Direction direction = "outgoing".equalsIgnoreCase(directionSlug)
                ? Direction.OUTGOING
                : Direction.INCOMING;
        final String counterLabel = obj.optString("counter_label", "-");
        final String amount = obj.getString("amount");
        final String peer = obj.getString("peer");
        final long timestamp = obj.optLong("timestamp_ms", System.currentTimeMillis());
        final String memo = obj.optString("memo", null);
        return new OfflineTransferRecord(txId, direction, counterLabel, amount, peer, timestamp, memo);
    }
}
