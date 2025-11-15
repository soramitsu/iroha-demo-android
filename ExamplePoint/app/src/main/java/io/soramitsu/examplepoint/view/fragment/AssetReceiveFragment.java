package io.soramitsu.examplepoint.view.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.List;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.FragmentAssetReceiveBinding;
import io.soramitsu.examplepoint.navigator.Navigator;
import io.soramitsu.examplepoint.presenter.AssetReceivePresenter;
import io.soramitsu.examplepoint.sdk.model.AccountAsset;
import io.soramitsu.examplepoint.sdk.model.AccountShareInfo;
import io.soramitsu.examplepoint.view.AssetReceiveView;
import io.soramitsu.examplepoint.view.activity.MainActivity;

public class AssetReceiveFragment extends Fragment implements AssetReceiveView, MainActivity.MainActivityListener {

    public static final String TAG = AssetReceiveFragment.class.getSimpleName();

    private FragmentAssetReceiveBinding binding;
    private AssetReceivePresenter presenter;
    private String lastIh58;
    private String lastCompressed;
    private String lastIdentityStatement;

    public static AssetReceiveFragment newInstance() {
        return new AssetReceiveFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new AssetReceivePresenter(requireContext());
        presenter.setView(this);
        presenter.onCreate();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAssetReceiveBinding.inflate(inflater, container, false);
        binding.swipeRefresh.setColorSchemeResources(
                R.color.red600,
                R.color.green600,
                R.color.blue600,
                R.color.orange600
        );
        binding.swipeRefresh.setOnRefreshListener(presenter::onPullToRefresh);
        binding.copyIh58.setOnClickListener(v -> copyToClipboard(lastIh58, getString(R.string.receive_clipboard_ih58)));
        binding.copyCompressed.setOnClickListener(v -> copyToClipboard(lastCompressed, getString(R.string.receive_clipboard_compressed)));
        binding.copyIas.setOnClickListener(v -> copyToClipboard(lastIdentityStatement, getString(R.string.receive_clipboard_ias)));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.onStart();
    }

    @Override
    public void onStop() {
        presenter.onStop();
        super.onStop();
    }

    @Override
    public Context getContext() {
        return requireContext();
    }

    @Override
    public void showProgress() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean isRefreshing() {
        return binding.swipeRefresh.isRefreshing();
    }

    @Override
    public void setRefreshing(boolean refreshing) {
        binding.swipeRefresh.setRefreshing(refreshing);
    }

    @Override
    public void showError(String error) {
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void renderShareInfo(AccountShareInfo info) {
        binding.accountName.setText(info.getDisplayName());
        binding.accountId.setText(getString(R.string.wallet_account_label, info.getAccountId()));
        if (info.getIdentityStatement() == null || info.getIdentityStatement().isEmpty()) {
            binding.accountIas.setText(R.string.receive_ias_unknown);
            lastIdentityStatement = null;
        } else {
            binding.accountIas.setText(info.getIdentityStatement());
            lastIdentityStatement = info.getIdentityStatement();
        }
        binding.ih58Value.setText(String.format("%s@%s", info.getIh58Literal(), info.getDomain()));
        binding.compressedValue.setText(String.format("%s@%s", info.getCompressedLiteral(), info.getDomain()));
        binding.compressedWarning.setText(info.getCompressedWarning());
        lastIh58 = binding.ih58Value.getText().toString();
        lastCompressed = binding.compressedValue.getText().toString();
    }

    @Override
    public void renderAssets(List<AccountAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            binding.pocketMoney.setText(R.string.wallet_empty_balance);
        } else {
            binding.pocketMoney.setText(assets.get(0).getQuantity());
        }
    }

    @Override
    public void showQr(Bitmap qrBitmap) {
        binding.qrCode.setImageBitmap(qrBitmap);
    }

    @Override
    public void promptReRegistration() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.error_message_user_not_found)
                .setPositiveButton(R.string.register, (dialog, which) -> {
                    Navigator.getInstance().navigateToRegisterActivity(requireContext());
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void copyToClipboard(String value, String label) {
        if (value == null || value.isEmpty()) {
            showError(getString(R.string.error_message_receiver_required));
            return;
        }
        ClipboardManager manager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            ClipData data = ClipData.newPlainText(label, value);
            manager.setPrimaryClip(data);
            Toast.makeText(requireContext(), R.string.message_copy_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNavigationItemClicked() {
        // no-op
    }
}
