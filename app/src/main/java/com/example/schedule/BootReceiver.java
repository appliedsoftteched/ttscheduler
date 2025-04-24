package com.example.schedule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "📦 Reboot detected. Attempting to reschedule tasks...");

            SharedPreferences prefs = context.getSharedPreferences("scheduled", Context.MODE_PRIVATE);
            String workMapJson = prefs.getString("work_map", null);

            if (workMapJson != null) {
                try {
                    JSONObject map = new JSONObject(workMapJson);
                    Iterator<String> keys = map.keys();

                    Gson gson = new Gson();
                    Type type = new TypeToken<ScheduleItem>() {}.getType();

                    while (keys.hasNext()) {
                        String key = keys.next();
                        String itemJson = map.getString(key);
                        ScheduleItem item = gson.fromJson(itemJson, type);
                        ScheduleWorker.scheduleItem(context, item);
                        Log.d("BootReceiver", "🔁 Rescheduled from map: " + item.message);
                    }

                    Log.d("BootReceiver", "✅ All tasks rescheduled from work_map");

                } catch (Exception e) {
                    Log.e("BootReceiver", "❌ Failed to reschedule from work_map", e);
                }
            } else {
                Log.w("BootReceiver", "⚠️ No scheduled tasks found in work_map");
            }
        }
    }
}
