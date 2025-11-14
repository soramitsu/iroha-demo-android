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
import androidx.annotation.Nullable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.exception.ErrorMessageFactory;
import io.soramitsu.examplepoint.exception.RequiredArgumentException;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.sdk.registration.AccountRegistrationRequest;
import io.soramitsu.examplepoint.util.CrashReporter;
import io.soramitsu.examplepoint.view.AccountRegisterView;
import io.soramitsu.examplepoint.view.AccountRegisterView.RegistrationField;

public class AccountRegisterPresenter implements Presenter<AccountRegisterView> {
    public static final String TAG = AccountRegisterPresenter.class.getSimpleName();

    private AccountRegisterView accountRegisterView;
    private IrohaRepository irohaRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private String preparedKeyAlias;

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

    public void setPreparedKeyAlias(@Nullable String keyAlias) {
        this.preparedKeyAlias = keyAlias;
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
                accountRegisterView.clearFieldErrors();
                final AccountRegistrationRequest request = buildRequest();
                if (request == null) {
                    return;
                }
                accountRegisterView.showProgress();
                register(request);
            }
        };
    }

    private AccountRegistrationRequest buildRequest() {
        final Context context = accountRegisterView.getContext();
        boolean invalid = false;

        final String displayName = accountRegisterView.getDisplayName().trim();
        if (displayName.isEmpty()) {
            accountRegisterView.showFieldError(
                    RegistrationField.DISPLAY_NAME,
                    ErrorMessageFactory.create(context, new RequiredArgumentException(), context.getString(R.string.name)));
            invalid = true;
        }

        final String legalName = accountRegisterView.getLegalName().trim();
        if (legalName.isEmpty()) {
            accountRegisterView.showFieldError(
                    RegistrationField.LEGAL_NAME,
                    context.getString(R.string.registration_legal_name_required));
            invalid = true;
        }

        final String documentType = accountRegisterView.getDocumentType().trim().toUpperCase(Locale.US);
        if (documentType.isEmpty()) {
            accountRegisterView.showFieldError(
                    RegistrationField.DOCUMENT_TYPE,
                    context.getString(R.string.registration_document_type_required));
            invalid = true;
        }

        final String documentNumber = accountRegisterView.getDocumentNumber().trim();
        if (documentNumber.isEmpty()) {
            accountRegisterView.showFieldError(
                    RegistrationField.DOCUMENT_NUMBER,
                    context.getString(R.string.registration_document_number_required));
            invalid = true;
        }

        final String residency = accountRegisterView.getResidencyCountry().trim().toUpperCase(Locale.US);
        if (residency.isEmpty()) {
            accountRegisterView.showFieldError(
                    RegistrationField.RESIDENCY,
                    context.getString(R.string.registration_residency_required));
            invalid = true;
        }

        final String contact = accountRegisterView.getContactInfo().trim();
        if (contact.isEmpty()) {
            accountRegisterView.showFieldError(
                    RegistrationField.CONTACT,
                    context.getString(R.string.registration_contact_required));
            invalid = true;
        }

        if (invalid) {
            return null;
        }

        if (preparedKeyAlias == null || preparedKeyAlias.trim().isEmpty()) {
            accountRegisterView.showError(context.getString(R.string.error_message_retry_again));
            return null;
        }

        return new AccountRegistrationRequest(
                displayName,
                legalName,
                documentType,
                documentNumber,
                residency,
                contact,
                preparedKeyAlias
        );
    }

    private void register(final AccountRegistrationRequest request) {
        if (irohaRepository == null) {
            accountRegisterView.hideProgress();
            accountRegisterView.showError(accountRegisterView.getContext().getString(R.string.error_message_retry_again));
            return;
        }

        CompletableFuture<AccountProfile> future = irohaRepository.registerAccount(request);
        future.thenAccept(profile -> mainHandler.post(() -> registerSuccessful(profile)))
                .exceptionally(throwable -> {
                    mainHandler.post(() -> registerFailure(throwable.getCause() != null ? throwable.getCause() : throwable));
                    return null;
                });
    }

    private void registerSuccessful(AccountProfile profile) {
        accountRegisterView.hideProgress();
        accountRegisterView.registerSuccessful(profile);
    }

    private void registerFailure(Throwable throwable) {
        accountRegisterView.hideProgress();
        Log.e(TAG, "registerFailure: ", throwable);
        CrashReporter.logError(TAG, "Registration failed", throwable);
        accountRegisterView.showError(
                ErrorMessageFactory.create(accountRegisterView.getContext(), throwable));
    }
}
