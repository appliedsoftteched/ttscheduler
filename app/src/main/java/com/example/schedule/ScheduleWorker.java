package com.example.schedule;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import android.util.Log;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.util.Log;

public class ScheduleWorker extends Worker {

    private TextToSpeech tts;

    public ScheduleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String message = getInputData().getString("message");
        String language = getInputData().getString("language");
        String repeat = getInputData().getString("repeat");

        Log.i("ScheduleWorker", "🚀 doWork triggered for: " + message + " | language = " + language);

        sendNotification(message);

        if (message == null || message.trim().isEmpty()) {
            Log.e("ScheduleWorker", "❌ Message is empty or null — skipping TTS");
            return Result.success();
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            int originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            // 🔊 Max volume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);

            final TextToSpeech[] tts = new TextToSpeech[1];

            tts[0] = new TextToSpeech(getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int result = "hindi".equalsIgnoreCase(language)
                            ? tts[0].setLanguage(new Locale("hi", "IN"))
                            : tts[0].setLanguage(Locale.ENGLISH);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("ScheduleWorker", "❌ TTS language not supported: " + language);
                        Toast.makeText(getApplicationContext(), "TTS language not supported", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String utteranceId = "TTS_" + System.currentTimeMillis();

                    tts[0].setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            Log.d("TTS", "🗣️ Started: " + utteranceId);
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            Log.d("TTS", "✅ Done: " + utteranceId);
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                            tts[0].shutdown();
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.e("TTS", "❌ Error: " + utteranceId);
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                            tts[0].shutdown();
                        }
                    });

                    Log.d("ScheduleWorker", "🔊 Speaking: " + message);
                    tts[0].speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId);

                    Toast.makeText(getApplicationContext(), "✅ TTS triggered: " + message, Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("ScheduleWorker", "❌ TTS initialization failed");
                    Toast.makeText(getApplicationContext(), "TTS init failed", Toast.LENGTH_SHORT).show();
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                }
            });
        });

        // 🔁 Repeat scheduling
        if (repeat != null) {
            ScheduleItem repeatItem = new ScheduleItem();
            repeatItem.message = message;
            repeatItem.language = language;
            repeatItem.enabled = true;
            repeatItem.repeat = repeat;

            long delayMillis = "daily".equalsIgnoreCase(repeat)
                    ? TimeUnit.DAYS.toMillis(1)
                    : "weekly".equalsIgnoreCase(repeat)
                    ? TimeUnit.DAYS.toMillis(7)
                    : 0;

            if (delayMillis > 0) {
                Log.d("ScheduleWorker", "🔁 Re-scheduling repeat task in " + (delayMillis / 60000) + " min");
                ScheduleWorker.scheduleItem(getApplicationContext(), repeatItem, delayMillis);
            }
        }

        return Result.success();
    }

