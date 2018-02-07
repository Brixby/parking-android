package io.brixby.parking.logic;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.api.MppApi;
import io.brixby.parking.api.request.DocAddRequest;
import io.brixby.parking.api.request.DocDeleteRequest;
import io.brixby.parking.api.request.DocUpdateRequest;
import io.brixby.parking.api.request.MppRequest;
import io.brixby.parking.api.response.MppResponse;
import rx.Observable;


@Singleton
public class DocumentManager {

    private final MppApi mppApi;
    private final PersonManager personManager;

    @Inject
    public DocumentManager(MppApi mppApi, PersonManager personManager) {
        this.mppApi = mppApi;
        this.personManager = personManager;
    }

    public Observable<String> docAdd(int docType, String docNumber, String docNumberSeries) {
        MppRequest request = new DocAddRequest(docType, docNumber, docNumberSeries);
        MppResponse response = mppApi.call(request).toBlocking().single();
        return processResponse(response);
    }

    public Observable<String> docDelete(String docId) {
        MppRequest request = new DocDeleteRequest(docId);
        MppResponse response = mppApi.call(request).toBlocking().single();
        return processResponse(response);
    }

    public Observable<String> docEdit(String docId, int docType, String docNumber, String docNumberSeries) {
        MppRequest request = new DocUpdateRequest(docType, docId, docNumber, docNumberSeries);
        MppResponse response = mppApi.call(request).toBlocking().single();
        return processResponse(response);
    }

    private Observable<String> processResponse(MppResponse response) {
        return response.isOk() ? personManager.updatePerson() : Observable.just(response.getErrorInfo());
    }
}
