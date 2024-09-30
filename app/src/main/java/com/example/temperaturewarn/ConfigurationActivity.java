package com.example.temperaturewarn;

import static com.example.temperaturewarn.Constants.PREFS_NAME;
import static com.example.temperaturewarn.Constants.PREF_TEMPERATURE_THRESHOLD;
import static com.example.temperaturewarn.Constants.PREF_UPDATE_FREQUENCY;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ConfigurationActivity extends AppCompatActivity {

    private Spinner frequencySpinner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("ConfigurationActivity", "onCreate: ConfigurationActivity started");
        setContentView(R.layout.activity_configuration);

        EditText editTextThreshold = findViewById(R.id.edit_threshold);
        editTextThreshold.setTextColor(Color.parseColor("#e1c289"));
        editTextThreshold.setHintTextColor(Color.parseColor("#e1c289"));

        frequencySpinner = findViewById(R.id.frequencySpinner);
        String[] stringArray = getResources().getStringArray(R.array.update_frequency_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, stringArray);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        frequencySpinner.setAdapter(adapter);

        Button buttonSave = findViewById(R.id.button_save);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentThreshold = prefs.getInt(PREF_TEMPERATURE_THRESHOLD, 450); // Default 45.0°C
        int savedFrequency = prefs.getInt(PREF_UPDATE_FREQUENCY, 5);
        editTextThreshold.setText(String.valueOf(currentThreshold / 10.0f));
        setSpinnerSelectionBasedOnSavedFrequency(savedFrequency);

        buttonSave.setOnClickListener(view -> {
            try {
                float thresholdValue = Float.parseFloat(editTextThreshold.getText().toString());
                int thresholdInDeciCelsius = (int) (thresholdValue * 10);
                int selectedFrequency = getSelectedFrequencyFromSpinner();

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(PREF_UPDATE_FREQUENCY, selectedFrequency);
                editor.putInt(PREF_TEMPERATURE_THRESHOLD, thresholdInDeciCelsius);
                editor.apply();

                Toast.makeText(ConfigurationActivity.this, "Parameters saved", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ConfigurationActivity.this, BatteryWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                sendBroadcast(intent);

                finish();
            } catch (NumberFormatException e) {
                Toast.makeText(ConfigurationActivity.this, "Invalid input", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setSpinnerSelectionBasedOnSavedFrequency(int savedFrequency) {
        String[] frequencyOptions = getResources().getStringArray(R.array.update_frequency_options);
        for (int i = 0; i < frequencyOptions.length; i++) {
            if (frequencyOptions[i].contains(String.valueOf(savedFrequency))) {
                frequencySpinner.setSelection(i);
                break;
            }
        }
    }

    private int getSelectedFrequencyFromSpinner() {
        String selectedItem = (String) frequencySpinner.getSelectedItem();
        if (selectedItem.contains("5")) return 5;
        else if (selectedItem.contains("7")) return 7;
        else if (selectedItem.contains("20")) return 20;
        else if (selectedItem.contains("60")) return 60;
        else return 0;
    }
}
