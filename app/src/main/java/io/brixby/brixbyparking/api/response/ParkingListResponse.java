package io.brixby.parking.api.response;

import java.util.List;

import io.brixby.parking.model.Parking;


public class ParkingListResponse extends MppResponse {

    private List<Parking> sessionsList;

    public List<Parking> getSessionsList() {
        return sessionsList;
    }
}
