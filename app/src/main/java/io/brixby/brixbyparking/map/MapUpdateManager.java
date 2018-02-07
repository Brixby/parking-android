package io.brixby.parking.map;

import android.content.Context;
import android.content.SharedPreferences;

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

import static io.brixby.parking.utils.Logger.log;


public class MapUpdateManager {

    public static final String PREF_NAME = "map_update";
    public static final String PREF_UPDATE_USAGE = "startUsage";

    private MapManager mapManager;
    private MapObjectsAdapter mapObjectsAdapter;
    private SharedPreferences mSharedPreferences;

    private ScheduledExecutorService service;
    private ScheduledFuture taskUpdateAll, taskUpdateUsage;
    private UpdateAll updateAll;

    public void init(MapManager mapManager, MapObjectsAdapter mapObjectsAdapter, Context context) {
        this.mapManager = mapManager;
        this.mapObjectsAdapter = mapObjectsAdapter;
        this.mSharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Logger.log("MapUpdateManager init");
    }

    public void terminate() {
        if (taskUpdateAll != null) {
            taskUpdateAll.cancel(true);
            updateAll.alive = false;
        }
        if (taskUpdateUsage != null) taskUpdateUsage.cancel(true);
        if (service != null && !service.isShutdown()) service.shutdown();
        Logger.log("MapUpdateManager terminate");
    }

    public void startUpdate() {
        terminate();
        updateAll = new UpdateAll(mapObjectsAdapter, mapManager, mSharedPreferences);
        UpdateUsage updateUsage = new UpdateUsage(mapObjectsAdapter, mapManager, mSharedPreferences);
        service = Executors.newScheduledThreadPool(2);
        taskUpdateAll = service.scheduleWithFixedDelay(updateAll, 0, 15, TimeUnit.SECONDS);
        taskUpdateUsage = service.scheduleWithFixedDelay(updateUsage, 0, 15, TimeUnit.SECONDS);
        Logger.log("MapUpdateManager startUpdate");
    }

    public void stopUpdate() {
        terminate();
    }

    public static class UpdateAll implements Runnable {
        private MapObjectsAdapter mapObjectsAdapter;
        private MapManager mapManager;
        private SharedPreferences mSharedPreferences;

        volatile boolean alive;
        private long period = 1000 * 60 * 60 * 20; // 20 часов
        //        private long period = 1000 * 60 * 3;
        public static final String PREF_NAME_UPDATE_ALL = "updateAll";

        public UpdateAll(MapObjectsAdapter mapObjectsAdapter, MapManager mapManager, SharedPreferences sharedPreferences) {
            this.mapObjectsAdapter = mapObjectsAdapter;
            this.mapManager = mapManager;
            this.mSharedPreferences = sharedPreferences;
            this.alive = true;
        }

        public void run() {
            try {
                Date now = new Date();
                long diff = now.getTime() - mSharedPreferences.getLong(PREF_NAME_UPDATE_ALL, 0);
                Logger.log(PREF_NAME_UPDATE_ALL + " " + diff);
                if (diff > 0 && diff < period)
                    return;

                // для мпп слоев
                for (int i = 0; i < 200; i++) {
                    String limit = i * 400 + "-" + (i * 400 + 400);
                    final List<MapObject> newMapObjects = mapObjectsAdapter.updateMapObjectsFromAPI(true, limit);
                    if (!alive) return;
                    if (newMapObjects.size() > 0)
                        mapManager.getHandler().post(new Runnable() {
                            public void run() {
                                mapManager.updateObjects(newMapObjects);
                            }
                        });
                    else
                        break;
                }

                Logger.log("clearMPPOldObjects before: " + mapObjectsAdapter.getObjectsCount());
                mapObjectsAdapter.clearOldObjects(true, now.getTime() - period);
                Logger.log("clearMPPOldObjects after: " + mapObjectsAdapter.getObjectsCount());

                // закончили грузить МПП, разрешаем обновлять загруженность.
                // без этого при первом запуске загруженность обновится сразу и для пустого кэша - впустую, а потом вызовется только через 5 минут
                mSharedPreferences.edit().putBoolean(PREF_UPDATE_USAGE, true).apply();

                // для всех слоев

                for (int i = 0; i < 200; i++) {
                    String limit = i * 400 + "-" + (i * 400 + 400);
                    final List<MapObject> newMapObjects = mapObjectsAdapter.updateMapObjectsFromAPI(false, limit);
                    if (!alive) {
                        return;
                    }
                    if (newMapObjects.size() > 0) {
                        mapManager.getHandler().post(new Runnable() {
                            public void run() {
                                mapManager.updateObjects(newMapObjects);
                            }
                        });
                    } else {
                        break;
                    }
                }

                Logger.log("clearAllOldObjects before: " + mapObjectsAdapter.getObjectsCount());
                mapObjectsAdapter.clearOldObjects(false, now.getTime() - period);
                Logger.log("clearMPPOldObjects after: " + mapObjectsAdapter.getObjectsCount());

                mSharedPreferences.edit().putLong(PREF_NAME_UPDATE_ALL, now.getTime()).apply();
            } catch (JSONException e) {
                // чтото с АПИ - не будем сохранять время - будем пробовать регулярно.
                Logger.log(e.toString());
            } catch (IOException e) {
                // нет инета скорее всего - не сохраним время - еще раз попробуем
            } catch (Exception e) {
                Logger.log(e.toString());
            }
        }
    }

    public static class UpdateUsage implements Runnable {
        private MapObjectsAdapter mapObjectsAdapter;
        private MapManager mapManager;
        private SharedPreferences mSharedPreferences;

        private long period = 1000 * 60 * 5; //5 минут
        //        private long period = 1000 * 30;
        private String prefName = "updateUsage";

        public UpdateUsage(MapObjectsAdapter mapObjectsAdapter, MapManager mapManager, SharedPreferences sharedPreferences) {
            this.mapObjectsAdapter = mapObjectsAdapter;
            this.mapManager = mapManager;
            this.mSharedPreferences = sharedPreferences;
        }

        public void run() {
            try {
                Date now = new Date();
                boolean start = mSharedPreferences.getBoolean(PREF_UPDATE_USAGE, false);
                long diff = now.getTime() - mSharedPreferences.getLong(prefName, 0);
                Logger.log(prefName + " " + diff);
                if (!start || (diff > 0 && diff < period)) {
                    return;
                }

                mapObjectsAdapter.updateZoneUsage();
                mapManager.getHandler().post(new Runnable() {
                    public void run() {
                        mapManager.updateZoneUsage();
                    }
                });
                mSharedPreferences.edit().putLong(prefName, now.getTime()).apply();
            } catch (JSONException e) {

                Logger.log(e.toString());
            } catch (IOException e) {

            } catch (Exception e) {
                Logger.log(e.toString());
            }
        }
    }


}
