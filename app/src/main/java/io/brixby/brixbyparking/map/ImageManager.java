package io.brixby.parking.map;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.Network;
import io.brixby.parking.R;
import io.brixby.parking.Utils;
import io.brixby.parking.model.MapObject;




@Singleton
public class ImageManager {

    //to avoid lint warnings
    private static final int[] ICON_NAMES = new int[]{R.drawable.map_capital, R.drawable.map_eln, R.drawable.map_capital, R.drawable.map_evac, R.drawable.map_parking, R.drawable.map_parking_busy, R.drawable.map_parking_free, R.drawable.map_parking_medium, R.drawable.map_parking_paid, R.drawable.map_parking_paid_busy, R.drawable.map_parking_paid_free, R.drawable.map_parking_paid_medium, R.drawable.map_payment, R.drawable.map_svyaznoy, R.drawable.map_taxi, R.drawable.map_truck, R.drawable.map_mgt, R.drawable.map_infopanel, R.drawable.map_eln, R.drawable.map_qiwi, R.drawable.map_eur, R.drawable.map_rezident};

    public static final String PREFIX = "map_";
    public static final String DRAWABLE = "drawable";
    public static final String DEFAULT_ICON = "parking";

    public static final int PREFERRED_WIDTH = 1080;

    private Map<String, MapBitmap> icons;
    private volatile MapBitmap defaultIcon;
    private BitmapFactory.Options decodeOptions;
    private File cacheDir;

    @Inject
    public ImageManager(Context context) {
        icons = Collections.synchronizedMap(new HashMap<>(ICON_NAMES.length));
        decodeOptions = new BitmapFactory.Options();
        decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        cacheDir = context.getCacheDir();
    }

    public ImageManager initBackground(Context context) {
        Resources resources = context.getResources();
        for (String icon : resources.getStringArray(R.array.map_icons)) {
            int resourceId = resources.getIdentifier(PREFIX + icon, DRAWABLE, context.getPackageName());
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(resourceId);
            icons.put(icon, new MapBitmap(bitmapDescriptor));
        }
        int resourceId = resources.getIdentifier(PREFIX + DEFAULT_ICON, DRAWABLE, context.getPackageName());
        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(resourceId);
        defaultIcon = new MapBitmap(bitmapDescriptor);
        return this;
    }

    public MapBitmap getMapImage(String name) {
        MapBitmap icon = icons.get(name);
        if (icon != null) {
            return icon;
        } else {
            return defaultIcon;
        }
    }


    public void setIconToMapObjects(Collection<MapObject> mapObjects) {
        for (MapObject mapObject : mapObjects) {
            setIconToMapObject(mapObject);
        }
    }

    public void setIconToMapObject(MapObject mapObject) {
        mapObject.setIconBitmap(getMapImage(mapObject.getIconName()));
    }

    public boolean hasCachedImage(String file) {
        String cacheFilePath = cacheDir.getPath() + "/" + file + ".jpg";
        return new File(cacheFilePath).exists();
    }

    public Bitmap loadImageById(String file) {
        String cacheFilePath = cacheDir.getPath() + "/" + file + ".jpg";

        if (new File(cacheFilePath).exists()) {
            return BitmapFactory.decodeFile(cacheFilePath, decodeOptions);
        } else {
            String body = "format=json&partnerID=" + Utils.PARTNER_ID + "&operation=file_get&data=" + file;
            InputStream is = null;
            FileOutputStream outStream = null;
            try {
                is = Network.requestStream(Utils.API_URL, "POST", body);
                Bitmap bitmapBig = BitmapFactory.decodeStream(is, null, decodeOptions);
                Bitmap bitmap;
                if (bitmapBig.getWidth() - PREFERRED_WIDTH > PREFERRED_WIDTH / 2) {
                    double scale = ((double) bitmapBig.getWidth()) / PREFERRED_WIDTH;
                    int w = (int) (bitmapBig.getWidth() / scale), h = (int) (bitmapBig.getHeight() / scale);
                    bitmap = Bitmap.createScaledBitmap(bitmapBig, w, h, true);
                    bitmapBig.recycle();
                } else {
                    bitmap = bitmapBig;
                }
                outStream = new FileOutputStream(cacheFilePath);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
                outStream.flush();
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    File f = new File(cacheFilePath);
                    if (f.exists()) {
                        f.delete();
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
                return null;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (IOException e) {
                    }
                }
            }

        }
    }

    public BitmapFactory.Options getDecodeOptions() {
        return decodeOptions;
    }
}
