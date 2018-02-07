package io.brixby.parking.api.response;


public class ParkingStartResponse extends MppResponse {

    private String sessionID;
    private String status;
    private Double totalCharge;
    private String startTime;

    public String getStatus() {
        return status;
    }

    public String getSessionID() {
        return sessionID;
    }

    public Double getTotalCharge() {
        return totalCharge;
    }

    public String getStartTime() {
        return startTime;
    }
}
