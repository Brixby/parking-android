package io.brixby.parking.map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import io.brixby.parking.BuildConfig;
import io.brixby.parking.R;
import io.brixby.parking.ui.screens.MainMenuActivity;
import io.brixby.parking.utils.Logger;

import static io.brixby.parking.utils.Logger.log;


public class GeofenceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        log("Geofence... broadcast transition!");
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event.hasError()) {
            log("Geofence... transition error: " + event.getErrorCode());
            if (BuildConfig.DEBUG) {
                sendNotification(context, "Error: " + event.getErrorCode());
            }
        } else {
            log("Geofence... transition location: " + event.getTriggeringLocation() + " transition: " + event.getGeofenceTransition());
            sendNotification(context, "Stop and back money");
            for (Geofence geofence : event.getTriggeringGeofences()) {
                GeofenceIntentService.stop(context, geofence.getRequestId());
            }
        }

    }

    private void sendNotification(Context context, String msg) {
        Context ctx = context.getApplicationContext();
        Intent intent = new Intent(ctx, MainMenuActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx)
                .setDefaults(Notification.DEFAULT_ALL)
                .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.drawable.icon))
                .setSmallIcon(R.drawable.ic_tab_park_tile)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentTitle(ctx.getString(R.string.app_name))
                .setContentText(msg)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
        try {
            NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(986, mBuilder.build());
        } catch (Exception e) {
            Logger.log("Error sending notification", e);
        }
    }
}
