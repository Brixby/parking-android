package io.brixby.parking.map;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;


public class LocationAdapter implements LocationListener {

    public static int TIME = 1000 * 60; // 1 min
    public static int METERS = 500;

    private LocationManager locationManager;
    private static Location currentLocation;

    public LocationAdapter(Context context) {
        locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    }

    public void activate() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TIME, METERS, this);
        } catch (IllegalArgumentException e) {
            //provider is null or doesn't exist on this device
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TIME, METERS, this);
        } catch (IllegalArgumentException e) {
            //provider is null or doesn't exist on this device
        }
    }

    public void terminate() {
        locationManager.removeUpdates(this);
    }

    public Location getCurrentLocation() {
        if (currentLocation == null) {
            Location locGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locNET = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            currentLocation = isBetterLocation(locNET, locGPS) ? locNET : locGPS;
        }
        return currentLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, currentLocation)) {
            currentLocation = location;
        }
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (location == null) {
            return false;
        }

        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }
        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TIME;
        boolean isSignificantlyOlder = timeDelta < -TIME;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }
}
