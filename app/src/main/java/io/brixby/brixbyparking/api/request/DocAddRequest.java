package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class DocAddRequest extends MppRequest {

    private final int docType;
    private final String docNumber;
    private final String docNumberSeries;

    public DocAddRequest(int docType, String docNumber, String docNumberSeries) {
        this.docType = docType;
        this.docNumber = docNumber;
        this.docNumberSeries = docNumberSeries;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "document_add")
                .add("data", "docType:" + docType + "\ndocNumber:" + docNumber + "\ndocNumberSeries:" + docNumberSeries)
                .build();
    }
}