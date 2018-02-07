package io.brixby.parking.api.response;

import java.util.List;

import io.brixby.parking.model.ZoneInfo;


public class ZoneInfoResponse extends MppResponse {

    private List<ZoneInfo> objectsList;

    public List<ZoneInfo> getZoneList() {
        return objectsList;
    }
}
