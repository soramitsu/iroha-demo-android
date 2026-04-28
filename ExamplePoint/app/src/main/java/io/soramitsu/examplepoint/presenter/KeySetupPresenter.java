package io.soramitsu.examplepoint.presenter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import io.soramitsu.examplepoint.BuildConfig;
import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.exception.ErrorMessageFactory;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.sdk.backup.KeyBackupManager;
import io.soramitsu.examplepoint.sdk.backup.KeyBackupPlan;
import io.soramitsu.examplepoint.sdk.backup.MnemonicGenerator;
import io.soramitsu.examplepoint.view.KeySetupView;

public class KeySetupPresenter implements Presenter<KeySetupView> {

    private static final String TAG = KeySetupPresenter.class.getSimpleName();

    private KeySetupView keySetupView;
    private IrohaRepository irohaRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private CompletableFuture<KeyBackupPlan> planFuture;
    private CompletableFuture<Void> persistFuture;
    private KeyBackupPlan currentPlan;

    @Override
    public void setView(@NonNull KeySetupView view) {
        this.keySetupView = view;
        if (irohaRepository == null) {
            irohaRepository = new IrohaRepository(view.getContext());
        }
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
        if (planFuture != null) {
            planFuture.cancel(true);
        }
        if (persistFuture != null) {
            persistFuture.cancel(true);
        }
    }

    @Override
    public void onDestroy() {
        // no-op
    }

    public void onGenerateClicked(@NonNull MnemonicGenerator.Length length) {
        if (keySetupView == null || irohaRepository == null) {
            return;
        }
        keySetupView.clearPlan();
        currentPlan = null;
        keySetupView.showProgress();
        planFuture = irohaRepository.getKeyBackupManager()
                .generateBackupPlan(length);
        planFuture.thenAccept(plan -> mainHandler.post(() -> handlePlanGenerated(plan)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    mainHandler.post(() -> handlePlanError(cause));
                    return null;
                });
    }

    public void onPersistPlan(@NonNull EnumSet<KeyBackupManager.BackupDestination> destinations) {
        if (keySetupView == null || irohaRepository == null) {
            return;
        }
        if (currentPlan == null) {
            keySetupView.showError(keySetupView.getContext().getString(R.string.key_setup_plan_error));
            return;
        }
        keySetupView.showProgress();
        persistFuture = irohaRepository.getKeyBackupManager()
                .persistPlan(currentPlan, destinations);
        persistFuture.thenRun(() -> mainHandler.post(() -> handlePersisted(destinations)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    mainHandler.post(() -> handlePersistError(cause));
                    return null;
                });
    }

    public void onConnectWithIrohaConnect() {
        if (keySetupView == null) {
            return;
        }
        Context context = keySetupView.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("iroha://connect?chain_id=" + BuildConfig.TORII_CHAIN_ID));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            keySetupView.launchIrohaConnect(intent);
        } else {
            keySetupView.showConnectUnavailable();
        }
    }

    private void handlePlanGenerated(KeyBackupPlan plan) {
        if (keySetupView == null) {
            return;
        }
        currentPlan = plan;
        keySetupView.hideProgress();
        keySetupView.renderPlan(plan);
    }

    private void handlePlanError(Throwable throwable) {
        if (keySetupView == null) {
            return;
        }
        keySetupView.hideProgress();
        Log.e(TAG, "Failed to generate key backup plan", throwable);
        String message = ErrorMessageFactory.create(keySetupView.getContext(), throwable);
        keySetupView.showError(message);
    }

    private void handlePersisted(EnumSet<KeyBackupManager.BackupDestination> destinations) {
        if (keySetupView == null || currentPlan == null) {
            return;
        }
        keySetupView.hideProgress();
        keySetupView.onPlanPersisted(currentPlan, destinations);
    }

    private void handlePersistError(Throwable throwable) {
        if (keySetupView == null) {
            return;
        }
        keySetupView.hideProgress();
        Log.e(TAG, "Failed to persist key backup plan", throwable);
        String message = ErrorMessageFactory.create(keySetupView.getContext(), throwable);
        keySetupView.showError(message);
    }
}
