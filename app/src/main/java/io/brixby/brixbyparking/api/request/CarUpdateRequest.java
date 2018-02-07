package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


// TODO: 12/17/16 same CarAddRequest, extend from it or make only one class with operation choosing

public class CarUpdateRequest extends MppRequest {

    private final String carNo;
    private final String carDescription;
    private final String carClass;
    private final int isDefault;

    public CarUpdateRequest(String carNo, String carDescription, String carClass, boolean isDefault) {
        this.carNo = carNo;
        this.carDescription = carDescription;
        this.carClass = carClass;
        this.isDefault = isDefault ? 1 : 0;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "car_update")
                .add("data", "classID:" + carClass
                        + "\ncarNo:" + carNo
                        + "\ndescription:" + carDescription
                        + "\nisDefault:" + isDefault)
                .build();
    }
}