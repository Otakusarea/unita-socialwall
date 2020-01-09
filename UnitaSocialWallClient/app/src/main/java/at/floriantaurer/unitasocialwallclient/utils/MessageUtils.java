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
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import at.ac.fhstp.sonitalk.SoniTalkConfig;
import at.ac.fhstp.sonitalk.SoniTalkMultiMessage;
import at.ac.fhstp.sonitalk.utils.EncoderUtils;
import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.Broadcast;
import at.floriantaurer.unitabeaconmodule.Peer;
import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitasocialwallclient.TextMessage;

public class MessageUtils {


    public static byte[] convertSocialWallTextMessageAdditionalParametersToByteArray(boolean isPublic, byte[] messageBody){
        byte[] additionalParameterByteArray = null;

        Log.d("convertSocialWallTextMe", String.valueOf((byte)(isPublic ? 1 : 0)));

        additionalParameterByteArray = ArrayUtils.addAll(additionalParameterByteArray, (byte)(isPublic ? 1 : 0));
        additionalParameterByteArray = ArrayUtils.addAll(additionalParameterByteArray, messageBody);

        return additionalParameterByteArray;
    }

    public static TextMessage convertUnitaTextMessageToSocialWallTextMessage(Peer sender, Peer receiver, at.floriantaurer.unitabeaconmodule.TextMessage textMessage){
        byte[] isPublicArray = new byte[1];
        System.arraycopy(textMessage.getMessageBody().getMessageBodyRaw(), 0, isPublicArray,0, 1);
        Log.d("convertUnitaTextMesSo", String.valueOf(textMessage.getMessageBody().getMessageBodyRaw().length-1));
        byte[] messageBodyArray = new byte[textMessage.getMessageBody().getMessageBodyRaw().length-1];
        System.arraycopy(textMessage.getMessageBody().getMessageBodyRaw(), 1, messageBodyArray,0, textMessage.getMessageBody().getMessageBodyRaw().length-1);
        return new TextMessage(sender, receiver, textMessage.getCommunicationPartner(), new String(messageBodyArray, StandardCharsets.UTF_8)/*, messageBodyArray*/, ((int) isPublicArray[0] == 1));
    }

    public static Peer convertJSONToPeer(JSONObject peerDataResponse){
        Gson gson = new Gson();

        int peerType = -1;
        try {
            peerType = peerDataResponse.getInt("type");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(peerType == 1){
            return gson.fromJson(String.valueOf(peerDataResponse), Beacon.class);
        }else if(peerType == 2){
            return gson.fromJson(String.valueOf(peerDataResponse), User.class);
        }else if(peerType == 0){
            return gson.fromJson(String.valueOf(peerDataResponse), Broadcast.class);
        }
        return null;
    }

    public static ArrayList<TextMessage> getMessageList(Context context, User loggedInUser){
        Gson gson = new Gson();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String json = sp.getString(loggedInUser.getName()+"Messages", "");
        if(json.equals("")){
            ArrayList<TextMessage> messageList = new ArrayList<TextMessage>();
            return messageList;
        }else {
            return gson.fromJson(json, new TypeToken<ArrayList<TextMessage>>() {}.getType());
        }
    }

    public static void setMessageList(ArrayList<TextMessage> messageList, Context context, User loggedInUser){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = sp.edit();
        if(messageList != null){
            Gson gson = new Gson();
            String json = gson.toJson(messageList);
            prefsEditor.putString(loggedInUser.getName()+"Messages", json);
        }else{
            prefsEditor.putString(loggedInUser.getName()+"Messages", null);
        }
        prefsEditor.apply();
        prefsEditor.commit();
    }
}
