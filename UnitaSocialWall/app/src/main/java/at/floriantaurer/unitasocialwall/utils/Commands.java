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

package at.floriantaurer.unitasocialwall.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import at.floriantaurer.unitabeaconmodule.Peer;
import at.floriantaurer.unitabeaconmodule.SocketController;
import at.floriantaurer.unitabeaconmodule.TextMessage;
import at.floriantaurer.unitabeaconmodule.utils.ConfigConstants;
import at.floriantaurer.unitabeaconmodule.utils.LocalMessageDatabase;
import at.floriantaurer.unitabeaconmodule.utils.LoginUtils;
import at.floriantaurer.unitasocialwall.MainActivity;
import at.floriantaurer.unitasocialwall.PairingMessage;
import at.floriantaurer.unitasocialwall.Routines;

public class Commands {

    public static void getUrlAllMessages(int senderId, byte[] messageRaw, Context context){
        JSONObject commandJSON = new JSONObject();
        try {
            commandJSON.put("sender", senderId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SocketController socket = SocketController.getInstance();
        socket.getUrlForCommandGetAllMessages(commandJSON);
    }

    public static void getUrlAllBroadcastMsg(int senderId, byte[] messageRaw, Context context){
        JSONObject commandJSON = new JSONObject();
        try {
            commandJSON.put("sender", senderId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SocketController socket = SocketController.getInstance();
        socket.getUrlForCommandGetAllMessages(commandJSON);
    }

    public static void getUrlAllMsgContact(int senderId, byte[] messageRaw, Context context){
        JSONObject commandJSON = new JSONObject();
        try {
            commandJSON.put("sender", senderId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SocketController socket = SocketController.getInstance();
        socket.getUrlForCommandGetAllMessages(commandJSON);
    }

    public static void getLastLocalMsg(int senderId, byte[] messageRaw, Context context){
        LocalMessageDatabase localMessageDatabase = LocalMessageDatabase.getInstance();
        byte[] helpMessageByteArray = Arrays.copyOfRange(messageRaw, 8, 9);
        localMessageDatabase.getLatestMessage(senderId, (int) helpMessageByteArray[0]);
    }

    public static void tryToPairWithUser(int senderId, byte[] messageRaw, Context context){
        PairingMessage pairingMessage = new PairingMessage(LoginUtils.getLoggedInBeacon(context), new Peer(senderId), LoginUtils.getLoggedInBeacon(context).getName().getBytes(StandardCharsets.UTF_8));
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, false).commit();
        MainActivity.sender.stopResending();
        Routines.sendMessage(pairingMessage, MainActivity.sender, MainActivity.unitaSettings, context);
    }
}
