package com.example.schedule;

import android.content.Context;

import com.google.gson.Gson;

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

public class ScheduleManager {

    private static final String TAG = "ScheduleManager";

    public static void parseAndSchedule(String json, Context context) {
        try {
            Gson gson = new Gson();
            ScheduleConfig config = gson.fromJson(json, ScheduleConfig.class);

            // Store language and schedules in SharedPreferences
            ConfigStore.saveLanguage(context, config.language);
            ConfigStore.saveScheduleList(context, config.schedules);

            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Calendar now = Calendar.getInstance();
            String today = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(now.getTime());

            for (ScheduleItem item : config.schedules) {
                if (!item.enabled) continue;

                // Check if today is in item's days list
                boolean isToday = false;
                for (String day : item.days) {
                    if (day.equalsIgnoreCase(today)) {
                        isToday = true;
                        break;
                    }
                }
                if (!isToday) {
                    Log.d(TAG, "⏭️ Skipping (wrong day): " + item.message);
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
                        Log.d(TAG,"Done scheduling the task");
                    } else {
                        Log.d(TAG, "⏭️ Time already passed for: " + item.message);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "❌ Time parse error for " + item.message, e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ JSON parse failed", e);
        }
    }
}
