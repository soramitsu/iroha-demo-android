package io.soramitsu.examplepoint.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.HttpUrl;

import org.hyperledger.iroha.android.norito.NoritoException;
import org.hyperledger.iroha.android.norito.SignedTransactionEncoder;
import org.hyperledger.iroha.android.tx.SignedTransaction;
import org.hyperledger.iroha.android.nexus.UaidBindingsResponse;
import org.hyperledger.iroha.android.nexus.UaidJsonParser;
import org.hyperledger.iroha.android.nexus.UaidLiteral;
import org.hyperledger.iroha.android.nexus.UaidManifestsResponse;
import org.hyperledger.iroha.android.nexus.UaidPortfolioResponse;

import io.soramitsu.examplepoint.data.ToriiConfig;
import io.soramitsu.examplepoint.sdk.model.AccountAsset;
import io.soramitsu.examplepoint.sdk.model.AccountTransaction;
import io.soramitsu.examplepoint.sdk.model.UaidBindings;
import io.soramitsu.examplepoint.sdk.model.UaidManifestInventory;
import io.soramitsu.examplepoint.sdk.model.UaidPortfolio;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Minimal Torii REST client used by the demo application. Focuses on the endpoints that the app
 * needs right now (submitting signed transactions).
 */
public final class ToriiClient {

    private static final MediaType NORITO_MEDIA = MediaType.get("application/x-norito");
    private static final MediaType JSON_MEDIA = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final ToriiConfig config;
    private final Gson gson = new Gson();

