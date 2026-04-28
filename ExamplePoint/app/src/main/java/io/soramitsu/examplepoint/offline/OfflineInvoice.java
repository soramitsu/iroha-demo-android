package io.soramitsu.examplepoint.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Lightweight invoice payload that receivers present to payers during offline-offline flows.
 */
public final class OfflineInvoice {

    private final String invoiceId;
    private final String receiver;
    private final String assetId;
    private final String amount;
    private final long createdAtMs;
    private final long expiresAtMs;
    private final String memo;

    public OfflineInvoice(
            @NonNull String invoiceId,
            @NonNull String receiver,
            @NonNull String assetId,
            @NonNull String amount,
            long createdAtMs,
            long expiresAtMs,
            @Nullable String memo
    ) {
        this.invoiceId = Objects.requireNonNull(invoiceId, "invoiceId");
        this.receiver = Objects.requireNonNull(receiver, "receiver");
        this.assetId = Objects.requireNonNull(assetId, "assetId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.createdAtMs = createdAtMs;
        this.expiresAtMs = expiresAtMs;
        this.memo = memo;
    }

    public static OfflineInvoice newInvoice(
            @NonNull String receiver,
            @NonNull String assetId,
            @NonNull String amount,
            long validityMillis,
            @Nullable String memo
    ) {
        final long created = System.currentTimeMillis();
        final long expires = created + Math.max(validityMillis, 0L);
        return new OfflineInvoice(UUID.randomUUID().toString(), receiver, assetId, amount, created, expires, memo);
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAmount() {
        return amount;
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }

    public long getExpiresAtMs() {
        return expiresAtMs;
    }

    @Nullable
    public String getMemo() {
        return memo;
    }

    public String toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("invoice_id", invoiceId);
            obj.put("receiver", receiver);
            obj.put("asset", assetId);
            obj.put("amount", amount);
            obj.put("created_at_ms", createdAtMs);
            obj.put("expires_at_ms", expiresAtMs);
            if (memo != null && !memo.trim().isEmpty()) {
                obj.put("memo", memo);
            }
            return obj.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to encode invoice JSON", e);
        }
    }

    public static OfflineInvoice fromJson(@NonNull String json) {
        try {
            JSONObject obj = new JSONObject(Objects.requireNonNull(json, "json"));
            final String invoiceId = obj.getString("invoice_id");
            final String receiver = obj.getString("receiver");
            final String assetId = obj.getString("asset");
            final String amount = obj.getString("amount");
            final long createdAt = obj.optLong("created_at_ms", System.currentTimeMillis());
            final long expiresAt = obj.optLong("expires_at_ms", createdAt);
            final String memo = obj.optString("memo", null);
            return new OfflineInvoice(invoiceId, receiver, assetId, amount, createdAt, expiresAt, memo);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid offline invoice payload", e);
        }
    }
}
