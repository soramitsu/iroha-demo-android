package io.soramitsu.examplepoint.view.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.databinding.FragmentWalletBinding;
import io.soramitsu.examplepoint.navigator.Navigator;
import io.soramitsu.examplepoint.presenter.WalletPresenter;
import io.soramitsu.examplepoint.sdk.model.AccountAsset;
import io.soramitsu.examplepoint.sdk.model.AccountTransaction;
import io.soramitsu.examplepoint.view.WalletView;
import io.soramitsu.examplepoint.view.activity.MainActivity;
import io.soramitsu.examplepoint.view.adapter.WalletAdapter;

public class WalletFragment extends Fragment implements WalletView,
        MainActivity.MainActivityListener {

    public static final String TAG = WalletFragment.class.getSimpleName();

    private FragmentWalletBinding binding;
    private WalletPresenter walletPresenter;
    private WalletAdapter walletAdapter;

    public static WalletFragment newInstance() {
        return new WalletFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        walletPresenter = new WalletPresenter(requireContext());
        walletPresenter.setView(this);
        walletPresenter.onCreate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWalletBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        walletAdapter = new WalletAdapter(requireContext());
        binding.walletList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.walletList.setAdapter(walletAdapter);
        binding.swipeRefresh.setColorSchemeResources(
                R.color.red600,
                R.color.green600,
                R.color.blue600,
                R.color.orange600
        );
        binding.swipeRefresh.setOnRefreshListener(() -> walletPresenter.onPullToRefresh());
    }

    @Override
    public void onStart() {
        super.onStart();
        walletPresenter.onStart();
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
    public void renderWallet(AccountProfile profile,
                              List<AccountAsset> assets,
                              List<AccountTransaction> transactions,
                              long syncedAtMs) {
        walletAdapter.submit(profile, assets, transactions, syncedAtMs);
    }

    @Override
    public void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void promptReRegistration() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.error_message_user_not_found)
                .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Navigator.getInstance().navigateToRegisterActivity(requireContext());
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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
    public void onNavigationItemClicked() {
        // no-op
    }

}
