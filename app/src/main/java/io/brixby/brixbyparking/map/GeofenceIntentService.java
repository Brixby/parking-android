package io.brixby.parking.map;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Collections;

import static io.brixby.parking.utils.Logger.log;


public class GeofenceIntentService extends IntentService {

    public static final String ACTION_ADD = "add_geofence", ACTION_REMOVE = "remove_geofence";
    public static final String EXTRA_REQUEST_ID = "session", EXTRA_DURATION_IN_MIN = "duration", EXTRA_LAT = "lat", EXTRA_LNG = "lng";

    public static void start(Context context, String requestId, int durationMinutes, double lat, double lng) {
        Intent intent = new Intent(context, GeofenceIntentService.class);
        intent.setAction(ACTION_ADD);
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        intent.putExtra(EXTRA_DURATION_IN_MIN, durationMinutes);
        intent.putExtra(EXTRA_LAT, lat);
        intent.putExtra(EXTRA_LNG, lng);
        log("Geofence start service to add geofence");
        context.startService(intent);
    }

    public static void stop(Context context, String requestId) {
        Intent intent = new Intent(context, GeofenceIntentService.class);
        intent.setAction(ACTION_REMOVE);
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        log("Geofence start service to remove geofence");
        context.startService(intent);
    }

    public GeofenceIntentService() {super("GeofenceIntentService");}

    private GoogleApiClient googleApiClient;

    @Override
    protected void onHandleIntent(Intent intent) {
        log("Geofence onHandleIntent");
        googleApiClient = buildApiClient();
        ConnectionResult connectionResult = googleApiClient.blockingConnect();
        log("Geofence api connection result success: " + connectionResult.isSuccess());

        if (connectionResult.isSuccess()) {
            if (intent.getAction().equals(ACTION_ADD)) addGeofence(googleApiClient, intent);
            else if (intent.getAction().equals(ACTION_REMOVE)) removeGeofence(googleApiClient, intent);
        } else {
            handleErrorConnect(connectionResult);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (googleApiClient != null) googleApiClient.disconnect();
    }

    private GoogleApiClient buildApiClient() {
        return new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void addGeofence(GoogleApiClient googleApiClient, Intent intent) {
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        int durationMinutes = intent.getIntExtra(EXTRA_DURATION_IN_MIN, 60);
        long duration = durationMinutes * 60 * 1000;
        double lat = intent.getDoubleExtra(EXTRA_LAT, 0);
        double lng = intent.getDoubleExtra(EXTRA_LNG, 0);
        GeofencingRequest geofencingRequest = buildGeofencingRequest(requestId, duration, lat, lng);
        Status status = LocationServices.GeofencingApi.addGeofences(googleApiClient, geofencingRequest, getGeofenceTransitionPendingIntent()).await();
        log("Geofence status: " + status.toString());
    }

    private GeofencingRequest buildGeofencingRequest(String requestId, long duration, double lat, double lng) {
        log("Geofence... Start tracking user location for " + duration + " at " + lat + "," + lng);
        Geofence geofence = new Geofence.Builder()
                .setRequestId(requestId)
                .setExpirationDuration(duration)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .setNotificationResponsiveness(5000) //todo: 5 sec for debug, for release 5-10 min
                .setCircularRegion(lat, lng, 2000) // 2 km
                .build();
        return new GeofencingRequest.Builder().addGeofence(geofence).build();
    }

    private PendingIntent getGeofenceTransitionPendingIntent() {
        Intent intent = new Intent("io.brixby.parking.mobile.debug.ACTION_RECEIVE_GEOFENCE");
        return PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void removeGeofence(GoogleApiClient googleApiClient, Intent intent) {
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        log("Geofenct remove request:" + requestId);
        Status status = LocationServices.GeofencingApi.removeGeofences(googleApiClient, Collections.singletonList(requestId)).await();
        log("Geofence status: " + status.toString());
    }

    private void handleErrorConnect(ConnectionResult connectionResult) {
        log("Geofence api connection error: " + connectionResult.getErrorMessage());
    }

}
