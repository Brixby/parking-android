package io.brixby.parking.map;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import com.androidmapsextensions.GoogleMap;
import com.androidmapsextensions.Marker;
import com.androidmapsextensions.MarkerOptions;
import com.androidmapsextensions.TileOverlay;
import com.androidmapsextensions.TileOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.UrlTileProvider;

import org.json.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.Utils;
import io.brixby.parking.db.LoadMapCacheIntentService;
import io.brixby.parking.filter.FilterManager;
import io.brixby.parking.model.MapObject;
import io.brixby.parking.model.MapObjectsAdapter;
import io.brixby.parking.model.MapObjectsRepository;
import io.brixby.parking.ui.screens.MapFragment;
import io.brixby.parking.utils.Logger;


@Singleton
public class MapManager {

    private FilterManager filterManager;
    private String filterLayers; // from filterManager
    private String layers, layersForList, layersForRastr; //concat with MPP_layers if enabled
    private int parkPrice;
    private int parkPlaces;
    private int parkPlacesForInvalid;

    private ImageManager imageManager;
    private MapObjectsRepository mapObjectsRepository;
    private Map<String, MapObject> mapObjects; //key = placeId
    private AsyncTask<Object, Collection<MapObject>, Collection<MapObject>> taskCache;
    private Handler handler;

    private WeakReference<MapFragment> mMapFragment;

    private final Map<String, TileOverlay> overlays = new HashMap<>();

    // for polygon visible
    private float zoom;
    private LatLngBounds bounds;
    private final float zoomVisible = 16;
    private Map<MapObject, MapDrawing> objectsWithVisibility;

    private MapObject showingParking;

    @Inject
    public MapManager(Context context, FilterManager filterManager, ImageManager imageManager, MapObjectsRepository mapObjectsRepository) {
        this.filterManager = filterManager;
        this.imageManager = imageManager;
        this.mapObjectsRepository = mapObjectsRepository;
        this.filterLayers = "";
        this.layers = "-1";
        this.parkPlaces = 0;
        this.parkPrice = 0;
        this.parkPlacesForInvalid = 0;
        this.handler = new Handler();
        this.objectsWithVisibility = new HashMap<>();
        this.mMapFragment = new WeakReference<>(null);

        mapObjects = Collections.synchronizedMap(new HashMap<>());
        updateFilter(null);
        startUpdateMap(context);
    }

    public void reset() {
        mapObjects.clear();
        overlays.clear();
    }

    public void terminate() {
//        mapUpdateManager.terminate();
    }

