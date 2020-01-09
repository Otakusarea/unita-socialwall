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

public class ContactsUitls {

    public static ArrayList<User> getContactList(Context context, User loggedInUser){
        Gson gson = new Gson();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String json = sp.getString(loggedInUser.getName()+"Contacts", "");
        if(json.equals("")){
            ArrayList<User> contacts = new ArrayList<User>();
            return contacts;
        }else {
            return gson.fromJson(json, new TypeToken<ArrayList<User>>() {}.getType());
        }
    }

    public static void setContactList(ArrayList<User> contacts, Context context, User loggedInUser){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = sp.edit();
        if(contacts != null){
            Gson gson = new Gson();
            String json = gson.toJson(contacts);
            prefsEditor.putString(loggedInUser.getName()+"Contacts", json);
        }else{
            prefsEditor.putString(loggedInUser.getName()+"Contacts", null);
        }
        prefsEditor.apply();
        prefsEditor.commit();
    }
}