    public ToriiClient(ToriiConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void submitTransaction(SignedTransaction transaction) throws IOException, ToriiException {
        final byte[] payloadBytes;
        try {
            payloadBytes = SignedTransactionEncoder.encode(transaction);
        } catch (NoritoException e) {
            throw new ToriiException("Failed to encode signed transaction payload", e);
        }

        HttpUrl url = config.baseUrl().newBuilder()
                .addPathSegments("v1/pipeline/transactions")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(payloadBytes, NORITO_MEDIA))
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ToriiException(buildErrorMessage("submitTransaction", response));
            }
        }
    }

    public List<AccountAsset> fetchAccountAssets(String accountId) throws IOException, ToriiException {
        HttpUrl url = config.baseUrl().newBuilder()
                .addPathSegments("v1/accounts")
                .addPathSegment(accountId)
                .addPathSegments("assets")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ToriiException(buildErrorMessage("fetchAccountAssets", response));
            }
            ResponseBody body = response.body();
            if (body == null) {
                return new ArrayList<>();
            }
            AssetsResponse assetsResponse = gson.fromJson(body.charStream(), AssetsResponse.class);
            List<AccountAsset> assets = new ArrayList<>();
            if (assetsResponse != null && assetsResponse.items != null) {
                for (AssetItem item : assetsResponse.items) {
                    if (item.assetId != null && item.quantity != null) {
                        assets.add(new AccountAsset(item.assetId, item.quantity));
                    }
                }
            }
            return assets;
        }
    }

    public List<AccountTransaction> fetchAccountTransactions(String accountId, int limit) throws IOException, ToriiException {
        HttpUrl.Builder builder = config.baseUrl().newBuilder()
                .addPathSegments("v1/accounts")
                .addPathSegment(accountId)
                .addPathSegments("transactions");
        if (limit > 0) {
            builder.addQueryParameter("limit", String.valueOf(limit));
        }
        builder.addQueryParameter("address_format", "ih58");

        Request request = new Request.Builder()
                .url(builder.build())
                .get()
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ToriiException(buildErrorMessage("fetchAccountTransactions", response));
            }
            ResponseBody body = response.body();
            if (body == null) {
                return new ArrayList<>();
            }
            TransactionsResponse transactionsResponse = gson.fromJson(body.charStream(), TransactionsResponse.class);
            List<AccountTransaction> transactions = new ArrayList<>();
            if (transactionsResponse != null && transactionsResponse.items != null) {
                for (TransactionItem item : transactionsResponse.items) {
                    transactions.add(item.toModel());
                }
            }
            return transactions;
        }
    }

    public UaidPortfolio fetchUaidPortfolio(String uaidLiteral) throws IOException, ToriiException {
        if (uaidLiteral == null || uaidLiteral.trim().isEmpty()) {
            throw new IllegalArgumentException("UAID must be provided");
        }
        final String uaid = UaidLiteral.canonicalize(uaidLiteral.trim());
        HttpUrl url = config.baseUrl().newBuilder()
                .addPathSegments("v1/accounts")
                .addPathSegment(uaid)
                .addPathSegments("portfolio")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ToriiException(buildErrorMessage("fetchUaidPortfolio", response));
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ToriiException("Torii UAID portfolio endpoint returned no payload");
            }
            UaidPortfolioResponse dto = UaidJsonParser.parsePortfolio(body.bytes());
            UaidPortfolio.Totals totals = new UaidPortfolio.Totals(
                    Math.toIntExact(dto.totals().accounts()),
                    Math.toIntExact(dto.totals().positions()));
            List<UaidPortfolio.Dataspace> dsModels = new ArrayList<>();
            for (UaidPortfolioResponse.UaidPortfolioDataspace ds : dto.dataspaces()) {
                List<UaidPortfolio.Account> accountModels = new ArrayList<>();
                for (UaidPortfolioResponse.UaidPortfolioAccount account : ds.accounts()) {
                    List<UaidPortfolio.Asset> assets = new ArrayList<>();
                    for (UaidPortfolioResponse.UaidPortfolioAsset asset : account.assets()) {
                        assets.add(new UaidPortfolio.Asset(
                                asset.assetId(),
                                asset.assetDefinitionId(),
                                asset.quantity()));
                    }
                    accountModels.add(new UaidPortfolio.Account(
                            account.accountId(),
                            account.label(),
                            assets));
                }
                dsModels.add(new UaidPortfolio.Dataspace(
                        ds.dataspaceId(),
                        ds.dataspaceAlias(),
                        accountModels));
            }
            return new UaidPortfolio(dto.uaid(), totals, dsModels);
        }
    }

    public UaidBindings fetchUaidBindings(String uaidLiteral) throws IOException, ToriiException {
        if (uaidLiteral == null || uaidLiteral.trim().isEmpty()) {
            throw new IllegalArgumentException("UAID must be provided");
        }
        final String uaid = UaidLiteral.canonicalize(uaidLiteral.trim());
        HttpUrl url = config.baseUrl().newBuilder()
                .addPathSegments("v1/space-directory/uaids")
                .addPathSegment(uaid)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ToriiException(buildErrorMessage("fetchUaidBindings", response));
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ToriiException("Torii UAID bindings endpoint returned no payload");
            }
            UaidBindingsResponse dto = UaidJsonParser.parseBindings(body.bytes());
            List<UaidBindings.DataspaceBinding> dsModels = new ArrayList<>();
            for (UaidBindingsResponse.UaidBindingsDataspace dataspace : dto.dataspaces()) {
                dsModels.add(new UaidBindings.DataspaceBinding(
                        dataspace.dataspaceId(),
                        dataspace.dataspaceAlias(),
                        dataspace.accounts()));
            }
            return new UaidBindings(dto.uaid(), dsModels);
        }
    }

    public UaidManifestInventory fetchUaidManifests(String uaidLiteral) throws IOException, ToriiException {
        if (uaidLiteral == null || uaidLiteral.trim().isEmpty()) {
            throw new IllegalArgumentException("UAID must be provided");
        }
        final String uaid = UaidLiteral.canonicalize(uaidLiteral.trim());
        HttpUrl url = config.baseUrl().newBuilder()
                .addPathSegments("v1/space-directory/uaids")
                .addPathSegment(uaid)
                .addPathSegments("manifests")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ToriiException(buildErrorMessage("fetchUaidManifests", response));
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ToriiException("Torii UAID manifests endpoint returned no payload");
            }
            UaidManifestsResponse dto = UaidJsonParser.parseManifests(body.bytes());
            List<UaidManifestInventory.ManifestRecord> records = new ArrayList<>();
            for (UaidManifestsResponse.UaidManifestRecord record : dto.manifests()) {
                UaidManifestsResponse.UaidManifestRevocation revocationDto = record.lifecycle().revocation();
                UaidManifestInventory.Revocation revocation = revocationDto == null
                        ? null
                        : new UaidManifestInventory.Revocation(
                        revocationDto.epoch(),
                        revocationDto.reason());
                UaidManifestInventory.Lifecycle lifecycle = new UaidManifestInventory.Lifecycle(
                        record.lifecycle().activatedEpoch(),
                        record.lifecycle().expiredEpoch(),
                        revocation);
                records.add(new UaidManifestInventory.ManifestRecord(
                        record.dataspaceId(),
                        record.dataspaceAlias(),
                        record.manifestHash(),
                        record.status().name().toLowerCase(Locale.US),
                        lifecycle,
                        record.accounts(),
                        record.manifestJson()));
            }
            return new UaidManifestInventory(dto.uaid(), records);
        }
    }

    public ExplorerAccountQrSnapshot fetchExplorerAccountQr(String accountId, String addressFormat)
            throws IOException, ToriiException {
        HttpUrl.Builder builder = config.baseUrl().newBuilder()
                .addPathSegments("v1/explorer/accounts")
                .addPathSegment(accountId)
                .addPathSegment("qr");
        if (addressFormat != null && !addressFormat.trim().isEmpty()) {
            builder.addQueryParameter("address_format", addressFormat.trim());
        }

        Request request = new Request.Builder()
                .url(builder.build())
                .get()
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new ToriiException(buildErrorMessage("fetchExplorerAccountQr", response));
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ToriiException("Torii explorer account QR endpoint returned no payload");
            }
            ExplorerAccountQrResponse dto = gson.fromJson(body.charStream(), ExplorerAccountQrResponse.class);
            if (dto == null) {
                throw new ToriiException("Torii explorer account QR endpoint returned malformed payload");
            }
            return dto.toSnapshot();
        }
    }

    private static String buildErrorMessage(String operation, Response response) {
        final StringBuilder builder = new StringBuilder();
        builder.append("Torii ")
                .append(operation)
                .append(" failed with code ")
                .append(response.code());
        final ResponseBody body = response.body();
        if (body != null) {
            try {
                final String payload = body.string();
                if (!payload.isEmpty()) {
                    builder.append(": ").append(payload);
                }
            } catch (IOException ignored) {
                // Swallow body read errors; the status code is still helpful.
            }
        }
        return builder.toString();
    }

    private static final class AssetsResponse {
        List<AssetItem> items;
    }

    private static final class AssetItem {
        @SerializedName("asset_id")
        String assetId;
        String quantity;
    }

    private static final class TransactionsResponse {
        List<TransactionItem> items;
    }

    private static final class TransactionItem {
        @SerializedName("entrypoint_hash")
        String entrypointHash;
        @SerializedName("result_ok")
        Boolean resultOk;
        String authority;
        @SerializedName("timestamp_ms")
        Long timestampMs;
        @SerializedName("error")
        String errorMessage;

        AccountTransaction toModel() {
            final String hash = entrypointHash != null ? entrypointHash : "";
            final boolean ok = resultOk != null && resultOk;
            final long ts = timestampMs != null ? timestampMs : 0L;
            return new AccountTransaction(hash, ok, authority, ts, errorMessage);
        }
    }

    private static final class ExplorerAccountQrResponse {
        @SerializedName("canonical_id")
        String canonicalId;
        String literal;
        @SerializedName("address_format")
        String addressFormat;
        @SerializedName("network_prefix")
        Integer networkPrefix;
        @SerializedName("error_correction")
        String errorCorrection;
        Integer modules;
        @SerializedName("qr_version")
        Integer qrVersion;
        String svg;

        ExplorerAccountQrSnapshot toSnapshot() {
            if (canonicalId == null || literal == null || addressFormat == null
                    || networkPrefix == null || errorCorrection == null
                    || modules == null || qrVersion == null || svg == null) {
                throw new IllegalStateException("ExplorerAccountQrResponse fields must not be null");
            }
            return new ExplorerAccountQrSnapshot(
                    canonicalId,
                    literal,
                    addressFormat,
                    networkPrefix,
                    errorCorrection,
                    modules,
                    qrVersion,
                    svg
            );
        }
    }

    public static final class ExplorerAccountQrSnapshot {
        private final String canonicalId;
        private final String literal;
        private final String addressFormat;
        private final int networkPrefix;
        private final String errorCorrection;
        private final int modules;
        private final int qrVersion;
        private final String svg;

        public ExplorerAccountQrSnapshot(
                String canonicalId,
                String literal,
                String addressFormat,
                int networkPrefix,
                String errorCorrection,
                int modules,
                int qrVersion,
                String svg
        ) {
            this.canonicalId = canonicalId;
            this.literal = literal;
            this.addressFormat = addressFormat;
            this.networkPrefix = networkPrefix;
            this.errorCorrection = errorCorrection;
            this.modules = modules;
            this.qrVersion = qrVersion;
            this.svg = svg;
        }

        public String getCanonicalId() {
            return canonicalId;
        }

        public String getLiteral() {
            return literal;
        }

        public String getAddressFormat() {
            return addressFormat;
        }

        public int getNetworkPrefix() {
            return networkPrefix;
        }

        public String getErrorCorrection() {
            return errorCorrection;
        }

        public int getModules() {
            return modules;
        }

        public int getQrVersion() {
            return qrVersion;
        }

        public String getSvg() {
            return svg;
        }
    }
}
