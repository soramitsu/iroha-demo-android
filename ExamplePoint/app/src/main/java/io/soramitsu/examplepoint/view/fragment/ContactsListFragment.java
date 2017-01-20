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

package io.soramitsu.examplepoint.view.fragment;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmResults;
import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.FragmentContactsListBinding;
import io.soramitsu.examplepoint.model.Contact;
import io.soramitsu.examplepoint.view.ContactsListView;
import io.soramitsu.examplepoint.view.adapter.ContactsListAdapter;

public class ContactsListFragment extends Fragment implements ContactsListView {
    public static final String TAG = ContactsListFragment.class.getSimpleName();

    private FragmentContactsListBinding binding;
    private ContactsListAdapter contactsListAdapter;

    private Realm realm;
    private RealmResults<Contact> contacts;

    private ContactsListListener contactsListListener;

    public interface ContactsListListener {
        void onContactsListItemClickListener(Contact target);
    }

    public static ContactsListFragment newInstance() {
        ContactsListFragment fragment = new ContactsListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = DataBindingUtil.bind(view);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!(getActivity() instanceof ContactsListListener)) {
            throw new ClassCastException();
        }
        contactsListListener = (ContactsListListener) getActivity();

        realm = Realm.getDefaultInstance();
        contacts = realm.where(Contact.class).findAll();
        contactsListAdapter = new ContactsListAdapter(getContext(), contacts);
    }

    @Override
    public void onStart() {
        super.onStart();

//        if (contacts.size() == 0) {
//            // Create mock
//            realm.beginTransaction();
//            Contact contact = realm.createObject(Contact.class, "hogehogehogehoge");
//            contact.alias = "alias";
//            realm.commitTransaction();
//        }

        binding.contactList.setEmptyView(binding.emptyView);
        binding.contactList.setAdapter(contactsListAdapter);
        binding.contactList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Contact target = (Contact) adapterView.getItemAtPosition(i);
                contactsListListener.onContactsListItemClickListener(target);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void showError(String error, Throwable throwable) {
    }

    @Override
    public void renderContacts(OrderedRealmCollection<Contact> contacts) {
    }

    @Override
    public void showProgress() {
    }

    @Override
    public void hideProgress() {
    }
}
