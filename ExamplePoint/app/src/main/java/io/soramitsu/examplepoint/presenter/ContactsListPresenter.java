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

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.gson.Gson;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;
import io.soramitsu.examplepoint.model.Contact;
import io.soramitsu.examplepoint.model.TransferQRParameter;
import io.soramitsu.examplepoint.view.ContactsListView;

public class ContactsListPresenter implements Presenter<ContactsListView> {
    public static final String TAG = ContactsListPresenter.class.getSimpleName();

    private ContactsListView contactsListView;

    private Realm realm;

    @Override
    public void setView(@NonNull ContactsListView view) {
        contactsListView = view;
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

    public AdapterView.OnItemClickListener onItemClicked() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Contact target = (Contact) adapterView.getItemAtPosition(i);
                contactsListView.getContactsListListener().onContactsListItemClickListener(target);
            }
        };
    }

    public RealmResults<Contact> findAll() {
        return realm.where(Contact.class).findAll();
    }

    public void qrReadSuccessful(String result) {
        try {
            final TransferQRParameter qrParam = new Gson().fromJson(result, TransferQRParameter.class);
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    Contact contact = realm.createObject(Contact.class, qrParam.account);
                    contact.alias = qrParam.alias;
                }
            });

            Toast.makeText(contactsListView.getContext(), result, Toast.LENGTH_SHORT).show();
        } catch (RealmPrimaryKeyConstraintException e) {
            Toast.makeText(contactsListView.getContext(), "既に登録済みです", Toast.LENGTH_SHORT).show();
        }
    }
}
