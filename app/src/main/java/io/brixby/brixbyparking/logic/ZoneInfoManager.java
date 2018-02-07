package io.brixby.parking.logic;

import android.content.Context;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.R;
import io.brixby.parking.api.MppApi;
import io.brixby.parking.api.Response;
import io.brixby.parking.api.request.ZoneInfoRequest;
import io.brixby.parking.api.response.ZoneInfoResponse;
import io.brixby.parking.model.ZoneInfo;
import rx.Observable;


@Singleton
public class ZoneInfoManager {

    private final MppApi mppApi;

    @Inject
    public ZoneInfoManager(MppApi mppApi) {
        this.mppApi = mppApi;
    }

    public Observable<Response> getParkInfoById(String placeId, Context context) {
        return mppApi.call(new ZoneInfoRequest(placeId), ZoneInfoResponse.class)
                .map(response -> {
                    if (response.isOk()) {
                        List<ZoneInfo> zoneList = response.getZoneList();
                        if (zoneList != null && zoneList.size() > 0) {
                            return Response.success("").setData(zoneList.get(0));
                        } else return Response.error(context.getString(R.string.info_no_info));
                    } else return Response.error(response.getErrorInfo());
                })
                .onErrorReturn(throwable -> Response.ERROR());
    }

}
