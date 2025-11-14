package io.soramitsu.examplepoint.presenter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.zxing.WriterException;

import java.util.concurrent.CompletableFuture;

import io.soramitsu.examplepoint.exception.ErrorMessageFactory;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.sdk.model.AccountReceiveState;
import io.soramitsu.examplepoint.util.QrCodeRenderer;
import io.soramitsu.examplepoint.view.AssetReceiveView;

public class AssetReceivePresenter implements Presenter<AssetReceiveView> {

    private final IrohaRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AssetReceiveView assetReceiveView;

    public AssetReceivePresenter(@NonNull Context context) {
        this.repository = new IrohaRepository(context.getApplicationContext());
    }

    @Override
    public void setView(@NonNull AssetReceiveView view) {
        assetReceiveView = view;
    }

    @Override
    public void onCreate() {
        // no-op
    }

    @Override
    public void onStart() {
        refresh();
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
        refresh();
    }

    private void refresh() {
        if (assetReceiveView == null) {
            return;
        }
        if (!repository.hasAccountProfile()) {
            assetReceiveView.setRefreshing(false);
            assetReceiveView.hideProgress();
            assetReceiveView.promptReRegistration();
            return;
        }
        assetReceiveView.showProgress();
        CompletableFuture<AccountReceiveState> future = repository.fetchAccountReceiveState();
        future.thenAccept(state -> mainHandler.post(() -> render(state)))
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                    mainHandler.post(() -> handleError(cause));
                    return null;
                });
    }

    private void render(AccountReceiveState state) {
        if (assetReceiveView == null) {
            return;
        }
        assetReceiveView.hideProgress();
        assetReceiveView.setRefreshing(false);
        assetReceiveView.renderShareInfo(state.getShareInfo());
        assetReceiveView.renderAssets(state.getAssets());
        try {
            Bitmap bitmap = QrCodeRenderer.render(state.getQrSnapshot().getLiteral(), 768);
            assetReceiveView.showQr(bitmap);
        } catch (WriterException e) {
            assetReceiveView.showError(ErrorMessageFactory.create(assetReceiveView.getContext(), e));
        }
    }

    private void handleError(Throwable throwable) {
        if (assetReceiveView == null) {
            return;
        }
        assetReceiveView.hideProgress();
        assetReceiveView.setRefreshing(false);
        assetReceiveView.showError(ErrorMessageFactory.create(assetReceiveView.getContext(), throwable));
    }
}
