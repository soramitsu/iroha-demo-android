package io.soramitsu.examplepoint.view;

import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.EnumSet;

import io.soramitsu.examplepoint.sdk.backup.KeyBackupManager;
import io.soramitsu.examplepoint.sdk.backup.KeyBackupPlan;

public interface KeySetupView extends LoadingView {

    void showError(@NonNull String message);

    void renderPlan(@NonNull KeyBackupPlan plan);

    void clearPlan();

    void launchIrohaConnect(@NonNull Intent intent);

    void showConnectUnavailable();

    void onPlanPersisted(@NonNull KeyBackupPlan plan, @NonNull EnumSet<KeyBackupManager.BackupDestination> destinations);
}
