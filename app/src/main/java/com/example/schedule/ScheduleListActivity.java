package com.example.schedule;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ScheduleListActivity extends android.app.Activity {
    private static final String TAG = "ScheduleListActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadScheduleList();
    }

    private void loadScheduleList() {
        TableLayout tableLayout = findViewById(R.id.scheduleTable);
        tableLayout.removeAllViews(); // Clear old views

        // ✅ Add header row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#ECEFF1"));

        addHeaderText(headerRow, "Actions", "🛠️");
        addHeaderText(headerRow, "Time", "⏱");
        addHeaderText(headerRow, "Days", "📅");
        addHeaderText(headerRow, "Message", "💬");

        tableLayout.addView(headerRow);

        SharedPreferences prefs = getSharedPreferences("config", Context.MODE_PRIVATE);
        String json = prefs.getString("schedules", null);

        if (json == null || json.trim().isEmpty()) {
            Log.w(TAG, "⚠️ No schedules found in config");
            return;
        }

        try {
            Gson gson = new Gson();
            ScheduleConfig config;

            // 👇 Handle both raw list and wrapped config object
            if (json.trim().startsWith("{")) {
                config = gson.fromJson(json, ScheduleConfig.class);
            } else {
                List<ScheduleItem> list = gson.fromJson(json, new TypeToken<List<ScheduleItem>>() {}.getType());
                config = new ScheduleConfig();
                config.schedules = list;
            }

            if (config.schedules == null || config.schedules.isEmpty()) {
                Log.w(TAG, "⚠️ No tasks in 'schedules'");
                return;
            }

            for (ScheduleItem item : config.schedules) {
                TableRow row = new TableRow(this);
                row.setPadding(8, 8, 8, 8);

                // Time
                LinearLayout timeLayout = new LinearLayout(this);
                timeLayout.setOrientation(LinearLayout.VERTICAL);

                TextView timeView = new TextView(this);
                timeView.setText(item.time);
                timeView.setTextColor(Color.parseColor("#37474F"));
                timeView.setPadding(8, 8, 8, 4);
                timeLayout.addView(timeView);

                // Days
                TextView daysView = new TextView(this);
                daysView.setText(item.days != null ? TextUtils.join("\n", item.days) : "-");
                daysView.setPadding(8, 8, 8, 8);
                daysView.setTextColor(Color.parseColor("#37474F"));

                // Message with wrap
                TextView messageView = new TextView(this);
                messageView.setText(item.message);
                messageView.setPadding(8, 8, 8, 8);
                messageView.setTextColor(Color.parseColor("#37474F"));
                messageView.setMaxWidth(400); // 🧩 Adjust as needed
                messageView.setSingleLine(false);

                // Buttons - Vertical
                LinearLayout buttonLayout = new LinearLayout(this);
                buttonLayout.setOrientation(LinearLayout.VERTICAL);
                buttonLayout.setPadding(8, 8, 8, 8);

                Button btnEdit = new Button(this);
                btnEdit.setText("Edit");
                btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(this, EditScheduleActivity.class);
                    intent.putExtra("item_id", item.id);
                    startActivity(intent);
                });
                buttonLayout.addView(btnEdit);

                Button btnDelete = new Button(this);
                btnDelete.setText("Delete");
                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Task")
                            .setMessage("Are you sure you want to delete this task?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                ScheduleManager.deleteById(this, item.id);
                                loadScheduleList(); // Refresh UI
                            })
                            .setNegativeButton("No", null)
                            .show();
                });
                buttonLayout.addView(btnDelete);

                row.addView(buttonLayout);
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
            }

        } catch (Exception e) {
            Log.e("ScheduleListActivity", "❌ Failed to parse schedule config", e);
        }
    }

    private void addHeaderText(TableRow headerRow, String label, String iconUnicode) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(12, 12, 12, 12);

        // Icon
        TextView iconView = new TextView(this);
        iconView.setText(iconUnicode);  // example: ⏱, 🗓️ etc.
        iconView.setTextSize(12);
        iconView.setTextColor(Color.BLACK);
        iconView.setPadding(0, 0, 8, 0); // space between icon and label

        // Label
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(16);
        labelView.setTextColor(Color.BLACK);

        container.addView(iconView);
        container.addView(labelView);
        headerRow.addView(container);
    }

}
