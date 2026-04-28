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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.FragmentAssetSenderBinding;
import io.soramitsu.examplepoint.presenter.AssetSenderPresenter;
import io.soramitsu.examplepoint.view.AssetSenderView;
import io.soramitsu.examplepoint.view.activity.MainActivity;
import io.soramitsu.examplepoint.view.activity.QrScannerActivity;
import io.soramitsu.examplepoint.view.dialog.ErrorDialog;
import io.soramitsu.examplepoint.view.dialog.ProgressDialog;
import io.soramitsu.examplepoint.view.dialog.SuccessDialog;

public class AssetSenderFragment extends Fragment
        implements AssetSenderView, MainActivity.MainActivityListener {
    public static final String TAG = AssetSenderFragment.class.getSimpleName();

    private AssetSenderPresenter assetSenderPresenter;

    private FragmentAssetSenderBinding binding;
    private ActivityResultLauncher<Intent> qrScannerLauncher;
    private ErrorDialog errorDialog;
    private SuccessDialog successDialog;
    private ProgressDialog progressDialog;

    public static AssetSenderFragment newInstance() {
        AssetSenderFragment fragment = new AssetSenderFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assetSenderPresenter = new AssetSenderPresenter(requireContext());
        assetSenderPresenter.setView(this);
        assetSenderPresenter.onCreate();
        qrScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        return;
                    }
                    Intent data = result.getData();
                    if (data == null) {
                        assetSenderPresenter.onQrScanned(null);
                        return;
                    }
                    String payload = data.getStringExtra(QrScannerActivity.EXTRA_QR_TEXT);
                    assetSenderPresenter.onQrScanned(payload);
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        errorDialog = new ErrorDialog(inflater);
        successDialog = new SuccessDialog(inflater);
        progressDialog = new ProgressDialog(inflater);
        binding = FragmentAssetSenderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.submitButton.setOnClickListener(assetSenderPresenter.onSubmitClicked());
        binding.amount.addTextChangedListener(assetSenderPresenter.textWatcher());
        binding.qrButton.setOnClickListener(v -> launchQrScanner());
    }

    @Override
    public void onStart() {
        super.onStart();
        assetSenderPresenter.onStart();
    }

    @Override
    public void onStop() {
        assetSenderPresenter.onStop();
        super.onStop();
    }

    @Override
    public void showError(String error) {
        errorDialog.show(getActivity(), error, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
    }

    @Override
    public void showSuccess(String title, String message, View.OnClickListener onClickListener) {
        successDialog.show(getActivity(), title, message, onClickListener);
    }

    @Override
    public void hideSuccess() {
        successDialog.hide();
    }

    @Override
    public String getAmount() {
        return binding.amount.getText().toString();
    }

    @Override
    public void setAmount(String amount) {
        binding.amount.setText(amount);
    }

    @Override
    public String getReceiver() {
        return binding.receiver.getText().toString();
    }

    @Override
    public void setReceiver(String receiver) {
        binding.receiver.setText(receiver);
        binding.receiver.setSelection(receiver.length());
    }

    @Override
    public void resetForm() {
        binding.receiver.setText("");
        binding.amount.setText("");
    }

    @Override
    public void showProgress() {
        progressDialog.show(getActivity(), getString(R.string.sending));
    }

    @Override
    public void hideProgress() {
        progressDialog.hide();
    }

    @Override
    public void onNavigationItemClicked() {
        // nothing
    }

    private void launchQrScanner() {
        Intent intent = new Intent(requireContext(), QrScannerActivity.class);
        qrScannerLauncher.launch(intent);
    }
}
