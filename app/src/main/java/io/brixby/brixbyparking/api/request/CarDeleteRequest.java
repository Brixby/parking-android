package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class CarDeleteRequest extends MppRequest {

    private final String carNo;

    public CarDeleteRequest(String carNo) {
        this.carNo = carNo;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "car_delete")
                .addEncoded("data", "carNo:" + carNo)
                .build();
    }
}