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

import android.Manifest;
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
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import at.ac.fhstp.sonitalk.SoniTalkContext;
import at.floriantaurer.unitabeaconmodule.CommandMessage;
import at.floriantaurer.unitabeaconmodule.Peer;
import at.floriantaurer.unitabeaconmodule.ReceiveController;
import at.floriantaurer.unitabeaconmodule.SendController;
import at.floriantaurer.unitabeaconmodule.StatusMessage;
import at.floriantaurer.unitabeaconmodule.TokenMessage;
import at.floriantaurer.unitabeaconmodule.UnitaMessage;
import at.floriantaurer.unitabeaconmodule.UrlMessage;
import at.floriantaurer.unitabeaconmodule.utils.LocalMessageDatabase;
import at.floriantaurer.unitabeaconmodule.utils.UnitaPermissionsResultReceiver;
import at.floriantaurer.unitabeaconmodule.utils.UnitaSettings;
import at.floriantaurer.unitasocialwallclient.fragments.SendFragment;
import at.floriantaurer.unitasocialwallclient.fragments.MessagesFragment;
import at.floriantaurer.unitasocialwallclient.fragments.PairingFragment;
import at.floriantaurer.unitasocialwallclient.fragments.SettingsFragment;
import at.floriantaurer.unitasocialwallclient.utils.FragmentAdapter;
import at.floriantaurer.unitasocialwallclient.utils.LoginUtils;
import at.floriantaurer.unitasocialwallclient.utils.MessageUtils;
import at.floriantaurer.unitasocialwallclient.utils.ServiceConstants;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class MainActivity extends BaseActivity implements ViewPager.OnPageChangeListener, ReceiveController.BeaconListener, LocalMessageDatabase.LocalMessageDatabaseListener, UnitaPermissionsResultReceiver.Receiver, SendController.SendControllerListener  {
    BottomNavigationView navView;
    ViewPager viewPager;

    ReceiveController receiver;
    public static SendController sender;

    private ViewGroup rootViewGroup;
    private UnitaPermissionsResultReceiver unitaPermissionsResultReceiver;

    public static UnitaSettings unitaSettings = null;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 42;
    private String [] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET};
    public static final int ON_SENDING_REQUEST_CODE = 2001;
    public static final int ON_RECEIVING_REQUEST_CODE = 2002;
    private Toast currentToast;

    static PairingFragment pairingFragment;
    static MessagesFragment messagesFragment;
    static SendFragment sendFragment;
    static SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        pairingFragment = new PairingFragment();
        messagesFragment = new MessagesFragment();
        sendFragment = new SendFragment();
        settingsFragment = new SettingsFragment();


        viewPager = findViewById(R.id.viewpager); //Init Viewpager
        setupFragments(getSupportFragmentManager(), viewPager); //Setup Fragment
        viewPager.setCurrentItem(0); //Set Currrent Item When Activity Start
        viewPager.setOnPageChangeListener(this);

        rootViewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);


        unitaPermissionsResultReceiver = new UnitaPermissionsResultReceiver(new Handler());
        unitaPermissionsResultReceiver.setReceiver(this);

        receiver = ReceiveController.getInstance();
        receiver.init(unitaPermissionsResultReceiver);
        sender = new SendController(unitaPermissionsResultReceiver);
        sender.addStateListener(this);

        unitaSettings = settingsFragment.setSettings(this);
        startSocialWall();
    }

    public static void setupFragments(FragmentManager fragmentManager, ViewPager viewPager){
        FragmentAdapter Adapter = new FragmentAdapter(fragmentManager);
        //Add All Fragment To List
        Adapter.add(pairingFragment, "Send");
        Adapter.add(sendFragment, "Commands");
        Adapter.add(messagesFragment, "Messages");
        Adapter.add(settingsFragment, "Settings");
        viewPager.setAdapter(Adapter);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_send:
                    viewPager.setCurrentItem(0);
                    return true;
                case R.id.navigation_commands:
                    viewPager.setCurrentItem(1);
                    return true;
                case R.id.navigation_messages:
                    viewPager.setCurrentItem(2);
                    return true;
                case R.id.navigation_settings:
                    viewPager.setCurrentItem(3);
                    return true;
            }
            return false;
        }
    };


    public interface FragmentListener {

        void onSendingMessagesFinished();

    }

    private static List<MainActivity.FragmentListener> fragmentListeners = new ArrayList<>();

    public static void notifySendingMessageFinished(){
        for(MainActivity.FragmentListener listener: fragmentListeners) {
            listener.onSendingMessagesFinished();
        }
    }

    public static void addFragmentListener(MainActivity.FragmentListener listener) {
        fragmentListeners.add(listener);
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case 0:
                navView.setSelectedItemId(R.id.navigation_send);
                break;
            case 1:
                navView.setSelectedItemId(R.id.navigation_commands);
                break;
            case 2:
                navView.setSelectedItemId(R.id.navigation_messages);
                break;
            case 3:
                navView.setSelectedItemId(R.id.navigation_settings);
                break;
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    public void startSocialWall(){
        receiver.addMessageListener(this);
        if(!hasPermissions(MainActivity.this, PERMISSIONS)){
            requestAudioPermission();
        }
        else {
            unitaSettings = settingsFragment.setSettings(this);
            receiver.startReceiving(this, unitaSettings);
        }
    }

    public void logout(){
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
        Intent myIntent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivityForResult(myIntent, 0);
    }

    public boolean checkIfMessageIsNotFromMyself(Peer sender, Peer localPeer){
        return sender.getId() != localPeer.getId();
    }

    //TODO check if message is for me
    public boolean checkIfMessageIsForMyself(Peer receiver, Peer localPeer){
        return receiver.getId() == localPeer.getId();
    }

    @Override
    public void onUnitaMessageReceived(UnitaMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInUser(this)) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInUser(this))){

            switch (receivedMessage.getHeader().getHeaderMessageCode()) {
                case 0:
                    Routines.onUnitaMessageReceivedRoutine(receivedMessage, this);
                    break;
                case 6:
                    PairingMessage pairingMessage = new PairingMessage(receivedMessage.getHeader().getSender(), receivedMessage.getHeader().getReceiver(), receivedMessage.getMessageBody().getMessageBodyRaw());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showPairedDeviceDialog(pairingMessage);
                        }
                    });
                    break;
            }
        }else{
        }
    }

    @Override
    public void onTextMessageReceived(at.floriantaurer.unitabeaconmodule.TextMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInUser(this)) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInUser(this))){
            Routines.onTextMessageReceivedRoutine(receivedMessage, this);
        }else{
        }
    }

    @Override
    public void onCommandMessageReceived(CommandMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInUser(this)) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInUser(this))){
            Routines.onCommandMessageReceivedRoutine(receivedMessage, this);
        }else{
        }
    }

    @Override
    public void onTokenMessageReceived(TokenMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInUser(this)) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInUser(this))){
            Routines.onTokenMessageReceivedRoutine(receivedMessage, this);
        }else{
        }
    }

    @Override
    public void onUrlMessageReceived(UrlMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInUser(this)) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInUser(this))){
            Routines.onUrlMessageReceivedRoutine(receivedMessage, this);
        }else{
        }
    }

    @Override
    public void onStatusMessageReceived(StatusMessage receivedMessage) {
        if(checkIfMessageIsNotFromMyself(receivedMessage.getHeader().getSender(), LoginUtils.getLoggedInUser(this)) &&
                checkIfMessageIsForMyself(receivedMessage.getHeader().getReceiver(), LoginUtils.getLoggedInUser(this))){
            Routines.onStatusMessageReceivedRoutine(receivedMessage, this);
        }else{
        }
    }

    @Override
    public void onUnitaMessageError(String errorMessage) {

    }


    public void showPairedDeviceDialog(PairingMessage receivedPairingMessage){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
        builder.setMessage(String.format(getString(R.string.paired_device_dialog_message), new String(receivedPairingMessage.getMessageBody().getMessageBodyRaw(), StandardCharsets.UTF_8)));
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.paired_device_dialog_positive_button,new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {

                        Routines.onPairingMessageReceivedRoutine(receivedPairingMessage, MainActivity.this);
                    }
                }
        );
        builder.setNegativeButton(R.string.paired_device_dialog_negative_button, null);
        builder.show();
    }

    @Override
    public void onLocalTextMessageReceived(at.floriantaurer.unitabeaconmodule.TextMessage receivedLocalTextMessage) {
        TextMessage textMessage = new TextMessage(receivedLocalTextMessage.getHeader().getSender(),  receivedLocalTextMessage.getHeader().getReceiver(), receivedLocalTextMessage.getCommunicationPartner(), new String(receivedLocalTextMessage.getMessageBody().getMessageBodyRaw(), StandardCharsets.UTF_8), false);
        at.floriantaurer.unitabeaconmodule.TextMessage unitaTextMessage = new at.floriantaurer.unitabeaconmodule.TextMessage(textMessage.getHeader().getSender(), textMessage.getHeader().getReceiver(), textMessage.getCommunicationPartner(), MessageUtils.convertSocialWallTextMessageAdditionalParametersToByteArray(textMessage.isPublic(), textMessage.getMessageBody().getMessageBodyRaw()));
        Routines.sendMessage(unitaTextMessage, sender, unitaSettings, getApplicationContext());
    }


    @Override
    public void onSendStateMessages() {
        Snackbar.make(rootViewGroup, R.string.snackbar_text_beacon_not_get_message,
                Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .show();
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
                    unitaSettings = settingsFragment.setSettings(this);
                    receiver.startReceiving(this, unitaSettings);
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
                notifySendingMessageFinished();
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
