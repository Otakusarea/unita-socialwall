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

package at.floriantaurer.unitasocialwallclient.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.CommandMessage;
import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.MainActivity;
import at.floriantaurer.unitasocialwallclient.R;
import at.floriantaurer.unitasocialwallclient.Routines;
import at.floriantaurer.unitasocialwallclient.TextMessage;
import at.floriantaurer.unitasocialwallclient.utils.BeaconUtils;
import at.floriantaurer.unitasocialwallclient.utils.ContactListSpinnerAdapter;
import at.floriantaurer.unitasocialwallclient.utils.ContactsUitls;
import at.floriantaurer.unitasocialwallclient.utils.LoginUtils;
import at.floriantaurer.unitasocialwallclient.utils.MessageAdapter;
import at.floriantaurer.unitasocialwallclient.utils.MessageUtils;
import at.floriantaurer.unitasocialwallclient.utils.PairedBeaconSpinnerAdapter;

public class MessagesFragment extends Fragment implements Routines.RoutineListener {

    TextView txtNoMessages;
    ListView lv;
    ListAdapter messageAdapter;
    AdapterView<?> parentLongClick;
    int positionLongClick;
    AlertDialog alertDelete = null;

    Button btnAllMessages;
    Button btnLatestLocalMessage;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.messages_fragment, null);
        Routines.addMessageEntryChangeListener(this);

        btnAllMessages = (Button) rootView.findViewById(R.id.btnAllMessages);
        btnLatestLocalMessage = (Button) rootView.findViewById(R.id.btnLatestLocalMessage);

        btnAllMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openComandAllPublicMessagesDialog();
            }
        });


        btnLatestLocalMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCommandLatestLocalMessageDialog();
            }
        });

        txtNoMessages = rootView.findViewById(R.id.txtNoMessages);
        lv = (ListView) rootView.findViewById(R.id.storedListView);
        lv.setAdapter(null);
        ArrayList<User> contacts = ContactsUitls.getContactList(getContext(), LoginUtils.getLoggedInUser(getContext()));
        ArrayList<TextMessage> messages = MessageUtils.getMessageList(getContext(), LoginUtils.getLoggedInUser(getContext()));
        final Context listContext = getActivity();
        messageAdapter = new MessageAdapter(getActivity(), messages, contacts);

        lv.setAdapter(messageAdapter);

        final AlertDialog.Builder deleteContactDialog = new AlertDialog.Builder(listContext);
        deleteContactDialog.setTitle(getString(R.string.dialog_message_delete_title))
                .setMessage(getString(R.string.dialog_message_delete_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        TextMessage singleMessageItem = (TextMessage) parentLongClick.getItemAtPosition(positionLongClick);

                        ArrayList<TextMessage> messages = MessageUtils.getMessageList(getContext(), LoginUtils.getLoggedInUser(getContext()));
                        for(TextMessage t :  messages){
                            if(t.getCommunicationPartner().getId() == singleMessageItem.getCommunicationPartner().getId() && t.getHeader().getSender().getId() == singleMessageItem.getHeader().getSender().getId() &&
                                    Arrays.equals(t.getMessageBody().getMessageBodyRaw(), singleMessageItem.getMessageBody().getMessageBodyRaw())){
                                messages.remove(t);
                                MessageUtils.setMessageList(messages, getContext(), LoginUtils.getLoggedInUser(getContext()));
                                checkIfMessagesAvailable();
                            }
                        }

                        ArrayList<User> contacts = ContactsUitls.getContactList(getContext(), LoginUtils.getLoggedInUser(getContext()));
                        lv.setAdapter(null);
                        messageAdapter = new MessageAdapter(getContext(), messages, contacts);
                        lv.setAdapter(messageAdapter);

                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertDelete.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);

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

        checkIfMessagesAvailable();

        return rootView;
    }

    private void checkIfMessagesAvailable(){
        ArrayList<TextMessage> messages = MessageUtils.getMessageList(getContext(), LoginUtils.getLoggedInUser(getContext()));
        if(messages.size()==0){
            txtNoMessages.setVisibility(View.VISIBLE);
        }else {
            txtNoMessages.setVisibility(View.INVISIBLE);
        }
    }

    private void openCommandLatestLocalMessageDialog(){
        final AlertDialog.Builder activateReceiverContactDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater receiverContactDialogInflater = LayoutInflater.from(getActivity());
        View cbReceiverContactLayout = receiverContactDialogInflater.inflate(R.layout.alert_latest_local_message, null);
        Spinner spnReceiver = (Spinner) cbReceiverContactLayout.findViewById(R.id.spnReceiver);
        Spinner spnContact = (Spinner) cbReceiverContactLayout.findViewById(R.id.spnContact);

        ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(getActivity(), LoginUtils.getLoggedInUser(getActivity()));
        PairedBeaconSpinnerAdapter receiverAdapter = new PairedBeaconSpinnerAdapter(getActivity(), pairedBeacons);
        spnReceiver.setAdapter(receiverAdapter);

        ArrayList<User> contacts = ContactsUitls.getContactList(getContext(), LoginUtils.getLoggedInUser(getContext()));
        ContactListSpinnerAdapter contactAdapter = new ContactListSpinnerAdapter(getActivity(), contacts);
        spnContact.setAdapter(contactAdapter);
        activateReceiverContactDialog.setView(cbReceiverContactLayout);
        activateReceiverContactDialog.setTitle(getString(R.string.dialog_command_latest_local_title))
                .setMessage(getString(R.string.dialog_command_latest_local_message))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.dialog_command_latest_local_pos_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        byte[] helpMessageByteArray = null;
                        helpMessageByteArray = ArrayUtils.addAll(helpMessageByteArray, "DTauTJj8".getBytes(StandardCharsets.UTF_8));
                        helpMessageByteArray = ArrayUtils.addAll(helpMessageByteArray, (byte)((User)spnContact.getSelectedItem()).getId());
                        CommandMessage commandMessage = new CommandMessage(LoginUtils.getLoggedInUser(getActivity()), (Beacon)spnReceiver.getSelectedItem(), helpMessageByteArray);
                        Routines.sendMessage(commandMessage, MainActivity.sender, MainActivity.unitaSettings, getActivity());
                    }
                })
                .setNegativeButton(getString(R.string.dialog_command_latest_local_neg_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info);

        activateReceiverContactDialog.show();
    }

    private void openComandAllPublicMessagesDialog(){
        final AlertDialog.Builder activateReceiverContactDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater receiverContactDialogInflater = LayoutInflater.from(getActivity());
        View cbReceiverContactLayout = receiverContactDialogInflater.inflate(R.layout.alert_receiver, null);
        Spinner spnReceiver = (Spinner) cbReceiverContactLayout.findViewById(R.id.spnReceiver);

        ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(getActivity(), LoginUtils.getLoggedInUser(getActivity()));
        PairedBeaconSpinnerAdapter receiverAdapter = new PairedBeaconSpinnerAdapter(getActivity(), pairedBeacons);
        spnReceiver.setAdapter(receiverAdapter);
        activateReceiverContactDialog.setView(cbReceiverContactLayout);
        activateReceiverContactDialog.setTitle(getString(R.string.dialog_command_all_public_title))
                .setMessage(getString(R.string.dialog_command_all_public_message))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.dialog_command_all_public_pos_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        CommandMessage commandMessage = new CommandMessage(LoginUtils.getLoggedInUser(getActivity()), (Beacon)spnReceiver.getSelectedItem(), "ymrGn29y".getBytes(StandardCharsets.UTF_8));
                        Routines.sendMessage(commandMessage, MainActivity.sender, MainActivity.unitaSettings, getActivity());
                    }
                })
                .setNegativeButton(getString(R.string.dialog_command_all_public_neg_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info);

        activateReceiverContactDialog.show();
    }

    @Override
    public void onStateMessages(int state) {

    }

    @Override
    public void onMessageEntryChange() {
        ArrayList<TextMessage> messages = MessageUtils.getMessageList(getActivity(), LoginUtils.getLoggedInUser(getActivity()));
        ArrayList<User> contacts = ContactsUitls.getContactList(getActivity(), LoginUtils.getLoggedInUser(getActivity()));
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lv.setAdapter(null);
                messageAdapter = new MessageAdapter(getActivity(), messages, contacts);
                lv.setAdapter(messageAdapter);
                checkIfMessagesAvailable();
            }
        });
    }
}
