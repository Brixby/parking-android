package io.brixby.parking.parking;

import android.content.Context;
import android.content.SharedPreferences;

import io.brixby.parking.ui.screens.SettingsFragment;


public class Config {

    private static boolean IS_PROD_URL = true;

    static void init(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Utils.PREFFS_NAME, Context.MODE_PRIVATE);
        IS_PROD_URL = prefs.getBoolean(SettingsFragment.SETTING_URL_CONFIG, false);
    }

    public static boolean isDevUrl() {
        return BuildConfig.DEBUG && !IS_PROD_URL;
    }


}
