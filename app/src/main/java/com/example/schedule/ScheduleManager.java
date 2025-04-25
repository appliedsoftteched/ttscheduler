package com.example.schedule;

import android.content.Context;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.SharedPreferences;
import android.util.Log;
import android.content.Context;
import android.util.Log;

import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ScheduleManager {

    private static final String TAG = "ScheduleManager";

//    public static void parseAndSchedule(String json, Context context) {
//        try {
//            Gson gson = new Gson();
//            ScheduleConfig config = gson.fromJson(json, ScheduleConfig.class);
//
//            // Store language and schedules in SharedPreferences
//            ConfigStore.saveLanguage(context, config.language);
//            ConfigStore.saveScheduleList(context, config.schedules);
//
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//            Calendar now = Calendar.getInstance();
//            String today = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(now.getTime());
//
//            for (ScheduleItem item : config.schedules) {
//                if (!item.enabled) {
//                    Log.d(TAG, "⏭️ Skipping (disabled): " + item.message);
//                    continue;
//                }
//
//                // Check if today is valid for the task
//                boolean isToday = item.days == null || item.days.stream().anyMatch(d -> d.equalsIgnoreCase(today));
//                if (!isToday) {
//                    Log.d(TAG, "⏭️ Skipping (not today's day): " + item.message);
//                    continue;
//                }
//
//                try {
//                    Date scheduledTime = sdf.parse(item.time);
//                    Calendar scheduled = Calendar.getInstance();
//                    scheduled.setTime(scheduledTime);
//                    scheduled.set(Calendar.YEAR, now.get(Calendar.YEAR));
//                    scheduled.set(Calendar.MONTH, now.get(Calendar.MONTH));
//                    scheduled.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
//
//                    long delay = scheduled.getTimeInMillis() - now.getTimeInMillis();
//
//                    if (delay > 0) {
//                        Log.d(TAG, "✅ Scheduling '" + item.message + "' after " + (delay / 60000) + " mins");
//                        ScheduleWorker.scheduleItem(context, item);
//                    } else {
//                        Log.d(TAG, "⏭️ Skipping (time already passed): " + item.message);
//                    }
//
//                } catch (Exception e) {
//                    Log.e(TAG, "❌ Time parse error for " + item.message, e);
//                }
//            }
//
//            Log.d(TAG, "✅ Finished scheduling all valid tasks");
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ JSON parse failed", e);
//        }
//    }

//    public static void parseAndSchedule(String json, Context context) {
//        try {
//            Gson gson = new Gson();
//            Log.d("ScheduleManager", "Json is "+json);
//            ScheduleConfig config = gson.fromJson(json, ScheduleConfig.class);
//
//            // ✅ Convert mealTimes to ScheduleItems (repeat = daily)
//            if (config.mealTimes != null) {
//                for (Map.Entry<String, MealTimeItem> entry : config.mealTimes.entrySet()) {
//                    MealTimeItem meal = entry.getValue();
//                    ScheduleItem item = new ScheduleItem();
//                    item.id = "meal-" + entry.getKey(); // ✅ Assign unique ID like "meal-lunch"
//
//                    item.message = meal.message;
//                    item.time = meal.time;
//                    item.repeat = "daily";
//                    item.enabled = true;
//                    // ✅ Add all 7 days
//                    item.days = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
//
//                    config.schedules.add(item);
//                }
//            }
//
//            // ✅ Store in SharedPreferences
//            ConfigStore.saveLanguage(context, config.language);
//            ConfigStore.saveScheduleList(context, config.schedules);
//
//            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
//            Calendar now = Calendar.getInstance();
//            String today = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(now.getTime());
//
//            for (ScheduleItem item : config.schedules) {
//                if (!item.enabled) {
//                    Log.d(TAG, "⏭️ Skipping (disabled): " + item.message);
//                    continue;
//                }
//
//                boolean isToday = item.days == null || item.days.stream().anyMatch(d -> d.equalsIgnoreCase(today));
//                if (!isToday) {
//                    Log.d(TAG, "⏭️ Skipping (not today's day): " + item.message);
//                    continue;
//                }
//
//                try {
//                    Date scheduledTime = sdf.parse(item.time);
//                    Calendar scheduled = Calendar.getInstance();
//                    scheduled.setTime(scheduledTime);
//                    scheduled.set(Calendar.YEAR, now.get(Calendar.YEAR));
//                    scheduled.set(Calendar.MONTH, now.get(Calendar.MONTH));
//                    scheduled.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
//
//                    long delay = scheduled.getTimeInMillis() - now.getTimeInMillis();
//
//                    if (delay > 0) {
//                        Log.d(TAG, "✅ Scheduling '" + item.message + "' after " + (delay / 60000) + " mins");
//                        ScheduleWorker.scheduleItem(context, item);
//                    } else {
//                        Log.d(TAG, "⏭️ Skipping (time already passed): " + item.message);
//                    }
//
//                } catch (Exception e) {
//                    Log.e(TAG, "❌ Time parse error for " + item.message, e);
//                }
//            }
//
//            Log.d(TAG, "✅ Finished scheduling all valid tasks");
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ JSON parse failed", e);
//        }
//    }

    public static void parseAndSchedule(String json, Context context) {
        try {
            Gson gson = new Gson();
            Log.d("ScheduleManager", "Json is "+json);
            ScheduleConfig config = gson.fromJson(json, ScheduleConfig.class);

            // ✅ Convert mealTimes to ScheduleItems (repeat = daily)
            if (config.mealTimes != null) {
                for (Map.Entry<String, MealTimeItem> entry : config.mealTimes.entrySet()) {
                    MealTimeItem meal = entry.getValue();
                    ScheduleItem item = new ScheduleItem();
                    item.id = "meal-" + entry.getKey(); // ✅ Assign unique ID like "meal-lunch"

                    item.message = meal.message;
                    item.time = meal.time;
                    item.repeat = "daily";
                    item.enabled = true;
                    // ✅ Add all 7 days
                    item.days = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

                    config.schedules.add(item);
                }
            }

            // ✅ Store in SharedPreferences
            ConfigStore.saveLanguage(context, config.language);
            ConfigStore.saveScheduleList(context, config.schedules);

            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Calendar now = Calendar.getInstance();
            String today = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(now.getTime());

            for (ScheduleItem item : config.schedules) {
                if (!item.enabled) {
                    Log.d(TAG, "⏭️ Skipping (disabled): " + item.message);
                    continue;
                }

                boolean isToday = item.days == null || item.days.stream().anyMatch(d -> d.equalsIgnoreCase(today));
                if (!isToday) {
                    Log.d(TAG, "⏭️ Skipping (not today's day): " + item.message);
                    continue;
                }

                try {
                    Date scheduledTime = sdf.parse(item.time);
                    Calendar scheduled = Calendar.getInstance();
                    scheduled.setTime(scheduledTime);
                    scheduled.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    scheduled.set(Calendar.MONTH, now.get(Calendar.MONTH));
                    scheduled.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

                    long delay = scheduled.getTimeInMillis() - now.getTimeInMillis();

                    if (delay > 0) {
                        Log.d(TAG, "✅ Scheduling '" + item.message + "' after " + (delay / 60000) + " mins");
                        ScheduleWorker.scheduleItem(context, item);
                    } else {
                        Log.d(TAG, "⏭️ Skipping (time already passed): " + item.message);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "❌ Time parse error for " + item.message, e);
                }
            }

            Log.d(TAG, "✅ Finished scheduling all valid tasks");

        } catch (Exception e) {
            Log.e(TAG, "❌ JSON parse failed", e);
        }
    }
        // ... other methods like parseAndSchedule

    public static void deleteById(Context context, String itemId) {
        // 🔹 Load from config (could be either list or object)
        Log.d(TAG,"Deleting by id "+itemId);
        SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        String json = prefs.getString("schedules", null);
        Log.d(TAG,"Deleting by json "+json);
        Gson gson = new Gson();

        try {
            List<ScheduleItem> updatedList = new ArrayList<>();

            if (json != null && json.trim().startsWith("[")) {
                // ✅ Legacy list format
                Type listType = new TypeToken<List<ScheduleItem>>() {
                }.getType();
                List<ScheduleItem> list = gson.fromJson(json, listType);
                for (ScheduleItem item : list) {
                    if (!item.id.equals(itemId)) {
                        updatedList.add(item);
                    }
                }
                prefs.edit().putString("schedules", gson.toJson(updatedList)).apply();

            } else {
                // ✅ Config object format
                ScheduleConfig config = gson.fromJson(json, ScheduleConfig.class);
                if (config != null && config.schedules != null) {
                    for (ScheduleItem item : config.schedules) {
                        if (!item.id.equals(itemId)) {
                            updatedList.add(item);
                        }
                    }
                    config.schedules = updatedList;
                    prefs.edit().putString("schedules", gson.toJson(config)).apply();
                }
            }

        } catch (Exception e) {
            Log.e("ScheduleManager", "❌ Failed to remove from config", e);
        }

        // 🔹 Remove from scheduled (work_map)
        SharedPreferences scheduledPrefs = context.getSharedPreferences("scheduled", Context.MODE_PRIVATE);
        String mapJson = scheduledPrefs.getString("work_map", "{}");

        try {
            Type mapType = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> workMap = gson.fromJson(mapJson, mapType);
            if (workMap != null) {
                workMap.remove(itemId);
                scheduledPrefs.edit().putString("work_map", gson.toJson(workMap)).apply();
            }
        } catch (Exception e) {
            Log.e("ScheduleManager", "❌ Failed to remove from scheduled map", e);
        }

        // 🔹 Cancel WorkManager jobs
        WorkManager.getInstance(context).cancelAllWorkByTag("task-" + itemId);
        Log.d("ScheduleManager", "🗑️ Deleted task: " + itemId);

    }
}
