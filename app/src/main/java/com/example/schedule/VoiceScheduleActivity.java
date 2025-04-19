package com.example.schedule;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class VoiceScheduleActivity extends Activity {

    private TextView tvConversation;
    private Button btnStartVoice;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private final ScheduleItem scheduleItem = new ScheduleItem();
    private final String[] prompts = {"message", "time", "days", "repeat", "language"};
    private int currentPromptIndex = 0;
    private final StringBuilder conversationLog = new StringBuilder();
    private Handler timeoutHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_schedule);

        tvConversation = findViewById(R.id.tvConversation);
        btnStartVoice = findViewById(R.id.btnStartVoice);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        btnStartVoice.setOnClickListener(v -> startConversation());
    }

    private void startConversation() {
        conversationLog.setLength(0);
        currentPromptIndex = 0;
        scheduleItem.id = UUID.randomUUID().toString();
        askNextPrompt();
    }

    private void askNextPrompt() {
        if (currentPromptIndex >= prompts.length) {
            saveAndSchedule();
            return;
        }
        String prompt = prompts[currentPromptIndex];
        String question = getPromptQuestion(prompt);
        appendToConversation("🤖 " + question);
        timeoutHandler.postDelayed(this::promptTimeout, 10000);
        listenForSpeech();
    }

    private String getPromptQuestion(String prompt) {
        switch (prompt) {
            case "message": return "What should I remind you about?";
            case "time": return "What time should I remind you?";
            case "days": return "Which days should I repeat this? Say days like Monday, Tuesday.";
            case "repeat": return "Should this be repeated daily or weekly?";
            case "language": return "Should I speak in English or Hindi?";
            default: return "Please respond.";
        }
    }

    private void promptTimeout() {
        appendToConversation("⚠️ I’m still waiting for your answer...");
    }

    private void listenForSpeech() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {
                timeoutHandler.removeCallbacksAndMessages(null);
            }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                appendToConversation("⚠️ Didn't catch that. Please try again.");
                askNextPrompt();
            }

            @Override
            public void onResults(Bundle results) {
                timeoutHandler.removeCallbacksAndMessages(null);
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spoken = matches.get(0);
                    appendToConversation("🗣️ You: " + spoken);
                    if (!handleSpokenInput(spoken)) {
                        appendToConversation("⚠️ That doesn't seem right. Please try again.");
                        return;
                    }
                    currentPromptIndex++;
                    askNextPrompt();
                } else {
                    appendToConversation("⚠️ I didn't hear anything.");
                    askNextPrompt();
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(recognizerIntent);
    }

    private boolean handleSpokenInput(String spoken) {
        String currentPrompt = prompts[currentPromptIndex];

        switch (currentPrompt) {
            case "message":
                if (spoken.trim().length() < 2) return false;
                scheduleItem.message = spoken;
                return true;
            case "time":
                try {
                    String cleanedTime = spoken.trim()
                            .replaceAll("(?i)[.]", "")
                            .replaceAll("(?i)a\\s*m", "AM")
                            .replaceAll("(?i)p\\s*m", "PM")
                            .replaceAll("(?i)am", "AM")
                            .replaceAll("(?i)pm", "PM");

                    SimpleDateFormat spokenFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                    spokenFormat.setLenient(false);
                    Date date = spokenFormat.parse(cleanedTime);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
                    scheduleItem.time = outputFormat.format(date);
                    return true;
                } catch (Exception e) {
                    Log.e("VoiceSchedule", "Time parse failed for: " + spoken);
                    return false;
                }
            case "days":
                String[] tokens = spoken.split("[\\s,]+");
                List<String> parsedDays = new ArrayList<>();
                for (String token : tokens) {
                    String day = token.trim().toLowerCase();
                    if (Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday").contains(day)) {
                        parsedDays.add(day.substring(0, 1).toUpperCase() + day.substring(1));
                    }
                }
                if (parsedDays.isEmpty()) return false;
                scheduleItem.days = parsedDays;
                return true;
            case "repeat":
                if (spoken.toLowerCase().contains("week")) {
                    scheduleItem.repeat = "weekly";
                } else if (spoken.toLowerCase().contains("day")) {
                    scheduleItem.repeat = "daily";
                } else {
                    return false;
                }
                return true;
            case "language":
                if (spoken.toLowerCase().contains("hindi")) {
                    scheduleItem.language = "hindi";
                } else if (spoken.toLowerCase().contains("english")) {
                    scheduleItem.language = "english";
                } else {
                    return false;
                }
                return true;
            default:
                return false;
        }
    }

    private void saveAndSchedule() {
        scheduleItem.enabled = true;

        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        String json = prefs.getString("schedules", null);

        ScheduleConfig config;
        try {
            if (json != null && json.trim().startsWith("[")) {
                Type listType = new TypeToken<List<ScheduleItem>>() {}.getType();
                List<ScheduleItem> legacyList = new Gson().fromJson(json, listType);
                config = new ScheduleConfig();
                config.language = "english";
                config.schedules = legacyList;
            } else {
                config = json != null ? new Gson().fromJson(json, ScheduleConfig.class) : new ScheduleConfig();
            }
        } catch (Exception e) {
            Log.e("VoiceSchedule", "❌ Failed to parse existing JSON, creating new config", e);
            config = new ScheduleConfig();
        }

        if (config.schedules == null) config.schedules = new ArrayList<>();
        config.schedules.add(scheduleItem);

        String updatedJson = new Gson().toJson(config);
        prefs.edit().putString("schedules", updatedJson).apply();

        Intent serviceIntent = new Intent(this, ScheduleForegroundService.class);
        serviceIntent.putExtra("json", updatedJson);
        startService(serviceIntent);

        appendToConversation("✅ Your task has been scheduled successfully!");
        Toast.makeText(getApplicationContext(), "✅ Scheduled and running in background", Toast.LENGTH_SHORT).show();

        // ✅ Slight delay to avoid DeadObjectException
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, ScheduleListActivity.class));
            finish();
        }, 800);
    }

    private void appendToConversation(String line) {
        conversationLog.append(line).append("\n");
        tvConversation.setText(conversationLog.toString());
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }
}