    public void startUpdateMap(Context context) {
        ResultReceiver resultReceiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                updateMap();
            }
        };
        LoadMapCacheIntentService.start(context, resultReceiver);
    }

    public void stopUpdateMap() {
//        mapUpdateManager.stopUpdate();
    }

    public void updateFilter(MapFragment mapView) {
        int parkCostNew = filterManager.getCost();
        int parkPlacesNew = filterManager.getParkPlaces();
        int parkPlacesForInvalidNew = filterManager.getParkPlacesForInvalids();
        String[] filterLayersNew = filterManager.getLayers();
        Logger.log("updateFilter, filterLayersNew: " + Arrays.toString(filterLayersNew));
        if (parkPlacesForInvalidNew != parkPlacesForInvalid || parkPrice != parkCostNew || parkPlaces != parkPlacesNew || !filterLayers.equals(filterLayersNew[0])) {
            parkPrice = parkCostNew;
            parkPlaces = parkPlacesNew;
            parkPlacesForInvalid = parkPlacesForInvalidNew;
            layers = filterLayersNew[0];
            layersForList = filterLayersNew[1];
            layersForRastr = filterLayersNew[2];
            mMapFragment = new WeakReference<>(mapView);
            updateMap();
        }
    }

    public void updateMap() {
        Logger.log("updateMap " + layers);
        MapFragment mapFragment = mMapFragment.get();
        if (mapFragment != null && mapFragment.getMap() != null) {
            addCacheOnMap(mapFragment.getMap());
            addRastrLayers(layersForRastr);
        }
    }

    public void addRastrLayers(String rastrLayers) {
        synchronized (overlays) {
            List<String> overlaysToDelete = new ArrayList<String>();
            for (String overlayKey : overlays.keySet()) {
                if (!rastrLayers.contains(overlayKey)) {
                    overlaysToDelete.add(overlayKey);

                }
            }
            for (String overlayToDelete : overlaysToDelete) {
                TileOverlay overlay = overlays.get(overlayToDelete);
                overlays.remove(overlayToDelete);
                overlay.remove();
            }

            for (String layer : rastrLayers.split(",")) {
                if ("".equals(layer) || overlays.containsKey(layer)) {
                    continue;
                }
                RastrTileProvider rastrTileProvider = new RastrTileProvider(256, 256, layer);
                MapFragment mapFragment = mMapFragment.get();
                if (mapFragment != null) {
                    TileOverlay tileOverlay = mapFragment.getMap().addTileOverlay(new TileOverlayOptions().tileProvider(rastrTileProvider));
                    overlays.put(layer, tileOverlay);
                }
            }
        }
    }

    private void addCacheOnMap(final GoogleMap map) {
        if (taskCache != null) {
            taskCache.cancel(true);
        }
        taskCache = new CacheTask(map);
        taskCache.execute();
    }


    private class CacheTask extends AsyncTask<Object, Collection<MapObject>, Collection<MapObject>> {

        private WeakReference<GoogleMap> map;

        public CacheTask(GoogleMap map) {
            this.map = new WeakReference<>(map);
        }

        @Override
        protected Collection<MapObject> doInBackground(Object... objects) {
            Collection<MapObject> mapObjects = mapObjectsRepository.getMapObjects(layers, parkPrice, parkPlaces, parkPlacesForInvalid);
            imageManager.setIconToMapObjects(mapObjects);
            publishProgress(mapObjects);
            return null;
        }

        @Override
        protected void onProgressUpdate(Collection<MapObject>... values) {
            if (values[0].size() > 0) {
                Logger.log("updateObjectsOnMap : " + values[0].size());
                MapFragment mapFragment = mMapFragment.get();
                if (mapFragment != null) {
                    mapFragment.hideProgress();
                }
            }
            GoogleMap map = this.map.get();
            if (map != null) updateObjectsOnMap(map, values[0]);
        }
    }

    private void updateObjectsOnMap(GoogleMap map, Collection<MapObject> objects) {
        HashMap<String, MapObject> newHashMap = new HashMap<>(objects.size());
        for (MapObject object : objects) {
            newHashMap.put(object.getPlaceId(), object);
        }

        List<String> keysToRemove = new ArrayList<>();
        for (String key : mapObjects.keySet()) {
            if (!newHashMap.containsKey(key)) {
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            MapObject mapObject = mapObjects.remove(key);
            mapObject.removeFromMap();
            objectsWithVisibility.remove(mapObject);
        }


        for (MapObject object : objects) {
            MapObject prevObject = mapObjects.get(object.getPlaceId());
            if (prevObject == null) {
                addObjectOnMap(map, object);
            } else if (!object.equals(prevObject)) {
                prevObject.removeFromMap();
                addObjectOnMap(map, object);
            }

        }
    }

    private void addObjectOnMap(GoogleMap map, MapObject object) {
        if (!object.isVisible()) {
            return;
        }
        if (showingParking != null && showingParking.getPlaceId().equalsIgnoreCase(object.getPlaceId())) {
            return;
        }

        mapObjects.put(object.getPlaceId(), object);
        if (object.isMarker()) {
            object.setMarker(map.addMarker(object.buildMarker()));
        } else if (object.isDrawing()) {
            MapDrawing mapDrawing = object.buildDrawing();
            if (!object.isDrawingAlwaysShow()) {
                mapDrawing.setVisible(isDrawingShow(mapDrawing.getPoints()));
            }
            if (mapDrawing.getPolygonOptions() != null) {
                mapDrawing.setPolygon(map.addPolygon(mapDrawing.getPolygonOptions()));
            }

            if (mapDrawing.getPolylineOptions() != null) {
                mapDrawing.setPolyline(map.addPolyline(mapDrawing.getPolylineOptions()));
            }

            if (!object.isDrawingAlwaysShow()) {
                objectsWithVisibility.put(object, mapDrawing);
            }
            //маркер в центре полигона для кликабельных
            if (object.isPopup()) {
                MarkerOptions markerOptions = object.buildMarkerForDrawing(mapDrawing);
                object.setMarker(map.addMarker(markerOptions));
                mapDrawing.setCenter(markerOptions.getPosition());
                //если полигон еще не виден - попробуем по центру маркера его отобразить
                if (!mapDrawing.isVisible()) {
                    mapDrawing.setVisible(isDrawingShow(mapDrawing));
                }
            }
        }

    }

    public MapObject getMapObjectByMarker(Marker marker) {
        //marker.snippet == MapObject.placeID
        if (showingParking != null && showingParking.getPlaceId().equals(marker.getSnippet())) {
            return showingParking;
        }
        return mapObjects.get(marker.getSnippet());
    }

    public MapObject getMapObjectByPlaceId(String placeId) {
        return mapObjectsRepository.getMapObjectByPlaceId(placeId);
    }

    public List<MapObject> getNearestObjectsWithName(String name, String resultLimit, Location location) {
        return getNearestObjectsWithName(name, resultLimit, location, layersForList, parkPrice, parkPlaces);
    }

    public List<MapObject> getNearestObjectsWithName(String name, String resultLimit, Location location, String layers, int price, int free) {
        LatLng latLng = location == null ? new LatLng(55.754684, 37.623164) : new LatLng(location.getLatitude(), location.getLongitude());
        try {
            List<MapObject> list = MapObjectsAdapter.getMapObjectsFromAPIForList(latLng, layers, name, resultLimit, price, free);
            Logger.log("getNearestObjectsWithName: Found " + list.size());
            return list;
        } catch (IOException e) {
            // нет инета - пустой лист
            Logger.log("getNearestObjectsWithName: ", e);
            return new ArrayList<>(0);
        } catch (JSONException e) {
            // чтото с апи
            Logger.log("getNearestObjectsWithName: ", e);
            return new ArrayList<>(0);
        }
    }

    public void updateObjects(final Collection<MapObject> objects) {
        updateMap();
    }

    public void updateZoneUsage() {
        updateMap();
    }

    public Handler getHandler() {
        return handler;
    }

    public boolean isEmptyCache() {
        return false;
    }

    public void updateVisiblePolygons(float zoom, LatLngBounds bounds) {
        this.zoom = zoom;
        this.bounds = bounds;

        for (MapDrawing mapDrawing : objectsWithVisibility.values()) {
            mapDrawing.setVisible(isDrawingShow(mapDrawing));
        }
    }

    private boolean isDrawingShow(MapDrawing mapDrawing) {
        if (zoom < zoomVisible) {
            return false;
        }

        // если маркер полигона (центр) входит в область
//        Object oMarker = polygon.getData();
//        if (oMarker != null && oMarker instanceof Marker) {
//            if (bounds.contains(((Marker) oMarker).getPosition())) return true;
//        }
        LatLng center = mapDrawing.getCenter();
        if (center != null) {
            if (bounds.contains(center)) {
                return true;
            }
        }

        // если хоть одна точка входит в область
        return isDrawingShow(mapDrawing.getPoints());
    }

    private boolean isDrawingShow(List<LatLng> points) {
        if (zoom < zoomVisible) {
            return false;
        }

        // если хоть одна точка входит в область
        for (LatLng p : points) {
            if (bounds.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private static class RastrTileProvider extends UrlTileProvider {
        // Web Mercator n/w corner of the map.
        private static final double[] TILE_ORIGIN = {-20037508.34789244, 20037508.34789244};
        // array indexes for that data
        private static final int ORIG_X = 0;
        private static final int ORIG_Y = 1; // "

        // Size of square world map in meters, using WebMerc projection.
        private static final double MAP_SIZE = 20037508.34789244 * 2;

        // array indexes for array to hold bounding boxes.
        protected static final int MINX = 0;
        protected static final int MAXX = 1;
        protected static final int MINY = 2;
        protected static final int MAXY = 3;

        private static final DecimalFormat DECIMAL_FORMAT;

        static {
            DECIMAL_FORMAT = (DecimalFormat) NumberFormat.getInstance(Locale.US);
            DECIMAL_FORMAT.setGroupingUsed(false);
            DecimalFormatSymbols symbols = DECIMAL_FORMAT.getDecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
        }

        private String layer;

        public RastrTileProvider(int width, int height, String layer) {
            super(width, height);
            this.layer = layer;
        }

        @Override
        public URL getTileUrl(int x, int y, int zoom) {
            try {
                double[] bbox = getBoundingBox(x, y, zoom);
                String url = String.format(Utils.MAP_TILES, layer, DECIMAL_FORMAT.format(bbox[MINX]), DECIMAL_FORMAT.format(bbox[MINY]), DECIMAL_FORMAT.format(bbox[MAXX]), DECIMAL_FORMAT.format(bbox[MAXY]));
                return new URL(url);
            } catch (Exception e) {
                Logger.log(e.getMessage());
                Logger.log(e.getMessage(), e);
            }
            return null;
        }

        protected double[] getBoundingBox(int x, int y, int zoom) {
            double tileSize = MAP_SIZE / Math.pow(2, zoom);
            double minx = TILE_ORIGIN[ORIG_X] + x * tileSize;
            double maxx = TILE_ORIGIN[ORIG_X] + (x + 1) * tileSize;
            double miny = TILE_ORIGIN[ORIG_Y] - (y + 1) * tileSize;
            double maxy = TILE_ORIGIN[ORIG_Y] - y * tileSize;

            double[] bbox = new double[4];
            bbox[MINX] = minx;
            bbox[MINY] = miny;
            bbox[MAXX] = maxx;
            bbox[MAXY] = maxy;

            return bbox;
        }
    }

    public void updateShowingParking(MapObject showingParking) {
        this.showingParking = showingParking;
        imageManager.setIconToMapObject(showingParking);
    }
}