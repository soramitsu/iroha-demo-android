package io.soramitsu.examplepoint.view.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.FragmentSubscriptionHubBinding;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.subscription.SubscriptionAmountFormatter;
import io.soramitsu.examplepoint.subscription.SubscriptionAmountType;
import io.soramitsu.examplepoint.subscription.SubscriptionCadence;
import io.soramitsu.examplepoint.subscription.SubscriptionCreateInput;
import io.soramitsu.examplepoint.subscription.SubscriptionRecord;
import io.soramitsu.examplepoint.subscription.SubscriptionStatus;
import io.soramitsu.examplepoint.subscription.SubscriptionUsageCapPolicy;
import io.soramitsu.examplepoint.subscription.SubscriptionUsageInput;
import io.soramitsu.examplepoint.view.activity.MainActivity;

public class SubscriptionHubFragment extends Fragment implements MainActivity.MainActivityListener {

    public static final String TAG = SubscriptionHubFragment.class.getSimpleName();

    private FragmentSubscriptionHubBinding binding;
    private IrohaRepository repository;
    private final List<SubscriptionRecord> records = new ArrayList<>();
    private final List<SubscriptionRecord> usageTargets = new ArrayList<>();
    private ArrayAdapter<String> usageTargetAdapter;
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    public static SubscriptionHubFragment newInstance() {
        return new SubscriptionHubFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new IrohaRepository(requireContext());
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSubscriptionHubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.subscriptionAddButton.setOnClickListener(v -> showAddDialog());
        binding.subscriptionUsageSubmit.setOnClickListener(v -> submitUsage());
        usageTargetAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        usageTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.subscriptionUsageTarget.setAdapter(usageTargetAdapter);
        refreshState();
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshState();
    }

