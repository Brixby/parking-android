package io.brixby.parking.db;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import io.brixby.parking.App;
import io.brixby.parking.R;
import io.brixby.parking.model.MapObject;
import io.brixby.parking.model.MapObjectsRepository;
import timber.log.Timber;


public class LoadMapCacheIntentService extends IntentService {

    public static void start(Context context, ResultReceiver resultReceiver) {
        Intent intent = new Intent(context, LoadMapCacheIntentService.class);
        intent.putExtra("receiver", resultReceiver);
        context.startService(intent);
        Timber.d("LoadMapCache start");
    }

    @Inject Gson gson;
    @Inject MapObjectsRepository mapObjectsRepository;

    public LoadMapCacheIntentService() {
        super("LoadMapCacheIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        App.getInjector(this).inject(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("LoadMapCache handleIntent");

        int jsonVersion = jsonVersion();
        int dbVersion = mapObjectsRepository.getMapDataVersion();

        Timber.d("LoadMapCache json %s db %s", jsonVersion, dbVersion);

        if (dbVersion < jsonVersion) {
            long timeLoadJson = System.currentTimeMillis();
            List<MapObject> mapObjects = jsonMapObjects();
            Timber.d("LoadMapCache load json took %s", System.currentTimeMillis() - timeLoadJson);

            long timeRewrite = System.currentTimeMillis();
            mapObjectsRepository.rewriteMapObjects(mapObjects, jsonVersion);
            Timber.d("LoadMapCache rewrite took %s", System.currentTimeMillis() - timeRewrite);
        }

        dbVersion = mapObjectsRepository.getMapDataVersion();

        Timber.d("LoadMapCache db " + dbVersion);

        if (dbVersion >= 0) {
            MapObjectsDelta delta = delta(dbVersion);
            if (delta.version > 0) {
                Timber.d("LoadMapCache delta %s %s %s", delta.version, Arrays.toString(delta.deleted), delta.modified);
                mapObjectsRepository.updateMapObjects(delta.modified, delta.version, delta.deleted);
                Timber.d("LoadMapCache finish delta");
            }
        }

        ResultReceiver resultReceiver = intent.getParcelableExtra("receiver");
        if (resultReceiver != null) resultReceiver.send(Activity.RESULT_OK, Bundle.EMPTY);
    }

    private int jsonVersion() {
        int version = -1;

        try {
            InputStream is = getResources().openRawResource(R.raw.parking_data);
            JsonReader reader = new JsonReader(new InputStreamReader(is));
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("version")) {
                    version = reader.nextInt();
                    break;
                } else {
                    reader.skipValue();
                }
            }
            reader.close();
        } catch (Exception e) {
            // todo Timber.e
        }
        return version;
    }

    private List<MapObject> jsonMapObjects() {
        InputStream is = getResources().openRawResource(R.raw.parking_data);
        MapObjectsDelta mapObjects = gson.fromJson(new InputStreamReader(is), MapObjectsDelta.class);
        return mapObjects.modified;
    }

    private MapObjectsDelta delta(int version) {
        try {
            InputStream is = new URL("http://sandbox.brixby.io/api/delta?version=" + version).openStream();
            return gson.fromJson(new InputStreamReader(is), MapObjectsDelta.class);
        } catch (Exception e) {
            // todo Timber.e
            return new MapObjectsDelta();
        }
    }

    private static class MapObjectsDelta {
        int version;
        List<MapObject> modified = new ArrayList<>();
        String[] deleted = new String[0];
    }
}
