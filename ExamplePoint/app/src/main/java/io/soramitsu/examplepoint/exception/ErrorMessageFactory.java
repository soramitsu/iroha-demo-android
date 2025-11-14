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

package io.soramitsu.examplepoint.exception;

import android.content.Context;

import com.google.zxing.WriterException;

import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.network.ToriiException;

import org.hyperledger.iroha.android.address.AccountAddress.AccountAddressException;

public class ErrorMessageFactory {

    private ErrorMessageFactory() {
    }

    public static String create(Context context, Throwable exception, String... params) {
        final String fallback = context.getString(R.string.error_message_retry_again);
        if (exception instanceof AccountAddressException) {
            AccountAddressException addressException = (AccountAddressException) exception;
            return context.getString(
                    R.string.error_message_invalid_address,
                    addressException.getCodeValue()
            );
        } else if (exception instanceof RequiredArgumentException) {
            return context.getString(R.string.validation_message_required, (Object[]) params);
        } else if (exception instanceof WriterException) {
            return context.getString(R.string.error_message_cannot_generate_qr);
        } else if (exception instanceof ToriiException) {
            return sanitizeMessage(exception, fallback);
        } else if (exception instanceof IllegalArgumentException) {
            return sanitizeMessage(exception, fallback);
        }
        return fallback;
    }

    private static String sanitizeMessage(Throwable throwable, String fallback) {
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return fallback;
        }
        return message;
    }
}
