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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import io.soramitsu.examplepoint.IrohaApplication;
import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.FragmentContactBinding;
import io.soramitsu.examplepoint.presenter.ContactPresenter;
import io.soramitsu.examplepoint.view.ContactView;
import io.soramitsu.examplepoint.view.activity.ContactActivityListener;

public class ContactFragment extends Fragment
        implements ContactView, ContactActivityListener {
    public static final String TAG = ContactFragment.class.getSimpleName();

    public static final String ARGS_PUBLIC_KEY = "public_key";
    public static final String ARGS_CONTACT_NAME = "contact_name";

    private ContactPresenter contactPresenter = new ContactPresenter();

    private FragmentContactBinding binding;

    private ContactListener contactListener;

    public static ContactFragment newInstance(@NotNull String publicKey, @Nullable String name) {
        ContactFragment fragment = new ContactFragment();
        Bundle args = new Bundle();
        args.putString(ARGS_PUBLIC_KEY, publicKey);
        args.putString(ARGS_CONTACT_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactPresenter.setView(this);
        contactPresenter.onCreate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle args = getArguments();
        final String publicKey =  args.getString(ARGS_PUBLIC_KEY);
        final String alias = args.getString(ARGS_CONTACT_NAME);

        contactPresenter.setPublicKey(publicKey);

        binding = DataBindingUtil.bind(view);
        binding.contactPublicKey.setText(publicKey);
        binding.contactName.setText(alias);
        binding.updateButton.setOnClickListener(contactPresenter.onUpdateButtonClicked());
        binding.deleteButton.setOnClickListener(contactPresenter.onDeleteButtonClicked());
        binding.sendButton.setOnClickListener(contactPresenter.onSendButtonClicked());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!(getActivity() instanceof ContactListener)) {
            throw new ClassCastException();
        }
        contactListener = (ContactListener) getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        contactPresenter.onStart();
    }

    @Override
    public void onStop() {
        contactPresenter.onStop();
        super.onStop();
    }

    @Override
    public void finish() {
        getActivity().finish();
    }

    @Override
    public void showError(String error, Throwable throwable) {
        // nothing
    }

    @Override
    public void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public String getAlias() {
        return binding.contactName.getText().toString();
    }

    @Override
    public void setPublicKeyForContactToSend(String publicKey) {
        ((IrohaApplication)getActivity().getApplication()).publicKeyForContactToSend = publicKey;
    }

    @Override
    public void onContactDeleteSuccessful() {
        contactListener.onContactDeleteSuccessful();
    }

    @Override
    public void onQrReadeSuccessful(String result) {
        contactPresenter.qrReadSuccessful(result);
    }
}
