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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import at.ac.fhstp.sonitalk.SoniTalkSender;
import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.CommandMessage;
import at.floriantaurer.unitabeaconmodule.Peer;
import at.floriantaurer.unitabeaconmodule.SendController;
import at.floriantaurer.unitabeaconmodule.StatusMessage;
import at.floriantaurer.unitabeaconmodule.TokenMessage;
import at.floriantaurer.unitabeaconmodule.UnitaMessage;
import at.floriantaurer.unitabeaconmodule.UrlMessage;
import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitabeaconmodule.utils.ConfigConstants;
import at.floriantaurer.unitabeaconmodule.utils.LocalMessageDatabase;
import at.floriantaurer.unitabeaconmodule.utils.UnitaSettings;
import at.floriantaurer.unitasocialwallclient.rest.RESTController;
import at.floriantaurer.unitasocialwallclient.rest.SocialWallAPI;
import at.floriantaurer.unitasocialwallclient.utils.BeaconUtils;
import at.floriantaurer.unitasocialwallclient.utils.LoginUtils;
import at.floriantaurer.unitasocialwallclient.utils.MessageUtils;
import at.floriantaurer.unitasocialwallclient.utils.PairedBeaconSpinnerAdapter;
import at.floriantaurer.unitasocialwallclient.utils.StatusCodes;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class Routines {
    public interface RoutineListener {

        void onStateMessages(int state);
        void onMessageEntryChange();

    }

    private static List<Routines.RoutineListener> routineListeners = new ArrayList<>();

    public static void notifyState(int state){
        for(Routines.RoutineListener listener: routineListeners) {
            listener.onStateMessages(state);
        }
    }

    public static void addStateListener(Routines.RoutineListener listener) {
        routineListeners.add(listener);
    }

    private static List<Routines.RoutineListener> messageListListeners = new ArrayList<>();

    public static void notifyMessageEntryChange(){
        for(Routines.RoutineListener listener: messageListListeners) {
            listener.onMessageEntryChange();
        }
    }

    public static void addMessageEntryChangeListener(Routines.RoutineListener listener) {
        messageListListeners.add(listener);
    }


    /*package-private*/ static void onTextMessageReceivedRoutine(at.floriantaurer.unitabeaconmodule.TextMessage receivedTextMessage, Activity activity){

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, false).commit();
        MainActivity.sender.stopResending();
        ArrayList<TextMessage> messages = MessageUtils.getMessageList(activity, LoginUtils.getLoggedInUser(activity));
        TextMessage textMessage = MessageUtils.convertUnitaTextMessageToSocialWallTextMessage(receivedTextMessage.getHeader().getSender(), receivedTextMessage.getHeader().getReceiver(),  receivedTextMessage);
        if(!messages.isEmpty()) {
            for (TextMessage t : messages) {
                if (t.getCommunicationPartner().getId() == textMessage.getCommunicationPartner().getId() && t.getHeader().getSender().getId() == textMessage.getHeader().getSender().getId() &&
                        Arrays.equals(t.getMessageBody().getMessageBodyRaw(), textMessage.getMessageBody().getMessageBodyRaw())) {
                    Snackbar.make(activity.findViewById(android.R.id.content).getRootView(), "You already received that message before!",
                            Snackbar.LENGTH_LONG)
                            .show();
                } else {
                    messages.add(textMessage);
                    MessageUtils.setMessageList(messages, activity, LoginUtils.getLoggedInUser(activity));
                    Snackbar.make(activity.findViewById(android.R.id.content).getRootView(), "You received a message!",
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }else{
            messages.add(textMessage);
            MessageUtils.setMessageList(messages, activity, LoginUtils.getLoggedInUser(activity));
            Snackbar.make(activity.findViewById(android.R.id.content).getRootView(), "You received a message!",
                    Snackbar.LENGTH_LONG)
                    .show();
            Log.d("Routines", "messages received");
        }
        notifyMessageEntryChange();

    }

    /*package-private*/ static void onCommandMessageReceivedRoutine(CommandMessage receivedCommandMessage, Context context){
        //Not in scenario
    }

    /*package-private*/ static void onUrlMessageReceivedRoutine(UrlMessage receivedUrlMessage, Activity activity){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, false).commit();
        MainActivity.sender.stopResending();
        UrlMessage urlMessage = new UrlMessage(receivedUrlMessage.getHeader().getSender(), receivedUrlMessage.getHeader().getReceiver(), receivedUrlMessage.getMessageBody().getMessageBodyRaw());
        // Thread handling
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(NUMBER_OF_CORES + 1);

        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                final SocialWallAPI restService = RESTController.getRetrofitInstance().create(SocialWallAPI.class);

                String url = new String(urlMessage.getMessageBody().getMessageBodyRaw(), StandardCharsets.UTF_8) + "/" + urlMessage.getHeader().getReceiver().getId();

                restService.sendUrlToServer(url).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if(response.isSuccessful()) {
                            String detailsString = getStringFromRetrofitResponse(response);
                            Log.i("Routine", "post submitted to API." + detailsString);
                            JSONArray jArray = null;
                            Peer sender = null;
                            Peer receiver = null;
                            Peer communicationPartner = null;
                            byte[] messageRaw = null;
                            try {
                                jArray = new JSONArray(detailsString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            ArrayList<TextMessage> textmessageList = MessageUtils.getMessageList(activity, LoginUtils.getLoggedInUser(activity));
                            for (int i = 0; i < jArray.length(); i++)
                            {
                                JSONObject jObj = null;
                                try {
                                    jObj = jArray.getJSONObject(i);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    sender = new Peer(jObj.getInt("sender"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    receiver = new Peer(jObj.getInt("receiver"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    communicationPartner = new Peer(jObj.getJSONObject("communicationPartner").getInt("id"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    JSONArray messageRawJSON = jObj.getJSONArray("messageRaw");
                                    messageRaw = new byte[messageRawJSON.length()];
                                    System.out.println(messageRawJSON);
                                    for (int l = 0; l < messageRawJSON.length(); l++)
                                    {
                                        messageRaw[l] = (byte) messageRawJSON.getInt(l);
                                    }
                                    System.out.println(Arrays.toString(messageRaw));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                TextMessage textMessage;
                                if(communicationPartner==null){
                                    textMessage = new TextMessage(sender, receiver, new Peer(0), new String(messageRaw, StandardCharsets.UTF_8), true);
                                }else{
                                    textMessage = new TextMessage(sender, receiver, communicationPartner, new String(messageRaw, StandardCharsets.UTF_8), false);
                                }
                                textmessageList.add(textMessage);
                                notifyMessageEntryChange();

                                System.out.println(jObj);
                            }
                            MessageUtils.setMessageList(textmessageList, activity, LoginUtils.getLoggedInUser(activity));

                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("Routine", "Unable to submit post to API." + t);
                    }
                });
            }
        });
    }

    /*package-private*/ static String getStringFromRetrofitResponse(Response<ResponseBody> response) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));

        String line;

        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();

    }

    /*package-private*/ static void onTokenMessageReceivedRoutine(TokenMessage receivedTokenMessage, Activity activity){
        //Not in scenario
    }

    /*package-private*/ static void onUnitaMessageReceivedRoutine(UnitaMessage receivedUnitaMessage, Activity activity){
        //Not in scenario
    }

    /*package-private*/ static void onStatusMessageReceivedRoutine(StatusMessage receivedStatusMessage, Activity activity){
        String statusCode = new String(receivedStatusMessage.getMessageBody().getMessageBodyRaw(),StandardCharsets.UTF_8);
        Beacon pairedBeacon = null;
        if(statusCode.equals(StatusCodes.MESSAGE_PACKET_RECEIVED_SUCCESS.toString())) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
            sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, false).commit();
            MainActivity.sender.stopResending();
        }

        if(statusCode.equals(StatusCodes.DEVICE_PAIRED_SUCCESS.toString())){
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
            sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, false).commit();
            MainActivity.sender.stopResending();
            for(Beacon b: MainActivity.beaconsWaitForPairing){
                if(b.getId() == receivedStatusMessage.getHeader().getSender().getId()){
                    pairedBeacon = b;
                }
            }

            if(pairedBeacon != null) {
                final String beaconNameToast = pairedBeacon.getName();
                ArrayList<Beacon> pairedBeacons = BeaconUtils.getPairedBeacons(activity, LoginUtils.getLoggedInUser(activity));
                pairedBeacons.add(pairedBeacon);
                MainActivity.beaconsWaitForPairing.remove(pairedBeacon);
                BeaconUtils.setPairedBeacons(pairedBeacons, activity, LoginUtils.getLoggedInUser(activity));
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(activity.findViewById(android.R.id.content).getRootView(), String.format(activity.getString(R.string.paired_beacon_snackbar_success),beaconNameToast),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                });
                notifyState(at.floriantaurer.unitasocialwallclient.utils.ConfigConstants.UI_STATUS_DEVICE_PAIRED_SUCCESS);
            }
        }

    }

    /*package-private*/ static void onPairingMessageReceivedRoutine(PairingMessage receivedPairingMessage, Context context){
        notifyState(at.floriantaurer.unitasocialwallclient.utils.ConfigConstants.UI_PAIRING_MESSAGE_SUCCESS);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, false).commit();
        Gson gson = new Gson();
        String json = sp.getString("LoggedInUser", "");
        //TODO: Store beacon to wanted to pairDevices
        MainActivity.beaconsWaitForPairing.add(new Beacon(receivedPairingMessage.getHeader().getSender().getId(), new String(receivedPairingMessage.getMessageBody().getMessageBodyRaw(), StandardCharsets.UTF_8)));
        PairingMessage pairingMessage = new PairingMessage(gson.fromJson(json, User.class), new Beacon(receivedPairingMessage.getHeader().getSender().getId(), new String(receivedPairingMessage.getMessageBody().getMessageBodyRaw(), StandardCharsets.UTF_8)), gson.fromJson(json, User.class).getName().getBytes(StandardCharsets.UTF_8));
        MainActivity.sender.stopResending();
        Routines.sendMessage(pairingMessage, MainActivity.sender, MainActivity.unitaSettings, context);
    }

    public static void sendMessage(CommandMessage commandMessage, SendController sender, UnitaSettings unitaSettings, Context context){
        sender.sendMessage(context, unitaSettings, commandMessage, true, 1, 200);
    }

    public static void sendMessage(UrlMessage urlMessage, SendController sender, UnitaSettings unitaSettings, Context context){
        sender.sendMessage(context, unitaSettings, urlMessage, true, 2, 200);
    }

    public static void sendMessage(PairingMessage pairingMessage, SendController sender, UnitaSettings unitaSettings, Context context){
        notifyState(at.floriantaurer.unitasocialwallclient.utils.ConfigConstants.UI_PAIRING_IN_PROGRESS);
        sender.sendMessage(context, unitaSettings, pairingMessage, true, 3, 200);
    }

    public static void sendMessage(at.floriantaurer.unitabeaconmodule.TextMessage textMessage, SendController sender, UnitaSettings unitaSettings, Context context){
        sender.sendMessage(context, unitaSettings, textMessage, true, 1, 200);
    }
}
