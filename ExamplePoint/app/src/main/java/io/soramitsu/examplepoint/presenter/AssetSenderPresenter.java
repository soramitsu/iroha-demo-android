package io.soramitsu.examplepoint.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.concurrent.CompletableFuture;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.data.ToriiConfig;
import io.soramitsu.examplepoint.exception.ErrorMessageFactory;
import io.soramitsu.examplepoint.sdk.AccountLiteralFormatter;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.view.AssetSenderView;

public class AssetSenderPresenter implements Presenter<AssetSenderView> {

    private final IrohaRepository repository;
    private final ToriiConfig toriiConfig;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AssetSenderView assetSenderView;

    public AssetSenderPresenter(@NonNull Context context) {
        this.repository = new IrohaRepository(context.getApplicationContext());
        this.toriiConfig = ToriiConfig.fromBuildConfig();
    }

    @Override
    public void setView(@NonNull AssetSenderView view) {
        assetSenderView = view;
    }

    @Override
    public void onCreate() {
        // no-op
    }

    @Override
    public void onStart() {
        // no-op
    }

    @Override
    public void onResume() {
        // no-op
    }

    @Override
    public void onPause() {
        // no-op
    }

    @Override
    public void onStop() {
        // no-op
    }

    @Override
    public void onDestroy() {
        // no-op
    }

    public View.OnClickListener onSubmitClicked() {
        return v -> send();
    }

    public TextWatcher textWatcher() {
        return new TextWatcher() {
            private boolean isAmountEmpty;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                String amount = assetSenderView.getAmount();
                isAmountEmpty = amount == null || amount.isEmpty();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isAmountEmpty && "0".contentEquals(s)) {
                    assetSenderView.setAmount("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        };
    }

    private void send() {
        final String receiverLiteral;
        final String amount = assetSenderView.getAmount() == null
                ? ""
                : assetSenderView.getAmount().trim();

        if (amount.isEmpty()) {
            assetSenderView.showError(ErrorMessageFactory.create(
                    assetSenderView.getContext(),
                    new IllegalArgumentException("Receiver and amount are required")));
            return;
        }

        try {
            receiverLiteral = normalizeReceiver(assetSenderView.getReceiver());
        } catch (IllegalArgumentException e) {
            assetSenderView.showError(ErrorMessageFactory.create(assetSenderView.getContext(), e));
            return;
        }

        if (!repository.hasAccountProfile()) {
            assetSenderView.showError(ErrorMessageFactory.create(
                    assetSenderView.getContext(),
                    new IllegalStateException("Account is not registered")));
            return;
        }

        assetSenderView.showProgress();
        CompletableFuture<String> future = repository.transferAsset(receiverLiteral, amount);
        future.thenAccept(resolvedReceiver -> mainHandler.post(() -> onTransferSuccess(resolvedReceiver, amount)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    mainHandler.post(() -> handleError(cause));
                    return null;
                });
    }

    public void onQrScanned(String payload) {
        if (payload == null) {
            assetSenderView.showError(ErrorMessageFactory.create(
                    assetSenderView.getContext(),
                    new IllegalArgumentException(assetSenderView.getContext().getString(R.string.error_invalid_qr_payload))));
            return;
        }
        try {
            String normalized = normalizeReceiver(payload);
            assetSenderView.setReceiver(normalized);
        } catch (IllegalArgumentException e) {
            assetSenderView.showError(ErrorMessageFactory.create(assetSenderView.getContext(), e));
        }
    }

    private String normalizeReceiver(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException(assetSenderView.getContext()
                    .getString(R.string.error_message_receiver_required));
        }
        return AccountLiteralFormatter.normalize(raw, toriiConfig).literal();
    }

    private void onTransferSuccess(String receiver, String amount) {
        assetSenderView.hideProgress();
        assetSenderView.showSuccess(
                assetSenderView.getContext().getString(R.string.successful_title_sent),
                assetSenderView.getContext().getString(
                        R.string.message_send_asset_successful,
                        receiver,
                        amount),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        assetSenderView.hideSuccess();
                        assetSenderView.resetForm();
                    }
                });
    }

    private void handleError(Throwable throwable) {
        assetSenderView.hideProgress();
        assetSenderView.showError(ErrorMessageFactory.create(assetSenderView.getContext(), throwable));
    }
}
