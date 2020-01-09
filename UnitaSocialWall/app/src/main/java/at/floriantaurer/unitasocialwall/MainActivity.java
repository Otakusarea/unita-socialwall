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

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import at.ac.fhstp.sonitalk.SoniTalkContext;
import at.floriantaurer.unitabeaconmodule.Beacon;
import at.floriantaurer.unitabeaconmodule.Broadcast;
import at.floriantaurer.unitabeaconmodule.CommandMessage;
import at.floriantaurer.unitabeaconmodule.Peer;
import at.floriantaurer.unitabeaconmodule.ReceiveController;
import at.floriantaurer.unitabeaconmodule.SendController;
import at.floriantaurer.unitabeaconmodule.SocketController;
import at.floriantaurer.unitabeaconmodule.StatusMessage;
import at.floriantaurer.unitabeaconmodule.TokenMessage;
import at.floriantaurer.unitabeaconmodule.UnitaMessage;
import at.floriantaurer.unitabeaconmodule.UrlMessage;
import at.floriantaurer.unitabeaconmodule.User;
import at.floriantaurer.unitabeaconmodule.utils.LocalMessageDatabase;
import at.floriantaurer.unitabeaconmodule.utils.LocationController;
import at.floriantaurer.unitabeaconmodule.utils.LoginUtils;
import at.floriantaurer.unitabeaconmodule.utils.SettingsUtils;
import at.floriantaurer.unitabeaconmodule.utils.UnitaPermissionsResultReceiver;
import at.floriantaurer.unitabeaconmodule.utils.UnitaSettings;
import at.floriantaurer.unitasocialwall.utils.MessageUtils;
import at.floriantaurer.unitasocialwall.utils.ServiceConstants;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class MainActivity extends AppCompatActivity implements ReceiveController.BeaconListener, SocketController.SocketListener, LocalMessageDatabase.LocalMessageDatabaseListener, UnitaPermissionsResultReceiver.Receiver, SocketController.LoginListener  {

    ReceiveController receiver;
    public static SendController sender;
    public static UnitaSettings unitaSettings = null;
    SocketController socket;
    LocationController locationController;
    LocalMessageDatabase localMessageDatabase;


    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 42;
    private String [] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION};

    private ViewGroup rootViewGroup;
    private UnitaPermissionsResultReceiver unitaPermissionsResultReceiver;
    public static final int ON_SENDING_REQUEST_CODE = 2001;
    public static final int ON_RECEIVING_REQUEST_CODE = 2002;
    private Toast currentToast;
    private boolean btnStartedState = false;
    private boolean btnSendStartedState = false;

    private static EditText edtBaseFrequency;
    private static EditText edtBitperiod;
    private static EditText edtPausePeriod;
    private static EditText edtFrequencySpace;
    private static EditText edtMessageLength;
    private static TextView txtPreset;
    Button btnChangePreset;
    Button btnStartReceiver;
    Button btnLogout;
    private static Spinner spnNumberOfFrequencies;

    Button btnSend;

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootViewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);


        socket = SocketController.getInstance();
        socket.addLoginListener(this);
        socket.addMessageListener(this);
        socket.loginBeacon(createBeaconName());

        unitaPermissionsResultReceiver = new UnitaPermissionsResultReceiver(new Handler());
        unitaPermissionsResultReceiver.setReceiver(this);

        receiver = ReceiveController.getInstance();
        receiver.init(unitaPermissionsResultReceiver);

        locationController = LocationController.getInstance();
        locationController.setCurrentContext(this);
        locationController.initLocationTracker();

        sender = new SendController(unitaPermissionsResultReceiver);

        edtBaseFrequency = (EditText) findViewById(R.id.edtBaseFrequency);
        edtBitperiod = (EditText) findViewById(R.id.edtBitperiod);
        edtPausePeriod = (EditText) findViewById(R.id.edtPausePeriod);
        edtFrequencySpace = (EditText) findViewById(R.id.edtFrequencySpace);
        edtMessageLength = (EditText) findViewById(R.id.edtMessageLength);
        txtPreset = (TextView) findViewById(R.id.txtPreset);
        btnChangePreset = (Button) findViewById(R.id.btnChangePreset);
        btnChangePreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAndSetPresets();
            }
        });
        spnNumberOfFrequencies = (Spinner) findViewById(R.id.spnNumberOfFrequencies);
        String[] numberOfFrequencyElements = new String[] {
                "8", "16", "24", "32", "40", "48"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, numberOfFrequencyElements);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnNumberOfFrequencies.setAdapter(adapter);
        spnNumberOfFrequencies.setSelection(1);
        btnStartReceiver = (Button) findViewById(R.id.btnStartReceiver);
        btnStartReceiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!btnStartedState) {
                    startSocialWall();
                }else{
                    receiver.stopReceiveing();
                    btnStartReceiver.setText("Start Receiver");
                    btnStartedState = false;
                }
            }
        });

        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!btnSendStartedState) {
                    sendMessage();
                }else{
                    sender.stopSending();
                    btnSend.setText("Send");
                    btnSendStartedState = false;
                }
            }
        });

        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SocialWallService.IS_SERVICE_RUNNING) {
                    Intent service = new Intent(MainActivity.this, SocialWallService.class);
                    service.setAction(ServiceConstants.ACTION.STOPFOREGROUND_ACTION);
                    startService(service);
                }
                receiver.stopReceiveing();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor ed = sp.edit();
                ed.remove("PREFERENCES_APP_STATE");
                ed.commit();
            }
        });

        localMessageDatabase = LocalMessageDatabase.getInstance();
        localMessageDatabase.addMessageListener(this);
        localMessageDatabase.setCurrentContext(this);
    }


    public static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public String createBeaconName(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String beaconname;
        if(sp.getBoolean("first-start", true)){
            beaconname = randomAlphaNumeric(8);
            SharedPreferences.Editor ed = sp.edit();
            ed.putBoolean("first-start", false);
            ed.putString("beacon-name", beaconname);
            ed.apply();
        }else{
            beaconname = sp.getString("beacon-name", "ABCDEFGH");
        }
        return beaconname;
    }

    @Override
    public void onLoginResponse(JSONObject loginResponse) {
        int id = -1;
        String name = null;
        try {
            id = loginResponse.getInt("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            name = loginResponse.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(id > (-1) && name != null){
            Beacon loggedInBeacon = new Beacon(id, name);
            LoginUtils.saveLoggedInBeacon(getApplicationContext(), loggedInBeacon);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startSocialWall();
                }
            });
        } else{
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void startSocialWall(){
        receiver.addMessageListener(this);
        if(!hasPermissions(MainActivity.this, PERMISSIONS)){
            requestAudioPermission();
        }
        else {
            setSettings();
            receiver.startReceiving(this, unitaSettings);
            btnStartedState=true;
            btnStartReceiver.setText("Stop Receiver");
        }

    }

    public void sendMessage(){
        setSettings();
        User user = new User(2, "Smartphone1");
        Beacon beacon = new Beacon(2, "BeaconTwo");
        Broadcast broadcast = new Broadcast();

        CommandMessage commandMessage = new CommandMessage(LoginUtils.getLoggedInBeacon(getApplicationContext()), beacon, "getLastLocalMsg".getBytes(StandardCharsets.UTF_8));
        TextMessage textMessage = new TextMessage(LoginUtils.getLoggedInBeacon(getApplicationContext()), beacon, broadcast,/*numberOfPackets,*/  "Hallo SoniTalk und Unita"/*,"Hallo SoniTalk und Unita".getBytes(StandardCharsets.UTF_8)*/, false);
        at.floriantaurer.unitabeaconmodule.TextMessage unitaTextMessage = new at.floriantaurer.unitabeaconmodule.TextMessage(textMessage.getHeader().getSender(), textMessage.getHeader().getReceiver(), textMessage.getCommunicationPartner(), MessageUtils.convertSocialWallTextMessageAdditionalParametersToByteArray(textMessage.isPublic(), textMessage.getMessageBody().getMessageBodyRaw()));
        sender.sendMessage(this, unitaSettings, commandMessage, true, 1, 200);
        btnSendStartedState=true;
        btnSend.setText("Stop");

    }

    public void showAndSetPresets(){
        final String[] configList = SettingsUtils.getConfigList(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Setting Preset");
        builder.setItems(configList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                unitaSettings = SettingsUtils.loadSettingsFromJson(configList[which], getApplicationContext());
                if(unitaSettings != null){
                    setToPresetConfig(unitaSettings);
                }
            }
        });
        builder.show();
    }

    private void setToPresetConfig(UnitaSettings unitaSettings){
        edtBaseFrequency.setText(String.valueOf(unitaSettings.getFrequencyZero()));
        edtBitperiod.setText(String.valueOf(unitaSettings.getBitperiod()));
        edtPausePeriod.setText(String.valueOf(unitaSettings.getPauseperiod()));
        edtFrequencySpace.setText(String.valueOf(unitaSettings.getFrequencySpace()));
        edtMessageLength.setText(String.valueOf(unitaSettings.getnMessageBlocks()));
        spnNumberOfFrequencies.setSelection(checkFrequencySpinnerIndex(String.valueOf(unitaSettings.getnFrequencies())));
        txtPreset.setText(String.valueOf(unitaSettings.getSettingName()));
    }

    public static void updateSettings(){
        setSettings();
    }

    private static void setSettings(){
        if(unitaSettings!=null){
            unitaSettings.setFrequencyZero(Integer.valueOf(edtBaseFrequency.getText().toString()));
            unitaSettings.setBitperiod(Integer.valueOf(edtBitperiod.getText().toString()));
            unitaSettings.setPauseperiod(Integer.valueOf(edtPausePeriod.getText().toString()));
            unitaSettings.setFrequencySpace(Integer.valueOf(edtFrequencySpace.getText().toString()));
            unitaSettings.setnMessageBlocks(Integer.valueOf(edtMessageLength.getText().toString()));
            unitaSettings.setnFrequencies(Integer.valueOf(spnNumberOfFrequencies.getSelectedItem().toString()));
        }else{
            unitaSettings = new UnitaSettings(Integer.valueOf(edtBaseFrequency.getText().toString()), Integer.valueOf(edtBitperiod.getText().toString()),
                    Integer.valueOf(edtPausePeriod.getText().toString()), Integer.valueOf(edtMessageLength.getText().toString()),
                    Integer.valueOf(spnNumberOfFrequencies.getSelectedItem().toString()), Integer.valueOf(edtFrequencySpace.getText().toString()), txtPreset.getText().toString());
        }
    }

    private int checkFrequencySpinnerIndex(String numberOfFrequencies){
        SpinnerAdapter adapter = spnNumberOfFrequencies.getAdapter();
        int n = adapter.getCount();
        int frequencySpinnerIndex = 0;
        for (int i = 0; i < n; i++) {
           if(numberOfFrequencies.equals(adapter.getItem(i).toString())){
               frequencySpinnerIndex = i;
           }
        }
        return frequencySpinnerIndex;
    }

    public boolean checkIfMessageIsNotFromMyself(Peer receiver, Peer localPeer){
        return receiver.getId() != localPeer.getId();
    }

    //TODO check if message is for me
    public boolean checkIfMessageIsForMyself(Peer receiver, Peer localPeer){
        return (receiver.getId() == localPeer.getId() || receiver.getId() == 1);
    }
    /*Listeners*/

    @Override
    public void onUnitaMessageReceived(UnitaMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInBeacon(getApplicationContext())) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInBeacon(getApplicationContext()))){

            switch (receivedMessage.getHeader().getHeaderMessageCode()) {
                case 0:
                    Routines.onUnitaMessageReceivedRoutine(receivedMessage, this);
                    break;
                case 6:
                    PairingMessage pairingMessage = new PairingMessage(receivedMessage.getHeader().getSender(), receivedMessage.getHeader().getReceiver(), receivedMessage.getMessageBody().getMessageBodyRaw());
                    Routines.onPairingMessageReceivedRoutine(pairingMessage, this);
                    break;
            }
        }else{
        }
    }

    @Override
    public void onTextMessageReceived(at.floriantaurer.unitabeaconmodule.TextMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInBeacon(getApplicationContext())) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInBeacon(getApplicationContext()))){
            Routines.onTextMessageReceivedRoutine(receivedMessage, socket, this);
        }else{
        }
    }

    @Override
    public void onCommandMessageReceived(CommandMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInBeacon(getApplicationContext())) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInBeacon(getApplicationContext()))){
            Routines.onCommandMessageReceivedRoutine(receivedMessage, this);
        }else{
        }
    }

    @Override
    public void onTokenMessageReceived(TokenMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInBeacon(getApplicationContext())) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInBeacon(getApplicationContext()))){
            Routines.onTokenMessageReceivedRoutine(receivedMessage, this);
        }else{
        }
    }

    @Override
    public void onStatusMessageReceived(StatusMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInBeacon(getApplicationContext())) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInBeacon(getApplicationContext()))){
            Routines.onStatusMessageReceivedRoutine(receivedMessage, this);
        }else{
        }
    }

    @Override
    public void onUrlMessageReceived(UrlMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInBeacon(getApplicationContext())) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInBeacon(getApplicationContext()))){
            Routines.onUrlMessageReceivedRoutine(receivedMessage, this);
        }else{
        }
    }

    @Override
    public void onUnitaMessageError(String errorMessage) {

    }

    @Override
    public void onSendMessageResponse(JSONObject loginResponse) {
    }

    @Override
    public void onGetUrlForCommandGetAllMessagesResult(JSONObject urlResponse, JSONObject senderResponse) {
        String url = null;
        try {
            url = urlResponse.getString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        UrlMessage urlMessage = new UrlMessage(LoginUtils.getLoggedInBeacon(getApplicationContext()), MessageUtils.convertJSONToPeer(senderResponse), url.getBytes(StandardCharsets.UTF_8));
        Routines.sendMessage(urlMessage, sender, unitaSettings, getApplicationContext());
    }

    @Override
    public void onGetUrlForCommandgetAllBroadcastMessages(JSONObject urlResponse, JSONObject senderResponse) {

    }

    @Override
    public void onGetUrlForCommandgetAllMessagesFromContact(JSONObject urlResponse, JSONObject senderResponse) {

    }

    @Override
    public void onLocalTextMessageReceived(at.floriantaurer.unitabeaconmodule.TextMessage receivedLocalTextMessage) {
        TextMessage textMessage = new TextMessage(receivedLocalTextMessage.getHeader().getSender(),  receivedLocalTextMessage.getHeader().getReceiver(), receivedLocalTextMessage.getCommunicationPartner(), new String(receivedLocalTextMessage.getMessageBody().getMessageBodyRaw(), StandardCharsets.UTF_8), false);
        at.floriantaurer.unitabeaconmodule.TextMessage unitaTextMessage = new at.floriantaurer.unitabeaconmodule.TextMessage(textMessage.getHeader().getSender(), textMessage.getHeader().getReceiver(), textMessage.getCommunicationPartner(), MessageUtils.convertSocialWallTextMessageAdditionalParametersToByteArray(textMessage.isPublic(), textMessage.getMessageBody().getMessageBodyRaw()));
        Routines.sendMessage(unitaTextMessage, sender, unitaSettings, getApplicationContext());
    }

    /*Permission Part*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) {
            //we will show an explanation next time the user click on start
            showRequestPermissionExplanation(R.string.permissionRequestExplanation);
        }
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setSettings();
                    receiver.startReceiving(this, unitaSettings);
                    btnStartedState=true;
                    btnStartReceiver.setText("Stop Receiver");
                }
                else if(grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    showRequestPermissionExplanation(R.string.permissionRequestExplanation);
                }
                break;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void showRequestPermissionExplanation(int messageId) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
        builder.setMessage(messageId);
        builder.setPositiveButton(R.string.permission_request_explanation_positive,new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
        );
        builder.setNegativeButton(R.string.permission_request_explanation_negative, null);
        builder.show();
    }

    public void requestAudioPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.RECORD_AUDIO)) {
            Snackbar.make(rootViewGroup, R.string.permissionRequestExplanation,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET},
                                    REQUEST_RECORD_AUDIO_PERMISSION);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onUnitaPermissionResult(int resultCode, Bundle resultData) {
        int actionCode = 0;
        if(resultData != null){
            actionCode = resultData.getInt(getString(R.string.bundleRequestCode_key));
        }
        switch (resultCode) {
            case SoniTalkContext.ON_PERMISSION_LEVEL_DECLINED:
                if (currentToast != null) {
                    currentToast.cancel();
                }
                switch (actionCode) {
                    case ON_RECEIVING_REQUEST_CODE:
                        currentToast = Toast.makeText(MainActivity.this, getString(R.string.on_receiving_listening_permission_required), Toast.LENGTH_SHORT);
                        currentToast.show();

                        break;
                    case ON_SENDING_REQUEST_CODE:
                        currentToast = Toast.makeText(MainActivity.this, getString(R.string.on_sending_sending_permission_required), Toast.LENGTH_SHORT);
                        currentToast.show();
                        break;
                }


                break;
            case SoniTalkContext.ON_REQUEST_GRANTED:

                switch (actionCode){
                    case ON_RECEIVING_REQUEST_CODE:
                        break;

                    case ON_SENDING_REQUEST_CODE:
                        break;
                }

                break;
            case SoniTalkContext.ON_REQUEST_DENIED:
                if (currentToast != null) {
                    currentToast.cancel();
                }
                switch (actionCode) {
                    case ON_RECEIVING_REQUEST_CODE:
                        currentToast = Toast.makeText(MainActivity.this, getString(R.string.on_receiving_listening_permission_required), Toast.LENGTH_SHORT);
                        currentToast.show();
                        break;
                    case ON_SENDING_REQUEST_CODE:
                        currentToast = Toast.makeText(MainActivity.this, getString(R.string.on_sending_sending_permission_required), Toast.LENGTH_SHORT);
                        currentToast.show();
                }
                break;

            case SoniTalkContext.ON_SEND_JOB_FINISHED:
                btnSend.setText("Send");
                btnSendStartedState = false;
                break;

            case SoniTalkContext.ON_SHOULD_SHOW_RATIONALE_FOR_ALLOW_ALWAYS:
                break;

            case SoniTalkContext.ON_REQUEST_L0_DENIED:
                switch (actionCode) {
                    case ON_RECEIVING_REQUEST_CODE:
                        showRequestPermissionExplanation(R.string.on_receiving_listening_permission_required);

                        break;
                    case ON_SENDING_REQUEST_CODE:
                        showRequestPermissionExplanation(R.string.on_sending_sending_permission_required);

                }
                break;

            default:
                Log.w("MainActivityOnPerm", "onUnitaPermissionResult unknown resultCode: " + resultCode);
                break;

        }
    }
}
