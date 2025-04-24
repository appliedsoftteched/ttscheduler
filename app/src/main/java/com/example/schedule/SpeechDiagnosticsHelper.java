package com.example.schedule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechDiagnosticsHelper {

    private static final String TAG = "SpeechDiagnostics";

    public static void runDiagnostics(Activity activity) {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "❌ Speech recognition not available on this device", Toast.LENGTH_LONG).show();
            Log.e(TAG, "❌ Speech recognition not available on this device");
            return;
        }

        SpeechRecognizer recognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "✅ onReadyForSpeech triggered");
                Toast.makeText(activity, "✅ onReadyForSpeech triggered", Toast.LENGTH_SHORT).show();
            }

            @Override public void onBeginningOfSpeech() {
                Log.d(TAG, "✅ onBeginningOfSpeech");
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                Log.d(TAG, "✅ onEndOfSpeech");
            }

            @Override public void onError(int error) {
                Log.e(TAG, "❌ Error Listening code is " + error);
                Toast.makeText(activity, "❌ Error Listening code is " + error, Toast.LENGTH_SHORT).show();
            }

            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Log.d(TAG, "✅ onResults: " + matches);
                Toast.makeText(activity, "✅ Results: " + matches, Toast.LENGTH_LONG).show();
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        recognizer.startListening(intent);
        Toast.makeText(activity, "🎤 Listening started for diagnostics...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "🎤 Listening started for diagnostics...");
    }
}
