package io.soramitsu.examplepoint.presenter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.exception.ErrorMessageFactory;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.sdk.model.AccountAsset;
import io.soramitsu.examplepoint.sdk.model.AccountTransaction;
import io.soramitsu.examplepoint.view.WalletView;

public class WalletPresenter implements Presenter<WalletView> {

    private static final int DEFAULT_HISTORY_LIMIT = 25;

    private final IrohaRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private WalletView walletView;

    public WalletPresenter(@NonNull Context context) {
        this.repository = new IrohaRepository(context.getApplicationContext());
    }

    @Override
    public void setView(@NonNull WalletView view) {
        walletView = view;
    }

    @Override
    public void onCreate() {
        // no-op
    }

    @Override
    public void onStart() {
        refreshWallet();
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

    public void onPullToRefresh() {
        refreshWallet();
    }

    private void refreshWallet() {
        if (!repository.hasAccountProfile()) {
            walletView.setRefreshing(false);
            walletView.promptReRegistration();
            return;
        }

        walletView.showProgress();
        final AccountProfile profile = repository.getAccountProfile();
        CompletableFuture<List<AccountAsset>> assetsFuture = repository.fetchAccountAssets();
        CompletableFuture<List<AccountTransaction>> transactionsFuture =
                repository.fetchAccountTransactions(DEFAULT_HISTORY_LIMIT);
        CompletableFuture<Void> combined = CompletableFuture.allOf(assetsFuture, transactionsFuture);
        combined.thenRun(() -> mainHandler.post(() -> {
            renderWallet(
                    profile,
                    assetsFuture.join(),
                    transactionsFuture.join(),
                    System.currentTimeMillis());
        }))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    mainHandler.post(() -> handleError(cause));
                    return null;
                });
    }

    private void renderWallet(AccountProfile profile,
                              List<AccountAsset> assets,
                              List<AccountTransaction> transactions,
                              long timestampMs) {
        walletView.hideProgress();
        walletView.setRefreshing(false);
        walletView.renderWallet(profile, assets, transactions, timestampMs);
    }

    private void handleError(Throwable throwable) {
        walletView.hideProgress();
        walletView.setRefreshing(false);
        walletView.showError(ErrorMessageFactory.create(walletView.getContext(), throwable));
    }
}
