package io.soramitsu.examplepoint.offline;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.hyperledger.iroha.android.client.ClientConfig;
import org.hyperledger.iroha.android.client.HttpClientTransport;
import org.hyperledger.iroha.android.client.OfflineToriiClient;
import org.hyperledger.iroha.android.offline.OfflineAllowanceList;
import org.hyperledger.iroha.android.offline.OfflineListParams;
import org.hyperledger.iroha.android.offline.OfflineTransferList;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.data.ToriiConfig;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * Coordinates offline allowance sync, local state management, and payload construction.
 */
public final class OfflineRepository {

    private final ToriiConfig toriiConfig;
    private final OfflineToriiClient offlineToriiClient;
    private final OfflineStateStoreGateway stateStore;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "OfflineRepository");
        t.setDaemon(true);
        return t;
    });

    private volatile OfflineAllowanceSnapshot lastSnapshot;
    private volatile OfflinePlatformSnapshot lastPlatformSnapshot;

    public OfflineRepository(@NonNull Context context) {
        this(context, ToriiConfig.fromBuildConfig(), new OkHttpClient.Builder().build());
    }

    public OfflineRepository(@NonNull Context context,
                             @NonNull ToriiConfig config,
                             @NonNull OkHttpClient client) {
        this(config, client, new OfflineStateStore(context.getApplicationContext()));
    }

    public OfflineRepository(@NonNull ToriiConfig config,
                             @NonNull OkHttpClient client,
                             @NonNull OfflineStateStoreGateway stateStore) {
        this.toriiConfig = Objects.requireNonNull(config, "toriiConfig");
        Objects.requireNonNull(client, "httpClient");
        ClientConfig clientConfig = ClientConfig.builder()
                .setBaseUri(URI.create(config.baseUrl().toString()))
                .setRequestTimeout(Duration.ofSeconds(30))
                .build();
        this.offlineToriiClient = HttpClientTransport.withDefaultExecutor(clientConfig).offlineToriiClient();
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }

    public OfflineState currentState() {
        return stateStore.load();
    }

    public void overwriteState(OfflineState state) {
        stateStore.save(state);
    }

    public OfflineAllowanceSnapshot lastSnapshot() {
        return lastSnapshot;
    }

    public OfflinePlatformSnapshot lastPlatformSnapshot() {
        return lastPlatformSnapshot;
    }

    public OfflineAllowanceSummary summarize() {
        if (lastSnapshot == null) {
            return new OfflineAllowanceSummary(
                    BigDecimal.ZERO,
                    -1L,
                    null,
                    -1L,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        return new OfflineAllowanceSummary(
                lastSnapshot.getTotalRemaining(),
                lastSnapshot.getNextPolicyExpiryMs(),
                lastSnapshot.getNextRefreshMs(),
                lastSnapshot.getNextCertificateExpiryMs(),
                lastSnapshot.getEarliestDeadlineMs(),
                lastSnapshot.getDeadlineKind(),
                lastSnapshot.getAllowanceCount(),
                lastSnapshot.getPrimaryVerdictIdHex(),
                lastSnapshot.getPrimaryAttestationNonceHex(),
                lastSnapshot.getPrimaryPolicy(),
                lastSnapshot.getDeadlineState(),
                lastSnapshot.getDeadlineMsRemaining());
    }

    public CompletableFuture<OfflineAllowanceSnapshot> syncAllowances(AccountProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return syncAllowancesInternal(profile);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    private OfflineAllowanceSnapshot syncAllowancesInternal(AccountProfile profile) throws IOException {
        try {
            OfflineAllowanceList list = offlineToriiClient
                    .listAllowances(buildAllowancesParams(profile))
                    .join();
            List<OfflineAllowanceList.OfflineAllowanceItem> allowances =
                    list == null ? new ArrayList<>() : list.items();
            OfflineAllowanceSummary summary = OfflineAllowanceSummarizer.summarize(allowances);
            OfflineAllowanceSnapshot snapshot = new OfflineAllowanceSnapshot(
                    allowances,
                    summary.getTotalRemaining(),
                    System.currentTimeMillis(),
                    summary.getEarliestPolicyExpiryMs(),
                    summary.getEarliestRefreshMs(),
                    summary.getAllowanceCount(),
                    summary.getPrimaryVerdictIdHex(),
                    summary.getPrimaryAttestationNonceHex(),
                    summary.getPrimaryPolicy(),
                    summary.getEarliestCertificateExpiryMs(),
                    summary.getEarliestDeadlineMs(),
                    summary.getDeadlineKind(),
                    summary.getDeadlineState(),
                    summary.getDeadlineMsRemaining());
            lastSnapshot = snapshot;
            OfflineState state = stateStore.load();
            OfflineState updated = new OfflineState(
                    summary.getTotalRemaining(),
                    state.getNextCounter(),
                    state.getReplayLog(),
                    state.getHistory());
            stateStore.save(updated);
            return snapshot;
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IOException("Failed to sync offline allowances", cause);
        }
    }

    public CompletableFuture<OfflinePlatformSnapshot> fetchLatestPlatformSnapshot(
            AccountProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchLatestPlatformSnapshotInternal(profile);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    private OfflinePlatformSnapshot fetchLatestPlatformSnapshotInternal(AccountProfile profile) throws IOException {
        try {
            OfflineTransferList list = offlineToriiClient
                    .listTransfers(buildTransfersParams(profile))
                    .join();
            if (list == null || list.items().isEmpty()) {
                lastPlatformSnapshot = null;
                return null;
            }
            for (OfflineTransferList.OfflineTransferItem item : list.items()) {
                OfflineTransferList.OfflineTransferItem.PlatformTokenSnapshot snap = item.platformTokenSnapshot();
                String policy = snap != null ? snap.policy() : item.platformPolicy();
                if ((snap != null && snap.attestationJwsB64() != null && !snap.attestationJwsB64().isBlank())
                        || (policy != null && !policy.isBlank())) {
                    OfflinePlatformSnapshot snapshot = new OfflinePlatformSnapshot(
                            item.bundleIdHex(),
                            policy,
                            snap != null ? snap.attestationJwsB64() : null);
                    lastPlatformSnapshot = snapshot;
                    return snapshot;
                }
            }
            lastPlatformSnapshot = null;
            return null;
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new IOException("Failed to load offline transfers", cause);
        }
    }

    public OfflineState withdrawToOnline(
            @NonNull String accountId,
            @NonNull String receiver,
            @NonNull BigDecimal amount,
            @Nullable String memo
    ) {
        OfflineState state = stateStore.load();
        OfflineState updated = withdrawLocally(state, accountId, receiver, amount, memo, System.currentTimeMillis());
        stateStore.save(updated);
        return updated;
    }

    static OfflineState withdrawLocally(
            OfflineState state,
            String accountId,
            String receiver,
            BigDecimal amount,
            @Nullable String memo,
            long timestampMs
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(amount, "amount");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (state.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient offline balance");
        }
        long counter = state.getNextCounter();
        String txId = OfflinePayloadFactory.computeTxId(accountId, "online-deposit", amount.toPlainString(), counter);
        OfflineTransferRecord record = new OfflineTransferRecord(
                txId,
                OfflineTransferRecord.Direction.OUTGOING,
                "#" + counter,
                amount.toPlainString(),
                receiver,
                timestampMs,
                memo);
        return state
                .nextCounterState()
                .debit(amount, record);
    }

    static HttpUrl buildAllowancesUrl(ToriiConfig config, AccountProfile profile) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(profile, "profile");
        HttpUrl.Builder builder = config.baseUrl().newBuilder()
                .addPathSegments("v1/offline/allowances");
        appendQueryParams(builder, buildAllowancesParams(profile).toQueryParameters());
        return builder.build();
    }

    static HttpUrl buildTransfersUrl(ToriiConfig config, AccountProfile profile) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(profile, "profile");
        HttpUrl.Builder builder = config.baseUrl().newBuilder()
                .addPathSegments("v1/offline/transfers");
        appendQueryParams(builder, buildTransfersParams(profile).toQueryParameters());
        return builder.build();
    }

    static OfflineListParams buildAllowancesParams(AccountProfile profile) {
        return OfflineListParams.builder()
                .filter("controller==" + profile.getAccountId())
                .build();
    }

    static OfflineListParams buildTransfersParams(AccountProfile profile) {
        return OfflineListParams.builder()
                .filter("controller==" + profile.getAccountId())
                .sort("recorded_at_ms:desc")
                .limit(1L)
                .build();
    }

    private static void appendQueryParams(HttpUrl.Builder builder, Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.addQueryParameter(entry.getKey(), entry.getValue());
        }
    }

    public OfflineInvoice createInvoice(String amount, String memo, long validityMillis, String receiverAccount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (receiverAccount == null || receiverAccount.trim().isEmpty()) {
            throw new IllegalArgumentException("Receiver account is required");
        }
        return OfflineInvoice.newInvoice(receiverAccount.trim(), toriiConfig.defaultAssetId(), amount.trim(), validityMillis, memo);
    }

    public OfflinePaymentPayload createPayment(
            @NonNull OfflineInvoice invoice,
            @NonNull AccountProfile profile,
            @NonNull String channel,
            String memo
    ) {
        if (System.currentTimeMillis() > invoice.getExpiresAtMs()) {
            throw new IllegalStateException("Invoice has expired");
        }
        BigDecimal amount = new BigDecimal(invoice.getAmount());
        OfflineState state = stateStore.load();
        if (state.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient offline balance for this payment");
        }
        final long counter = state.getNextCounter();
        OfflinePaymentPayload payload = OfflinePayloadFactory.buildPayment(
                invoice,
                profile.getAccountId(),
                counter,
                channel,
                memo);
        OfflineTransferRecord record = new OfflineTransferRecord(
                payload.getTxId(),
                OfflineTransferRecord.Direction.OUTGOING,
                "#" + counter,
                payload.getAmount(),
                payload.getTo(),
                payload.getTimestampMs(),
                memo);
        OfflineState updated = state
                .nextCounterState()
                .debit(amount, record);
        stateStore.save(updated);
        return payload;
    }

    public OfflineReceiveResult acceptPayment(
            @NonNull String payloadJson,
            @NonNull AccountProfile profile
    ) {
        OfflinePaymentPayload payload = OfflinePaymentPayload.fromJson(payloadJson);
        if (!payload.getTo().equalsIgnoreCase(profile.getAccountId())) {
            throw new IllegalArgumentException("Payment is addressed to a different account");
        }
        OfflineState state = stateStore.load();
        if (state.hasSeen(payload.getTxId())) {
            throw new IllegalStateException("This payment has already been recorded");
        }
        BigDecimal amount = new BigDecimal(payload.getAmount());
        OfflineTransferRecord record = new OfflineTransferRecord(
                payload.getTxId(),
                OfflineTransferRecord.Direction.INCOMING,
                "#" + payload.getCounter(),
                payload.getAmount(),
                payload.getFrom(),
                payload.getTimestampMs(),
                payload.getMemo());
        OfflineState updated = state
                .markSeen(payload.getTxId())
                .credit(amount, record);
        stateStore.save(updated);
        return new OfflineReceiveResult(payload, updated);
    }
}
