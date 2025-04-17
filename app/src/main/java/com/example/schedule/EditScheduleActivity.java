package com.example.schedule;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class EditScheduleActivity extends AppCompatActivity {

    private EditText messageEdit, timeEdit, repeatEdit, languageEdit;
    private TextView idText, daysText;
    private String taskId;
    private ScheduleItem item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_schedule);

        idText = findViewById(R.id.editId);
        messageEdit = findViewById(R.id.editMessage);
        timeEdit = findViewById(R.id.editTime);
        daysText = findViewById(R.id.editDays);
        repeatEdit = findViewById(R.id.editRepeat);
        languageEdit = findViewById(R.id.editLanguage);
        Button saveBtn = findViewById(R.id.btnSave);

        taskId = getIntent().getStringExtra("id");

        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        String scheduleJson = prefs.getString("schedules", "[]");
        Type listType = new TypeToken<List<ScheduleItem>>() {}.getType();
        List<ScheduleItem> schedules = new Gson().fromJson(scheduleJson, listType);

        // Find the item
        for (ScheduleItem si : schedules) {
            if (si.id != null && si.id.equals(taskId)) {
                item = si;
                idText.setText("ID: " + si.id);
                messageEdit.setText(si.message);
                timeEdit.setText(si.time);
                daysText.setText(si.days != null ? TextUtils.join(",", si.days) : "");
                repeatEdit.setText(si.repeat != null ? si.repeat : "");
                languageEdit.setText(si.language != null ? si.language : "");
                break;
            }
        }

        saveBtn.setOnClickListener(v -> {
            // Update item
            item.message = messageEdit.getText().toString();
            item.time = timeEdit.getText().toString();
            item.repeat = repeatEdit.getText().toString();
            item.language = languageEdit.getText().toString();
            item.days = Arrays.asList(daysText.getText().toString().split("\\s*,\\s*"));

            // ✅ Save updated list
            String updatedSchedulesJson = new Gson().toJson(schedules);
            prefs.edit().putString("schedules", updatedSchedulesJson).apply();

            // ✅ Wrap it back in ScheduleConfig
            ScheduleConfig config = new ScheduleConfig();
            config.language = item.language != null ? item.language : ConfigStore.getLanguage(this);
            config.schedules = schedules;

            String updatedFullJson = new Gson().toJson(config);
            prefs.edit().putString("full_config", updatedFullJson).apply();

            // ✅ Cancel all work and reschedule
            WorkManager.getInstance(this).cancelAllWork();
            ScheduleManager.parseAndSchedule(updatedFullJson, this);

            Toast.makeText(this, "✅ Task updated and rescheduled", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
    }

}
