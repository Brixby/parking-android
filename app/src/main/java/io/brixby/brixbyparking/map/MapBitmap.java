package io.brixby.parking.map;

import com.google.android.gms.maps.model.BitmapDescriptor;



public class MapBitmap {

    private BitmapDescriptor bitmapDescriptor;

    public MapBitmap(BitmapDescriptor bitmapDescriptor) {
        this.bitmapDescriptor = bitmapDescriptor;
    }

    public BitmapDescriptor getBitmapDescriptor() {
        return bitmapDescriptor;
    }
}
