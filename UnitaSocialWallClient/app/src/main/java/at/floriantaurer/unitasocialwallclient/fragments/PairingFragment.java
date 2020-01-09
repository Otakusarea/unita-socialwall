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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.Broadcast;
import at.floriantaurer.unitabeaconmodule.CommandMessage;
import at.floriantaurer.unitabeaconmodule.Peer;
import at.floriantaurer.unitabeaconmodule.SendController;
import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.MainActivity;
import at.floriantaurer.unitasocialwallclient.PairedBeaconActivity;
import at.floriantaurer.unitasocialwallclient.R;
import at.floriantaurer.unitasocialwallclient.Routines;
import at.floriantaurer.unitasocialwallclient.TextMessage;
import at.floriantaurer.unitasocialwallclient.utils.BeaconUtils;
import at.floriantaurer.unitasocialwallclient.utils.ContactListSpinnerAdapter;
import at.floriantaurer.unitasocialwallclient.utils.ContactsUitls;
import at.floriantaurer.unitasocialwallclient.utils.LoginUtils;
import at.floriantaurer.unitasocialwallclient.utils.MessageUtils;
import at.floriantaurer.unitasocialwallclient.utils.PairedBeaconSpinnerAdapter;
import at.floriantaurer.unitasocialwallclient.utils.ConfigConstants;

public class PairingFragment extends Fragment implements Routines.RoutineListener, SendController.SendControllerListener {
    private ScrollView claMessages;
    Button btnSearchBeacons;
    TextView txtPairingProcess;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.pairing_fragment, null);
        claMessages = (ScrollView) rootView.findViewById(R.id.claMessages);
        Routines.addStateListener(this);
        SendController.addStateListener(this);

        btnSearchBeacons = (Button) rootView.findViewById(R.id.btnSearchBeacons);
        txtPairingProcess = (TextView) rootView.findViewById(R.id.txtPairingProcess);

        btnSearchBeacons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtPairingProcess.setText(getString(R.string.state_searching_beacons));
                CommandMessage commandMessage = new CommandMessage(LoginUtils.getLoggedInUser(getActivity()), new Broadcast(), "qG7SWLj2".getBytes(StandardCharsets.UTF_8));
                Routines.sendMessage(commandMessage, MainActivity.sender, MainActivity.unitaSettings, getActivity());
            }
        });
        return rootView;
    }

    @Override
    public void onStateMessages(int state) {
        switch (state){
            case ConfigConstants.UI_PAIRING_MESSAGE_SUCCESS:
                txtPairingProcess.setText(getString(R.string.state_waiting_pairing));
                break;
            case ConfigConstants.UI_STATUS_DEVICE_PAIRED_SUCCESS:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtPairingProcess.setText(getString(R.string.state_pairing_successful));
                    }
                });
                break;
            case ConfigConstants.UI_STATUS_MESSAGE_PACKET_RECEIVED_SUCCESS:
                break;
            case ConfigConstants.UI_PAIRING_IN_PROGRESS:
                txtPairingProcess.setText(getString(R.string.state_pairing_progress));
                break;
        }
    }

    @Override
    public void onMessageEntryChange() {

    }

    @Override
    public void onSendStateMessages() {
        txtPairingProcess.setText("");
    }


}
