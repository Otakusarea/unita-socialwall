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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import at.floriantaurer.unitasocialwallclient.utils.ServiceConstants;

public class SocialWallService extends Service{

    private static final String LOG_TAG = "ForegroundService";
    public static boolean IS_SERVICE_RUNNING = false;

    private static NotificationCompat.Builder statusBuilder;
    private static final int NOTIFICATION_STATUS_REQUEST_CODE = 1;
    private static Notification notificationStatus;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null){

            Intent service = new Intent(getApplicationContext(), SocialWallService.class);
            service.setAction(ServiceConstants.ACTION.STOPFOREGROUND_ACTION);

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor ed = sp.edit();
            ed.putString("PREFERENCES_APP_STATE", "Social Wall");
            ed.apply();

            SocialWallService.IS_SERVICE_RUNNING = false;
            stopForeground(true);
            stopSelf();
        }else if (ServiceConstants.ACTION.STARTFOREGROUND_ACTION.equals(intent.getAction())) {
            showNotification();
            SocialWallService.IS_SERVICE_RUNNING = true;
        } else if (ServiceConstants.ACTION.STOPFOREGROUND_ACTION.equals(intent.getAction())) {
            SocialWallService.IS_SERVICE_RUNNING = false;
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    private void showNotification() {
        startForeground(1, initStatusNotification(getApplicationContext()));

    }

    public static Notification initStatusNotification(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManagerOreoAbove = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int channelImportance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel statusChannel = new NotificationChannel("1", context.getString(R.string.statusChannelName), channelImportance);
            notificationManagerOreoAbove.createNotificationChannel(statusChannel);
        }

        statusBuilder = //create a builder for the detection notification
                new NotificationCompat.Builder(context, "1")
                        .setSmallIcon(R.drawable.st_send_grey_receive_grey) //adding the icon
                        .setContentTitle("Social Wall") //adding the title
                        .setContentText(context.getString(R.string.service_status_running)) //adding the text
                        .setPriority(NotificationCompat.PRIORITY_LOW) // Required for Android 7.1 and lower
                        //Requires API 21 .setCategory(Notification.CATEGORY_SERVICE)
                        .setOngoing(true); //it's canceled when tapped on it

        PendingIntent resultPendingIntent = getPendingIntentStatusFlagUpdateCurrent(context);

        statusBuilder.setContentIntent(resultPendingIntent);

        notificationStatus = statusBuilder.build(); //build the notification
        return notificationStatus;
    }

    private static PendingIntent getPendingIntentStatusFlagUpdateCurrent(Context context) {
        Intent resultIntent = new Intent(context, MainActivity.class); //the intent is still the main-activity
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(
                context,
                NOTIFICATION_STATUS_REQUEST_CODE,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
