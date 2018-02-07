package io.brixby.parking.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.Utils;
import io.brixby.parking.map.GeofenceIntentService;
import io.brixby.parking.ui.screens.SettingsFragment;


@Singleton
public class GeofenceManager {

    private final Context context;

    @Inject
    public GeofenceManager(Context context) {
        this.context = context;
    }

    public void onParkingStart(String parkingSessionId, int parkingDuration, Location location) {
        if (isGeoNotifyEnabled() && location != null) {
            GeofenceIntentService.start(context, parkingSessionId, parkingDuration, location.getLatitude(), location.getLongitude());
        }
    }

    public void onParkingStop(String parkingSessionId) {
        GeofenceIntentService.stop(context, parkingSessionId);
    }

    private boolean isGeoNotifyEnabled() {
        SharedPreferences preferences = context.getSharedPreferences(Utils.PREFFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(SettingsFragment.SETTING_GEO, true);
    }

}
