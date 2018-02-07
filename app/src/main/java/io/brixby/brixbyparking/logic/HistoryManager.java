package io.brixby.parking.logic;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.Utils;
import io.brixby.parking.api.MppApi;
import io.brixby.parking.api.request.AccountHistoryEmailRequest;
import io.brixby.parking.api.request.AccountHistoryRequest;
import io.brixby.parking.api.response.AccountHistoryResponse;
import io.brixby.parking.model.History;
import io.brixby.parking.ui.screens.HistoryFragment;
import rx.Observable;


@Singleton
public class HistoryManager {

    public static final String PREF_EMAIL = "email";

    private SharedPreferences preferences;
    private MppApi mppApi;

    @Inject
    public HistoryManager(Context context, MppApi mppApi) {
        preferences = context.getSharedPreferences(Utils.PREFFS_NAME, Context.MODE_PRIVATE);
        this.mppApi = mppApi;
    }

    public Observable<List<History>> getHistory(long startTime, long endTime, HistoryFragment.HistoryType type) {
        String historyType = type == HistoryFragment.HistoryType.Payments ? "payments" :
                type == HistoryFragment.HistoryType.Parking ? "parking" : "parking,payments";
        return mppApi.call(new AccountHistoryRequest(historyType, startTime, endTime), AccountHistoryResponse.class)
                .map(AccountHistoryResponse::getHistory)
                .onErrorReturn(throwable -> new ArrayList<>(0));
    }

    public Observable<String> emailHistory(long startTime, long endTime, String email) {
        return mppApi.call(new AccountHistoryEmailRequest(email, startTime, endTime))
                .map(response -> {
                    if (response.isOk()) {
                        preferences.edit().putString(PREF_EMAIL, email).apply();
                        return null;
                    } else return response.getErrorInfo();
                })
                .onErrorReturn(throwable -> "Ошибка");
    }
}
