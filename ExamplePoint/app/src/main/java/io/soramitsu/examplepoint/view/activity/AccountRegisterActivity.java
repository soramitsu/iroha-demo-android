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

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.ActivityAccountRegisterBinding;
import io.soramitsu.examplepoint.navigator.Navigator;
import java.util.EnumSet;

import io.soramitsu.examplepoint.sdk.backup.KeyBackupManager;
import io.soramitsu.examplepoint.sdk.backup.KeyBackupPlan;
import io.soramitsu.examplepoint.view.fragment.AccountRegisterFragment;
import io.soramitsu.examplepoint.view.fragment.KeySetupFragment;

public class AccountRegisterActivity extends AppCompatActivity
        implements AccountRegisterFragment.AccountRegisterListener, KeySetupFragment.KeySetupListener {
    public static final String TAG = AccountRegisterActivity.class.getSimpleName();
    private static final String STATE_KEY_ALIAS = "state_key_alias";

    private Navigator navigator = Navigator.getInstance();

    private ActivityAccountRegisterBinding binding;
    private InputMethodManager inputMethodManager;
    private String pendingKeyAlias;

    public static Intent getCallingIntent(Context context) {
        Intent intent = new Intent(context, AccountRegisterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_account_register);
        binding.backgroundImage.setAlpha(0.2f);
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.rotate);
        set.setTarget(binding.backgroundImage);
        set.start();
        if (savedInstanceState != null) {
            pendingKeyAlias = savedInstanceState.getString(STATE_KEY_ALIAS);
        }
        if (savedInstanceState == null) {
            openKeySetupFragment();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View focused = getCurrentFocus();
            if (focused instanceof EditText) {
                Rect bounds = new Rect();
                focused.getGlobalVisibleRect(bounds);
                if (!bounds.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    focused.clearFocus();
                    inputMethodManager.hideSoftInputFromWindow(
                            focused.getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS
                    );
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onAccountRegisterSuccessful() {
        final Context context = getApplicationContext();
        navigator.navigateToMainActivity(context);
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_KEY_ALIAS, pendingKeyAlias);
    }

    @Override
    public void onKeySetupFinished(@NonNull KeyBackupPlan plan, @NonNull EnumSet<KeyBackupManager.BackupDestination> destinations) {
        pendingKeyAlias = plan.getKeyAlias();
        openAccountRegisterFragment();
    }

    private void openKeySetupFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.account_register_container, KeySetupFragment.newInstance(), KeySetupFragment.TAG)
                .commit();
    }

    private void openAccountRegisterFragment() {
        if (pendingKeyAlias == null) {
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.account_register_container, AccountRegisterFragment.newInstance(pendingKeyAlias), AccountRegisterFragment.TAG)
                .commit();
    }
}
