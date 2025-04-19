package com.example.schedule;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ScheduleForegroundService extends Service {

    private static final String CHANNEL_ID = "SCHEDULE_CHANNEL";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(); // Mandatory on Android 8+
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ScheduleService", "✅ Foreground service started");

        // Show notification to comply with foreground execution policy
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("⏰ Scheduling Reminder")
                .setContentText("Your reminder is being scheduled")
                .setSmallIcon(R.drawable.ic_notification1)

                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // API 34+ requires foregroundServiceType=shortService
        startForeground(1, notification);

        String json = intent.getStringExtra("json");
        if (json != null) {
            ScheduleManager.parseAndSchedule(json, getApplicationContext());
        }

        stopForeground(STOP_FOREGROUND_REMOVE); // Optional: stop foreground
        stopSelf(); // Exit once done
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Reminder Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for scheduling reminders");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
