package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class CarAddRequest extends MppRequest {

    private final String carNo;
    private final String carDescription;
    private final String carClass;
    private final int isDefault;

    public CarAddRequest(String carNo, String carDescription, String carClass, boolean isDefault) {
        this.carNo = carNo;
        this.carDescription = carDescription;
        this.carClass = carClass;
        this.isDefault = isDefault ? 1 : 0;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "car_add")
                .add("data", "classID:" + carClass
                        + "\ncarNo:" + carNo
                        + "\ndescription:" + carDescription
                        + "\nisDefault:" + isDefault)
                .build();
    }
}