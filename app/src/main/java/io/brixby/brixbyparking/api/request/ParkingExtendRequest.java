package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class ParkingExtendRequest extends MppRequest {

    private final int mins;
    private final String sessionId;

    public ParkingExtendRequest(int mins, String sessionId) {
        this.mins = mins;
        this.sessionId = sessionId;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "parking_extend")
                .add("data", mins + "," + sessionId)
                .build();
    }
}
