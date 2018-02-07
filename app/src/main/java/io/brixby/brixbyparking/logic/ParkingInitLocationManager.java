package io.brixby.parking.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.api.response.ParkingStartResponse;
import io.brixby.parking.model.History;
import io.brixby.parking.model.Parking;


@Singleton
public class ParkingInitLocationManager {

    private final SharedPreferences preferences;

    @Inject
    public ParkingInitLocationManager(Context context) {
        preferences = context.getSharedPreferences("parking_location_prefs", Context.MODE_PRIVATE);
    }

    public void saveLocation(ParkingStartResponse parkingStartResponse, Location location) {
        if (location == null) return;
        Log.d("++++++", "Save location " + parkingStartResponse.getStartTime());
        preferences.edit().putString(parkingStartResponse.getStartTime(), locationToString(location)).apply();
    }

    @Nullable
    public Location getLocation(Parking parking) {
        return locationFromString(preferences.getString(parking.getStartTime(), null));
    }

    @Nullable
    public Location getLocation(History history) {
        String startTime = String.valueOf(history.getParkingStartTime());
        Log.d("++++++", "Get location " + startTime);
        return locationFromString(preferences.getString(startTime, null));
    }

    private String locationToString(Location location) {
        if (location == null) return null;
        return location.getLatitude() + "," + location.getLongitude();
    }

    private Location locationFromString(String str) {
        if (str == null) return null;

        String[] latlng = str.split(",");

        try {
            Location location = new Location("InitLocation");
            location.setLatitude(Double.parseDouble(latlng[0]));
            location.setLongitude(Double.parseDouble(latlng[1]));
            return location;
        } catch (Exception e) {
            return null;
        }
    }
}
