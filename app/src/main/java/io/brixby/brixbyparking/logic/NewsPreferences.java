package io.brixby.parking.logic;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.brixby.parking.Utils;


@Singleton
public class NewsPreferences {

    private final SharedPreferences preferences;

    @Inject
    NewsPreferences(Context context) {
        preferences = context.getSharedPreferences(Utils.PREFFS_NAME, Context.MODE_PRIVATE);
    }

    public List<String> getReadNews() {
        String readNews = preferences.getString("readNewsString", "");
        return Arrays.asList(readNews.split(","));
    }

    public void addReadNews(String readNews) {
        String savedReadNews = preferences.getString("readNewsString", null);
        if (savedReadNews == null) {
            savedReadNews = readNews;
        } else {
            savedReadNews = savedReadNews + "," + readNews;
        }
        preferences.edit().putString("readNewsString", savedReadNews).apply();
    }

    public String getNews() {
        return preferences.getString("news", null);
    }

    public void saveNews(String news, String date) {
        preferences.edit().putString("news", news).putString("newsDate", date).apply();
    }

    public String getNewsDate() {
        return preferences.getString("newsDate", "");
    }
}
