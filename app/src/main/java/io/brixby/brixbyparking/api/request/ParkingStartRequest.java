package io.brixby.parking.api.request;

import android.location.Location;
import android.support.annotation.Nullable;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class ParkingStartRequest extends MppRequest {

    private final String zoneId;
    private final String carId;
    private final int mins;
    private final Location location;

    public ParkingStartRequest(String zoneId, String carId, int mins, @Nullable Location location) {
        this.zoneId = zoneId;
        this.carId = carId;
        this.mins = mins;
        this.location = location;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        formBuilder
                .add("operation", "parking_start")
                .add("data", zoneId + "," + carId + "," + mins);

        if (location != null) {
            formBuilder.add("userCoords", location.getLongitude() + "," + location.getLatitude());
        }

        return formBuilder.build();
    }
}
