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

package io.soramitsu.examplepoint.view.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import io.realm.OrderedRealmCollection;
import io.realm.RealmBaseAdapter;
import io.soramitsu.examplepoint.R;
import io.soramitsu.examplepoint.databinding.RowContactsListBinding;
import io.soramitsu.examplepoint.model.Contact;

public class ContactsListAdapter extends RealmBaseAdapter<Contact> implements ListAdapter {

    public ContactsListAdapter(Context context, OrderedRealmCollection<Contact> contacts) {
        super(context, contacts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RowContactsListBinding binding;
        if (convertView == null) {
            binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.row_contacts_list, parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (RowContactsListBinding) convertView.getTag();
        }

        if (adapterData != null) {
            Contact contact = adapterData.get(position);
            binding.setContact(contact);
        }

        return convertView;
    }
}
