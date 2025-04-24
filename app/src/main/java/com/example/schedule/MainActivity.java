package com.example.schedule;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.work.WorkManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> filePickerLauncher;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Android 13+ Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleFile);

        Button uploadButton = findViewById(R.id.uploadButton);
        Button skipButton = findViewById(R.id.btn_skip);
        Button voiceButton = findViewById(R.id.btnVoiceCommand);

        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        boolean hasUploaded = prefs.getBoolean("hasUploadedJson", false);

        if (hasUploaded) {
            skipButton.setVisibility(View.VISIBLE);
        } else {
            skipButton.setVisibility(View.GONE);
        }

        skipButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ScheduleListActivity.class));
        });

        uploadButton.setOnClickListener(v -> filePickerLauncher.launch("application/json"));

        voiceButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VoiceScheduleActivity.class);
            startActivity(intent);
        });
    }

    private void handleFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String jsonFromFile = reader.lines().collect(Collectors.joining());

            SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
            prefs.edit()
                    .putString("schedules", jsonFromFile)
                    .putBoolean("hasUploadedJson", true)  // ✅ Mark JSON upload successful
                    .apply();

            WorkManager.getInstance(this).cancelAllWork();
            ScheduleManager.parseAndSchedule(jsonFromFile, this);

            startActivity(new Intent(this, ScheduleListActivity.class));
            Toast.makeText(this, "✅ New schedule applied", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Failed to load file", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Notification permission denied!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh Skip button visibility after returning from other activities
        Button skipButton = findViewById(R.id.btn_skip);
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        boolean hasUploaded = prefs.getBoolean("hasUploadedJson", false);

        if (hasUploaded) {
            skipButton.setVisibility(View.VISIBLE);
        } else {
            skipButton.setVisibility(View.GONE);
        }

//        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
//        startActivity(intent);

        //SpeechDiagnosticsHelper.runDiagnostics(this);

    }

}