    @Override
    public void onNavigationItemClicked() {
        if (binding != null) {
            binding.subscriptionScroll.smoothScrollTo(0, 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void refreshState() {
        repository.fetchSubscriptions()
                .thenAccept(items -> {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        if (binding == null) {
                            return;
                        }
                        records.clear();
                        records.addAll(items);
                        Collections.sort(records, Comparator.comparingLong(item -> item.nextChargeAtMs));
                        renderSummary();
                        renderList();
                        renderUsageTargets();
                    });
                })
                .exceptionally(throwable -> {
                    if (!isAdded()) {
                        return null;
                    }
                    requireActivity().runOnUiThread(() -> showActionError(
                            getString(R.string.subscription_action_failed, rootMessage(throwable))
                    ));
                    return null;
                });
    }

    private void renderSummary() {
        int active = 0;
        int paused = 0;
        int pastDue = 0;
        int suspended = 0;
        int canceled = 0;
        for (SubscriptionRecord record : records) {
            if (record.status == SubscriptionStatus.ACTIVE) {
                active += 1;
            } else if (record.status == SubscriptionStatus.PAUSED) {
                paused += 1;
            } else if (record.status == SubscriptionStatus.PAST_DUE) {
                pastDue += 1;
            } else if (record.status == SubscriptionStatus.SUSPENDED) {
                suspended += 1;
            } else {
                canceled += 1;
            }
        }
        binding.subscriptionSummary.setText(
                getString(R.string.subscription_summary_format, active, paused, pastDue, suspended, canceled)
        );

        SubscriptionRecord next = null;
        for (SubscriptionRecord record : records) {
            if (record.status == SubscriptionStatus.ACTIVE || record.status == SubscriptionStatus.PAST_DUE) {
                next = record;
                break;
            }
        }
        if (next != null) {
            String nextDate = dateFormat.format(new Date(next.nextChargeAtMs));
            binding.subscriptionNextDue.setText(
                    getString(R.string.subscription_next_due_format, next.merchantName, nextDate)
            );
        } else {
            binding.subscriptionNextDue.setText(R.string.subscription_next_due_empty);
        }
    }

    private void renderList() {
        binding.subscriptionList.removeAllViews();
        binding.subscriptionEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
        String unit = getString(R.string.subscription_unit);
        for (int i = 0; i < records.size(); i++) {
            SubscriptionRecord record = records.get(i);
            View row = LayoutInflater.from(getContext())
                    .inflate(R.layout.row_subscription, binding.subscriptionList, false);
            TextView name = row.findViewById(R.id.subscription_item_name);
            TextView status = row.findViewById(R.id.subscription_item_status);
            TextView amount = row.findViewById(R.id.subscription_item_amount);
            TextView detail = row.findViewById(R.id.subscription_item_detail);
            Button pause = row.findViewById(R.id.subscription_item_pause);
            Button cancel = row.findViewById(R.id.subscription_item_cancel);
            Button chargeNow = row.findViewById(R.id.subscription_item_charge_now);

            name.setText(record.merchantName);
            amount.setText(SubscriptionAmountFormatter.label(
                    record.amountType,
                    resolveDisplayAmount(record),
                    record.maxAmount,
                    unit
            ));
            status.setText(statusLabel(record));
            status.setBackgroundResource(statusBackground(record));
            detail.setText(detailLabel(record, unit));

            boolean canPause = record.status == SubscriptionStatus.ACTIVE
                    || record.status == SubscriptionStatus.PAUSED
                    || record.status == SubscriptionStatus.PAST_DUE;
            pause.setEnabled(canPause);
            pause.setText(record.status == SubscriptionStatus.PAUSED
                    ? R.string.subscription_resume
                    : R.string.subscription_pause);

            boolean canCancelToggle = record.status != SubscriptionStatus.CANCELED;
            cancel.setEnabled(canCancelToggle);
            cancel.setText(record.cancelAtPeriodEnd
                    ? R.string.subscription_keep
                    : R.string.subscription_cancel_at_period_end);

            boolean canChargeNow = record.status == SubscriptionStatus.ACTIVE
                    || record.status == SubscriptionStatus.PAST_DUE;
            chargeNow.setEnabled(canChargeNow);

            pause.setOnClickListener(v -> {
                CompletableFuture<Void> action = record.status == SubscriptionStatus.PAUSED
                        ? repository.resumeSubscription(record.id)
                        : repository.pauseSubscription(record.id);
                runAction(action,
                        getString(record.status == SubscriptionStatus.PAUSED
                                ? R.string.subscription_resume_done
                                : R.string.subscription_pause_done));
            });

            cancel.setOnClickListener(v -> {
                CompletableFuture<Void> action = record.cancelAtPeriodEnd
                        ? repository.keepSubscription(record.id)
                        : repository.cancelSubscription(record.id);
                runAction(action,
                        getString(record.cancelAtPeriodEnd
                                ? R.string.subscription_keep_done
                                : R.string.subscription_cancel_done));
            });

            chargeNow.setOnClickListener(v -> runAction(
                    repository.chargeNowSubscription(record.id),
                    getString(R.string.subscription_charge_now_done)
            ));

            binding.subscriptionList.addView(row);
        }
    }

    private BigDecimal resolveDisplayAmount(SubscriptionRecord record) {
        if (record.amountType == SubscriptionAmountType.VARIABLE) {
            return record.unitPrice;
        }
        return record.amount;
    }

    private String statusLabel(SubscriptionRecord record) {
        if (record.status == SubscriptionStatus.CANCELED) {
            return getString(R.string.subscription_status_canceled);
        }
        if (record.status == SubscriptionStatus.SUSPENDED) {
            return getString(R.string.subscription_status_suspended);
        }
        if (record.status == SubscriptionStatus.PAST_DUE) {
            return getString(R.string.subscription_status_past_due);
        }
        if (record.cancelAtPeriodEnd) {
            return getString(R.string.subscription_status_canceling);
        }
        if (record.status == SubscriptionStatus.PAUSED) {
            return getString(R.string.subscription_status_paused);
        }
        return getString(R.string.subscription_status_active);
    }

    private int statusBackground(SubscriptionRecord record) {
        if (record.status == SubscriptionStatus.CANCELED) {
            return R.drawable.bg_subscription_status_canceled;
        }
        if (record.status == SubscriptionStatus.SUSPENDED) {
            return R.drawable.bg_subscription_status_suspended;
        }
        if (record.status == SubscriptionStatus.PAST_DUE) {
            return R.drawable.bg_subscription_status_past_due;
        }
        if (record.cancelAtPeriodEnd) {
            return R.drawable.bg_subscription_status_canceling;
        }
        if (record.status == SubscriptionStatus.PAUSED) {
            return R.drawable.bg_subscription_status_paused;
        }
        return R.drawable.bg_subscription_status_active;
    }

    private String detailLabel(SubscriptionRecord record, String unit) {
        if (record.status == SubscriptionStatus.CANCELED) {
            String when = record.lastChargeAtMs != null
                    ? dateFormat.format(new Date(record.lastChargeAtMs))
                    : dateFormat.format(new Date(record.nextChargeAtMs));
            return getString(R.string.subscription_detail_canceled, when);
        }
        String next = dateFormat.format(new Date(record.nextChargeAtMs));
        String cadence = cadenceLabel(record.cadence);
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.subscription_detail_next, next, cadence));

