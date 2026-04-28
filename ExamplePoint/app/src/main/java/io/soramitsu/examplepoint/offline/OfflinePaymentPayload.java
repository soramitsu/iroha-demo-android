package io.soramitsu.examplepoint.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Canonical payload exchanged between payer and payee in offline-offline settlement.
 */
public final class OfflinePaymentPayload {

    private final String txId;
    private final String from;
    private final String to;
    private final String asset;
    private final String amount;
    private final String invoiceId;
    private final long counter;
    private final long timestampMs;
    private final String channel;
    private final String memo;

    public OfflinePaymentPayload(
            @NonNull String txId,
            @NonNull String from,
            @NonNull String to,
            @NonNull String asset,
            @NonNull String amount,
            @NonNull String invoiceId,
            long counter,
            long timestampMs,
            @NonNull String channel,
            @Nullable String memo
    ) {
        this.txId = Objects.requireNonNull(txId, "txId");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.asset = Objects.requireNonNull(asset, "asset");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.invoiceId = Objects.requireNonNull(invoiceId, "invoiceId");
        this.counter = counter;
        this.timestampMs = timestampMs;
        this.channel = Objects.requireNonNull(channel, "channel");
        this.memo = memo;
    }

    public String getTxId() {
        return txId;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getAsset() {
        return asset;
    }

    public String getAmount() {
        return amount;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public long getCounter() {
        return counter;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public String getChannel() {
        return channel;
    }

    @Nullable
    public String getMemo() {
        return memo;
    }

    public String toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("tx_id", txId);
            obj.put("from", from);
            obj.put("to", to);
            obj.put("asset", asset);
            obj.put("amount", amount);
            obj.put("invoice_id", invoiceId);
            obj.put("counter", counter);
            obj.put("timestamp_ms", timestampMs);
            obj.put("channel", channel);
            if (memo != null && !memo.trim().isEmpty()) {
                obj.put("memo", memo);
            }
            return obj.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to encode offline payment payload", e);
        }
    }

    public static OfflinePaymentPayload fromJson(@NonNull String json) {
        try {
            JSONObject obj = new JSONObject(Objects.requireNonNull(json, "json"));
            final String txId = obj.getString("tx_id");
            final String from = obj.getString("from");
            final String to = obj.getString("to");
            final String asset = obj.getString("asset");
            final String amount = obj.getString("amount");
            final String invoiceId = obj.getString("invoice_id");
            final long counter = obj.optLong("counter", 0L);
            final long timestamp = obj.optLong("timestamp_ms", System.currentTimeMillis());
            final String channel = obj.optString("channel", "qr");
            final String memo = obj.optString("memo", null);
            return new OfflinePaymentPayload(
                    txId,
                    from,
                    to,
                    asset,
                    amount,
                    invoiceId,
                    counter,
                    timestamp,
                    channel,
                    memo);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid offline payment payload", e);
        }
    }
}
