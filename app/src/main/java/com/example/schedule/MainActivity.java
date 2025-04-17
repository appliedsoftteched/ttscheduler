package com.example.schedule;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.work.WorkManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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

        // ✅ Android 13+ notification permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleFile);

        Button uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(v -> filePickerLauncher.launch("application/json"));

        Button skipButton = findViewById(R.id.btn_skip);
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        String existingJson = prefs.getString("schedules", null);

        if (existingJson != null && !existingJson.equals("[]") && !existingJson.isEmpty()) {
            skipButton.setVisibility(View.VISIBLE);
            skipButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ScheduleListActivity.class);
                startActivity(intent);
            });
        } else {
            skipButton.setVisibility(View.GONE);
        }
    }


    //    private void handleFile(Uri uri) {
//        try {
//            Log.d(TAG,"Uploading");
//            InputStream inputStream = getContentResolver().openInputStream(uri);
//            String json = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
//            File file = new File(getFilesDir(), "schedule.json");
//            Log.d(TAG,"Uploading schedule json");
//            try (FileOutputStream fos = new FileOutputStream(file)) {
//                fos.write(json.getBytes());
//            }
//            ScheduleManager.parseAndSchedule(json, getApplicationContext());
//
//            Log.d(TAG,"Uploading Done and starting SchedListActivity");
//            startActivity(new Intent(this, ScheduleListActivity.class));
//            Log.d(TAG,"Uploading Done and Started SchedListActivity");
//        } catch (Exception e) {
//            Log.d(TAG,"Exception While uploaind");
//            Toast.makeText(this, "Failed to process file", Toast.LENGTH_LONG).show();
//        }
//    }
private void handleFile(Uri uri) {
    try {
        // Read JSON content from selected file
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }
        String jsonFromFile = jsonBuilder.toString();

        // ✅ Save new JSON
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        prefs.edit().putString("schedules", jsonFromFile).apply();

        // ✅ Cancel all old scheduled tasks
        WorkManager.getInstance(this).cancelAllWork();

        // ✅ Parse and reschedule new tasks
        ScheduleManager.parseAndSchedule(jsonFromFile, this);

        // ✅ Navigate to schedule view
        startActivity(new Intent(this, ScheduleListActivity.class));

        // ✅ User feedback
        Toast.makeText(this, "✅ New schedule applied", Toast.LENGTH_SHORT).show();

    } catch (Exception e) {
        e.printStackTrace();
        Toast.makeText(this, "❌ Failed to load file", Toast.LENGTH_LONG).show();
    }
}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Notification permission denied!", Toast.LENGTH_SHORT).show();
        }
    }
}
