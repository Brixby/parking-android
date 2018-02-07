package io.brixby.parking.logic;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.brixby.parking.api.response.ParkingListResponse;
import io.brixby.parking.model.Person;


public class ParkingSessionUpdater {

    private ScheduledExecutorService mService;
    private PersonManager personManager;
    private String sessionId;

    public ParkingSessionUpdater(PersonManager personManager, String sessionId) {
        mService = Executors.newSingleThreadScheduledExecutor();
        this.personManager = personManager;
        this.sessionId = sessionId;
    }

    public void start() {
        mService.scheduleWithFixedDelay(getRunnableLogic(), 0, 500, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        mService.shutdown();
    }

    private Runnable getRunnableLogic() {
        return new Runnable() {
            public static final int NUMBER_OF_QUERIES = 30;
            int counter = 0;

            public void run() {
                counter++;
                String status = personManager.getParkingBySessionId(sessionId);
                if ("active".equalsIgnoreCase(status)) {
                    ParkingListResponse response = personManager.parkingList().toBlocking().single();
                    if (response.isOk()) {
                        Person person = personManager.getPerson();
                        person.setParking(response.getSessionsList());
                    }
                    stop();
                } else if (counter > NUMBER_OF_QUERIES) {
                    stop();
                }
            }
        };
    }

}
