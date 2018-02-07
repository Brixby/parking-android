package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class AttachmentGetRequest extends MppRequest {

    private final String fileId;

    public AttachmentGetRequest(String fileId) {
        this.fileId = fileId;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "attachment_get")
                .add("data", "fileID:" + fileId)
                .build();
    }
}
