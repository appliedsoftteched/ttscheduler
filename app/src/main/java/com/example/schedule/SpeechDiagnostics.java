package com.example.schedule;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

public class SpeechDiagnostics {

    private static final String TAG = "SpeechDiagnostics";
    private final Activity activity;
    private final SpeechRecognizer speechRecognizer;
    private final Intent recognizerIntent;

    public SpeechDiagnostics(Activity activity) {
        this.activity = activity;
        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);

        this.recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        this.recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        this.recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        setupListener();
    }

    public void testSpeech() {
        Log.d(TAG, "🎤 Starting Speech Diagnostic Test...");
        Toast.makeText(activity, "🎤 Testing SpeechRecognizer...", Toast.LENGTH_SHORT).show();
        speechRecognizer.startListening(recognizerIntent);
    }

    private void setupListener() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "✅ onReadyForSpeech: Recognizer is ready.");
            }

            @Override public void onBeginningOfSpeech() {
                Log.d(TAG, "✅ onBeginningOfSpeech: Detected speech input.");
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                Log.d(TAG, "✅ onEndOfSpeech: Speech ended.");
            }

            @Override public void onError(int error) {
                String errorMsg = getErrorText(error);
                Log.e(TAG, "❌ onError: Code " + error + " - " + errorMsg);
                Toast.makeText(activity, "❌ Speech Error: " + errorMsg, Toast.LENGTH_LONG).show();
            }

            @Override public void onResults(Bundle results) {
                Log.d(TAG, "✅ onResults: Results received.");
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No recognition result matched";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "RecognitionService busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input";
            default: return "Unknown error (" + errorCode + ")";
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