        if (record.amountType == SubscriptionAmountType.VARIABLE && record.unitKey != null && record.unitPrice != null) {
            builder.append("\n").append(getString(
                    R.string.subscription_usage_detail,
                    record.unitKey,
                    record.unitPrice.stripTrailingZeros().toPlainString(),
                    unit,
                    record.usageAccumulated != null
                            ? record.usageAccumulated.stripTrailingZeros().toPlainString()
                            : "0"
            ));
            if (record.maxAmount != null) {
                builder.append("\n").append(getString(
                        R.string.subscription_usage_cap_detail,
                        unit,
                        record.maxAmount.stripTrailingZeros().toPlainString()
                ));
            }
        }

        if (record.lastChargeAmount != null && record.lastChargeAtMs != null) {
            String lastDate = dateFormat.format(new Date(record.lastChargeAtMs));
            builder.append("\n");
            builder.append(getString(
                    R.string.subscription_detail_last,
                    SubscriptionAmountFormatter.label(
                            SubscriptionAmountType.FIXED,
                            record.lastChargeAmount,
                            null,
                            unit
                    ),
                    lastDate
            ));
        }
        if (record.note != null && !record.note.trim().isEmpty()) {
            builder.append("\n").append(record.note);
        }
        return builder.toString();
    }

    private String cadenceLabel(SubscriptionCadence cadence) {
        if (cadence == SubscriptionCadence.QUARTERLY) {
            return getString(R.string.subscription_cadence_quarterly);
        }
        if (cadence == SubscriptionCadence.YEARLY) {
            return getString(R.string.subscription_cadence_yearly);
        }
        return getString(R.string.subscription_cadence_monthly);
    }

    private void renderUsageTargets() {
        usageTargets.clear();
        for (SubscriptionRecord record : records) {
            if (record.amountType == SubscriptionAmountType.VARIABLE
                    && record.unitKey != null
                    && !record.unitKey.isBlank()
                    && record.status != SubscriptionStatus.CANCELED) {
                usageTargets.add(record);
            }
        }

        List<String> labels = new ArrayList<>();
        for (SubscriptionRecord target : usageTargets) {
            labels.add(target.merchantName + " (" + target.id + ")");
        }
        usageTargetAdapter.clear();
        usageTargetAdapter.addAll(labels);
        usageTargetAdapter.notifyDataSetChanged();

        boolean hasTargets = !usageTargets.isEmpty();
        binding.subscriptionUsageTarget.setEnabled(hasTargets);
        binding.subscriptionUsageDelta.setEnabled(hasTargets);
        binding.subscriptionUsageSubmit.setEnabled(hasTargets);
        binding.subscriptionUsageEmpty.setVisibility(hasTargets ? View.GONE : View.VISIBLE);
    }

    private void submitUsage() {
        if (usageTargets.isEmpty()) {
            showActionError(getString(R.string.subscription_usage_no_targets));
            return;
        }
        int index = binding.subscriptionUsageTarget.getSelectedItemPosition();
        if (index < 0 || index >= usageTargets.size()) {
            showActionError(getString(R.string.subscription_usage_no_targets));
            return;
        }
        SubscriptionRecord record = usageTargets.get(index);

        BigDecimal delta = parseAmount(binding.subscriptionUsageDelta);
        if (delta == null || delta.signum() <= 0) {
            showActionError(getString(R.string.subscription_usage_delta_required));
            return;
        }
        if (record.unitPrice == null || record.unitKey == null) {
            showActionError(getString(R.string.subscription_usage_not_supported));
            return;
        }

        if (SubscriptionUsageCapPolicy.exceedsCap(
                record.usageAccumulated,
                delta,
                record.unitPrice,
                record.maxAmount
        )) {
            BigDecimal projected = SubscriptionUsageCapPolicy.projectedCharge(
                    record.usageAccumulated,
                    delta,
                    record.unitPrice
            );
            showActionError(getString(
                    R.string.subscription_usage_cap_exceeded,
                    getString(R.string.subscription_unit),
                    projected.stripTrailingZeros().toPlainString(),
                    record.maxAmount.stripTrailingZeros().toPlainString()
            ));
            return;
        }

        runAction(
                repository.recordSubscriptionUsage(new SubscriptionUsageInput(record.id, record.unitKey, delta)),
                getString(R.string.subscription_usage_submit_done)
        );
    }

    private void showAddDialog() {
        Context context = requireContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        EditText nameField = new EditText(context);
        nameField.setHint(R.string.subscription_field_name);
        layout.addView(nameField);

        Spinner typeSpinner = new Spinner(context);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                new String[]{
                        getString(R.string.subscription_type_fixed),
                        getString(R.string.subscription_type_usage)
                }
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        layout.addView(typeSpinner);

        EditText amountField = new EditText(context);
        amountField.setHint(R.string.subscription_field_amount);
        amountField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(amountField);

        EditText unitPriceField = new EditText(context);
        unitPriceField.setHint(R.string.subscription_field_unit_price);
        unitPriceField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(unitPriceField);

        EditText unitKeyField = new EditText(context);
        unitKeyField.setHint(R.string.subscription_field_unit_key);
        layout.addView(unitKeyField);

        EditText capField = new EditText(context);
        capField.setHint(R.string.subscription_field_usage_cap);
        capField.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(capField);

        Spinner cadenceSpinner = new Spinner(context);
        ArrayAdapter<String> cadenceAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                new String[]{
                        getString(R.string.subscription_cadence_monthly),
                        getString(R.string.subscription_cadence_quarterly),
                        getString(R.string.subscription_cadence_yearly)
                }
        );
        cadenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cadenceSpinner.setAdapter(cadenceAdapter);
        layout.addView(cadenceSpinner);

        EditText noteField = new EditText(context);
        noteField.setHint(R.string.subscription_field_note);
        layout.addView(noteField);

        typeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean usage = position == 1;
                amountField.setVisibility(usage ? View.GONE : View.VISIBLE);
                unitPriceField.setVisibility(usage ? View.VISIBLE : View.GONE);
                unitKeyField.setVisibility(usage ? View.VISIBLE : View.GONE);
                capField.setVisibility(usage ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // no-op
            }
        });
        typeSpinner.setSelection(0);

        new AlertDialog.Builder(context)
                .setTitle(R.string.subscription_add_title)
                .setMessage(R.string.subscription_add_message)
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.subscription_add, (dialog, which) -> {
                    String merchant = nameField.getText().toString().trim();
                    if (merchant.isEmpty()) {
                        showActionError(getString(
                                R.string.validation_message_required,
                                getString(R.string.subscription_field_name)
                        ));
                        return;
                    }
                    SubscriptionCadence cadence = cadenceFromIndex(cadenceSpinner.getSelectedItemPosition());
                    String note = noteField.getText().toString().trim();
                    boolean usage = typeSpinner.getSelectedItemPosition() == 1;

                    SubscriptionCreateInput input;
                    if (!usage) {
                        BigDecimal amount = parseAmount(amountField);
                        if (amount == null || amount.signum() <= 0) {
                            showActionError(getString(R.string.subscription_amount_required));
                            return;
                        }
                        input = new SubscriptionCreateInput(
                                merchant,
                                SubscriptionAmountType.FIXED,
                                amount,
                                null,
                                null,
                                null,
                                cadence,
                                note.isEmpty() ? null : note
                        );
                    } else {
                        BigDecimal unitPrice = parseAmount(unitPriceField);
                        String unitKey = unitKeyField.getText().toString().trim();
                        if (unitPrice == null || unitPrice.signum() <= 0 || unitKey.isEmpty()) {
                            showActionError(getString(R.string.subscription_usage_fields_required));
                            return;
                        }
                        BigDecimal cap = parseAmount(capField);
                        input = new SubscriptionCreateInput(
                                merchant,
                                SubscriptionAmountType.VARIABLE,
                                null,
                                unitPrice,
                                unitKey,
                                cap,
                                cadence,
                                note.isEmpty() ? null : note
                        );
                    }

                    repository.createSubscription(input)
                            .thenAccept(record -> {
                                if (!isAdded()) {
                                    return;
                                }
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), R.string.subscription_add_done, Toast.LENGTH_SHORT).show();
                                    refreshState();
                                });
                            })
                            .exceptionally(throwable -> {
                                if (!isAdded()) {
                                    return null;
                                }
                                requireActivity().runOnUiThread(() -> showActionError(
                                        getString(R.string.subscription_action_failed, rootMessage(throwable))
                                ));
                                return null;
                            });
                })
                .show();
    }

    private SubscriptionCadence cadenceFromIndex(int position) {
        if (position == 1) {
            return SubscriptionCadence.QUARTERLY;
        }
        if (position == 2) {
            return SubscriptionCadence.YEARLY;
        }
        return SubscriptionCadence.MONTHLY;
    }

    private void runAction(CompletableFuture<Void> actionFuture, String successMessage) {
        actionFuture
                .thenRun(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();
                        binding.subscriptionUsageDelta.setText("");
                        refreshState();
                    });
                })
                .exceptionally(throwable -> {
                    if (!isAdded()) {
                        return null;
                    }
                    requireActivity().runOnUiThread(() -> showActionError(
                            getString(R.string.subscription_action_failed, rootMessage(throwable))
                    ));
                    return null;
                });
    }

    private BigDecimal parseAmount(EditText field) {
        String raw = field.getText().toString().trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showActionError(String message) {
        if (getContext() == null) {
            return;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor instanceof java.util.concurrent.CompletionException && cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && !message.trim().isEmpty()) {
                return message;
            }
            cursor = cursor.getCause();
        }
        return getString(R.string.error_message_retry_again);
    }
}
