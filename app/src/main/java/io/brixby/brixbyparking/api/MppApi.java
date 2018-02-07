package io.brixby.parking.api;

import com.google.gson.Gson;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import io.brixby.parking.api.request.MppRequest;
import io.brixby.parking.api.response.MppErrorHandler;
import io.brixby.parking.api.response.MppResponse;
import io.brixby.parking.api.response.MppResponseWrapper;
import rx.Observable;
import rx.schedulers.Schedulers;


@Singleton
public class MppApi {

    private final MppHttpClient mppHttpClient;
    private final Gson gson;
    private final MppErrorHandler errorHandler;

    private String phone, pin;

    @Inject
    public MppApi(MppHttpClient mppHttpClient, Gson gson, MppErrorHandler errorHandler) {
        this.mppHttpClient = mppHttpClient;
        this.gson = gson;
        this.errorHandler = errorHandler;
    }

    public void setLogin(String phone, String pin) {
        this.phone = phone;
        this.pin = pin;
    }

    private void addLoginInfo(MppRequest request) {
        if (phone != null && pin != null) {
            request.addLoginInfo(phone, pin);
        }
    }

    public <T extends MppResponse> Observable<T> call(MppRequest request, Class<T> responseClass) {
        addLoginInfo(request);

        return Observable
                .fromCallable(() -> {
                    ResponseBody body = mppHttpClient.execute(request);
                    MppResponseWrapper<T> wrapper = gson.fromJson(body.charStream(), new MppResponseWrapper<>(responseClass));
                    return wrapper.updateStatus().getResponse();
                })
                .onErrorResumeNext(e -> errorHandler.onError(e, responseClass))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation());
    }

    public Observable<byte[]> callBytes(MppRequest request) {
        addLoginInfo(request);

        return Observable
                .fromCallable(() -> {
                    ResponseBody body = mppHttpClient.execute(request);
                    return body.bytes();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation());
    }

    public Observable<MppResponse> call(MppRequest request) {
        return call(request, MppResponse.class);
    }

    public void execute(MppRequest request) throws IOException {
        addLoginInfo(request);
        mppHttpClient.execute(request);
    }
}
