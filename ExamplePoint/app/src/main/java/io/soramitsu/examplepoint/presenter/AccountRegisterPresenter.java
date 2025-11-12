/*
Copyright Soramitsu Co., Ltd. 2016 All Rights Reserved.
http://soramitsu.co.jp

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.soramitsu.examplepoint.presenter;

import android.content.Context;
import androidx.annotation.NonNull;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.concurrent.CompletableFuture;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.exception.ErrorMessageFactory;
import io.soramitsu.examplepoint.exception.RequiredArgumentException;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.util.CrashReporter;
import io.soramitsu.examplepoint.view.AccountRegisterView;

public class AccountRegisterPresenter implements Presenter<AccountRegisterView> {
    public static final String TAG = AccountRegisterPresenter.class.getSimpleName();

    private AccountRegisterView accountRegisterView;
    private IrohaRepository irohaRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void setView(@NonNull AccountRegisterView view) {
        accountRegisterView = view;
        if (irohaRepository == null) {
            irohaRepository = new IrohaRepository(view.getContext());
        }
    }

    @Override
    public void onCreate() {
        // nothing
    }

    @Override
    public void onStart() {
        // nothing
    }

    @Override
    public void onResume() {
        // nothing
    }

    @Override
    public void onPause() {
        // nothing
    }

    @Override
    public void onStop() {
        // nothing
    }

    @Override
    public void onDestroy() {
        // nothing
    }

    public View.OnKeyListener onKeyEventOnUserName() {
        return new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    InputMethodManager inputMethodManager =
                            (InputMethodManager) accountRegisterView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        };
    }

    public View.OnClickListener onRegisterClicked() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Context context = accountRegisterView.getContext();
                final String alias = accountRegisterView.getAlias();

                if (alias.isEmpty()) {
                    accountRegisterView.showError(
                            ErrorMessageFactory.create(context, new RequiredArgumentException(), context.getString(R.string.name))
                    );
                    return;
                }

                accountRegisterView.showProgress();
                register(alias);
            }
        };
    }

    private void register(final String alias) {
        if (irohaRepository == null) {
            accountRegisterView.hideProgress();
            accountRegisterView.showError(accountRegisterView.getContext().getString(R.string.error_message_retry_again));
            return;
        }

        CompletableFuture<AccountProfile> future = irohaRepository.registerAccount(alias);
        future.thenAccept(profile -> mainHandler.post(() -> registerSuccessful(profile)))
                .exceptionally(throwable -> {
                    mainHandler.post(() -> registerFailure(throwable.getCause() != null ? throwable.getCause() : throwable));
                    return null;
                });
    }

    private void registerSuccessful(AccountProfile profile) {
        accountRegisterView.hideProgress();
        accountRegisterView.registerSuccessful(profile.getAccountAddress());
    }

    private void registerFailure(Throwable throwable) {
        accountRegisterView.hideProgress();
        Log.e(TAG, "registerFailure: ", throwable);
        CrashReporter.logError(TAG, "Registration failed", throwable);
        accountRegisterView.showError(
                ErrorMessageFactory.create(accountRegisterView.getContext(), throwable));
    }
}
