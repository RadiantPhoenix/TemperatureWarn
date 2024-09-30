package com.example.temperaturewarn;

import static com.example.temperaturewarn.Constants.DEFAULT_THRESHOLD;
import static com.example.temperaturewarn.Constants.PREFS_NAME;
import static com.example.temperaturewarn.Constants.PREF_TEMPERATURE_THRESHOLD;
import static com.example.temperaturewarn.Constants.PREF_UPDATE_FREQUENCY;
import static com.example.temperaturewarn.Constants.TAG;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BatteryWidgetProvider extends AppWidgetProvider {


    private MediaPlayer mediaPlayer;

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleWidgetUpdates(Context context) {
        int updateFrequency = getUpdateFrequency(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BatteryWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (updateFrequency == 0) {
            updateFrequency = 10000;
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (updateFrequency * 1000L), pendingIntent);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        scheduleWidgetUpdates(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
        scheduleWidgetUpdates(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        float batteryTemperature = getBatteryTemperature(context);
        int temperatureThreshold = getTemperatureThreshold(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setTextViewText(R.id.text_battery_temperature,
                "Battery Temp: " + (batteryTemperature / 10.0f) + "°C");
        views.setTextViewText(R.id.text_battery_threshold,
                "Temp threshold: " + (temperatureThreshold / 10.0f) + "°C");

        // Play sound alert if the temperature exceeds the threshold
        if (batteryTemperature > temperatureThreshold) {
            playSoundAlert(context);
            views.setTextColor(R.id.text_battery_status, Color.RED);
            views.setTextViewText(R.id.text_battery_status,
                    "Temp dangerous!");
        } else {
            views.setTextColor(R.id.text_battery_status, Color.GREEN);
            views.setTextViewText(R.id.text_battery_status,
                    "Temp normal");
        }

        int updateFrequency = getUpdateFrequency(context);
        String nextUpdate = " in " + updateFrequency + " s";
        if (updateFrequency == 0) {
            nextUpdate = " disabled";
        }
        views.setTextViewText(R.id.text_last_updated,
                "Last updated: " + getCurrentTime() + " Next update" + nextUpdate);

        Intent intent = new Intent(context, ConfigurationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.button_set_threshold, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d("Widget", "Button Pressed");
        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), BatteryWidgetProvider.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private float getBatteryTemperature(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            Log.d(TAG, "Battery Temperature: " + (temperature / 10.0f) + "°C");
            return temperature;
        } else {
            Log.e(TAG, "Failed to retrieve battery status");
            return 0;
        }
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void playSoundAlert(Context context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.alert_sound); // Add your sound file in res/raw
        }

        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Toast.makeText(context, "Temperature Alert! Battery is overheating!", Toast.LENGTH_SHORT).show();
        }
    }

    private int getTemperatureThreshold(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_TEMPERATURE_THRESHOLD, DEFAULT_THRESHOLD);
    }

    private int getUpdateFrequency(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_UPDATE_FREQUENCY, 5);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
