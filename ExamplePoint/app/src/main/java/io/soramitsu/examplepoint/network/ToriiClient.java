package io.soramitsu.examplepoint.network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

import io.soramitsu.examplepoint.data.ToriiConfig;

/**
 * Minimal Torii REST client used by the demo application. Focuses on the endpoints that the app
 * needs right now (submitting signed transactions).
 */
public final class ToriiClient {

    private static final MediaType NORITO_MEDIA = MediaType.get("application/x-norito");

    private final OkHttpClient httpClient;
    private final ToriiConfig config;

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
}
