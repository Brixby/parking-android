package io.brixby.parking.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.Message;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.brixby.parking.model.MapObject;
import io.brixby.parking.model.MapObjectsAdapter;
import io.brixby.parking.utils.Logger;



public class NearestZoneChecker {
    public static final String PREF_NAME = "check_nearest_zone";
    public static final long PERIOD = 1000 * 30; //30 seconds
    public static final int MESSAGE_NEAREST_PLACE = 1;

    private ScheduledFuture taskNearestZoneChecker;
    private ScheduledExecutorService worker;
    private NearestZoneCheckerImpl impl;
//    private MapObjectsAdapter mapObjectsAdapter;
    private LocationAdapter locationAdapter;
    private Location lastLocation;

    public NearestZoneChecker(Context context, Handler handler) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        worker = Executors.newSingleThreadScheduledExecutor();
        impl = new NearestZoneCheckerImpl(PERIOD, sp, handler);
//        mapObjectsAdapter = new MapObjectsAdapter(context);
        locationAdapter = new LocationAdapter(context);
    }

    public void stop() {
        if (taskNearestZoneChecker != null) {
            taskNearestZoneChecker.cancel(true);
        }
        locationAdapter.terminate();
    }

    public void start() {
        taskNearestZoneChecker = worker.scheduleWithFixedDelay(impl, 1000, PERIOD, TimeUnit.MILLISECONDS);
        locationAdapter.activate();
    }

    public class NearestZoneCheckerImpl implements Runnable {
        long period;
        SharedPreferences sp;

        Handler handler;
        boolean needUpdate;
        MapObject nearestParking;

        public NearestZoneCheckerImpl(long period, SharedPreferences sp, Handler handler) {
            this.period = period;
            this.sp = sp;
            this.handler = handler;
            needUpdate = false;
            nearestParking = null;
        }

        public void run() {
            try {
                Date now = new Date();
                long diff = now.getTime() - sp.getLong(PREF_NAME, 0);
                Logger.log(PREF_NAME + " " + diff);
                if (diff > 0 && diff < period) {
                    return;
                }

                Location location = locationAdapter.getCurrentLocation();
                if (location == null) {
                    needUpdate = nearestParking != null;
                    nearestParking = null;
                } else if (lastLocation == null || !lastLocation.equals(location)) {
                    try {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        List<MapObject> objects = MapObjectsAdapter.getMapObjectsFromAPIForList(latLng, "001,014", null, "0-1", 0, 0);
                        Logger.log("nearest object size:" + objects.size() + " distance to first:" + objects.get(0).getDistance());
                        if (objects.size() > 0 && objects.get(0).getDistance() < 200) {
                            needUpdate = true;
                            nearestParking = objects.get(0);
                            lastLocation = location;
                        } else {
                            needUpdate = nearestParking != null;
                            nearestParking = null;
                        }
                    } catch (IOException e) {
                        Logger.log(e.getMessage(), e);
                        needUpdate = nearestParking != null;
                        nearestParking = null;
                    } catch (JSONException e) {
                        Logger.log(e.getMessage(), e);
                        needUpdate = nearestParking != null;
                        nearestParking = null;
                    }
                    if (needUpdate) {
                        sendMessage();
                    }
                }
                sp.edit().putLong(PREF_NAME, now.getTime()).apply();
            } catch (Exception e) {
                Logger.log(e.toString());
            }
        }

        public void sendMessage() {
            Message.obtain(handler, MESSAGE_NEAREST_PLACE, nearestParking).sendToTarget();
        }
    }
}