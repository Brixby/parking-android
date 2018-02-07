package io.brixby.parking.api.request;

import java.io.File;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;


public class FeedbackRequest extends MppRequest {

    public static final String MIME_TYPE = "application/octet-stream";

    //    private final MultipartBuilder builder;
    private final MultipartBody.Builder builder;

    public FeedbackRequest(int categoryId, String question, Map<String, String> files) {
        builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("operation", "support_request_add")
                .addFormDataPart("format", "json")
                .addFormDataPart("data", "categoryID:" + categoryId + "\nquestion:" + question);

        for (Map.Entry<String, String> file : files.entrySet()) {
            builder.addPart(RequestBody.create(MediaType.parse(MIME_TYPE), new File(file.getValue())));
        }
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
