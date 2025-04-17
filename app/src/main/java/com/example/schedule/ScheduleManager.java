package com.example.schedule;

import android.content.Context;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.util.Log;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

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

    public static void parseAndSchedule(String json, Context context) {
        try {
            Gson gson = new Gson();
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


}
