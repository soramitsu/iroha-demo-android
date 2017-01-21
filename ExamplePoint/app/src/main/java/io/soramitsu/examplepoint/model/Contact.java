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

package io.soramitsu.examplepoint.model;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class Contact extends RealmObject {
    private static final String FIELD_NAME_PUBLIC_KEY = "publicKey";

    @PrimaryKey
    public String publicKey;
    public String alias;

    public static Contact newContact(Realm realm, String publicKey) {
        return realm.createObject(Contact.class, publicKey);
    }

    public static void deleteAll(Realm realm) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<Contact> results = realm.where(Contact.class).findAll();
                results.deleteAllFromRealm();
            }
        });
    }

    public static RealmResults<Contact> findAll(Realm realm) {
        return realm.where(Contact.class).findAll();
    }

    public static Contact findContactByPublicKey(Realm realm, String publicKey) {
        return realm.where(Contact.class)
                .equalTo(Contact.FIELD_NAME_PUBLIC_KEY, publicKey)
                .findFirst();
    }
}
