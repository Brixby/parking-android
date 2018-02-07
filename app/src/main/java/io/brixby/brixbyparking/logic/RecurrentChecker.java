package io.brixby.parking.logic;

import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.inject.Inject;


public class RecurrentChecker {

    @Inject
    RecurrentChecker() {}

    public boolean isRecurrent(@Nullable JsonObject parking) {
        if (parking == null) return false;

        JsonArray paymentSourcesList = parking.getAsJsonArray("paymentSourcesList");
        if (paymentSourcesList == null) return false;

        for (int i = 0; i < paymentSourcesList.size(); i++) {
            String f = paymentSourcesList.get(i).getAsJsonObject().get("facilitator").getAsString();
            if (f.contains("BankCard")) {
                return true;
            }
        }
        return false;
    }

}
