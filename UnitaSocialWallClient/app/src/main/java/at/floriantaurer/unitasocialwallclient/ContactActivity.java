/*
 * Copyright (c) 2019. Florian Taurer.
 *
 * This file is part of Unita SDK.
 *
 * Unita is free a SDK: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unita is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Unita.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.floriantaurer.unitasocialwallclient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.utils.BeaconUtils;
import at.floriantaurer.unitasocialwallclient.utils.ContactAdapter;
import at.floriantaurer.unitasocialwallclient.utils.ContactListSpinnerAdapter;
import at.floriantaurer.unitasocialwallclient.utils.ContactsUitls;
import at.floriantaurer.unitasocialwallclient.utils.LoginUtils;
import at.floriantaurer.unitasocialwallclient.utils.PairedBeaconSpinnerAdapter;

public class ContactActivity extends BaseActivity {

    TextView txtNoContacts;
    FloatingActionButton fabAddContact;
    ListView lv;
    ListAdapter contactAdapter;
    AdapterView<?> parentLongClick;
    int positionLongClick;
    AlertDialog alertDelete = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contactlist_view);

        lv = (ListView) findViewById(R.id.storedListView);
        lv.setAdapter(null);
        final Context listContext = this;

        ArrayList<User> contacts = ContactsUitls.getContactList(this, LoginUtils.getLoggedInUser(this));
        contactAdapter = new ContactAdapter(this, contacts);
        txtNoContacts = (TextView) findViewById(R.id.txtNoContacts);
        fabAddContact = (FloatingActionButton) findViewById(R.id.fabAddContact);
        fabAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAddContactDialog();
            }
        });

        final AlertDialog.Builder deleteContactDialog = new AlertDialog.Builder(ContactActivity.this);
        deleteContactDialog.setTitle(getString(R.string.contact_delete_dialog_title))
                .setMessage(getString(R.string.contact_delete_dialog_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        User singleUserItem = (User) parentLongClick.getItemAtPosition(positionLongClick);
                        ArrayList<User> contacts = ContactsUitls.getContactList(ContactActivity.this, LoginUtils.getLoggedInUser(ContactActivity.this));

                        for(User u :  contacts){
                            if(u.getName().equals(singleUserItem.getName()) && u.getId() == singleUserItem.getId()){
                                contacts.remove(u);
                                ContactsUitls.setContactList(contacts, ContactActivity.this, LoginUtils.getLoggedInUser(ContactActivity.this));
                                checkIfContactsAvailable();
                            }
                        }

                        lv.setAdapter(null);
                        contactAdapter = new ContactAdapter(listContext, contacts);
                        lv.setAdapter(contactAdapter);

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertDelete.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);

        lv.setAdapter(contactAdapter);

        lv.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener(){
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        parentLongClick = parent;
                        positionLongClick = position;
                        alertDelete = deleteContactDialog.show();
                        return true;
                    }
                }
        );

        checkIfContactsAvailable();
    }

    private void checkIfContactsAvailable(){
        ArrayList<User> contacts = ContactsUitls.getContactList(ContactActivity.this, LoginUtils.getLoggedInUser(ContactActivity.this));
        if(contacts.size()==0){
            txtNoContacts.setVisibility(View.VISIBLE);
        }else {
            txtNoContacts.setVisibility(View.INVISIBLE);
        }
    }

    private void openAddContactDialog(){
        final AlertDialog.Builder activateAddContactDialog = new AlertDialog.Builder(this);
        LayoutInflater addContactDialogInflater = LayoutInflater.from(this);
        View cbaddContactLayout = addContactDialogInflater.inflate(R.layout.add_contact_alert, null);
        EditText edtContactID = (EditText) cbaddContactLayout.findViewById(R.id.edtContactID);
        EditText edtContactName = (EditText) cbaddContactLayout.findViewById(R.id.edtContactName);
        activateAddContactDialog.setView(cbaddContactLayout);
        activateAddContactDialog.setTitle(getString(R.string.contact_add_dialog_title))
                .setMessage(getString(R.string.contact_add_dialog_message))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.contact_add_dialog_positive_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        addContactToList(Integer.valueOf(edtContactID.getText().toString()), edtContactName.getText().toString());
                    }
                })
                .setNegativeButton(getString(R.string.contact_add_dialog_negative_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info);

        activateAddContactDialog.show();
    }

    private void addContactToList(int contactID, String contactName){
        User addedContact = new User(contactID, contactName);
        ArrayList<User> contacts = ContactsUitls.getContactList(ContactActivity.this, LoginUtils.getLoggedInUser(ContactActivity.this));
        if(!contacts.isEmpty()) {
            for (User u : contacts) {
                if (u.getName().equals(addedContact.getName()) && u.getId() == addedContact.getId()) {
                } else {
                    contacts.add(addedContact);
                    ContactsUitls.setContactList(contacts, ContactActivity.this, LoginUtils.getLoggedInUser(ContactActivity.this));
                }
            }
        }else{
            contacts.add(addedContact);
            ContactsUitls.setContactList(contacts, ContactActivity.this, LoginUtils.getLoggedInUser(ContactActivity.this));
        }
        lv.setAdapter(null);
        contactAdapter = new ContactAdapter(this, contacts);
        lv.setAdapter(contactAdapter);
        checkIfContactsAvailable();
    }

}
