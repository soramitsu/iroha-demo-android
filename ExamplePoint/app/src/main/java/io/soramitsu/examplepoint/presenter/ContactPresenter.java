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

package io.soramitsu.examplepoint.presenter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;

import io.realm.Realm;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.model.Contact;
import io.soramitsu.examplepoint.model.TransferQRParameter;
import io.soramitsu.examplepoint.view.ContactView;

public class ContactPresenter implements Presenter<ContactView> {
    public static final String TAG = ContactPresenter.class.getSimpleName();

    private ContactView contactView;

    private Realm realm;

    private String publicKey;

    @Override
    public void setView(@NonNull ContactView view) {
        contactView = view;
    }

    @Override
    public void onCreate() {
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onStart() {
        // nothing
    }

    @Override
    public void onResume() {
        // nothing
    }

    @Override
    public void onPause() {
        // nothing
    }

    @Override
    public void onStop() {
        // nothing
    }

    @Override
    public void onDestroy() {
        // nothing
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public View.OnClickListener onUpdateButtonClicked() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Contact contact = Contact.findContactByPublicKey(realm, publicKey);
                        contact.alias = contactView.getAlias();
                    }
                });

                contactView.showSnackbar("Update!");
            }
        };
    }

    public View.OnClickListener onDeleteButtonClicked() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(contactView.getActivity())
                        .setMessage("Are you sure you want to delete your contact info?")
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                deleteContact();
                                contactView.onContactDeleteSuccessful();
                                contactView.showSnackbar("Delete!");
                            }
                        })
                        .setCancelable(true)
                        .create().show();
            }
        };
    }

    public View.OnClickListener onSendButtonClicked() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contactView.setPublicKeyForContactToSend(publicKey);
                contactView.finish();
            }
        };
    }

    public void qrReadSuccessful(String result) {
        final Context c = contactView.getContext();
        try {
            TransferQRParameter qrParam = parse(result, TransferQRParameter.class);
            saveContact(qrParam.account, qrParam.alias);

            Toast.makeText(
                    c,
                    c.getString(R.string.successfull_message_registerd_contact, qrParam.alias),
                    Toast.LENGTH_SHORT
            ).show();
        } catch (RealmPrimaryKeyConstraintException e) {
            Toast.makeText(c, R.string.error_message_already_registered, Toast.LENGTH_SHORT).show();
        }
    }

    private <T> T parse(String json, Class<T> clazz) {
        return new Gson().fromJson(json, clazz);
    }

    private void saveContact(final String publicKey, final String alias) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Contact contact = Contact.newContact(realm, publicKey);
                contact.alias = alias;
            }
        });
    }

    private void deleteContact() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Contact contact = Contact.findContactByPublicKey(realm, publicKey);
                contact.deleteFromRealm();
            }
        });
    }
}