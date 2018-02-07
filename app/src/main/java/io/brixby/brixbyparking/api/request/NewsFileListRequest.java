package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class NewsFileListRequest extends MppRequest {

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "file_list")
                .add("partnerID", "Qulix")
                .build();
    }

}
