package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class ParkingTicketRequest extends MppRequest {

    private final String zoneId;
    private final String ticket;

    public ParkingTicketRequest(String zoneId, String ticket) {
        this.zoneId = zoneId;
        this.ticket = ticket;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "parking_start")
                .add("data", zoneId + "," + ticket)
                .build();
    }
}
