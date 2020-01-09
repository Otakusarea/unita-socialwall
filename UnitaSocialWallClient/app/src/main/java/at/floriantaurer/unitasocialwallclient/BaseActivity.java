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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;

import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.ReceiveController;
import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.utils.LoginUtils;
import at.floriantaurer.unitasocialwallclient.utils.ServiceConstants;

public class BaseActivity extends AppCompatActivity {

    public static ArrayList<Beacon> beaconsWaitForPairing = new ArrayList<Beacon>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_contacts:
                openContacts();
                break;
            case R.id.open_paired_beacons:
                openPairedBeacons();
                break;
            case R.id.logout:
                logoutDevice();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
    public void openContacts(){
        Intent myIntent = new Intent(this.getApplicationContext(), ContactActivity.class);
        startActivityForResult(myIntent, 0);
    }

    public void openPairedBeacons(){
        Intent myIntent = new Intent(this.getApplicationContext(), PairedBeaconActivity.class);
        startActivityForResult(myIntent, 0);
    }

    public void logoutDevice(){
        if (SocialWallService.IS_SERVICE_RUNNING) {
            Intent service = new Intent(BaseActivity.this, SocialWallService.class);
            service.setAction(ServiceConstants.ACTION.STOPFOREGROUND_ACTION);
            startService(service);
        }
        ReceiveController receiver = ReceiveController.getInstance();
        receiver.stopReceiveing();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor ed = sp.edit();
        ed.remove("PREFERENCES_APP_STATE");
        ed.commit();
        LoginUtils.setLoggedInUser(null, getApplicationContext());
        Intent myIntent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivityForResult(myIntent, 0);
    }
}
