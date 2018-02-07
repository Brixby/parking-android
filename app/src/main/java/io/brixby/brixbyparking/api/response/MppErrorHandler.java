package io.brixby.parking.api.response;

import android.content.Context;

import javax.inject.Inject;

import io.brixby.parking.R;
import rx.Observable;

import static io.brixby.parking.utils.Logger.log;


public class MppErrorHandler {

    private final Context context;

    @Inject
    public MppErrorHandler(Context context) {
        this.context = context;
    }

    private String getErrorMessage() {
        return context.getString(R.string.network_error);
    }

    public <T extends MppResponse> Observable<? extends T> onError(Throwable error, Class<T> responseClass) {
        log("MppErrorHandler " + error.getMessage(), error);

        try {
            T t = responseClass.newInstance();
            t.setNetworkError(getErrorMessage());
            return Observable.just(t);
        } catch (Exception e) {
            return Observable.error(e);
        }
    }
}
