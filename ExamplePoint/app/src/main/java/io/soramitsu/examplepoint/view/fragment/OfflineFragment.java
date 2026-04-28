package io.soramitsu.examplepoint.view.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.WriterException;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.math.BigDecimal;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.BuildConfig;
import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.exception.ErrorMessageFactory;
import io.soramitsu.examplepoint.offline.OfflineAllowanceSnapshot;
import io.soramitsu.examplepoint.offline.OfflineInvoice;
import io.soramitsu.examplepoint.offline.OfflinePaymentPayload;
import io.soramitsu.examplepoint.offline.OfflineReceiveResult;
import io.soramitsu.examplepoint.offline.OfflineRepository;
import io.soramitsu.examplepoint.offline.OfflineState;
import io.soramitsu.examplepoint.offline.OfflineTransferRecord;
import io.soramitsu.examplepoint.offline.OfflinePlatformSnapshot;
import io.soramitsu.examplepoint.offline.DeadlineKind;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.util.QrCodeRenderer;
import io.soramitsu.examplepoint.util.TimeFormatUtils;
import io.soramitsu.examplepoint.view.activity.MainActivity;
import io.soramitsu.examplepoint.view.activity.QrScannerActivity;
import io.soramitsu.examplepoint.databinding.FragmentOfflineBinding;

/**
 * Offline-offline flow hub: top-up allowances, issue invoices, and exchange QR/Bluetooth/NFC payloads.
 */
public class OfflineFragment extends Fragment implements MainActivity.MainActivityListener {

    public static final String TAG = OfflineFragment.class.getSimpleName();

    private FragmentOfflineBinding binding;
    private OfflineRepository offlineRepository;
    private IrohaRepository irohaRepository;
    private OfflineInvoice lastInvoice;
    private OfflinePaymentPayload lastPayment;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    private ActivityResultLauncher<Intent> invoiceScannerLauncher;
    private ActivityResultLauncher<Intent> paymentScannerLauncher;
    private static final long DEADLINE_SOON_THRESHOLD_MS = 3 * 24 * 60 * 60 * 1000L; // 3 days

