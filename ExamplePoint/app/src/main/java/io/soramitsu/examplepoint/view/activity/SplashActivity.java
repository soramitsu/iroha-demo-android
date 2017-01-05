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
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.crypto.NoSuchPaddingException;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.navigator.Navigator;
import io.soramitsu.examplepoint.view.fragment.SplashFragment;
import io.soramitsu.irohaandroid.model.Account;

public class SplashActivity extends AppCompatActivity {
    public static final String TAG = SplashActivity.class.getSimpleName();

    private Navigator navigator = Navigator.getInstance();

    private SplashFragment splashFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        openSplashFragment();
    }

    private void openSplashFragment() {
        splashFragment = SplashFragment.newInstance();
        splashFragment.show(getSupportFragmentManager(), SplashFragment.TAG);
        try {
            final String uuid = Account.getUuid(getApplicationContext());
            new Handler().postDelayed(screenTransitionTask(uuid), 1000);
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | NoSuchPaddingException
                | KeyStoreException | InvalidKeyException | IOException e) {
            new Handler().postDelayed(screenTransitionTask(null), 1000);
        }
    }

    private Runnable screenTransitionTask(@Nullable final String uuid) {
        final Context context = getApplicationContext();
        return new Runnable() {
            @Override
            public void run() {
                if (uuid == null || uuid.isEmpty()) {
                    navigator.navigateToRegisterActivity(context);
                } else {
                    navigator.navigateToMainActivity(context, uuid);
                }
                finish();
                splashFragment.dismiss();
            }
        };
    }
}
