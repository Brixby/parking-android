package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class AccountCheckRequest extends MppRequest {

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "account_check")
                .add("data", "wallet,permits,private,carDetails,documents,phoneDetails,attachments")
                .add("services", "parking")
                .build();
    }
}
