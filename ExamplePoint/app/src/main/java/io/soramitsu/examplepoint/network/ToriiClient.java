package io.soramitsu.examplepoint.network;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.HttpUrl;

import org.hyperledger.iroha.android.tx.SignedTransaction;
import org.hyperledger.iroha.android.nexus.UaidBindingsResponse;
import org.hyperledger.iroha.android.nexus.UaidJsonParser;
import org.hyperledger.iroha.android.nexus.UaidLiteral;
import org.hyperledger.iroha.android.nexus.UaidManifestsResponse;
import org.hyperledger.iroha.android.nexus.UaidPortfolioResponse;
import org.hyperledger.iroha.android.client.ClientConfig;
import org.hyperledger.iroha.android.client.CanonicalRequestSigner;
import org.hyperledger.iroha.android.client.ClientResponse;
import org.hyperledger.iroha.android.client.HttpClientTransport;

import io.soramitsu.examplepoint.data.ToriiConfig;
import io.soramitsu.examplepoint.sdk.AccountIdCodec;
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

    private static final MediaType JSON_MEDIA = MediaType.get("application/json");
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final OkHttpClient httpClient;
    private final ToriiConfig config;
    private final HttpClientTransport transport;
    private final Gson gson = new Gson();

    public ToriiClient(ToriiConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.transport = HttpClientTransport.withDefaultExecutor(
                ClientConfig.builder()
                        .setBaseUri(URI.create(config.baseUrl().toString()))
                        .setRequestTimeout(DEFAULT_TIMEOUT)
                        .build()
        );
    }

    public void submitTransaction(SignedTransaction transaction) throws IOException, ToriiException {
        try {
            ClientResponse response = transport.submitTransaction(transaction).join();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String payload = new String(response.body(), StandardCharsets.UTF_8).trim();
                String message = "Torii submitTransaction failed with code " + response.statusCode();
                if (!payload.isEmpty()) {
                    message = message + ": " + payload;
                }
                throw new ToriiException(message);
            }
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new ToriiException("Failed to submit signed transaction via SDK transport", cause);
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
        builder.addQueryParameter("address_format", "i105");

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
                                asset.asset(),
                                asset.scope(),
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

    public String resolveAccountAlias(String alias,
                                      String authorityAccountId,
                                      PrivateKey authorityPrivateKey) throws IOException, ToriiException {
        if (alias == null || alias.trim().isEmpty()) {
            throw new IllegalArgumentException("Account alias is required");
        }
        if (authorityAccountId == null || authorityAccountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Authority account ID is required");
        }
        if (authorityPrivateKey == null) {
            throw new IllegalArgumentException("Authority private key is required");
        }

        HttpUrl url = config.baseUrl().newBuilder()
                .addPathSegments("v1/aliases/resolve")
                .build();
        AliasResolveRequest payload = new AliasResolveRequest(alias);
        byte[] bodyBytes = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);

        final Map<String, String> signedHeaders;
        try {
            signedHeaders = CanonicalRequestSigner.buildHeaders(
                    "POST",
                    URI.create(url.toString()),
                    bodyBytes,
                    authorityAccountId,
                    authorityPrivateKey
            );
        } catch (RuntimeException ex) {
            throw new ToriiException("Failed to sign alias resolution request", ex);
        }

        RequestBody requestBody = RequestBody.create(bodyBytes, JSON_MEDIA);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Accept", "application/json");
        for (Map.Entry<String, String> header : signedHeaders.entrySet()) {
            requestBuilder.header(header.getKey(), header.getValue());
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new ToriiException(buildErrorMessage("resolveAccountAlias", response));
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new ToriiException("Torii alias resolve endpoint returned no payload");
            }
            AliasResolveResponse dto = gson.fromJson(body.charStream(), AliasResolveResponse.class);
            if (dto == null
                    || dto.alias == null || dto.alias.trim().isEmpty()
                    || dto.accountId == null || dto.accountId.trim().isEmpty()) {
                throw new ToriiException("Torii alias resolve endpoint returned malformed payload");
            }
            if (!AccountIdCodec.isCanonicalAccountId(dto.accountId, config.i105Discriminant())) {
                throw new ToriiException("Torii alias resolve endpoint returned a non-canonical account ID");
            }
            return dto.accountId;
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

    private static final class AliasResolveRequest {
        String alias;

        AliasResolveRequest(String alias) {
            this.alias = alias;
        }
    }

    private static final class AliasResolveResponse {
        String alias;
        @SerializedName("account_id")
        String accountId;
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

}
