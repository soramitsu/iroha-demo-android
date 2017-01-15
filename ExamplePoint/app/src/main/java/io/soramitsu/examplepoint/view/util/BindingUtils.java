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

package io.soramitsu.examplepoint.view.util;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.BindingMethod;
import android.databinding.BindingMethods;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;
import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.model.TransactionHistory;
import io.soramitsu.examplepoint.util.AndroidSupportUtil;
import io.soramitsu.irohaandroid.model.Transaction;

@BindingMethods({
        @BindingMethod(type = View.class, attribute = "android:drawable", method = "setBackground"),
        @BindingMethod(type = TextView.class, attribute = "android:drawable", method = "setBackground"),
        @BindingMethod(type = TextView.class, attribute = "android:text", method = "setText")
})
@SuppressWarnings("unused")
public final class BindingUtils {
    @BindingAdapter({"background", "public_key", "context"})
    public static void setBackgroundDrawableByTransactionType(
            CircleImageView view, Transaction tx, String publicKey, Context c) {

        final Drawable target;
        String command = tx.params.command;
        if (tx.isSender(publicKey) && command.equals(TransactionHistory.TRANSFER)) {
            target = AndroidSupportUtil.getDrawable(c, R.drawable.icon_send);
        } else {
            target = AndroidSupportUtil.getDrawable(c, R.drawable.icon_rec);
        }

        view.setBackground(target);
    }

    @BindingAdapter({"prefix", "public_key"})
    public static void setTransactionPrefix(TextView textView, Transaction tx, String publicKey) {
        String prefix;
        String command = tx.params.command;
        if (tx.isSender(publicKey) && command.equals(TransactionHistory.TRANSFER)) {
            prefix = "to";
        } else {
            prefix = "from";
        }
        textView.setText(prefix);
    }

    @BindingAdapter({"opponent", "public_key"})
    public static void setTransactionOpponentText(TextView textView, Transaction tx, String publicKey) {
        String displayText = "";
        String command = tx.params.command;
        if (command.equals("Add")) {
            displayText = "Register";
        } else if (command.equals(TransactionHistory.TRANSFER)) {
            if (tx.isSender(publicKey)) {
                displayText = tx.params.receiver;
            } else {
                displayText = tx.params.sender;
            }
        }
        textView.setText(displayText);
    }

    @BindingAdapter({"tx"})
    public static void setTransactionValue(TextView textView, Transaction tx) {
        if (tx.params.command.equals("Add")) {
            textView.setText("100");
        } else {
            textView.setText(tx.params.value);
        }
    }
}