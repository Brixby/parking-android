package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class AccountHistoryEmailRequest extends MppRequest {

    private final String email;
    private final long startTime;
    private final long stopTime;

    public AccountHistoryEmailRequest(String email, long startTime, long stopTime) {
        this.email = email;
        this.startTime = startTime;
        this.stopTime = stopTime;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "account_history")
                .add("data", "type:summary\nemail:" + email + "\nstartTime:" + startTime + "\nstopTime:" + stopTime)
                .build();
    }
}