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

import androidx.appcompat.app.AppCompatActivity;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.navigator.Navigator;
import io.soramitsu.examplepoint.sdk.IrohaRepository;
import io.soramitsu.examplepoint.view.fragment.SplashFragment;

public class SplashActivity extends AppCompatActivity {
    public static final String TAG = SplashActivity.class.getSimpleName();

    private Navigator navigator = Navigator.getInstance();
    private IrohaRepository irohaRepository;

    private SplashFragment splashFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        irohaRepository = new IrohaRepository(getApplicationContext());
        openSplashFragment();
    }

    private void openSplashFragment() {
        splashFragment = SplashFragment.newInstance();
        splashFragment.show(getSupportFragmentManager(), SplashFragment.TAG);
        new Handler().postDelayed(this::routeFromSplash, 1000);
    }

    private void routeFromSplash() {
        final Context context = getApplicationContext();
        if (irohaRepository.hasAccountProfile()) {
            navigator.navigateToMainActivity(context);
        } else {
            navigator.navigateToRegisterActivity(context);
        }
        finish();
        splashFragment.dismiss();
    }
}
