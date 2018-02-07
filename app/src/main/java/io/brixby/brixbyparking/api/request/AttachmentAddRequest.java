package io.brixby.parking.api.request;

import java.io.File;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;


public class AttachmentAddRequest extends MppRequest {

    public static final String MIME_TYPE = "image/jpeg";

    private final MultipartBody.Builder builder;

    public AttachmentAddRequest(File file) {
        builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("operation", "attachment_add")
                .addFormDataPart("format", "json")
                .addFormDataPart("actingPartnerID", "-")
                .addFormDataPart("data", "type:avatar");

        builder.addPart(
                Headers.of("Content-Disposition", "form-data; name=\"file\"; filename=\"avatar\""),
                RequestBody.create(MediaType.parse(MIME_TYPE), file));
    }

    @Override
    public MppRequest addLoginInfo(String phone, String pin) {
        builder.addFormDataPart("phoneNo", phone).addFormDataPart("PIN", pin);
        return this;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return builder.build();
    }
}
