package com.example.schedule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
            String json = prefs.getString("schedules", null);
            if (json != null) {
                ScheduleManager.parseAndSchedule(json, context);
                Log.d("BootReceiver", "✅ Rescheduled tasks after reboot");
            }
        }
    }
}
