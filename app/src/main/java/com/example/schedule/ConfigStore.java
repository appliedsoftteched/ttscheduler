package com.example.schedule;

import android.content.Context;
import com.google.gson.Gson;
import java.util.List;

public class ConfigStore {
    private static final String PREFS = "config";
    public static void saveLanguage(Context context, String language) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("language", language).apply();
    }
    public static String getLanguage(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("language", "english");
    }
    public static void saveScheduleList(Context context, List<ScheduleItem> list) {
        String json = new Gson().toJson(list);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("schedules", json).apply();
    }
}