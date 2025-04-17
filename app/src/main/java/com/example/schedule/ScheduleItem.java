package com.example.schedule;

import java.util.List;

public class ScheduleItem {
    public String id; // ✅ Unique ID
    public String message;
    public String time;
    public List<String> days;
    public boolean enabled;
    public String repeat = "none";
    public String language; // ✅ Add this line
}
