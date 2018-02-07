package io.brixby.parking.logic;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.api.MppApi;
import io.brixby.parking.api.request.CarAddRequest;
import io.brixby.parking.api.request.CarDeleteRequest;
import io.brixby.parking.api.request.CarUpdateRequest;
import io.brixby.parking.api.request.MppRequest;
import io.brixby.parking.model.Car;
import rx.Observable;


@Singleton
public class CarManager {

    private final MppApi mppApi;
    private final PersonManager personManager;

    @Inject
    public CarManager(MppApi mppApi, PersonManager personManager) {
        this.mppApi = mppApi;
        this.personManager = personManager;
    }

    public Observable<String> carAdd(String carNo, String carDescription, String carClass, boolean isDefault) {
        MppRequest request = new CarAddRequest(carNo, carDescription, carClass, isDefault);
        return makeRequest(request);
    }

    public Observable<String> carDelete(String carNo) {
        MppRequest request = new CarDeleteRequest(carNo);
        return makeRequest(request);
    }

    public Observable<String> carEdit(Car car, String carNo, String carDescription, String carClass, boolean isDefault) {
        // если номер машины не поменялся - car_update
        // иначе придется удалить старое авто и добавить новое
        if (car.getCarNo().equalsIgnoreCase(carNo)) {
            MppRequest request = new CarUpdateRequest(carNo, carDescription, carClass, isDefault);
            return makeRequest(request);
        } else {
            return carDelete(car.getCarNo())
                    .flatMap(result -> result == null
                            ? carAdd(carNo, carDescription, carClass, isDefault)
                            : Observable.just(result));
        }
    }

    private Observable<String> makeRequest(MppRequest request) {
        return mppApi.call(request)
                .flatMap(response -> response.isOk()
                        ? personManager.updatePerson()
                        : Observable.just(response.getErrorInfo()));
    }
}
