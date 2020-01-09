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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.Broadcast;
import at.floriantaurer.unitabeaconmodule.CommandMessage;
import at.floriantaurer.unitabeaconmodule.Peer;
import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.MainActivity;
import at.floriantaurer.unitasocialwallclient.PairedBeaconActivity;
import at.floriantaurer.unitasocialwallclient.R;
import at.floriantaurer.unitasocialwallclient.Routines;
import at.floriantaurer.unitasocialwallclient.TextMessage;
import at.floriantaurer.unitasocialwallclient.utils.BeaconUtils;
import at.floriantaurer.unitasocialwallclient.utils.ConfigConstants;
import at.floriantaurer.unitasocialwallclient.utils.ContactListSpinnerAdapter;
import at.floriantaurer.unitasocialwallclient.utils.ContactsUitls;
import at.floriantaurer.unitasocialwallclient.utils.LoginUtils;
import at.floriantaurer.unitasocialwallclient.utils.MessageUtils;
import at.floriantaurer.unitasocialwallclient.utils.PairedBeaconSpinnerAdapter;

public class SendFragment extends Fragment implements Routines.RoutineListener, PairedBeaconActivity.BeaconListListener, MainActivity.FragmentListener {

    private ScrollView claCommands;



    Button btnMsgSend;
    TextView txtComPartner;
    EditText edtMsgMessage;
    public static Spinner spnMsgReceiver;
    Spinner spnContacts;
    public static PairedBeaconSpinnerAdapter receiverAdapter;
    ContactListSpinnerAdapter contactSpinnerAdapter;
    RadioButton rbtPublic;
    RadioButton rbtPrivate;
    RadioGroup rgrState;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.send_fragment, null);
        claCommands = (ScrollView) rootView.findViewById(R.id.claCommands);
        Routines.addStateListener(this);
        MainActivity.addFragmentListener(this);
        PairedBeaconActivity.addBeaconEntryChangeListener(this);

        btnMsgSend = (Button) rootView.findViewById(R.id.btnMsgSend);
        edtMsgMessage = (EditText) rootView.findViewById(R.id.edtMsgMessage);
        txtComPartner = (TextView) rootView.findViewById(R.id.txtComPartner);
        rbtPrivate = (RadioButton) rootView.findViewById(R.id.rbtPrivate);
        rbtPublic = (RadioButton) rootView.findViewById(R.id.rbtPublic);
        rgrState = (RadioGroup) rootView.findViewById(R.id.rgrState);

        rgrState.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                checkForInitialMessageUIState();
            }
        });

        spnMsgReceiver = (Spinner) rootView.findViewById(R.id.spnMsgReceiver);

        ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(getActivity(), LoginUtils.getLoggedInUser(getActivity()));
        receiverAdapter = new PairedBeaconSpinnerAdapter(getActivity(), pairedBeacons);
        spnMsgReceiver.setAdapter(receiverAdapter);

        spnContacts = (Spinner) rootView.findViewById(R.id.spnContacts);
        ArrayList<User> contacts = ContactsUitls.getContactList(getContext(), LoginUtils.getLoggedInUser(getContext()));
        contactSpinnerAdapter = new ContactListSpinnerAdapter(getActivity(), contacts);
        spnContacts.setAdapter(contactSpinnerAdapter);

        btnMsgSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(getActivity(), LoginUtils.getLoggedInUser(getActivity()));
                if(pairedBeacons.size() == 0){
                    Toast.makeText(getActivity(),getString(R.string.send_message_no_receiver), Toast.LENGTH_LONG).show();
                }else {
                    Peer beacon = (Beacon)spnMsgReceiver.getSelectedItem();
                    TextMessage textMessage = null;
                    if(rbtPublic.isChecked()){
                        Peer communicationPartner = new Broadcast();
                        textMessage = new TextMessage(LoginUtils.getLoggedInUser(getActivity()), beacon, communicationPartner, edtMsgMessage.getText().toString(), true);
                    }else if(rbtPrivate.isChecked()){
                        Peer communicationPartner = (User)spnContacts.getSelectedItem();
                        textMessage = new TextMessage(LoginUtils.getLoggedInUser(getActivity()), beacon, communicationPartner, edtMsgMessage.getText().toString(), false);
                    }
                    at.floriantaurer.unitabeaconmodule.TextMessage unitaTextMessage = new at.floriantaurer.unitabeaconmodule.TextMessage(textMessage.getHeader().getSender(), textMessage.getHeader().getReceiver(), textMessage.getCommunicationPartner(), MessageUtils.convertSocialWallTextMessageAdditionalParametersToByteArray(textMessage.isPublic(), textMessage.getMessageBody().getMessageBodyRaw()));
                    Routines.sendMessage(unitaTextMessage, MainActivity.sender, MainActivity.unitaSettings, getActivity());
                }
            }
        });


        checkForInitialMessageUIState();

        return rootView;
    }

    private void checkForInitialMessageUIState(){
        if(rbtPublic.isChecked()){
            spnContacts.setVisibility(View.INVISIBLE);
            txtComPartner.setVisibility(View.INVISIBLE);
        }else if(rbtPrivate.isChecked()){
            spnContacts.setVisibility(View.VISIBLE);
            txtComPartner.setVisibility(View.VISIBLE);
            spnContacts.setAdapter(contactSpinnerAdapter);
        }
    }

    @Override
    public void onStateMessages(int state) {
        switch (state){
            case ConfigConstants.UI_PAIRING_MESSAGE_SUCCESS:
                break;
            case ConfigConstants.UI_STATUS_DEVICE_PAIRED_SUCCESS:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(getActivity(), LoginUtils.getLoggedInUser(getActivity()));
                        receiverAdapter = new PairedBeaconSpinnerAdapter(getActivity(), pairedBeacons);
                        spnMsgReceiver.setAdapter(receiverAdapter);
                    }
                });
                break;
            case ConfigConstants.UI_STATUS_MESSAGE_PACKET_RECEIVED_SUCCESS:
                break;
            case ConfigConstants.UI_PAIRING_IN_PROGRESS:
                break;
        }
    }

    @Override
    public void onMessageEntryChange() {

    }

    @Override
    public void onBeaconEntryChange() {
        ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(getActivity(), LoginUtils.getLoggedInUser(getActivity()));
        receiverAdapter = new PairedBeaconSpinnerAdapter(getActivity(), pairedBeacons);
        spnMsgReceiver.setAdapter(receiverAdapter);
    }

    @Override
    public void onSendingMessagesFinished() {
        btnMsgSend.setEnabled(true);
    }

}
