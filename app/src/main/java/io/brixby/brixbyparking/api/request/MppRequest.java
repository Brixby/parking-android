package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import io.brixby.parking.Utils;


public abstract class MppRequest implements IRequest {

    private final FormBody.Builder formBuilder;

    public MppRequest() {
        formBuilder = new FormBody.Builder()
                .add("format", "json")
                .add("appVersion", "3");
    }

    public MppRequest addLoginInfo(String phone, String pin) {
        formBuilder
                .add("phoneNo", phone)
                .add("PIN", pin);
        return this;
    }

    public Request getRequest() {
        return new Request.Builder()
                .url(getUrl())
                .post(getRequestBody(formBuilder))
                .build();
    }

    protected String getUrl() {
        return Utils.API_URL;
    }

    abstract RequestBody getRequestBody(FormBody.Builder formBuilder);
}
