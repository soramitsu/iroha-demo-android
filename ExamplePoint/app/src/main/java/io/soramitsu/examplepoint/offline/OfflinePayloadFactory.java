package io.soramitsu.examplepoint.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.hyperledger.iroha.android.crypto.Blake2b;

import java.nio.charset.StandardCharsets;

/**
 * Helpers for constructing offline payloads with deterministic identifiers.
 */
public final class OfflinePayloadFactory {

    private OfflinePayloadFactory() {
    }

    public static String computeTxId(
            @NonNull String sender,
            @NonNull String invoiceId,
            @NonNull String amount,
            long counter
    ) {
        final String preimage = sender + "|" + invoiceId + "|" + amount + "|" + counter;
        byte[] hash = Blake2b.digest(preimage.getBytes(StandardCharsets.UTF_8));
        return toHex(hash);
    }

    public static OfflinePaymentPayload buildPayment(
            @NonNull OfflineInvoice invoice,
            @NonNull String senderAccount,
            long counter,
            @NonNull String channel,
            @Nullable String memo
    ) {
        final String txId = computeTxId(senderAccount, invoice.getInvoiceId(), invoice.getAmount(), counter);
        return new OfflinePaymentPayload(
                txId,
                senderAccount,
                invoice.getReceiver(),
                invoice.getAssetId(),
                invoice.getAmount(),
                invoice.getInvoiceId(),
                counter,
                System.currentTimeMillis(),
                channel,
                memo);
    }

    private static String toHex(byte[] data) {
        final StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte b : data) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
