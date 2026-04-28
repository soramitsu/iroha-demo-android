package io.soramitsu.examplepoint.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.sdk.model.AccountAsset;
import io.soramitsu.examplepoint.sdk.model.AccountTransaction;

/**
 * Recycler-backed adapter that renders the wallet summary, asset balances, and recent transactions.
 */
public class WalletAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ACCOUNT_HEADER = 0;
    private static final int TYPE_SECTION_HEADER = 1;
    private static final int TYPE_MESSAGE = 2;
    private static final int TYPE_ASSET = 3;
    private static final int TYPE_TRANSACTION = 4;

    private final List<Row> rows = new ArrayList<>();
    private final LayoutInflater inflater;
    private final Context context;
    private final DateFormat dateFormat;

    public WalletAdapter(@NonNull Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    }

    public void submit(AccountProfile profile,
                       @Nullable List<AccountAsset> assets,
                       @Nullable List<AccountTransaction> transactions,
                       long syncedAtMs) {
        rows.clear();
        final String identityStatement = profile.getIdentityManifest() != null
                ? profile.getIdentityManifest().toCanonicalJson()
                : null;
        rows.add(Row.account(
                profile.getDisplayName(),
                profile.getAccountId(),
                primaryBalanceLabel(assets),
                identityStatement,
                syncedAtMs));
        rows.add(Row.section(context.getString(R.string.wallet_assets_section)));
        appendAssetRows(assets);
        rows.add(Row.section(context.getString(R.string.wallet_history_section)));
        if (transactions == null || transactions.isEmpty()) {
            rows.add(Row.message(context.getString(R.string.wallet_history_empty)));
        } else {
            for (AccountTransaction transaction : transactions) {
                rows.add(Row.transaction(transaction));
            }
        }
        notifyDataSetChanged();
    }

    private String primaryBalanceLabel(@Nullable List<AccountAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            return context.getString(R.string.wallet_empty_balance);
        }
        return assets.get(0).getQuantity();
    }

    private void appendAssetRows(@Nullable List<AccountAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            rows.add(Row.message(context.getString(R.string.wallet_assets_empty)));
            return;
        }
        for (AccountAsset asset : assets) {
            rows.add(Row.asset(asset));
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_ACCOUNT_HEADER:
                return new AccountHeaderViewHolder(inflater.inflate(R.layout.row_wallet_header, parent, false));
            case TYPE_SECTION_HEADER:
                return new SectionViewHolder(inflater.inflate(R.layout.row_wallet_section_header, parent, false));
            case TYPE_MESSAGE:
                return new MessageViewHolder(inflater.inflate(R.layout.row_wallet_message, parent, false));
            case TYPE_ASSET:
                return new AssetViewHolder(inflater.inflate(R.layout.row_account_asset, parent, false));
            case TYPE_TRANSACTION:
            default:
                return new TransactionViewHolder(inflater.inflate(R.layout.row_account_transaction, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        switch (row.type) {
            case TYPE_ACCOUNT_HEADER:
                ((AccountHeaderViewHolder) holder).bind((HeaderData) row.payload);
                break;
            case TYPE_SECTION_HEADER:
                ((SectionViewHolder) holder).bind((String) row.payload);
                break;
            case TYPE_MESSAGE:
                ((MessageViewHolder) holder).bind((String) row.payload);
                break;
            case TYPE_ASSET:
                ((AssetViewHolder) holder).bind((AccountAsset) row.payload);
                break;
            case TYPE_TRANSACTION:
                ((TransactionViewHolder) holder).bind((AccountTransaction) row.payload);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private final class AccountHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView accountIdView;
        private final TextView iasView;
        private final TextView balanceView;
        private final TextView lastSyncedView;

        AccountHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.wallet_account_name);
            accountIdView = itemView.findViewById(R.id.wallet_account_id);
            iasView = itemView.findViewById(R.id.wallet_account_ias);
            balanceView = itemView.findViewById(R.id.wallet_account_balance);
            lastSyncedView = itemView.findViewById(R.id.wallet_last_synced);
        }

        void bind(HeaderData data) {
            nameView.setText(data.displayName);
            accountIdView.setText(context.getString(R.string.wallet_account_label, data.accountId));
            if (data.identityStatement == null || data.identityStatement.isEmpty()) {
                iasView.setText(R.string.wallet_account_ias_unknown);
            } else {
                iasView.setText(context.getString(R.string.wallet_account_ias, data.identityStatement));
            }
            balanceView.setText(data.balance);
            if (lastSyncedView != null) {
                if (data.syncedAtMs > 0) {
                    String formatted = dateFormat.format(new Date(data.syncedAtMs));
                    lastSyncedView.setText(context.getString(R.string.wallet_last_sync, formatted));
                } else {
                    lastSyncedView.setText(R.string.wallet_last_sync_unknown);
                }
            }
        }
    }

    private final class SectionViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;

        SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.wallet_section_title);
        }

        void bind(String title) {
            titleView.setText(title);
        }
    }

    private final class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageView;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageView = itemView.findViewById(R.id.wallet_message_text);
        }

        void bind(String message) {
            messageView.setText(message);
        }
    }

    private final class AssetViewHolder extends RecyclerView.ViewHolder {
        private final TextView assetIdView;
        private final TextView quantityView;

        AssetViewHolder(@NonNull View itemView) {
            super(itemView);
            assetIdView = itemView.findViewById(R.id.asset_id);
            quantityView = itemView.findViewById(R.id.asset_quantity);
        }

        void bind(AccountAsset asset) {
            assetIdView.setText(asset.getAssetId());
            quantityView.setText(asset.getQuantity());
        }
    }

    private final class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconView;
        private final TextView statusView;
        private final TextView hashView;
        private final TextView authorityView;
        private final TextView timestampView;
        private final TextView errorView;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.transaction_icon);
            statusView = itemView.findViewById(R.id.transaction_status);
            hashView = itemView.findViewById(R.id.transaction_hash);
            authorityView = itemView.findViewById(R.id.transaction_authority);
            timestampView = itemView.findViewById(R.id.transaction_timestamp);
            errorView = itemView.findViewById(R.id.transaction_error);
        }

        void bind(AccountTransaction transaction) {
            if (transaction.isSuccess()) {
                statusView.setText(R.string.wallet_transaction_success);
                statusView.setBackgroundResource(R.drawable.bg_wallet_status_success);
                statusView.setTextColor(ContextCompat.getColor(context, R.color.white));
                iconView.setImageResource(R.drawable.icon_send);
            } else {
                statusView.setText(R.string.wallet_transaction_failed);
                statusView.setBackgroundResource(R.drawable.bg_wallet_status_failure);
                statusView.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                iconView.setImageResource(R.drawable.icon_rec);
            }
            hashView.setText(transaction.getEntrypointHash());
            final String authority = transaction.getAuthority();
            if (authority == null || authority.trim().isEmpty()) {
                authorityView.setText(R.string.unknown_receiver);
            } else {
                authorityView.setText(authority);
            }
            if (transaction.getTimestampMs() > 0) {
                timestampView.setText(dateFormat.format(new Date(transaction.getTimestampMs())));
            } else {
                timestampView.setText(R.string.wallet_timestamp_unknown);
            }
            final String error = transaction.getErrorMessage();
            if (error == null || error.isEmpty()) {
                errorView.setVisibility(View.GONE);
            } else {
                errorView.setVisibility(View.VISIBLE);
                errorView.setText(error);
            }
        }
    }

    private static final class Row {
        final int type;
        final Object payload;

        private Row(int type, Object payload) {
            this.type = type;
            this.payload = payload;
        }

        static Row account(String displayName,
                           String accountId,
                           String balance,
                           String identityStatement,
                           long syncedAtMs) {
            return new Row(TYPE_ACCOUNT_HEADER, new HeaderData(displayName, accountId, balance, identityStatement, syncedAtMs));
        }

        static Row section(String title) {
            return new Row(TYPE_SECTION_HEADER, title);
        }

        static Row message(String message) {
            return new Row(TYPE_MESSAGE, message);
        }

        static Row asset(AccountAsset asset) {
            return new Row(TYPE_ASSET, asset);
        }

        static Row transaction(AccountTransaction transaction) {
            return new Row(TYPE_TRANSACTION, transaction);
        }

    }

    private static final class HeaderData {
        final String displayName;
        final String accountId;
        final String balance;
        final String identityStatement;
        final long syncedAtMs;

        HeaderData(String displayName, String accountId, String balance, String identityStatement,
                   long syncedAtMs) {
            this.displayName = displayName;
            this.accountId = accountId;
            this.balance = balance;
            this.identityStatement = identityStatement;
            this.syncedAtMs = syncedAtMs;
        }
    }
}
