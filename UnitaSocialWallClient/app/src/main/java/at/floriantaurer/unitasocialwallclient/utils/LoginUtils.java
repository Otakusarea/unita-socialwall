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
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import at.floriantaurer.unitabeaconmodule.User;

public class LoginUtils {

    public static User getLoggedInUser(Context context){
        Gson gson = new Gson();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String json = sp.getString("LoggedInUser", "");
        return gson.fromJson(json, User.class);
    }

    public static void setLoggedInUser(User user, Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = sp.edit();
        if(user != null){
            Gson gson = new Gson();
            String json = gson.toJson(user);
            prefsEditor.putString("LoggedInUser", json);
        }else{
            prefsEditor.putString("LoggedInUser", null);
        }
        prefsEditor.apply();
        prefsEditor.commit();
    }
}
