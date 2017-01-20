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
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.ActivityContactBinding;
import io.soramitsu.examplepoint.model.Contact;
import io.soramitsu.examplepoint.navigator.Navigator;
import io.soramitsu.examplepoint.view.fragment.ContactsListFragment;
import io.soramitsu.irohaandroid.callback.Callback;

public class ContactActivity extends AppCompatActivity implements ContactsListFragment.ContactsListListener {
    public static final String TAG = ContactActivity.class.getSimpleName();

    private ActivityContactBinding binding;

    private ContactsListFragment contactsListFragment;

    public static Intent getCallingIntent(Context context) {
        Intent intent = new Intent(context, ContactActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(savedInstanceState);
    }

    private void init(Bundle savedInstanceState) {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contact);
        initToolbar();
        initFragments(savedInstanceState);
    }

    private void initToolbar() {
        binding.toolbar.setTitle("Contact");
        binding.toolbar.setNavigationIcon(
                ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_arrow_back_white_24dp));
        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        binding.toolbar.inflateMenu(R.menu.toolbar_contact_menu);
        binding.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_qr_reader) {
                    Navigator.getInstance().navigateToQRReaderActivity(getApplicationContext(), new Callback<String>() {
                        @Override
                        public void onSuccessful(String result) {
                            // TODO アドレス帳に追加する
                            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Log.e(TAG, "onFailure: ", throwable);
                        }
                    });
                    return true;
                }
                return false;
            }
        });
    }

    private void initFragments(Bundle savedInstanceState) {
        final FragmentManager manager = getSupportFragmentManager();
        contactsListFragment = (ContactsListFragment) manager.findFragmentByTag(ContactsListFragment.TAG);

        if (contactsListFragment == null) {
            contactsListFragment = ContactsListFragment.newInstance();
        }

        if (savedInstanceState == null) {
            switchFragment(contactsListFragment, ContactsListFragment.TAG);
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

    @Override
    public void onContactsListItemClickListener(Contact target) {
        // TODO 編集画面に遷移する
        Toast.makeText(getApplicationContext(), target.publicKey, Toast.LENGTH_SHORT).show();
    }
}
