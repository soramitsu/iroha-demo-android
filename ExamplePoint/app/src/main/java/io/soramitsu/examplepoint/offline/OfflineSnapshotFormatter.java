package io.soramitsu.examplepoint.offline;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Formats platform token snapshots for sharing/export.
 */
public final class OfflineSnapshotFormatter {
    private OfflineSnapshotFormatter() {}

    /**
     * Builds a compact JSON blob containing the platform token snapshot plus bundle/account context.
     */
    public static String formatSharePayload(@NonNull OfflinePlatformSnapshot snapshot,
                                            @Nullable String accountId) {
        try {
            JSONObject json = new JSONObject();
            json.put("bundle_id_hex", snapshot.getBundleIdHex());
            if (snapshot.getPolicy() != null) {
                json.put("policy", snapshot.getPolicy());
            }
            if (snapshot.getAttestationJwsB64() != null) {
                json.put("attestation_jws_b64", snapshot.getAttestationJwsB64());
            }
            if (accountId != null && !accountId.isBlank()) {
                json.put("account_id", accountId);
            }
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
