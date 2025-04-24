package com.example.schedule;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class VoiceScheduleActivity extends Activity {

    private static final String TAG = "VoiceScheduleActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private TextView tvConversation;
    private Button btnStartVoice;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private Handler handler = new Handler();
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private final StringBuilder conversationLog = new StringBuilder();
    private final ScheduleItem scheduleItem = new ScheduleItem();
    private int currentPromptIndex = 0;

    private final String[] promptOrder = {"message", "time", "days", "repeat", "language"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_schedule);

        tvConversation = findViewById(R.id.tvConversation);
        btnStartVoice = findViewById(R.id.btnStartVoice);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new VoiceListener());

        btnStartVoice.setOnClickListener(v -> startConversation());
    }

    private void startConversation() {
        scheduleItem.id = UUID.randomUUID().toString();
        conversationLog.setLength(0);
        currentPromptIndex = 0;
        promptNextMissing();
    }

    private void promptNextMissing() {
        if (allFieldsPresent()) {
            finalizeSchedule();
            return;
        }

        String currentPrompt = promptOrder[currentPromptIndex];
        String question = getPromptQuestion(currentPrompt);
        speak(question);
        handler.postDelayed(() -> speechRecognizer.startListening(recognizerIntent), 1200);
    }

    private boolean allFieldsPresent() {
        return scheduleItem.message != null &&
                scheduleItem.time != null &&
                scheduleItem.days != null &&
                scheduleItem.repeat != null &&
                scheduleItem.language != null;
    }

    private String getPromptQuestion(String field) {
        switch (field) {
            case "message": return "What should I remind you about?";
            case "time": return "At what time?";
            case "days": return "Which days should I repeat this?";
            case "repeat": return "Should it repeat daily or weekly?";
            case "language": return "Should I speak in English or Hindi?";
            default: return "Please respond.";
        }
    }

    private void speak(String line) {
        conversationLog.append("🤖 ").append(line).append("\n");
        tvConversation.setText(conversationLog.toString());
    }

    private void processSpokenText(String spoken) {
        String field = promptOrder[currentPromptIndex];
        boolean valid = false;

        switch (field) {
            case "message":
                if (spoken.length() > 1) {
                    scheduleItem.message = spoken;
                    valid = true;
                }
                break;
            case "time":
                try {
                    String cleaned = spoken.replaceAll("[.]", "")
                            .replaceAll("a\\s*m", "AM").replaceAll("p\\s*m", "PM")
                            .replaceAll("am", "AM").replaceAll("pm", "PM");
                    Date parsed = new SimpleDateFormat("h:mm a", Locale.ENGLISH).parse(cleaned);
                    scheduleItem.time = new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(parsed);
                    valid = true;
                } catch (Exception e) {
                    Log.e(TAG, "❌ Time parsing failed: " + spoken, e);
                }
                break;
            case "days":
                List<String> days = new ArrayList<>();
                for (String token : spoken.split("[\\s,]+")) {
                    String day = token.trim().toLowerCase();
                    if (Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday").contains(day)) {
                        days.add(Character.toUpperCase(day.charAt(0)) + day.substring(1));
                    }
                }
                if (!days.isEmpty()) {
                    scheduleItem.days = days;
                    valid = true;
                }
                break;
            case "repeat":
                if (spoken.toLowerCase().contains("daily")) {
                    scheduleItem.repeat = "daily";
                    valid = true;
                } else if (spoken.toLowerCase().contains("weekly")) {
                    scheduleItem.repeat = "weekly";
                    valid = true;
                }
                break;
            case "language":
                if (spoken.toLowerCase().contains("english")) {
                    scheduleItem.language = "english";
                    valid = true;
                } else if (spoken.toLowerCase().contains("hindi")) {
                    scheduleItem.language = "hindi";
                    valid = true;
                }
                break;
        }

        if (valid) {
            conversationLog.append("🗣️ You: ").append(spoken).append("\n");
            currentPromptIndex++;
        } else {
            conversationLog.append("⚠️ Didn't understand that. Please try again.\n");
        }
        tvConversation.setText(conversationLog.toString());
        promptNextMissing();
    }

    private void finalizeSchedule() {
        scheduleItem.enabled = true;

        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        String json = prefs.getString("schedules", null);

        Gson gson = new Gson();
        ScheduleConfig config = new ScheduleConfig();
        config.language = "english"; // Default
        config.schedules = new ArrayList<>();

        try {
            if (json != null && json.trim().startsWith("{")) {
                config = gson.fromJson(json, ScheduleConfig.class);
                if (config.schedules == null) config.schedules = new ArrayList<>();
            } else if (json != null && json.trim().startsWith("[")) {
                // Handle legacy format (array only)
                List<ScheduleItem> legacyList = gson.fromJson(json, new TypeToken<List<ScheduleItem>>() {}.getType());
                config.schedules.addAll(legacyList);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse existing config", e);
        }

        // Add new item
        config.schedules.add(scheduleItem);

        String updatedJson = gson.toJson(config);
        prefs.edit().putString("schedules", updatedJson).apply();

        // ✅ Schedule it using manager
        ScheduleManager.parseAndSchedule(updatedJson, getApplicationContext());

        conversationLog.append("✅ Task scheduled successfully!\n");
        tvConversation.setText(conversationLog.toString());

        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, ScheduleListActivity.class));
            finish();
        }, 1000);
    }

    private class VoiceListener implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "🎤 Ready");
        }

        @Override public void onBeginningOfSpeech() {
            Log.d(TAG, "🎤 Started");
            handler.removeCallbacksAndMessages(null);
        }

        @Override public void onError(int error) {
            Log.e(TAG, "❌ Error Listening code is " + error);
            conversationLog.append("⚠️ Didn't catch that. Please repeat.\n");
            tvConversation.setText(conversationLog.toString());
            handler.postDelayed(() -> promptNextMissing(), 1200);
        }

        @Override public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                processSpokenText(matches.get(0));
            } else {
                conversationLog.append("⚠️ Didn't hear anything.\n");
                tvConversation.setText(conversationLog.toString());
                promptNextMissing();
            }
        }

        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}
        @Override public void onEndOfSpeech() {}
        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!permissionToRecordAccepted) {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
