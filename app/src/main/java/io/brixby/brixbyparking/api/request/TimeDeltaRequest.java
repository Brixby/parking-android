package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class TimeDeltaRequest extends MppRequest {

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        formBuilder
                .add("operation", "time_check")
                .add("data", String.valueOf(System.currentTimeMillis() / 1000));
        return formBuilder.build();
    }
}