    public static OfflineFragment newInstance() {
        return new OfflineFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        offlineRepository = new OfflineRepository(requireContext());
        irohaRepository = new IrohaRepository(requireContext());

        invoiceScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        return;
                    }
                    Intent data = result.getData();
                    if (data == null) {
                        return;
                    }
                    String payload = data.getStringExtra(QrScannerActivity.EXTRA_QR_TEXT);
                    if (payload != null) {
                        binding.offlinePaymentInvoicePayload.setText(payload);
                        try {
                            lastInvoice = OfflineInvoice.fromJson(payload);
                        } catch (Exception e) {
                            showError(ErrorMessageFactory.create(requireContext(), e));
                        }
                    }
                }
        );

        paymentScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        return;
                    }
                    Intent data = result.getData();
                    if (data == null) {
                        return;
                    }
                    String payload = data.getStringExtra(QrScannerActivity.EXTRA_QR_TEXT);
                    if (payload != null) {
                        binding.offlineAcceptPayload.setText(payload);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOfflineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.offlineTopUpButton.setOnClickListener(v -> onTopUp());
        binding.offlineGenerateInvoice.setOnClickListener(v -> onGenerateInvoice());
        binding.offlineSendPayment.setOnClickListener(v -> onSendPayment());
        binding.offlineAcceptButton.setOnClickListener(v -> onAcceptPayment());
        binding.offlineScanInvoice.setOnClickListener(v -> launchInvoiceScanner());
        binding.offlineScanPayment.setOnClickListener(v -> launchPaymentScanner());
        binding.offlineShareInvoiceBluetooth.setOnClickListener(v -> shareBluetooth(binding.offlineInvoicePayload.getText().toString()));
        binding.offlineSharePaymentBluetooth.setOnClickListener(v -> shareBluetooth(binding.offlinePaymentPayload.getText().toString()));
        binding.offlineShareInvoiceNfc.setOnClickListener(v -> pushNfc(binding.offlineInvoicePayload.getText().toString()));
        binding.offlineSharePaymentNfc.setOnClickListener(v -> pushNfc(binding.offlinePaymentPayload.getText().toString()));
        binding.offlineMoveToOnline.setOnClickListener(v -> onMoveToOnline());
        refreshState();
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshState();
        if (irohaRepository.hasAccountProfile()) {
            AccountProfile profile = irohaRepository.getAccountProfile();
            binding.offlineOnlineReceiver.setText(profile.getAccountId());
            refreshPlatformSnapshot(profile);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onNavigationItemClicked() {
        // no-op
    }

    private void onTopUp() {
        if (!irohaRepository.hasAccountProfile()) {
            showError(getString(R.string.error_message_user_not_found));
            return;
        }
        binding.offlineProgress.setVisibility(View.VISIBLE);
        AccountProfile profile = irohaRepository.getAccountProfile();
        CompletableFuture<OfflineAllowanceSnapshot> future = offlineRepository.syncAllowances(profile);
        future.thenAccept(snapshot -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                binding.offlineProgress.setVisibility(View.GONE);
                refreshState();
                String synced = dateFormat.format(new Date(snapshot.getSyncedAtMs()));
                binding.offlineLastSync.setText(getString(R.string.offline_last_sync_label, synced));
                binding.offlineAllowanceHint.setText(
                        getString(R.string.offline_balance_label, snapshot.getTotalRemaining().toPlainString()));
                String policyExpiry = TimeFormatUtils.format(snapshot.getNextPolicyExpiryMs());
                String refresh = snapshot.getNextRefreshMs() != null ? TimeFormatUtils.format(snapshot.getNextRefreshMs()) : null;
                String certificateExpiry = TimeFormatUtils.format(snapshot.getNextCertificateExpiryMs());
                if (policyExpiry != null) {
                    binding.offlinePolicyExpiry.setText(getString(R.string.offline_policy_expiry_label, policyExpiry));
                    binding.offlinePolicyExpiry.setVisibility(View.VISIBLE);
                } else {
                    binding.offlinePolicyExpiry.setVisibility(View.GONE);
                }
                if (refresh != null) {
                    binding.offlinePolicyRefresh.setText(getString(R.string.offline_policy_refresh_label, refresh));
                    binding.offlinePolicyRefresh.setVisibility(View.VISIBLE);
                } else {
                    binding.offlinePolicyRefresh.setVisibility(View.GONE);
                }
                if (certificateExpiry != null) {
                    binding.offlineCertificateExpiry.setText(getString(R.string.offline_certificate_expiry_label, certificateExpiry));
                    binding.offlineCertificateExpiry.setVisibility(View.VISIBLE);
                } else {
                    binding.offlineCertificateExpiry.setVisibility(View.GONE);
                }
                binding.offlineAllowanceCount.setText(
                        getString(R.string.offline_allowance_count_label, snapshot.getAllowanceCount()));
                if (snapshot.getPrimaryVerdictIdHex() != null && !snapshot.getPrimaryVerdictIdHex().isEmpty()) {
                    binding.offlineVerdictId.setText(
                            getString(R.string.offline_verdict_label, snapshot.getPrimaryVerdictIdHex()));
                    binding.offlineVerdictId.setVisibility(View.VISIBLE);
                } else {
                    binding.offlineVerdictId.setVisibility(View.GONE);
                }
                if (snapshot.getPrimaryAttestationNonceHex() != null && !snapshot.getPrimaryAttestationNonceHex().isEmpty()) {
                    binding.offlineAttestationNonce.setText(
                            getString(R.string.offline_attestation_nonce_label, snapshot.getPrimaryAttestationNonceHex()));
                    binding.offlineAttestationNonce.setVisibility(View.VISIBLE);
                } else {
                    binding.offlineAttestationNonce.setVisibility(View.GONE);
                }
                if (snapshot.getPrimaryPolicy() != null && !snapshot.getPrimaryPolicy().isEmpty()) {
                    binding.offlinePolicySlug.setText(
                            getString(R.string.offline_policy_slug_label, snapshot.getPrimaryPolicy()));
                    binding.offlinePolicySlug.setVisibility(View.VISIBLE);
                } else {
                    binding.offlinePolicySlug.setVisibility(View.GONE);
                }
                renderDeadlineWarning(snapshot.getEarliestDeadlineMs(), snapshot.getDeadlineKind(),
                        snapshot.getDeadlineState(), snapshot.getDeadlineMsRemaining());
                renderPlatformSnapshot(offlineRepository.lastPlatformSnapshot(), snapshot.getPrimaryPolicy());
                refreshPlatformSnapshot(profile);
            });
        }).exceptionally(throwable -> {
            if (!isAdded()) {
                return null;
            }
            requireActivity().runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                binding.offlineProgress.setVisibility(View.GONE);
                showError(ErrorMessageFactory.create(requireContext(), throwable.getCause() != null
                        ? throwable.getCause()
                        : throwable));
            });
            return null;
        });
    }

    private void onGenerateInvoice() {
        if (!irohaRepository.hasAccountProfile()) {
            showError(getString(R.string.error_message_user_not_found));
            return;
        }
        final String amount = binding.offlineInvoiceAmount.getText().toString().trim();
        if (amount.isEmpty()) {
            showError(getString(R.string.error_message_receiver_required));
            return;
        }
        final String memo = binding.offlineInvoiceMemo.getText().toString().trim();
        AccountProfile profile = irohaRepository.getAccountProfile();
        lastInvoice = offlineRepository.createInvoice(amount, memo, 10 * 60 * 1000L, profile.getAccountId());
        String payload = lastInvoice.toJson();
        binding.offlineInvoicePayload.setText(payload);
        renderQr(binding.offlineInvoiceQr, payload);
        binding.offlinePaymentInvoicePayload.setText(payload);
    }

    private void onSendPayment() {
        if (!irohaRepository.hasAccountProfile()) {
            showError(getString(R.string.error_message_user_not_found));
            return;
        }
        String rawInvoice = binding.offlinePaymentInvoicePayload.getText().toString();
        if (rawInvoice == null || rawInvoice.trim().isEmpty()) {
            showError(getString(R.string.error_message_receiver_required));
            return;
        }
        try {
            OfflineInvoice invoice = OfflineInvoice.fromJson(rawInvoice);
            AccountProfile profile = irohaRepository.getAccountProfile();
            lastPayment = offlineRepository.createPayment(invoice, profile, "qr", invoice.getMemo());
            String payload = lastPayment.toJson();
            binding.offlinePaymentPayload.setText(payload);
            renderQr(binding.offlinePaymentQr, payload);
        } catch (Exception e) {
            showError(ErrorMessageFactory.create(requireContext(), e));
        }
        refreshState();
    }

    private void onAcceptPayment() {
        if (!irohaRepository.hasAccountProfile()) {
            showError(getString(R.string.error_message_user_not_found));
            return;
        }
        String payload = binding.offlineAcceptPayload.getText().toString();
        if (payload == null || payload.trim().isEmpty()) {
            showError(getString(R.string.error_message_receiver_required));
            return;
        }
        try {
            AccountProfile profile = irohaRepository.getAccountProfile();
            OfflineReceiveResult result = offlineRepository.acceptPayment(payload, profile);
            binding.offlineAcceptPayload.setText(result.getPayload().toJson());
            Toast.makeText(requireContext(), R.string.offline_payment_recorded, Toast.LENGTH_SHORT).show();
            refreshState();
        } catch (Exception e) {
            showError(ErrorMessageFactory.create(requireContext(), e));
        }
    }

    private void onMoveToOnline() {
        if (!irohaRepository.hasAccountProfile()) {
            showError(getString(R.string.error_message_user_not_found));
            return;
        }
        final String amountInput = binding.offlineOnlineAmount.getText().toString().trim();
        final String receiver = binding.offlineOnlineReceiver.getText().toString().trim();
        final String memo = binding.offlineOnlineMemo.getText().toString().trim();
        if (receiver.isEmpty()) {
            showError(getString(R.string.error_message_receiver_required));
            return;
        }
        BigDecimal amount;
        try {
            if (amountInput.isEmpty()) {
                amount = offlineRepository.currentState().getBalance();
            } else {
                amount = new BigDecimal(amountInput);
            }
        } catch (NumberFormatException e) {
            showError(getString(R.string.error_message_receiver_required));
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            showError(getString(R.string.error_message_receiver_required));
            return;
        }
        AccountProfile profile = irohaRepository.getAccountProfile();
        OfflineState snapshot = offlineRepository.currentState();
        binding.offlineProgress.setVisibility(View.VISIBLE);
        CompletableFuture<String> future = irohaRepository.resolveAccountTarget(receiver)
                .thenApply(resolvedReceiver -> {
                    offlineRepository.withdrawToOnline(profile.getAccountId(), resolvedReceiver, amount, memo);
                    return resolvedReceiver;
                })
                .thenCompose(resolvedReceiver -> irohaRepository.transferAsset(resolvedReceiver, amount.toPlainString()));
        future.thenAccept(resolvedReceiver -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                binding.offlineProgress.setVisibility(View.GONE);
                binding.offlineOnlineReceiver.setText(resolvedReceiver);
                refreshState();
                Toast.makeText(requireContext(), R.string.offline_move_to_online_success, Toast.LENGTH_SHORT).show();
            });
        }).exceptionally(throwable -> {
            offlineRepository.overwriteState(snapshot);
            if (!isAdded()) {
                return null;
            }
            requireActivity().runOnUiThread(() -> {
                binding.offlineProgress.setVisibility(View.GONE);
                showError(ErrorMessageFactory.create(requireContext(), throwable.getCause() != null
                        ? throwable.getCause()
                        : throwable));
            });
            return null;
        });
    }

    private void refreshState() {
        if (binding == null) {
            return;
        }
        OfflineState state = offlineRepository.currentState();
        binding.offlineBalanceValue.setText(
                getString(R.string.offline_balance_label, state.getBalance().toPlainString()));
        if (binding.offlineOnlineAmount.getText() == null || binding.offlineOnlineAmount.getText().toString().trim().isEmpty()) {
            binding.offlineOnlineAmount.setText(state.getBalance().toPlainString());
        }
        if (offlineRepository.lastSnapshot() != null) {
            String ts = dateFormat.format(new Date(offlineRepository.lastSnapshot().getSyncedAtMs()));
            binding.offlineLastSync.setText(getString(R.string.offline_last_sync_label, ts));
            String policyExpiry = TimeFormatUtils.format(offlineRepository.lastSnapshot().getNextPolicyExpiryMs());
            String refresh = offlineRepository.lastSnapshot().getNextRefreshMs() != null
                    ? TimeFormatUtils.format(offlineRepository.lastSnapshot().getNextRefreshMs())
                    : null;
            String certificateExpiry = TimeFormatUtils.format(offlineRepository.lastSnapshot().getNextCertificateExpiryMs());
            if (policyExpiry != null) {
                binding.offlinePolicyExpiry.setText(getString(R.string.offline_policy_expiry_label, policyExpiry));
                binding.offlinePolicyExpiry.setVisibility(View.VISIBLE);
            } else {
                binding.offlinePolicyExpiry.setVisibility(View.GONE);
            }
            if (refresh != null) {
                binding.offlinePolicyRefresh.setText(getString(R.string.offline_policy_refresh_label, refresh));
                binding.offlinePolicyRefresh.setVisibility(View.VISIBLE);
            } else {
                binding.offlinePolicyRefresh.setVisibility(View.GONE);
            }
            if (certificateExpiry != null) {
                binding.offlineCertificateExpiry.setText(getString(R.string.offline_certificate_expiry_label, certificateExpiry));
                binding.offlineCertificateExpiry.setVisibility(View.VISIBLE);
            } else {
                binding.offlineCertificateExpiry.setVisibility(View.GONE);
            }
            binding.offlineAllowanceCount.setText(
                    getString(R.string.offline_allowance_count_label, offlineRepository.lastSnapshot().getAllowanceCount()));
            if (offlineRepository.lastSnapshot().getPrimaryVerdictIdHex() != null
                    && !offlineRepository.lastSnapshot().getPrimaryVerdictIdHex().isEmpty()) {
                binding.offlineVerdictId.setText(
                        getString(R.string.offline_verdict_label, offlineRepository.lastSnapshot().getPrimaryVerdictIdHex()));
                binding.offlineVerdictId.setVisibility(View.VISIBLE);
            } else {
                binding.offlineVerdictId.setVisibility(View.GONE);
            }
            if (offlineRepository.lastSnapshot().getPrimaryAttestationNonceHex() != null
                    && !offlineRepository.lastSnapshot().getPrimaryAttestationNonceHex().isEmpty()) {
                binding.offlineAttestationNonce.setText(
                        getString(R.string.offline_attestation_nonce_label, offlineRepository.lastSnapshot().getPrimaryAttestationNonceHex()));
                binding.offlineAttestationNonce.setVisibility(View.VISIBLE);
            } else {
                binding.offlineAttestationNonce.setVisibility(View.GONE);
            }
            if (offlineRepository.lastSnapshot().getPrimaryPolicy() != null
                    && !offlineRepository.lastSnapshot().getPrimaryPolicy().isEmpty()) {
                binding.offlinePolicySlug.setText(
                        getString(R.string.offline_policy_slug_label, offlineRepository.lastSnapshot().getPrimaryPolicy()));
                binding.offlinePolicySlug.setVisibility(View.VISIBLE);
            } else {
                binding.offlinePolicySlug.setVisibility(View.GONE);
            }
            renderDeadlineWarning(
                    offlineRepository.lastSnapshot().getEarliestDeadlineMs(),
                    offlineRepository.lastSnapshot().getDeadlineKind(),
                    offlineRepository.lastSnapshot().getDeadlineState(),
                    offlineRepository.lastSnapshot().getDeadlineMsRemaining());
            renderPlatformSnapshot(offlineRepository.lastPlatformSnapshot(),
                    offlineRepository.lastSnapshot().getPrimaryPolicy());
        }
        renderHistory(state);
    }

    private void renderDeadlineWarning(Long earliestDeadlineMs, DeadlineKind kind, @Nullable String serverState, @Nullable Long serverRemainingMs) {
        if (earliestDeadlineMs == null || earliestDeadlineMs <= 0) {
            binding.offlinePolicyStatus.setVisibility(View.GONE);
            return;
        }
        long now = System.currentTimeMillis();
        String suffix = "";
        if (kind != null) {
            suffix = " (" + kind.name().toLowerCase(java.util.Locale.ROOT) + ")";
        }
        if (serverState != null && !serverState.isBlank()) {
            binding.offlinePolicyStatus.setVisibility(View.VISIBLE);
            String remaining = serverRemainingMs != null ? " (" + serverRemainingMs + "ms)" : "";
            binding.offlinePolicyStatus.setText(serverState + suffix + remaining);
            binding.offlinePolicyStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
            return;
        }
        if (earliestDeadlineMs <= now) {
            binding.offlinePolicyStatus.setVisibility(View.VISIBLE);
            binding.offlinePolicyStatus.setText(getString(R.string.offline_deadline_warning_expired) + suffix);
            binding.offlinePolicyStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        } else if (earliestDeadlineMs - now <= DEADLINE_SOON_THRESHOLD_MS) {
            binding.offlinePolicyStatus.setVisibility(View.VISIBLE);
            binding.offlinePolicyStatus.setText(getString(R.string.offline_deadline_warning_soon) + suffix);
            binding.offlinePolicyStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark));
        } else {
            binding.offlinePolicyStatus.setVisibility(View.GONE);
        }
    }

    private void renderPlatformSnapshot(@Nullable OfflinePlatformSnapshot snapshot, @Nullable String policyFallback) {
        if (binding == null) {
            return;
        }
        final String policy = snapshot != null && snapshot.getPolicy() != null && !snapshot.getPolicy().isEmpty()
                ? snapshot.getPolicy()
                : (policyFallback == null ? null : policyFallback);
        if (policy != null && !policy.isEmpty()) {
            binding.offlinePlatformSnapshotPolicy.setText(
                    getString(R.string.offline_platform_snapshot_policy_label, policy));
            binding.offlinePlatformSnapshotPolicy.setVisibility(View.VISIBLE);
        } else {
            binding.offlinePlatformSnapshotPolicy.setVisibility(View.GONE);
        }

        if (snapshot != null && snapshot.hasAttestation()) {
            binding.offlinePlatformSnapshotToken.setText(
                    getString(R.string.offline_platform_snapshot_token_label, snapshot.getAttestationJwsB64()));
            binding.offlinePlatformSnapshotToken.setVisibility(View.VISIBLE);
            binding.offlineCopySnapshot.setVisibility(View.VISIBLE);
            binding.offlineCopySnapshot.setOnClickListener(v ->
                    copyToClipboard("platform_snapshot", snapshot.getAttestationJwsB64()));
            binding.offlineShareSnapshot.setVisibility(View.VISIBLE);
            binding.offlineShareSnapshot.setOnClickListener(v ->
                    shareSnapshot(snapshot));
            if (snapshot.getBundleIdHex() != null && !snapshot.getBundleIdHex().isEmpty()) {
                binding.offlinePlatformSnapshotBundle.setText(
                        getString(R.string.offline_platform_snapshot_bundle_label, snapshot.getBundleIdHex()));
                binding.offlinePlatformSnapshotBundle.setVisibility(View.VISIBLE);
            } else {
                binding.offlinePlatformSnapshotBundle.setVisibility(View.GONE);
            }
        } else if (policy != null && !policy.isEmpty()) {
            binding.offlinePlatformSnapshotToken.setText(R.string.offline_platform_snapshot_missing);
            binding.offlinePlatformSnapshotToken.setVisibility(View.VISIBLE);
            binding.offlineCopySnapshot.setVisibility(View.GONE);
            binding.offlineShareSnapshot.setVisibility(View.GONE);
            binding.offlinePlatformSnapshotBundle.setVisibility(View.GONE);
        } else {
            binding.offlinePlatformSnapshotToken.setVisibility(View.GONE);
            binding.offlineCopySnapshot.setVisibility(View.GONE);
            binding.offlineShareSnapshot.setVisibility(View.GONE);
            binding.offlinePlatformSnapshotBundle.setVisibility(View.GONE);
        }
    }

    private void refreshPlatformSnapshot(@NonNull AccountProfile profile) {
        binding.offlineProgress.setVisibility(View.VISIBLE);
        offlineRepository.fetchLatestPlatformSnapshot(profile).thenAccept(snapshot -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                binding.offlineProgress.setVisibility(View.GONE);
                renderPlatformSnapshot(snapshot, offlineRepository.summarize().getPrimaryPolicy());
            });
        }).exceptionally(throwable -> {
            if (!isAdded()) {
                return null;
            }
            requireActivity().runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                binding.offlineProgress.setVisibility(View.GONE);
                showError(ErrorMessageFactory.create(requireContext(), throwable.getCause() != null
                        ? throwable.getCause()
                        : throwable));
            });
            return null;
        });
    }

    private void copyToClipboard(String label, String contents) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || contents == null || contents.isEmpty()) {
            showError(getString(R.string.offline_unable_to_share));
            return;
        }
        ClipData clip = ClipData.newPlainText(label, contents);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), R.string.offline_copy_snapshot_done, Toast.LENGTH_SHORT).show();
    }

    private void shareSnapshot(OfflinePlatformSnapshot snapshot) {
        if (snapshot == null) {
            showError(getString(R.string.offline_unable_to_share));
            return;
        }
        String accountId = irohaRepository.hasAccountProfile()
                ? irohaRepository.getAccountProfile().getAccountId()
                : null;
        String payload = io.soramitsu.examplepoint.offline.OfflineSnapshotFormatter
                .formatSharePayload(snapshot, accountId);
        shareBluetooth(payload);
    }

    private void renderHistory(OfflineState state) {
        LinearLayout container = binding.offlineHistoryContainer;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (OfflineTransferRecord record : state.getHistory()) {
            View row = inflater.inflate(android.R.layout.simple_list_item_2, container, false);
            TextView title = row.findViewById(android.R.id.text1);
            TextView subtitle = row.findViewById(android.R.id.text2);
            String direction = record.getDirection() == OfflineTransferRecord.Direction.OUTGOING ? "OUT " : "IN  ";
            title.setText(direction + record.getPeer() + " (" + record.getAmount() + ")");
            subtitle.setText(dateFormat.format(new Date(record.getTimestampMs())) + " #" + record.getCounterLabel());
            container.addView(row);
        }
    }

    private void renderQr(View target, String payload) {
        if (!(target instanceof android.widget.ImageView) || payload == null || payload.isEmpty()) {
            return;
        }
        try {
            Bitmap bitmap = QrCodeRenderer.render(payload, 720);
            ((android.widget.ImageView) target).setImageBitmap(bitmap);
        } catch (WriterException e) {
            showError(ErrorMessageFactory.create(requireContext(), e));
        }
    }

    private void launchInvoiceScanner() {
        Intent intent = new Intent(requireContext(), QrScannerActivity.class);
        invoiceScannerLauncher.launch(intent);
    }

    private void launchPaymentScanner() {
        Intent intent = new Intent(requireContext(), QrScannerActivity.class);
        paymentScannerLauncher.launch(intent);
    }

    private void shareBluetooth(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            showError(getString(R.string.offline_unable_to_share));
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, payload);
            startActivity(Intent.createChooser(intent, getString(R.string.offline_share_bluetooth)));
        } catch (Exception e) {
            showError(getString(R.string.offline_unable_to_share));
        }
    }

    private void pushNfc(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            showError(getString(R.string.offline_unable_to_share));
            return;
        }
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(requireContext());
        if (adapter == null) {
            showError(getString(R.string.offline_unable_to_share));
            return;
        }
        NdefMessage message = new NdefMessage(new NdefRecord[]{
                NdefRecord.createMime("text/plain", payload.getBytes(StandardCharsets.UTF_8))
        });
        try {
            Method setNdef = adapter.getClass().getMethod(
                    "setNdefPushMessage",
                    NdefMessage.class,
                    android.app.Activity.class,
                    android.app.Activity[].class);
            setNdef.invoke(adapter, message, requireActivity(), null);
            Toast.makeText(requireContext(), R.string.offline_share_nfc, Toast.LENGTH_SHORT).show();
        } catch (Exception reflectionError) {
            showError(getString(R.string.offline_unable_to_share));
        }
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
