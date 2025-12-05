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

package io.soramitsu.examplepoint.view.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.ui.LibsSupportFragment;

import java.util.List;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.ActivityMainBinding;
import io.soramitsu.examplepoint.data.AccountProfile;
import io.soramitsu.examplepoint.exception.ErrorMessageFactory;
import io.soramitsu.examplepoint.navigator.Navigator;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.view.fragment.AssetReceiveFragment;
import io.soramitsu.examplepoint.view.fragment.AssetSenderFragment;
import io.soramitsu.examplepoint.view.fragment.WalletFragment;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int NAVIGATION_ITEM_RECEIVE = 0;
    private static final int NAVIGATION_ITEM_WALLET = 1;
    private static final int NAVIGATION_ITEM_SEND = 2;

    private Navigator navigator = Navigator.getInstance();
    private IrohaRepository irohaRepository;

    private ActivityMainBinding binding;
    private InputMethodManager inputMethodManager;

    private WalletFragment walletFragment;
    private AssetSenderFragment assetSenderFragment;
    private AssetReceiveFragment assetReceiveFragment;
    private LibsSupportFragment libsFragment;

    public interface MainActivityListener {
        void onNavigationItemClicked();
    }

    public interface OnKeyboardVisibilityListener {
        void onVisibilityChanged(boolean isVisible);
    }

    public static Intent getCallingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        irohaRepository = new IrohaRepository(getApplicationContext());
        if (!irohaRepository.hasAccountProfile()) {
            navigator.navigateToRegisterActivity(getApplicationContext());
            finish();
            return;
        }

        init(savedInstanceState);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, "dispatchTouchEvent: ");
        inputMethodManager.hideSoftInputFromWindow(
                binding.container.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS
        );
        binding.container.requestFocus();
        return super.dispatchTouchEvent(ev);
    }

    private void init(Bundle savedInstanceState) {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        initToolbar();
        initNavigationHeader();
        initNavigationView();
        initBottomNavigationView();
        initFragments(savedInstanceState);
        setKeyboardListener(new OnKeyboardVisibilityListener() {
            @Override
            public void onVisibilityChanged(boolean isVisible) {
                binding.bottomNavigation.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void initToolbar() {
        binding.toolbar.setTitle(getString(R.string.receive));
        binding.toolbar.setNavigationIcon(
                ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_menu_white_24dp));
        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void initNavigationHeader() {
        View headerView = binding.navigation.getHeaderView(0);
        headerView.setOnClickListener(v -> showAccountSwitcherDialog());
        try {
            AccountProfile profile = irohaRepository.getAccountProfile();
            ((TextView) headerView.findViewById(R.id.name)).setText(profile.getDisplayName());
            ((TextView) headerView.findViewById(R.id.email)).setText(profile.getAccountId());
        } catch (Exception e) {
            Context context = getApplicationContext();
            Toast.makeText(context, ErrorMessageFactory.create(context, e), Toast.LENGTH_SHORT).show();
        }
    }

    private void initNavigationView() {
        binding.navigation.getMenu().getItem(0).setChecked(true);
        binding.navigation.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        boolean isChecked = item.isChecked();
                        int itemId = item.getItemId();
                        if (itemId == R.id.action_receipt) {
                            if (!isChecked) {
                                Log.d(TAG, "onNavigationItemSelected: Receiver");
                                transitionTo(
                                        assetReceiveFragment,
                                        AssetReceiveFragment.TAG,
                                        R.string.receive,
                                        NAVIGATION_ITEM_RECEIVE
                                );
                            }
                        } else if (itemId == R.id.action_wallet) {
                            if (!isChecked) {
                                Log.d(TAG, "onNavigationItemSelected: Wallet");
                                transitionTo(
                                        walletFragment,
                                        WalletFragment.TAG,
                                        R.string.wallet,
                                        NAVIGATION_ITEM_WALLET
                                );
                            }
                        } else if (itemId == R.id.action_sender) {
                            if (!isChecked) {
                                Log.d(TAG, "onNavigationItemSelected: Sender");
                                transitionTo(
                                        assetSenderFragment,
                                        AssetSenderFragment.TAG,
                                        R.string.send,
                                        NAVIGATION_ITEM_SEND
                                );
                            }
                        } else if (itemId == R.id.action_switch_account) {
                            showAccountSwitcherDialog();
                        } else if (itemId == R.id.action_add_account) {
                            navigator.navigateToRegisterActivity(getApplicationContext());
                        } else if (itemId == R.id.action_unregister) {
                            final AccountProfile profile = irohaRepository.hasAccountProfile()
                                    ? irohaRepository.getAccountProfile()
                                    : null;
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage(profile != null
                                            ? getString(R.string.remove_account_confirmation, profile.getAccountId())
                                            : getString(R.string.unregister_confirmation))
                                    .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                        dialogInterface.dismiss();
                                        if (profile != null) {
                                            irohaRepository.removeAccount(profile.getAccountId());
                                        } else {
                                            irohaRepository.clearAccountProfile();
                                        }
                                        Context c = getApplicationContext();
                                        if (irohaRepository.hasAccountProfile()) {
                                            navigator.navigateToMainActivity(c);
                                        } else {
                                            navigator.navigateToRegisterActivity(c);
                                        }
                                        finish();
                                    })
                                    .setCancelable(true)
                                    .create().show();
                        } else if (itemId == R.id.action_oss) {
                            if (!isChecked) {
                                binding.bottomNavigation.setVisibility(View.GONE);
                                binding.toolbar.setTitle(getString(R.string.open_source_license));
                                switchFragment(libsFragment, "libs");
                                allClearNavigationMenuChecked();
                                Menu menu = binding.navigation.getMenu();
                                MenuItem ossItem = menu.findItem(R.id.action_oss);
                                if (ossItem != null) {
                                    ossItem.setChecked(true);
                                }
                            }
                        }
                        binding.drawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    }
                }
        );
    }

    private void initBottomNavigationView() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        if (!item.isChecked()) {
                            int itemId = item.getItemId();
                            if (itemId == R.id.action_receipt) {
                                Log.d(TAG, "onNavigationItemSelected: Receiver");
                                binding.toolbar.setTitle(getString(R.string.receive));
                                transitionTo(
                                        assetReceiveFragment,
                                        AssetReceiveFragment.TAG,
                                        R.string.receive,
                                        NAVIGATION_ITEM_RECEIVE
                                );
                            } else if (itemId == R.id.action_wallet) {
                                Log.d(TAG, "onNavigationItemSelected: Wallet");
                                transitionTo(
                                        walletFragment,
                                        WalletFragment.TAG,
                                        R.string.wallet,
                                        NAVIGATION_ITEM_WALLET
                                );
                            } else if (itemId == R.id.action_sender) {
                                Log.d(TAG, "onNavigationItemSelected: Sender");
                                transitionTo(
                                        assetSenderFragment,
                                        AssetSenderFragment.TAG,
                                        R.string.send,
                                        NAVIGATION_ITEM_SEND
                                );
                            }
                        } else {
                            Log.d(TAG, "onNavigationItemSelected: Topへ!");
                            final FragmentManager manager = getSupportFragmentManager();
                            MainActivityListener listener = (MainActivityListener) manager.findFragmentById(R.id.container);
                            listener.onNavigationItemClicked();
                        }
                        return true;
                    }
                });
    }

    private void allClearNavigationMenuChecked() {
        Menu menu = binding.navigation.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
    }

    private void allClearBottomNavigationMenuChecked() {
        Menu menu = binding.bottomNavigation.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
    }

    private void initFragments(Bundle savedInstanceState) {
        final FragmentManager manager = getSupportFragmentManager();
        assetReceiveFragment = (AssetReceiveFragment) manager.findFragmentByTag(AssetReceiveFragment.TAG);
        walletFragment = (WalletFragment) manager.findFragmentByTag(WalletFragment.TAG);
        assetSenderFragment = (AssetSenderFragment) manager.findFragmentByTag(AssetSenderFragment.TAG);

        if (assetReceiveFragment == null) {
            assetReceiveFragment = AssetReceiveFragment.newInstance();
        }
        if (walletFragment == null) {
            walletFragment = WalletFragment.newInstance();
        }
        if (assetSenderFragment == null) {
            assetSenderFragment = AssetSenderFragment.newInstance();
        }
        if (libsFragment == null) {
            libsFragment = new LibsBuilder()
                    .withAboutIconShown(true)
                    .withAboutVersionShown(true)
                    .withAboutDescription(getString(R.string.library_description))
                    .supportFragment();
        }

        if (savedInstanceState == null) {
            switchFragment(assetReceiveFragment, AssetReceiveFragment.TAG);
        }
    }

    private void switchFragment(@NonNull Fragment fragment, String tag) {
        if (fragment.isAdded()) {
            return;
        }

        final FragmentManager manager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = manager.beginTransaction();

        final Fragment currentFragment = manager.findFragmentById(R.id.container);
        if (currentFragment != null) {
            fragmentTransaction.detach(currentFragment);
        }

        if (fragment.isDetached()) {
            fragmentTransaction.attach(fragment);
        } else {
            fragmentTransaction.add(R.id.container, fragment, tag);
        }
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    private void transitionTo(Fragment to, String tag, int titleId, int nav) {
        binding.bottomNavigation.setVisibility(View.VISIBLE);
        binding.toolbar.setTitle(getString(titleId));
        switchFragment(to, tag);
        allClearNavigationMenuChecked();
        allClearBottomNavigationMenuChecked();
        binding.navigation.getMenu().getItem(nav).setChecked(true);
        binding.bottomNavigation.getMenu().getItem(nav).setChecked(true);
    }

    private void showAccountSwitcherDialog() {
        List<AccountProfile> profiles = irohaRepository.getAccountProfiles();
        if (profiles.isEmpty()) {
            Toast.makeText(getApplicationContext(), R.string.switch_account_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        AccountProfile active = irohaRepository.hasAccountProfile() ? irohaRepository.getAccountProfile() : null;
        int selectedIndex = -1;
        String[] labels = new String[profiles.size()];
        for (int i = 0; i < profiles.size(); i++) {
            AccountProfile profile = profiles.get(i);
            labels[i] = getString(R.string.switch_account_item, profile.getDisplayName(), profile.getAccountId());
            if (active != null && profile.getAccountId().equals(active.getAccountId())) {
                selectedIndex = i;
            }
        }
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        final int[] chosenIndex = new int[]{selectedIndex};
        new AlertDialog.Builder(this)
                .setTitle(R.string.switch_account)
                .setSingleChoiceItems(labels, selectedIndex, (dialog, which) -> chosenIndex[0] = which)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.add_account, (dialog, which) -> {
                    navigator.navigateToRegisterActivity(getApplicationContext());
                })
                .setPositiveButton(R.string.switch_account, (dialog, which) -> {
                    if (chosenIndex[0] < 0 || chosenIndex[0] >= profiles.size()) {
                        return;
                    }
                    String accountId = profiles.get(chosenIndex[0]).getAccountId();
                    if (irohaRepository.setActiveAccount(accountId)) {
                        navigator.navigateToMainActivity(getApplicationContext());
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.switch_account_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void setKeyboardListener(final OnKeyboardVisibilityListener listener) {
        final View activityRootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);

        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    private boolean wasOpened;
                    private final Rect r = new Rect();

                    @Override
                    public void onGlobalLayout() {
                        activityRootView.getWindowVisibleDisplayFrame(r);

                        // Compare root view height and layout height.
                        int heightDiff = activityRootView.getRootView().getHeight() - r.height();

                        boolean isOpen = heightDiff > 200;

                        if (isOpen == wasOpened) {
                            // Since display state of keyboard should not change, do nothing.
                            return;
                        }

                        wasOpened = isOpen;

                        listener.onVisibilityChanged(isOpen);
                    }
                });
    }
}
