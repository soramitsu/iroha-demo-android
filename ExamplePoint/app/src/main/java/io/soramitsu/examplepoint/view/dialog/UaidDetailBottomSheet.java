package io.soramitsu.examplepoint.view.dialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.sdk.model.UaidBindings;
import io.soramitsu.examplepoint.sdk.model.UaidManifestInventory;

/**
 * Bottom sheet that renders either a binding or manifest record with copy/share affordances.
 */
public class UaidDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TYPE = "type";
    private static final String ARG_ALIAS = "alias";
    private static final String ARG_DATASPACE_ID = "dataspace_id";
    private static final String ARG_STATUS = "status";
    private static final String ARG_HASH = "hash";
    private static final String ARG_ACCOUNTS = "accounts";
    private static final String ARG_LIFECYCLE = "lifecycle";
    private static final String ARG_TITLE = "title";

    public enum DetailType {
        BINDING,
        MANIFEST
    }

    public static UaidDetailBottomSheet forBinding(UaidBindings.DataspaceBinding binding, String alias) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, alias);
        args.putSerializable(ARG_TYPE, DetailType.BINDING);
        args.putLong(ARG_DATASPACE_ID, binding.getDataspaceId());
        args.putString(ARG_ACCOUNTS, join(binding.getAccounts()));
        UaidDetailBottomSheet sheet = new UaidDetailBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    public static UaidDetailBottomSheet forManifest(UaidManifestInventory.ManifestRecord record, String alias, String lifecycleSummary) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, alias);
        args.putSerializable(ARG_TYPE, DetailType.MANIFEST);
        args.putLong(ARG_DATASPACE_ID, record.getDataspaceId());
        args.putString(ARG_STATUS, record.getStatus());
        args.putString(ARG_HASH, record.getManifestHash());
        args.putString(ARG_ACCOUNTS, join(record.getAccounts()));
        args.putString(ARG_LIFECYCLE, lifecycleSummary);
        UaidDetailBottomSheet sheet = new UaidDetailBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sheet_uaid_detail, container, false);
        Bundle args = requireArguments();
        TextView title = view.findViewById(R.id.uaid_detail_title);
        TextView dataspace = view.findViewById(R.id.uaid_detail_dataspace);
        TextView status = view.findViewById(R.id.uaid_detail_status);
        TextView hash = view.findViewById(R.id.uaid_detail_hash);
        TextView lifecycle = view.findViewById(R.id.uaid_detail_lifecycle);
        TextView accounts = view.findViewById(R.id.uaid_detail_accounts);
        View copyAccounts = view.findViewById(R.id.uaid_detail_copy_accounts);
        View copyHash = view.findViewById(R.id.uaid_detail_copy_hash);

        DetailType type = (DetailType) args.getSerializable(ARG_TYPE);
        String alias = args.getString(ARG_TITLE, getString(R.string.wallet_dataspace_unknown_alias));
        long dataspaceId = args.getLong(ARG_DATASPACE_ID, -1);
        String accountsText = args.getString(ARG_ACCOUNTS, getString(R.string.wallet_bindings_no_accounts));

        title.setText(alias);
        dataspace.setText(getString(R.string.wallet_binding_title_template, alias, dataspaceId));
        accounts.setText(accountsText);
        copyAccounts.setOnClickListener(v -> copyToClipboard("uaid-accounts", accountsText));

        if (type == DetailType.MANIFEST) {
            String statusValue = args.getString(ARG_STATUS, getString(R.string.wallet_manifest_lifecycle_unknown));
            String hashValue = args.getString(ARG_HASH, "");
            String lifecycleValue = args.getString(ARG_LIFECYCLE, getString(R.string.wallet_manifest_lifecycle_unknown));
            status.setText(getString(R.string.wallet_manifest_status_template, statusValue, hashValue.length() > 8 ? hashValue.substring(0, 8) + "…" : hashValue));
            hash.setText(hashValue);
            lifecycle.setText(lifecycleValue);
            copyHash.setVisibility(View.VISIBLE);
            copyHash.setOnClickListener(v -> copyToClipboard("uaid-manifest", hashValue));
        } else {
            status.setVisibility(View.GONE);
            hash.setVisibility(View.GONE);
            lifecycle.setVisibility(View.GONE);
            copyHash.setVisibility(View.GONE);
        }

        return view;
    }

    private void copyToClipboard(String label, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        ClipboardManager manager =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) {
            return;
        }
        manager.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(requireContext(), R.string.wallet_copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    private static String join(@Nullable java.util.List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        return TextUtils.join(", ", entries);
    }
}