//    @NonNull
//    @Override
//    public Result doWork() {
//        String message = getInputData().getString("message");
//        String language = getInputData().getString("language");
//        String repeat = getInputData().getString("repeat");
//
//        Log.i("ScheduleWorker", "🚀 doWork triggered for: " + message + " | language = " + language);
//
//        sendNotification(message); // Your existing notification method
//
//        // Safeguard for empty message
//        if (message == null || message.trim().isEmpty()) {
//            Log.e("ScheduleWorker", "❌ Message is empty or null — skipping TTS");
//            return Result.success();
//        }
//
//        // Use array to allow mutation inside lambda
//        final TextToSpeech[] tts = new TextToSpeech[1];
//
//        new Handler(Looper.getMainLooper()).post(() -> {
//            tts[0] = new TextToSpeech(getApplicationContext(), status -> {
//                if (status == TextToSpeech.SUCCESS) {
//                    int result;
//                    if ("hindi".equalsIgnoreCase(language)) {
//                        result = tts[0].setLanguage(new Locale("hi", "IN"));
//                    } else {
//                        result = tts[0].setLanguage(Locale.ENGLISH);
//                    }
//
//                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                        Log.e("ScheduleWorker", "❌ TTS language not supported for: " + language);
//                        Toast.makeText(getApplicationContext(), "TTS language not supported", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    // Attach TTS listener for logs
//                    tts[0].setOnUtteranceProgressListener(new UtteranceProgressListener() {
//                        @Override
//                        public void onStart(String utteranceId) {
//                            Log.d("TTS", "🔊 TTS started: " + utteranceId);
//                        }
//
//                        @Override
//                        public void onDone(String utteranceId) {
//                            Log.d("TTS", "✅ TTS done: " + utteranceId);
//                        }
//
//                        @Override
//                        public void onError(String utteranceId) {
//                            Log.e("TTS", "❌ TTS error: " + utteranceId);
//                        }
//                    });
//
//                    Log.d("ScheduleWorker", "✅ Speaking: " + message);
//                    tts[0].speak(message, TextToSpeech.QUEUE_FLUSH, null, "SCHEDULE_TTS_UTTERANCE");
//
//                    Toast.makeText(getApplicationContext(), "✅ TTS triggered: " + message, Toast.LENGTH_LONG).show();
//                } else {
//                    Log.e("ScheduleWorker", "❌ TTS initialization failed");
//                    Toast.makeText(getApplicationContext(), "TTS initialization failed", Toast.LENGTH_SHORT).show();
//                }
//            });
//        });
//
//        // Self-rescheduling logic
//        if (repeat != null) {
//            ScheduleItem repeatItem = new ScheduleItem();
//            repeatItem.message = message;
//            repeatItem.language = language;
//            repeatItem.enabled = true;
//            repeatItem.repeat = repeat;
//
//            long delayMillis = 0;
//            if ("daily".equalsIgnoreCase(repeat)) {
//                delayMillis = TimeUnit.DAYS.toMillis(1);
//            } else if ("weekly".equalsIgnoreCase(repeat)) {
//                delayMillis = TimeUnit.DAYS.toMillis(7);
//            }
//
//            if (delayMillis > 0) {
//                Log.d("ScheduleWorker", "🔁 Re-scheduling repeat task in " + (delayMillis / 60000) + " minutes");
//                ScheduleWorker.scheduleItem(getApplicationContext(), repeatItem, delayMillis);
//            }
//        }
//
//        return Result.success();
//    }

    private void speakMessage(String message, String language) {
        tts = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale locale;

                if (language != null && language.equalsIgnoreCase("hindi")) {
                    locale = new Locale("hi", "IN");
                } else {
                    locale = Locale.ENGLISH;
                }

                int result = tts.setLanguage(locale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported: " + locale);
                    tts.setLanguage(Locale.ENGLISH);
                }

                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "tts_id");
            }
        });
    }

//    private void speakMessage(String message, String language) {
//        tts = new TextToSpeech(getApplicationContext(), status -> {
//            if (status == TextToSpeech.SUCCESS) {
//                Locale locale = language != null && language.equalsIgnoreCase("hindi")
//                        ? new Locale("hi", "IN") : Locale.ENGLISH;
//                tts.setLanguage(locale);
//                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "tts_id");
//            }
//        });
//    }

    private void sendNotification(String message) {
        String channelId = "scheduler_channel";
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Scheduler Alerts", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), AcknowledgeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setContentTitle("Reminder")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .addAction(android.R.drawable.ic_media_pause, "Acknowledge", pendingIntent)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public void onStopped() {
        super.onStopped();
        if (tts != null) {
            tts.shutdown(); // ✅ Clean up
        }
    }

    // ✅ This is what was missing in your ScheduleManager
