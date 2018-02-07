package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class ParkingCheckRequest extends MppRequest {

    private final String sessionId;

    public ParkingCheckRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "parking_check")
                .add("data", sessionId)
                .build();
    }
}
