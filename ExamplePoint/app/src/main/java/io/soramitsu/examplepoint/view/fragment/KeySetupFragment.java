package io.soramitsu.examplepoint.view.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.EnumSet;
import java.util.List;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.FragmentKeySetupBinding;
import io.soramitsu.examplepoint.presenter.KeySetupPresenter;
import io.soramitsu.examplepoint.sdk.backup.KeyBackupManager;
import io.soramitsu.examplepoint.sdk.backup.KeyBackupPlan;
import io.soramitsu.examplepoint.sdk.backup.MnemonicGenerator;
import io.soramitsu.examplepoint.view.KeySetupView;

public class KeySetupFragment extends Fragment implements KeySetupView {

    public static final String TAG = KeySetupFragment.class.getSimpleName();

    public interface KeySetupListener {
        void onKeySetupFinished(@NonNull KeyBackupPlan plan, @NonNull EnumSet<KeyBackupManager.BackupDestination> destinations);
    }

    private FragmentKeySetupBinding binding;
    private KeySetupPresenter presenter;
    private KeyBackupPlan activePlan;
    private KeySetupListener listener;

    public static KeySetupFragment newInstance() {
        return new KeySetupFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof KeySetupListener) {
            listener = (KeySetupListener) context;
        } else {
            throw new IllegalStateException("Parent activity must implement KeySetupListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new KeySetupPresenter();
        presenter.setView(this);
        presenter.onCreate();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentKeySetupBinding.inflate(inflater, container, false);
        initViews();
        return binding.getRoot();
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
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        presenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    private void initViews() {
        binding.wordCountToggle.check(R.id.word_count_option_12);
        binding.keySetupGenerate.setOnClickListener(v -> presenter.onGenerateClicked(currentLength()));
        binding.keySetupContinue.setOnClickListener(v -> presenter.onPersistPlan(selectedDestinations()));
        binding.keySetupCopy.setOnClickListener(v -> copyMnemonic());
        binding.keySetupConnect.setOnClickListener(v -> presenter.onConnectWithIrohaConnect());
        binding.keySetupContinue.setEnabled(false);
    }

    private MnemonicGenerator.Length currentLength() {
        int checkedId = binding.wordCountToggle.getCheckedButtonId();
        if (checkedId == R.id.word_count_option_24) {
            return MnemonicGenerator.Length.WORDS_24;
        }
        return MnemonicGenerator.Length.WORDS_12;
    }

    private EnumSet<KeyBackupManager.BackupDestination> selectedDestinations() {
        EnumSet<KeyBackupManager.BackupDestination> destinations = EnumSet.noneOf(KeyBackupManager.BackupDestination.class);
        if (binding.googleBackupSwitch.isChecked()) {
            destinations.add(KeyBackupManager.BackupDestination.GOOGLE_SECURE_STORAGE);
        }
        if (binding.icloudBackupSwitch.isChecked()) {
            destinations.add(KeyBackupManager.BackupDestination.ICLOUD_SECURE_STORAGE);
        }
        return destinations;
    }

    private void copyMnemonic() {
        if (activePlan == null) {
            showError(getString(R.string.key_setup_plan_error));
            return;
        }
        ClipboardManager manager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("mnemonic", activePlan.getPassphrase()));
            Toast.makeText(requireContext(), R.string.message_copy_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showProgress() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideProgress() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void showError(@NonNull String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void renderPlan(@NonNull KeyBackupPlan plan) {
        if (binding == null) {
            return;
        }
        activePlan = plan;
        binding.backupPlanContainer.setVisibility(View.VISIBLE);
        binding.mnemonicWords.setText(formatMnemonic(plan.getMnemonicWords()));
        binding.keySetupContinue.setEnabled(true);
    }

    @Override
    public void clearPlan() {
        activePlan = null;
        if (binding != null) {
            binding.backupPlanContainer.setVisibility(View.GONE);
            binding.keySetupContinue.setEnabled(false);
        }
    }

    @Override
    public void launchIrohaConnect(@NonNull Intent intent) {
        startActivity(intent);
    }

    @Override
    public void showConnectUnavailable() {
        showError(getString(R.string.key_setup_connect_missing));
    }

    @Override
    public void onPlanPersisted(@NonNull KeyBackupPlan plan, @NonNull EnumSet<KeyBackupManager.BackupDestination> destinations) {
        if (listener != null) {
            listener.onKeySetupFinished(plan, destinations);
        }
    }

    private String formatMnemonic(List<String> words) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            builder.append(i + 1).append(". ").append(words.get(i));
            if (i < words.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }
}
