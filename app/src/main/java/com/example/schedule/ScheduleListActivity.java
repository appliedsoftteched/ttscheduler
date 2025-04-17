package com.example.schedule;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ScheduleListActivity extends AppCompatActivity {
    private static final String TAG = "ScheduleListActivity";
    private ActivityResultLauncher<Intent> editLauncher;

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_schedule_list);
//        Log.d(TAG,"Starting ScheduleListActivity");
//        ListView listView = findViewById(R.id.scheduleList);
//        String json = getSharedPreferences("config", MODE_PRIVATE).getString("schedules", "[]");
//        Log.d(TAG,"Starting ScheduleListActivity got JSON as "+json);
//        try {
//            Type listType = new TypeToken<List<ScheduleItem>>() {}.getType();
//            List<ScheduleItem> list = new Gson().fromJson(json, listType);
//
//            if (list == null || list.isEmpty()) {
//                Log.e("ScheduleListActivity", "Parsed list is empty or null");
//            }
//
//            ArrayAdapter<String> adapter = new ArrayAdapter<>(
//                    this,
//                    android.R.layout.simple_list_item_1,
//                    list.stream().map(i -> i.time + " - " + i.message).toArray(String[]::new)
//            );
//            listView.setAdapter(adapter);
//
//        } catch (Exception e) {
//            Log.e("ScheduleListActivity", "JSON parse or adapter error", e);
//        }
//    }
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_schedule_list);

    editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    recreate(); // ✅ Refresh UI after edit
                }
            });

    Log.d("ScheduleListActivity", "Starting ScheduleListActivity");

    String json = getSharedPreferences("config", MODE_PRIVATE).getString("schedules", "[]");
    Log.d("ScheduleListActivity", "Got JSON: " + json);

    try {
        Type listType = new TypeToken<List<ScheduleItem>>() {}.getType();
        List<ScheduleItem> list = new Gson().fromJson(json, listType);

        if (list == null || list.isEmpty()) {
            Log.e("ScheduleListActivity", "❌ Parsed list is empty or null");
            return;
        }

        TableLayout tableLayout = findViewById(R.id.scheduleTable);

        for (ScheduleItem item : list) {
            TableRow row = new TableRow(this);

            // Vertical layout for time and buttons
            LinearLayout timeLayout = new LinearLayout(this);
            timeLayout.setOrientation(LinearLayout.VERTICAL);
            timeLayout.setPadding(8, 8, 8, 8);

            TextView timeView = new TextView(this);
            timeView.setText(item.time);
            timeView.setTextColor(Color.parseColor("#37474F"));
            timeView.setPadding(8, 8, 8, 4);

            // Edit Button
            Button editButton = new Button(this);
            editButton.setText("Edit");
            editButton.setTextSize(12f);
            editButton.setTextColor(Color.WHITE);
            editButton.setBackgroundResource(R.drawable.button_shadow);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(150, 80);
            btnParams.setMargins(0, 8, 0, 8);
            editButton.setLayoutParams(btnParams);

            editButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditScheduleActivity.class);
                intent.putExtra("id", item.id);
                editLauncher.launch(intent);
            });

            // Delete Button
            Button deleteButton = new Button(this);
            deleteButton.setText("Del");
            deleteButton.setTextSize(12f);
            deleteButton.setTextColor(Color.WHITE);
            deleteButton.setBackgroundResource(R.drawable.button_shadow);
            deleteButton.setLayoutParams(btnParams);

            deleteButton.setOnClickListener(v -> {
                Context context = ScheduleListActivity.this;

                // Filter the item by ID
                List<ScheduleItem> updatedList = new ArrayList<>();
                for (ScheduleItem i : list) {
                    if (!i.id.equals(item.id)) {
                        updatedList.add(i);
                    }
                }

                // Save updated list
                String updatedJson = new Gson().toJson(updatedList);
                context.getSharedPreferences("config", MODE_PRIVATE)
                        .edit()
                        .putString("schedules", updatedJson)
                        .apply();

                // Re-schedule everything
                WorkManager.getInstance(context).cancelAllWork();
                ScheduleManager.parseAndSchedule(updatedJson, context);

                recreate(); // ✅ Refresh UI
            });


            // Assemble column
            timeLayout.addView(timeView);
            timeLayout.addView(editButton);
            timeLayout.addView(deleteButton);

            // Days Column
            TextView daysView = new TextView(this);
            daysView.setText(item.days != null ? TextUtils.join("\n", item.days) : "-");
            daysView.setPadding(8, 8, 8, 8);
            daysView.setTextColor(Color.parseColor("#37474F"));

            // Message Column
            TextView messageView = new TextView(this);
            messageView.setText(item.message);
            messageView.setPadding(8, 8, 8, 8);
            messageView.setTextColor(Color.parseColor("#37474F"));

            // Assemble table row
            row.addView(timeLayout);
            row.addView(daysView);
            row.addView(messageView);

            tableLayout.addView(row);

            // Divider
            View divider = new View(this);
            TableLayout.LayoutParams params = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT, 2);
            divider.setLayoutParams(params);
            divider.setBackgroundColor(Color.parseColor("#B0BEC5"));
            tableLayout.addView(divider);

            Log.d("ScheduleListActivity", "✔️ Added row with ID: " + item.id);
        }

    } catch (Exception e) {
        Log.e("ScheduleListActivity", "❌ JSON parse or table setup error", e);
    }
}



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // ✅ Refresh screen after edit
            recreate();
        }
    }

}
