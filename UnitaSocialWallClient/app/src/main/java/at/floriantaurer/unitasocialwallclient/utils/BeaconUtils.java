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

package at.floriantaurer.unitasocialwallclient.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.User;

public class BeaconUtils {

    public static ArrayList<Beacon> getPairedBeacons(Context context, User loggedInUser){
        Gson gson = new Gson();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String json = sp.getString(loggedInUser.getName()+"PairedBeacons", "");
        if(json.equals("")){
            ArrayList<Beacon> pairedBeacons = new ArrayList<Beacon>();
            return pairedBeacons;
        }else {
            return gson.fromJson(json, new TypeToken<ArrayList<Beacon>>() {}.getType());
        }
    }

    public static void setPairedBeacons(ArrayList<Beacon> pairedBeacons, Context context, User loggedInUser){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = sp.edit();
        if(pairedBeacons != null){
            Gson gson = new Gson();
            String json = gson.toJson(pairedBeacons);
            prefsEditor.putString(loggedInUser.getName()+"PairedBeacons", json);
        }else{
            prefsEditor.putString(loggedInUser.getName()+"PairedBeacons", null);
        }
        prefsEditor.apply();
        prefsEditor.commit();
    }
}
