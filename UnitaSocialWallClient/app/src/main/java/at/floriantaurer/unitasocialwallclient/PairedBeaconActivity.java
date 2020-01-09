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
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.utils.BeaconUtils;
import at.floriantaurer.unitasocialwallclient.utils.ContactAdapter;
import at.floriantaurer.unitasocialwallclient.utils.LoginUtils;
import at.floriantaurer.unitasocialwallclient.utils.PairedBeaconAdapter;

public class PairedBeaconActivity extends BaseActivity {
    TextView txtNoPairedBeacons;
    ListView lv;
    ListAdapter pairedBeaconAdapter;
    AdapterView<?> parentLongClick;
    int positionLongClick;
    AlertDialog alertDelete = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pairedbeaconlist_view);

        lv = (ListView) findViewById(R.id.storedListView);
        lv.setAdapter(null);
        final Context listContext = this;

        ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(this, LoginUtils.getLoggedInUser(this));
        pairedBeaconAdapter = new PairedBeaconAdapter(this, pairedBeacons);
        txtNoPairedBeacons = (TextView) findViewById(R.id.txtNoPairedBeacona);

        lv.setAdapter(pairedBeaconAdapter);

        final AlertDialog.Builder removeBeacon = new AlertDialog.Builder(PairedBeaconActivity.this);
        removeBeacon.setTitle(getString(R.string.paired_beacon_remove_title))
                .setMessage(getString(R.string.paired_beacon_remove_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Beacon singleBeaconItem = (Beacon) parentLongClick.getItemAtPosition(positionLongClick);

                        ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(PairedBeaconActivity.this, LoginUtils.getLoggedInUser(PairedBeaconActivity.this));

                        for(Beacon b:  pairedBeacons){
                            if(b.getName().equals(singleBeaconItem.getName()) && b.getId() == singleBeaconItem.getId()){
                                pairedBeacons.remove(b);
                                notifyBeaconEntryChange();
                                BeaconUtils.setPairedBeacons(pairedBeacons, PairedBeaconActivity.this, LoginUtils.getLoggedInUser(PairedBeaconActivity.this));
                                checkIfPairedBeaconsAvailable();
                            }
                        }

                        lv.setAdapter(null);
                        pairedBeaconAdapter = new PairedBeaconAdapter(listContext, pairedBeacons);
                        lv.setAdapter(pairedBeaconAdapter);

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
                        alertDelete = removeBeacon.show();
                        return true;
                    }
                }
        );

        checkIfPairedBeaconsAvailable();
    }

    public interface BeaconListListener {

        void onBeaconEntryChange();

    }

    private static List<PairedBeaconActivity.BeaconListListener> beaconListListeners = new ArrayList<>();

    public static void notifyBeaconEntryChange(){
        for(PairedBeaconActivity.BeaconListListener listener: beaconListListeners) {
            listener.onBeaconEntryChange();
        }
    }

    public static void addBeaconEntryChangeListener(PairedBeaconActivity.BeaconListListener listener) {
        beaconListListeners.add(listener);
    }

    private void checkIfPairedBeaconsAvailable(){
        ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(this, LoginUtils.getLoggedInUser(this));
        if(pairedBeacons.size()==0){
            txtNoPairedBeacons.setVisibility(View.VISIBLE);
        }else {
            txtNoPairedBeacons.setVisibility(View.INVISIBLE);
        }
    }
}
