package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;
import io.brixby.parking.Utils;

public class ZoneInfoRequest extends MppRequest {

    private final String placeId;

    public ZoneInfoRequest(String placeId) {
        this.placeId = placeId;
    }

    @Override
    protected String getUrl() {
        return Utils.MAP_URL;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "zone_info")
                .add("partnerID", Utils.PARTNER_ID)
                .add("objectIDs", placeId)
                .build();
    }
}
