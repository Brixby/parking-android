package io.brixby.parking.logic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.App;
import io.brixby.parking.R;


@Singleton
public class LanguageManager {

    public static final String APP_LANG = "APP_LANG";
    public static final String PREF_SETTINGS = "SETTINGS";
    public static final String EN = "en";
    public static final String RU = "ru";

    @Inject
    LanguageManager() {}

    public void init(Context context) {
        String savedLang = getLanguage(context);
        if (!TextUtils.isEmpty(savedLang)) {
            initLanguage(savedLang, context);
        } else if (Locale.getDefault().toString().contains(RU)) {
            initLanguage(RU, context);
        } else {
            initLanguage(EN, context);
        }
    }

    private String getLanguage(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE);
        return settings.getString(APP_LANG, null);
    }

    // returns true if language was changed
    private boolean initLanguage(String language, Context context) {
        context = context.getApplicationContext();
        //Change locale only if current locale not already set
        if (!Locale.getDefault().toString().contains(language)) {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);
            Resources res = context.getResources();
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            conf.locale = locale;
            res.updateConfiguration(conf, dm);
            return true;
        }
        return false;
    }

    @SuppressLint("CommitPrefEdits")
    private void setLanguage(String language, Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE);
        if (initLanguage(language, context)) {
            preferences.edit().putString(APP_LANG, language).commit();
            App.get(context).restart();
        }
    }


    public void setRu(Context context) {
        setLanguage(RU, context);
    }

    public void setEn(Context context) {
        setLanguage(EN, context);
    }

    public boolean isRu(Context context) {
        return context.getString(R.string.language).equalsIgnoreCase("ru");
    }
}
