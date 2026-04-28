package io.soramitsu.examplepoint.view.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.hyperledger.iroha.android.connect.ConnectWalletRequest;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.soramitsu.examplepoint.BuildConfig;
import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.connect.ConnectSeedSignerFactory;
import io.soramitsu.examplepoint.connect.ConnectSigningIdentity;
import io.soramitsu.examplepoint.connect.ConnectWalletSessionRunner;
import io.soramitsu.examplepoint.sdk.IrohaRepository;

/** Handles inbound {@code iroha://connect?...} links and signs requests as wallet role. */
public final class IrohaWalletConnectActivity extends AppCompatActivity {

    private static final String TAG = "IrohaWalletConnect";

    public static final String EXTRA_CONNECT_DECISION = "connect_decision";
    public static final String EXTRA_CONNECT_SEED_HEX = "connect_seed_hex";
    public static final String EXTRA_CONNECT_ACCOUNT_DOMAIN = "connect_account_domain";
    public static final String EXTRA_CONNECT_REQUEST_ID = "connect_request_id";
    public static final String EXTRA_CONNECT_WALLET_URI_B64 = "connect_wallet_uri_b64";

    private static final AtomicBoolean SESSION_RUNNING = new AtomicBoolean(false);

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ConnectWalletActivity");
        thread.setDaemon(true);
        return thread;
    });

    private IrohaRepository irohaRepository;
    private CompletableFuture<?> currentTask;

    private TextView titleView;
    private TextView detailsView;
    private TextView statusView;
    private ProgressBar progressView;
    private Button rejectButton;
    private Button approveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iroha_wallet_connect);
        irohaRepository = new IrohaRepository(getApplicationContext());

        titleView = findViewById(R.id.connect_wallet_title);
        detailsView = findViewById(R.id.connect_wallet_details);
        statusView = findViewById(R.id.connect_wallet_status);
        progressView = findViewById(R.id.connect_wallet_progress);
        rejectButton = findViewById(R.id.connect_wallet_reject);
        approveButton = findViewById(R.id.connect_wallet_approve);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        executor.shutdownNow();
    }

    private void handleIntent(@Nullable Intent intent) {
        if (intent == null) {
            finishWithError("missing intent");
            return;
        }
        Uri deepLink = decodeUriFromExtras(intent);
        if (deepLink == null) {
            deepLink = intent.getData();
        }
        if (deepLink == null) {
            finishWithError("missing connect deep link");
            return;
        }

        final ConnectWalletRequest request;
        try {
            URI defaultBaseUri = URI.create(BuildConfig.TORII_BASE_URL);
            request = ConnectWalletRequest.parse(deepLink.toString(), defaultBaseUri);
        } catch (Exception e) {
            finishWithError("invalid connect request: " + e.getMessage());
            return;
        }

        String requestId = firstNonBlank(
                intent.getStringExtra(EXTRA_CONNECT_REQUEST_ID),
                deepLink.getQueryParameter("request_id"),
                UUID.randomUUID().toString());

        setHeader(request);

        ConnectWalletSessionRunner.Decision forcedDecision = parseDecision(intent, deepLink);
        if (forcedDecision != null) {
            startSession(request, requestId, forcedDecision, true);
            return;
        }

        rejectButton.setEnabled(true);
        approveButton.setEnabled(true);
        progressView.setIndeterminate(false);
        progressView.setVisibility(View.INVISIBLE);
        statusView.setText(R.string.connect_wallet_status_waiting);

        rejectButton.setOnClickListener(v -> startSession(
                request,
                requestId,
                ConnectWalletSessionRunner.Decision.REJECT,
                false));
        approveButton.setOnClickListener(v -> startSession(
                request,
                requestId,
                ConnectWalletSessionRunner.Decision.APPROVE,
                false));
    }

    private void setHeader(@NonNull ConnectWalletRequest request) {
        Objects.requireNonNull(request, "request");
        String host = request.baseUri().getHost();
        if (host == null || host.trim().isEmpty()) {
            host = request.baseUri().toString();
        }
        titleView.setText(getString(R.string.connect_wallet_title_text));
        detailsView.setText(getString(
                R.string.connect_wallet_details,
                host,
                request.sessionFingerprintHex()));
    }

    private void startSession(@NonNull ConnectWalletRequest request,
                              @NonNull String requestId,
                              @NonNull ConnectWalletSessionRunner.Decision decision,
                              boolean autoTriggered) {
        if (!SESSION_RUNNING.compareAndSet(false, true)) {
            finishWithError("another connect session is already running");
            return;
        }

        rejectButton.setEnabled(false);
        approveButton.setEnabled(false);
        progressView.setVisibility(View.VISIBLE);
        progressView.setIndeterminate(true);
        statusView.setText(getString(R.string.connect_wallet_status_connecting));

        emitEvent("start", requestId, request.sidBase64Url(), "decision=" + decision.name().toLowerCase(Locale.ROOT));

        currentTask = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        ConnectSigningIdentity signer = resolveSigner(getIntent());
                        return ConnectWalletSessionRunner.run(
                                request,
                                signer,
                                decision,
                                stage -> emitEvent("stage", requestId, request.sidBase64Url(), stage));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }, executor)
                .whenComplete((result, throwable) -> mainHandler.post(() -> {
                    SESSION_RUNNING.set(false);
                    progressView.setIndeterminate(false);
                    progressView.setVisibility(View.INVISIBLE);

                    if (throwable != null) {
                        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                        String message = summarizeThrowable(cause);
                        Log.e(
                                TAG,
                                "connect session failed request_id="
                                        + requestId
                                        + " sid="
                                        + request.sidBase64Url()
                                        + " summary="
                                        + message,
                                cause);
                        statusView.setText(getString(R.string.connect_wallet_status_failed, message));
                        emitEvent("failed", requestId, request.sidBase64Url(), message);
                        Toast.makeText(this, getString(R.string.connect_wallet_failed_toast), Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    ConnectWalletSessionRunner.Result outcome = result;
                    String details = "outcome=" + outcome.outcome().name().toLowerCase(Locale.ROOT)
                            + (outcome.domainTag() == null ? "" : ",domain_tag=" + outcome.domainTag());
                    emitEvent("result", requestId, request.sidBase64Url(), details);

                    if (outcome.outcome() == ConnectWalletSessionRunner.Outcome.SIGNED) {
                        statusView.setText(getString(R.string.connect_wallet_status_signed));
                        Toast.makeText(this, getString(R.string.connect_wallet_signed_toast), Toast.LENGTH_SHORT).show();
                    } else if (outcome.outcome() == ConnectWalletSessionRunner.Outcome.REJECTED_BY_USER) {
                        statusView.setText(getString(R.string.connect_wallet_status_rejected));
                        if (!autoTriggered) {
                            Toast.makeText(this, getString(R.string.connect_wallet_rejected_toast), Toast.LENGTH_SHORT).show();
                        }
                    } else if (outcome.outcome() == ConnectWalletSessionRunner.Outcome.REJECTED_AFTER_REQUEST) {
                        statusView.setText(getString(R.string.connect_wallet_status_rejected_after_request));
                    } else {
                        statusView.setText(getString(R.string.connect_wallet_status_closed));
                    }
                    finish();
                }));
    }

    @NonNull
    private ConnectSigningIdentity resolveSigner(@NonNull Intent intent) {
        String seedHex = firstNonBlank(intent.getStringExtra(EXTRA_CONNECT_SEED_HEX));
        if (seedHex != null) {
            String domain = firstNonBlank(
                    intent.getStringExtra(EXTRA_CONNECT_ACCOUNT_DOMAIN),
                    BuildConfig.TORII_DOMAIN);
            return ConnectSeedSignerFactory.fromSeedHex(seedHex, domain);
        }
        return irohaRepository.loadActiveConnectSigningIdentity();
    }

    @Nullable
    private ConnectWalletSessionRunner.Decision parseDecision(@NonNull Intent intent, @NonNull Uri deepLink) {
        String value = firstNonBlank(
                intent.getStringExtra(EXTRA_CONNECT_DECISION),
                deepLink.getQueryParameter("connect_decision"),
                deepLink.getQueryParameter("e2e_decision"));
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("approve".equals(normalized) || "sign".equals(normalized)) {
            return ConnectWalletSessionRunner.Decision.APPROVE;
        }
        if ("reject".equals(normalized)) {
            return ConnectWalletSessionRunner.Decision.REJECT;
        }
        if ("reject_after_request".equals(normalized)
                || "reject-after-request".equals(normalized)) {
            return ConnectWalletSessionRunner.Decision.REJECT_AFTER_REQUEST;
        }
        return null;
    }

    @NonNull
    private static String summarizeThrowable(@NonNull Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable cursor = throwable;
        int depth = 0;
        while (cursor != null && depth < 3) {
            if (depth > 0) {
                builder.append(" | caused_by=");
            }
            builder.append(cursor.getClass().getSimpleName());
            String message = cursor.getMessage();
            if (message != null && !message.trim().isEmpty()) {
                builder.append(": ")
                        .append(message.replace('\n', ' ').replace('\r', ' ').trim());
            }
            cursor = cursor.getCause();
            depth += 1;
        }
        String summary = builder.toString();
        if (summary.length() > 700) {
            return summary.substring(0, 700);
        }
        return summary;
    }

    @Nullable
    private static String firstNonBlank(@Nullable String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    @Nullable
    private Uri decodeUriFromExtras(@NonNull Intent intent) {
        String payload = firstNonBlank(intent.getStringExtra(EXTRA_CONNECT_WALLET_URI_B64));
        if (payload == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.decode(payload, Base64.DEFAULT);
            String uri = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            return Uri.parse(uri);
        } catch (RuntimeException ex) {
            Log.e(TAG, "failed to decode connect wallet uri extra", ex);
            return null;
        }
    }

    private void finishWithError(@NonNull String reason) {
        Log.e(TAG, reason);
        Toast.makeText(this, reason, Toast.LENGTH_LONG).show();
        finish();
    }

    private void emitEvent(@NonNull String event,
                           @NonNull String requestId,
                           @NonNull String sid,
                           @NonNull String details) {
        String line = "IROHA_CONNECT_E2E event=" + event
                + " request_id=" + requestId
                + " sid=" + sid
                + " " + details;
        Log.i(TAG, line);
    }
}
