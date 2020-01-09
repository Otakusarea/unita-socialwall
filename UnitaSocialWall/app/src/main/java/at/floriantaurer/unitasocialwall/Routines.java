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

package at.floriantaurer.unitasocialwall;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import at.floriantaurer.unitabeaconmodule.CommandMessage;
import at.floriantaurer.unitabeaconmodule.SendController;
import at.floriantaurer.unitabeaconmodule.SocketController;
import at.floriantaurer.unitabeaconmodule.StatusMessage;
import at.floriantaurer.unitabeaconmodule.TokenMessage;
import at.floriantaurer.unitabeaconmodule.UnitaMessage;
import at.floriantaurer.unitabeaconmodule.UrlMessage;
import at.floriantaurer.unitabeaconmodule.utils.CommandController;
import at.floriantaurer.unitabeaconmodule.utils.ConfigConstants;
import at.floriantaurer.unitabeaconmodule.utils.LocalMessageDatabase;
import at.floriantaurer.unitabeaconmodule.utils.LoginUtils;
import at.floriantaurer.unitabeaconmodule.utils.UnitaSettings;
import at.floriantaurer.unitasocialwall.utils.Commands;
import at.floriantaurer.unitasocialwall.utils.MessageUtils;
import at.floriantaurer.unitasocialwall.utils.StatusCodes;

public class Routines {

    public static void onTextMessageReceivedRoutine(at.floriantaurer.unitabeaconmodule.TextMessage receivedTextMessage, SocketController socket, Activity activity){
        StatusMessage statusMessage = new StatusMessage(LoginUtils.getLoggedInBeacon(activity), receivedTextMessage.getHeader().getSender(), StatusCodes.MESSAGE_PACKET_RECEIVED_SUCCESS.toString().getBytes(StandardCharsets.UTF_8));
        sendMessage(statusMessage, MainActivity.sender, MainActivity.unitaSettings, activity);
        TextMessage textMessage = MessageUtils.convertUnitaTextMessageToSocialWallTextMessage(receivedTextMessage.getHeader().getSender(), receivedTextMessage.getHeader().getReceiver(), receivedTextMessage);
        if(textMessage.isPublic()){
            Gson gson = new Gson();
            String jsonString = gson.toJson(textMessage);
            JSONObject messageJSON = null;
            try {
                messageJSON = new JSONObject(jsonString);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            socket.saveMessage(messageJSON);
        }else {
            LocalMessageDatabase localMessageDatabase = LocalMessageDatabase.getInstance();
            localMessageDatabase.saveMessage(textMessage);
        }

    }

    public static void onCommandMessageReceivedRoutine(CommandMessage receivedCommandMessage, Context context){
        CommandMessage commandMessage = new CommandMessage(receivedCommandMessage.getHeader().getSender(), receivedCommandMessage.getHeader().getReceiver(), receivedCommandMessage.getMessageBody().getMessageBodyRaw());
        ArrayList<String[]> commands = null;
        String methodName = null;
        String commandName = null;
        try {
            commands = CommandController.loadCommandsFromJson("socialWallCommands.json", context);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Class classobj = Commands.class;

            Method[] methods = classobj.getMethods();
            for(String[] c : commands){
                System.out.println("Name of the command code: " + c[0]);
                byte[] helpMessageByteArray = Arrays.copyOfRange(commandMessage.getMessageBody().getMessageBodyRaw(), 0, 8);
                System.out.println("helpMessageByteArray " + new String(helpMessageByteArray, StandardCharsets.UTF_8));
                if(c[0].equals(new String(helpMessageByteArray, StandardCharsets.UTF_8))){
                    commandName = c[1];
                    System.out.println("Command: " + commandName);
                    for (Method method : methods) {
                        System.out.println("Name of the method: " + method.getName());
                        if(method.getName().equals(commandName)){
                            methodName = method.getName();
                        }
                        if(methodName != null){
                            break;
                        }
                    }
                }
                if(commandName != null){
                    break;
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        Method commandMethod = null;

        System.out.println("Method: " + methodName);
        try {
            commandMethod = Commands.class.getMethod(methodName, int.class, byte[].class, Context.class);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        try {
            commandMethod.invoke(null, commandMessage.getHeader().getSender().getId(), commandMessage.getMessageBody().getMessageBodyRaw(), context);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public static void onUrlMessageReceivedRoutine(/*JSONObject senderDataResponse, JSONObject receiverDataResponse, */UrlMessage receivedUrlMessage, Activity activity){
        //Not in scenario
    }

    public static void onTokenMessageReceivedRoutine(/*JSONObject senderDataResponse, JSONObject receiverDataResponse, */TokenMessage receivedTokenMessage, Activity activity){
        //Not in scenario
    }

    public static void onUnitaMessageReceivedRoutine(/*JSONObject senderDataResponse, JSONObject receiverDataResponse, */UnitaMessage receivedUnitaMessage, Activity activity){
        //Not in scenario
    }

    public static void onStatusMessageReceivedRoutine(/*JSONObject senderDataResponse, JSONObject receiverDataResponse, */StatusMessage receivedStatusMessage, Activity activity){
        //Not in scenario
    }

    public static void onPairingMessageReceivedRoutine(UnitaMessage receivedPairingMessage, Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(ConfigConstants.RESENDING_RUNNING, false).commit();
        MainActivity.sender.stopResending();
        StatusMessage statusMessage = new StatusMessage(LoginUtils.getLoggedInBeacon(context), receivedPairingMessage.getHeader().getSender(), StatusCodes.DEVICE_PAIRED_SUCCESS.toString().getBytes(StandardCharsets.UTF_8));
        sendMessage(statusMessage, MainActivity.sender, MainActivity.unitaSettings, context);
    }


    public static void sendMessage(UrlMessage urlMessage, SendController sender, UnitaSettings unitaSettings, Context context){
        sender.sendMessage(context, unitaSettings, urlMessage, true, 3, 200);
    }

    public static void sendMessage(PairingMessage pairingMessage, SendController sender, UnitaSettings unitaSettings, Context context){
        sender.sendMessage(context, unitaSettings, pairingMessage, true, 2, 200);
    }

    public static void sendMessage(at.floriantaurer.unitabeaconmodule.TextMessage textMessage, SendController sender, UnitaSettings unitaSettings, Context context){
        sender.sendMessage(context, unitaSettings, textMessage, true, 1, 300);
    }

    public static void sendMessage(StatusMessage statusMessage, SendController sender, UnitaSettings unitaSettings, Context context){
        sender.sendMessage(context, unitaSettings, statusMessage, true, 2, 200);
    }
}
