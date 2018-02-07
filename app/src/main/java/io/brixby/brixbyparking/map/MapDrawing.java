package io.brixby.parking.map;

import android.graphics.Color;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONException;
import com.androidmapsextensions.*;

import java.util.ArrayList;
import java.util.List;

import io.brixby.parking.utils.Logger;

import static io.brixby.parking.utils.Logger.log;



public class MapDrawing {

    public static final String COLOR_FREE = "#17B219";
    public static final String COLOR_MEDIUM = "#FADE4B";
    public static final String COLOR_BUSY = "#F95855";
    public static final String DEFAULT_LINECOLOR = "#0055FF";
    public static final String LATLNG_SEPARATOR = ",";

    private PolygonOptions polygonOptions;
    private PolylineOptions polylineOptions;
    private Polygon polygon;
    private Polyline polyline;

    private boolean visible = true;
    private LatLng center;

    private MapDrawing() {
    }

    public static MapDrawing buildDrawing(String coords, boolean hasCounter, Integer countspaces, Integer freespaces, String linecolor, String layercolor) {
        MapDrawing mapDrawing = new MapDrawing();

        String points = coords;
        if (coords.contains("[")) {
            try {
                JSONArray jsonArray = new JSONArray(coords);
                points = jsonArray.getString(1);
            } catch (JSONException e) {
                e.printStackTrace();
                Logger.log(e.getMessage());
                points = "60,40|61,41";
            }
        }
        List<LatLng> latLngs = new ArrayList<LatLng>();
        for (String l : points.split("\\|")) {
            String[] cl = l.split(LATLNG_SEPARATOR);
            latLngs.add(new LatLng(Double.valueOf(cl[0]), Double.valueOf(cl[1])));
        }

        if (latLngs.size() < 2) {
            return mapDrawing;
        }

        //если первая координата = последней - делаем полигон
        //иначе линию
        if (latLngs.get(0).equals(latLngs.get(latLngs.size() - 1))) {
            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.strokeWidth(5);
            try {
                if (hasCounter && countspaces != null && freespaces != null) {
                    int procent = (int) (((double) freespaces) / countspaces * 100);
                    String colorS;
                    if (procent <= 30)
                        colorS = COLOR_BUSY;
                    else if (procent <= 85)
                        colorS = COLOR_MEDIUM;
                    else
                        colorS = COLOR_FREE;

                    int color = Color.parseColor(colorS);
                    polygonOptions.fillColor(Color.argb(102, Color.red(color), Color.green(color), Color.blue(color)));
                    polygonOptions.strokeColor(color);
                } else {
                    polygonOptions.strokeColor(Color.parseColor(linecolor != null ? linecolor : DEFAULT_LINECOLOR));
                    int colorLayer = Color.parseColor(layercolor != null ? layercolor : DEFAULT_LINECOLOR);
                    polygonOptions.fillColor(Color.argb(102, Color.red(colorLayer), Color.green(colorLayer), Color.blue(colorLayer)));

//                else if (linecolor == null || linecolor.equalsIgnoreCase(DEFAULT_LINECOLOR)) {
//                    int color = Color.parseColor(DEFAULT_LINECOLOR);
//                    polygonOptions.fillColor(Color.argb(102, Color.red(color), Color.green(color), Color.blue(color)));
//                }
                }
            } catch (RuntimeException e) {
                // цвет не запарсился
                e.printStackTrace();
                Logger.log(e.toString());
            }
            polygonOptions.addAll(latLngs);
            mapDrawing.polygonOptions = polygonOptions;
        } else {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.width(8);
            try {
                if (hasCounter && countspaces != null && freespaces != null) {
                    int procent = (int) (((double) freespaces) / countspaces * 100);
                    String colorS;
                    if (procent <= 30)
                        colorS = COLOR_BUSY;
                    else if (procent <= 85)
                        colorS = COLOR_MEDIUM;
                    else
                        colorS = COLOR_FREE;

                    int color = Color.parseColor(colorS);
                    polylineOptions.color(color);
                } else {
                    polylineOptions.color(Color.parseColor(linecolor != null ? linecolor : DEFAULT_LINECOLOR));
//                }
                }
            } catch (RuntimeException e) {
                // цвет не запарсился
                e.printStackTrace();
                Logger.log(e.toString());
            }
            polylineOptions.addAll(latLngs);
            mapDrawing.polylineOptions = polylineOptions;
        }
        return mapDrawing;
    }

    public void setZIndex(float zIndex) {
        if (polygonOptions != null) polygonOptions.zIndex(zIndex);
        if (polylineOptions != null) polylineOptions.zIndex(zIndex);
    }

    public List<LatLng> getPoints() {
        List<LatLng> points = null;
        if (polygonOptions != null) {
            points = polygonOptions.getPoints();
        } else if (polylineOptions != null) {
            points = polylineOptions.getPoints();
        } else {
            points = new ArrayList<LatLng>();
        }
        return points;
    }

    public PolygonOptions getPolygonOptions() {
        return polygonOptions;
    }

    public PolylineOptions getPolylineOptions() {
        return polylineOptions;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }

    public void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }

    public void setVisible(boolean visible) {
        if (polygon != null) polygon.setVisible(visible);
        if (polyline != null) polyline.setVisible(visible);
        this.visible = visible;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void removeFromMap() {
        if (polygon != null) polygon.remove();
        if (polyline != null) polyline.remove();
    }

    public void setCenter(LatLng position) {
        this.center = position;
    }

    public LatLng getCenter() {
        return this.center;
    }
}