//    public static void scheduleItem(Context context, ScheduleItem item) {
//        Data data = new Data.Builder()
//                .putString("message", item.message)
//                .putString("language", "english") // update if dynamic language is used
//                .build();
//
//        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ScheduleWorker.class)
//                .setInputData(data)
//                .build();
//
//        WorkManager.getInstance(context).enqueue(workRequest);
//    }
//

    public static void scheduleItem(Context context, ScheduleItem item) {
        try {
            Log.d("ScheduleWorker", "In scheduleItem scheduling");

            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date scheduledTime = sdf.parse(item.time);

            Calendar now = Calendar.getInstance();
            Calendar target = Calendar.getInstance();
            target.setTime(scheduledTime);
            target.set(Calendar.YEAR, now.get(Calendar.YEAR));
            target.set(Calendar.MONTH, now.get(Calendar.MONTH));
            target.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            long delayMillis = target.getTimeInMillis() - now.getTimeInMillis();

            if (delayMillis < 0) {
                Log.d("ScheduleWorker", "Skipping past schedule: " + item.message);
                return;
            }

            String today = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(now.getTime());
            boolean isToday = item.days == null || item.days.stream().anyMatch(d -> d.equalsIgnoreCase(today));
            if (!isToday) return;

            Data data = new Data.Builder()
                    .putString("message", item.message)
                    .putString("language", item.language != null ? item.language : ConfigStore.getLanguage(context))
                    .putString("repeat", item.repeat != null ? item.repeat : "none")
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ScheduleWorker.class)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build();

            WorkManager.getInstance(context).enqueue(workRequest);

            Log.d("ScheduleWorker", "✅ Scheduled '" + item.message + "' in " + (delayMillis / 60000) + " min");
        } catch (Exception e) {
            Log.e("ScheduleWorker", "Error scheduling task", e);
        }
    }
    // Overloaded method to schedule using fixed delayMillis (used for self-rescheduling)
    public static void scheduleItem(Context context, ScheduleItem item, long delayMillis) {
        try {
            Data data = new Data.Builder()
                    .putString("message", item.message)
                    .putString("language", item.language != null ? item.language : ConfigStore.getLanguage(context))
                    .putString("repeat", item.repeat != null ? item.repeat : "none")
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ScheduleWorker.class)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build();

            WorkManager.getInstance(context).enqueue(workRequest);

            Log.d("ScheduleWorker", "🔁 Self-rescheduled '" + item.message + "' in " + (delayMillis / 60000) + " min");
        } catch (Exception e) {
            Log.e("ScheduleWorker", "❌ Error in self-rescheduling task", e);
        }
    }

    public static void scheduleItem1(Context context, ScheduleItem item) {
        try {
            Log.d("ScheduleWorker","In scheduleItem scheduling");
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            Date scheduledTime = sdf.parse(item.time);

            Calendar now = Calendar.getInstance();
            Calendar target = Calendar.getInstance();
            target.setTime(scheduledTime);
            target.set(Calendar.YEAR, now.get(Calendar.YEAR));
            target.set(Calendar.MONTH, now.get(Calendar.MONTH));
            target.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // Calculate delay
            long delayMillis = target.getTimeInMillis() - now.getTimeInMillis();

            // Skip past events (optional)
            if (delayMillis < 0) {
                Log.d("ScheduleWorker", "Skipping past schedule: " + item.message);
                return;
            }

            // Check if today is a valid day
            String today = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(now.getTime());
            boolean isToday = item.days.stream().anyMatch(d -> d.equalsIgnoreCase(today));
            if (!isToday) return;

            // Prepare data
            Data data = new Data.Builder()
                    .putString("message", item.message)
                    .putString("language", ConfigStore.getLanguage(context))
                    .build();

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ScheduleWorker.class)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build();

            WorkManager.getInstance(context).enqueue(workRequest);

            Log.d("ScheduleWorker", "Scheduled '" + item.message + "' in " + (delayMillis / 60000) + " min");

            Log.d("ScheduleWorker", "Message: " + item.message);
            Log.d("ScheduleWorker", "Now: " + now.getTime());
            Log.d("ScheduleWorker", "Scheduled: " + target.getTime());
            Log.d("ScheduleWorker", "Delay (ms): " + delayMillis);
            Log.d("ScheduleWorker", "Day Match: " + isToday);

        } catch (Exception e) {
            Log.e("ScheduleWorker", "Error scheduling task", e);
        }
    }

}
