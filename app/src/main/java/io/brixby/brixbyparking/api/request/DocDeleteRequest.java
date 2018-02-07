package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class DocDeleteRequest extends MppRequest {

    private final String docId;

    public DocDeleteRequest(String docId) {
        this.docId = docId;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "document_delete")
                .addEncoded("data", "docID:" + docId)
                .build();
    }
}