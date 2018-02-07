package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class ParkingStopRequest extends MppRequest {

    private final String sessionId;

    public ParkingStopRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "parking_stop")
                .add("data", sessionId)
                .build();
    }
}
