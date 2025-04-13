package com.example.schedule;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import android.util.Log;

public class ScheduleListActivity extends AppCompatActivity {
    private static final String TAG = "ScheduleListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);
        Log.d(TAG,"Starting ScheduleListActivity");
        ListView listView = findViewById(R.id.scheduleList);
        String json = getSharedPreferences("config", MODE_PRIVATE).getString("schedules", "[]");
        Log.d(TAG,"Starting ScheduleListActivity got JSON as "+json);
        try {
            Type listType = new TypeToken<List<ScheduleItem>>() {}.getType();
            List<ScheduleItem> list = new Gson().fromJson(json, listType);

            if (list == null || list.isEmpty()) {
                Log.e("ScheduleListActivity", "Parsed list is empty or null");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    list.stream().map(i -> i.time + " - " + i.message).toArray(String[]::new)
            );
            listView.setAdapter(adapter);

        } catch (Exception e) {
            Log.e("ScheduleListActivity", "JSON parse or adapter error", e);
        }
    }

}
