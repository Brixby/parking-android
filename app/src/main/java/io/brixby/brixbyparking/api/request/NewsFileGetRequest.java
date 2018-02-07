package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class NewsFileGetRequest extends MppRequest {

    private final String fileId;

    public NewsFileGetRequest(String fileId) {
        this.fileId = fileId;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "file_get")
                .add("data", fileId)
                .build();
    }

}
