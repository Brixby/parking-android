package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class AccountHistoryRequest extends MppRequest {

    private final String historyType;
    private final long startTime;
    private final long stopTime;

    public AccountHistoryRequest(String historyType, long startTime, long stopTime) {
        this.historyType = historyType;
        this.startTime = startTime;
        this.stopTime = stopTime;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "account_history")
                .add("data", "type:" + historyType + "\nstartTime:" + startTime + "\nstopTime:" + stopTime)
                .build();
    }
}