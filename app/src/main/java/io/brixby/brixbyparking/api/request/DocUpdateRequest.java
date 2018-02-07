package io.brixby.parking.api.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;


public class DocUpdateRequest extends MppRequest {

    private final int docType;
    private final String docId, docNumber, docNumberSeries;

    public DocUpdateRequest(int docType, String docId, String docNumber, String docNumberSeries) {
        this.docType = docType;
        this.docId = docId;
        this.docNumber = docNumber;
        this.docNumberSeries = docNumberSeries;
    }

    @Override
    RequestBody getRequestBody(FormBody.Builder formBuilder) {
        return formBuilder
                .add("operation", "document_update")
                .add("data", "docID:" + docId + "\ndocType:" + docType + "\ndocNumber:" + docNumber
                        + "\ndocNumberSeries:" + docNumberSeries)
                .build();
    }
}