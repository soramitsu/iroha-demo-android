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

package io.soramitsu.examplepoint.view.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.databinding.FragmentAccountRegisterBinding;
import io.soramitsu.examplepoint.presenter.AccountRegisterPresenter;
import io.soramitsu.examplepoint.view.AccountRegisterView;
import io.soramitsu.examplepoint.view.AccountRegisterView.RegistrationField;
import io.soramitsu.examplepoint.view.dialog.ProgressDialog;
import io.soramitsu.examplepoint.view.dialog.SuccessDialog;

public class AccountRegisterFragment extends Fragment implements AccountRegisterView {
    public static final String TAG = AccountRegisterFragment.class.getSimpleName();
    private static final String ARG_KEY_ALIAS = "key_alias";

    private AccountRegisterPresenter accountRegisterPresenter = new AccountRegisterPresenter();

    private FragmentAccountRegisterBinding binding;
    private SuccessDialog successDialog;
    private ProgressDialog progressDialog;

    private AccountRegisterListener accountRegisterListener;

    public interface AccountRegisterListener {
        void onAccountRegisterSuccessful();
    }

    public static AccountRegisterFragment newInstance(@NonNull String keyAlias) {
        AccountRegisterFragment fragment = new AccountRegisterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEY_ALIAS, keyAlias);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountRegisterPresenter.setView(this);
        if (getArguments() != null) {
            accountRegisterPresenter.setPreparedKeyAlias(getArguments().getString(ARG_KEY_ALIAS));
        }
        accountRegisterPresenter.onCreate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        successDialog = new SuccessDialog(inflater);
        progressDialog = new ProgressDialog(inflater);
        return inflater.inflate(R.layout.fragment_account_register, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentAccountRegisterBinding.bind(view);
        binding.displayName.setOnKeyListener(accountRegisterPresenter.onKeyEventOnUserName());
        binding.registerButton.setOnClickListener(accountRegisterPresenter.onRegisterClicked());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!(getActivity() instanceof AccountRegisterListener)) {
            throw new ClassCastException();
        }
        accountRegisterListener = (AccountRegisterListener) getActivity();
    }

    @Override
    public void onStop() {
        super.onStop();
        accountRegisterPresenter.onStop();
    }

    @Override
    public void showError(final String error) {
        binding.displayNameContainer.setError(error);
        binding.displayNameContainer.setErrorEnabled(true);
    }

    @Override
    public void showFieldError(@NonNull RegistrationField field, @NonNull String error) {
        switch (field) {
            case DISPLAY_NAME:
                setFieldError(binding.displayNameContainer, error);
                break;
            case LEGAL_NAME:
                setFieldError(binding.legalNameContainer, error);
                break;
            case DOCUMENT_TYPE:
                setFieldError(binding.documentTypeContainer, error);
                break;
            case DOCUMENT_NUMBER:
                setFieldError(binding.documentNumberContainer, error);
                break;
            case RESIDENCY:
                setFieldError(binding.residencyContainer, error);
                break;
            case CONTACT:
                setFieldError(binding.contactContainer, error);
                break;
        }
    }

    @Override
    public void clearFieldErrors() {
        clearError(binding.displayNameContainer);
        clearError(binding.legalNameContainer);
        clearError(binding.documentTypeContainer);
        clearError(binding.documentNumberContainer);
        clearError(binding.residencyContainer);
        clearError(binding.contactContainer);
    }

    @Override
    public void registerSuccessful(final AccountProfile profile) {
        successDialog.show(
                getActivity(),
                getString(R.string.register),
                getString(
                        R.string.message_account_register_successful_with_id,
                        profile.getAccountId(),
                        profile.getUaid() != null ? profile.getUaid() : getString(R.string.receive_uaid_unknown)
                ),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        successDialog.hide();
                        accountRegisterListener.onAccountRegisterSuccessful();
                    }
                });
    }

    @Override
    public String getDisplayName() {
        return binding.displayName.getText() != null ? binding.displayName.getText().toString() : "";
    }

    @Override
    public String getLegalName() {
        return binding.legalName.getText() != null ? binding.legalName.getText().toString() : "";
    }

    @Override
    public String getDocumentNumber() {
        return binding.documentNumber.getText() != null ? binding.documentNumber.getText().toString() : "";
    }

    @Override
    public String getDocumentType() {
        return binding.documentType.getText() != null ? binding.documentType.getText().toString() : "";
    }

    @Override
    public String getResidencyCountry() {
        return binding.residency.getText() != null ? binding.residency.getText().toString() : "";
    }

    @Override
    public String getContactInfo() {
        return binding.contact.getText() != null ? binding.contact.getText().toString() : "";
    }

    @Override
    public void showProgress() {
        clearFieldErrors();
        progressDialog.show(getActivity(), getString(R.string.during_registration));
    }

    @Override
    public void hideProgress() {
        progressDialog.hide();
    }

    private void setFieldError(TextInputLayout layout, String error) {
        layout.setError(error);
        layout.setErrorEnabled(true);
    }

    private void clearError(TextInputLayout layout) {
        layout.setError(null);
        layout.setErrorEnabled(false);
    }
}
