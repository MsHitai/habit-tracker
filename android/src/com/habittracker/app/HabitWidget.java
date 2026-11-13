package com.habittracker.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// Home-screen widget: one chip per habit (up to 8); tapping a chip toggles it for today.
// Reads and writes the same JSON state the WebView UI keeps in SharedPreferences.
public class HabitWidget extends AppWidgetProvider {
    static final String ACTION_TOGGLE = "com.habittracker.app.TOGGLE";
    static final String ACTION_REFRESH = "com.habittracker.app.REFRESH";
    static final String EXTRA_ID = "habit";

    static final int[] ROWS = { R.id.row0, R.id.row1, R.id.row2, R.id.row3 };
    static final int[] SLOTS = { R.id.slot0, R.id.slot1, R.id.slot2, R.id.slot3, R.id.slot4, R.id.slot5, R.id.slot6, R.id.slot7 };
    static final int[] DOTS = { R.id.dot0, R.id.dot1, R.id.dot2, R.id.dot3, R.id.dot4, R.id.dot5, R.id.dot6, R.id.dot7 };
    static final int[] NAMES = { R.id.name0, R.id.name1, R.id.name2, R.id.name3, R.id.name4, R.id.name5, R.id.name6, R.id.name7 };

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        render(ctx, mgr, ids);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            toggle(ctx, intent.getStringExtra(EXTRA_ID));
            refresh(ctx);
        } else if (ACTION_REFRESH.equals(intent.getAction())) {
            refresh(ctx);
        }
    }

    static void refresh(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, HabitWidget.class));
        if (ids.length > 0) render(ctx, mgr, ids);
    }

    static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    static JSONObject loadState(Context ctx) {
        try {
            String s = prefs(ctx).getString("state", "");
            if (!s.isEmpty()) return new JSONObject(s);
        } catch (Exception ignored) {
        }
        return null;
    }

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("habits", Context.MODE_PRIVATE);
    }

    static void toggle(Context ctx, String habitId) {
        JSONObject state = loadState(ctx);
        if (state == null || habitId == null) return;
        try {
            JSONObject done = state.optJSONObject("done");
            if (done == null) { done = new JSONObject(); state.put("done", done); }
            String dk = todayKey();
            JSONObject day = done.optJSONObject(dk);
            if (day == null) { day = new JSONObject(); done.put(dk, day); }
            if (day.has(habitId)) day.remove(habitId); else day.put(habitId, 1);
            if (day.length() == 0) done.remove(dk);
            prefs(ctx).edit().putString("state", state.toString()).commit();
        } catch (Exception ignored) {
        }
    }

    static void render(Context ctx, AppWidgetManager mgr, int[] ids) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_habits);
        JSONObject state = loadState(ctx);
        JSONArray habits = state == null ? null : state.optJSONArray("habits");
        int n = habits == null ? 0 : habits.length();
        JSONObject done = state == null ? null : state.optJSONObject("done");
        JSONObject today = done == null ? null : done.optJSONObject(todayKey());

        Intent open = new Intent(ctx, MainActivity.class);
        v.setOnClickPendingIntent(R.id.header, PendingIntent.getActivity(ctx, 0, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        int doneCount = 0;
        float density = ctx.getResources().getDisplayMetrics().density;
        for (int i = 0; i < SLOTS.length; i++) {
            JSONObject h = i < n ? habits.optJSONObject(i) : null;
            if (h == null) { v.setViewVisibility(SLOTS[i], View.INVISIBLE); continue; }
            String id = h.optString("id");
            boolean on = today != null && today.has(id);
            if (on) doneCount++;
            v.setViewVisibility(SLOTS[i], View.VISIBLE);
            v.setTextViewText(NAMES[i], h.optString("name"));
            v.setImageViewBitmap(DOTS[i], dot(parseColor(h.optString("color")), on, density));
            v.setInt(SLOTS[i], "setBackgroundResource", on ? R.drawable.chip_done : R.drawable.chip_bg);

            Intent t = new Intent(ctx, HabitWidget.class).setAction(ACTION_TOGGLE).putExtra(EXTRA_ID, id);
            v.setOnClickPendingIntent(SLOTS[i], PendingIntent.getBroadcast(ctx, i + 1, t,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        }
        // Habits past the eighth still count toward the total.
        for (int i = SLOTS.length; i < n; i++) {
            JSONObject h = habits.optJSONObject(i);
            if (h != null && today != null && today.has(h.optString("id"))) doneCount++;
        }
        for (int r = 0; r < ROWS.length; r++) {
            v.setViewVisibility(ROWS[r], r * 2 < n ? View.VISIBLE : View.GONE);
        }

        String date = new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date());
        v.setTextViewText(R.id.title, n == 0 ? "Tap to add habits" : "Today · " + date);
        v.setTextViewText(R.id.count, n == 0 ? "" : (doneCount == n ? "All done ✓" : doneCount + "/" + n));

        mgr.updateAppWidget(ids, v);
        scheduleMidnight(ctx);
    }

    // Redraw just after midnight so yesterday's ticks don't linger.
    static void scheduleMidnight(Context ctx) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 5);
        Intent i = new Intent(ctx, HabitWidget.class).setAction(ACTION_REFRESH);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0, i,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.set(AlarmManager.RTC, c.getTimeInMillis(), pi);
    }

    static int parseColor(String s) {
        try { return Color.parseColor(s); } catch (Exception e) { return Color.GRAY; }
    }

    // Filled circle with a white check when done, an outlined ring when not — like the in-app buttons.
    static Bitmap dot(int color, boolean on, float density) {
        int size = Math.round(20 * density);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        float stroke = 2 * density, r = size / 2f;
        if (on) {
            c.drawCircle(r, r, r, p);
            Paint ck = new Paint(Paint.ANTI_ALIAS_FLAG);
            ck.setColor(Color.WHITE);
            ck.setStyle(Paint.Style.STROKE);
            ck.setStrokeWidth(2.4f * density);
            ck.setStrokeCap(Paint.Cap.ROUND);
            ck.setStrokeJoin(Paint.Join.ROUND);
            Path path = new Path();
            path.moveTo(size * 0.28f, size * 0.53f);
            path.lineTo(size * 0.44f, size * 0.69f);
            path.lineTo(size * 0.73f, size * 0.34f);
            c.drawPath(path, ck);
        } else {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke);
            c.drawCircle(r, r, r - stroke / 2, p);
        }
        return b;
    }
}
