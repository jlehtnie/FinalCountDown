package com.example.finalcountdown;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView mTextViewCountDown;
    private TextView mTextViewTargetTime;
    private TextView mTextViewTargetTimeName;
    private ImageView mImageViewFlagClass;
    private ImageView mImageViewFlagPapa;
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener;

    private SoundPool soundPool;
    //private int sound1;
    private SparseIntArray sounds;

    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mEndTime;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.settings:
                SettingsDialogFragment dialog = new SettingsDialogFragment();
                dialog.setOnTimeSelectedListener(this::startTimerFromSettings);
                dialog.show(getSupportFragmentManager(), "settings_dialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageViewFlagClass = (ImageView) findViewById(R.id.image_flag_class);
        mImageViewFlagPapa = (ImageView) findViewById(R.id.image_flag_papa);
        loadFlagImages();

        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefChangeListener = (prefs, key) -> {
            if ("pref_class_flag".equals(key) || "pref_prep_flag".equals(key)) {
                loadFlagImages();
            }
        };
        defaultPrefs.registerOnSharedPreferenceChangeListener(mPrefChangeListener);

        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mTextViewTargetTime = findViewById(R.id.text_view_target_time);
        mTextViewTargetTimeName = findViewById(R.id.text_view_target_time_name);
    }

    private void loadSounds() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
        soundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).build();
        sounds = new SparseIntArray();
        sounds.put(300, soundPool.load(this, R.raw.min5, 1));
        sounds.put(240, soundPool.load(this, R.raw.min4, 1));
        sounds.put(60, soundPool.load(this, R.raw.min1, 1));
        sounds.put(50, soundPool.load(this, R.raw.sec50, 1));
        sounds.put(40, soundPool.load(this, R.raw.sec40, 1));
        sounds.put(30, soundPool.load(this, R.raw.sec30, 1));
        sounds.put(20, soundPool.load(this, R.raw.sec20, 1));
        sounds.put(10, soundPool.load(this, R.raw.sec10, 1));
        sounds.put(9, soundPool.load(this, R.raw.sec9, 1));
        sounds.put(8, soundPool.load(this, R.raw.sec8, 1));
        sounds.put(7, soundPool.load(this, R.raw.sec7, 1));
        sounds.put(6, soundPool.load(this, R.raw.sec6, 1));
        sounds.put(5, soundPool.load(this, R.raw.sec5, 1));
        sounds.put(4, soundPool.load(this, R.raw.sec4, 1));
        sounds.put(3, soundPool.load(this, R.raw.sec3, 1));
        sounds.put(2, soundPool.load(this, R.raw.sec2, 1));
        sounds.put(1, soundPool.load(this, R.raw.sec1, 1));
        sounds.put(0, soundPool.load(this, R.raw.start, 1));
    }

    private void setTime(long milliseconds) {
        mEndTime = milliseconds;
        closeKeyboard();
    }

    private void startTimer() {
        long timeLeftInMillis = mEndTime - System.currentTimeMillis();
        mCountDownTimer = new CountDownTimer(timeLeftInMillis, 500) {
            @Override
            public void onTick(long millisLeftUntilFinished) {
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                updateWatchInterface();
            }
        }.start();
        mTimerRunning = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateWatchInterface();
    }

    private void pauseTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        mTimerRunning = false;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateWatchInterface();
    }

    private void playSound(int id) {
        Log.i("TAG", "playing sound " + id);
        soundPool.play(id, 1, 1, 0, 0, 1);
    }

    private void updateCountDownText() {
        long timeLeftInMillis = mEndTime - System.currentTimeMillis();
        int timeLeftInSecs = (int) timeLeftInMillis / 1000;
        if (timeLeftInSecs < -60) {
            pauseTimer();
            return;
        }
        if (timeLeftInSecs < 0) {
            mTextViewCountDown.setTextColor(android.graphics.Color.RED);
        }
        mTextViewCountDown.setText(formatTimeStr(timeLeftInSecs));

        if (null != sounds){
            int id = sounds.get(timeLeftInSecs, -1);
            if (id != -1) {
                playSound(id);
                sounds.delete(timeLeftInSecs); // avoid repeat; todo: is this recoverable
            }
        }
        updateFlags(timeLeftInSecs);
    }

    private void updateTargetTimeLabel(LocalDateTime targetTime) {
        if (mTextViewTargetTime == null || targetTime == null) return;
        String label = String.format(Locale.getDefault(), "%02d:%02d:%02d", targetTime.getHour(), targetTime.getMinute(), targetTime.getSecond());
        mTextViewTargetTime.setText(label);
    }

    private void updateTargetTimeNameLabel() {
        if (mTextViewTargetTimeName == null) return;
        
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedId = defaultPrefs.getString("pref_selected_time", "");
        
        if (selectedId != null && !selectedId.isEmpty()) {
            String json = defaultPrefs.getString("named_times", "[]");
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.ArrayList<NamedTime>>(){}.getType();
            java.util.List<NamedTime> times = gson.fromJson(json, type);
            
            for (NamedTime time : times) {
                if (time.getId().equals(selectedId)) {
                    mTextViewTargetTimeName.setText(time.getName());
                    mTextViewTargetTimeName.setVisibility(View.VISIBLE);
                    return;
                }
            }
        }
        
        // No named time selected or found
        mTextViewTargetTimeName.setVisibility(View.GONE);
    }

    private void startTimerFromSettings() {
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String timeString = SettingsFragment.getSelectedTimeValue(defaultPrefs);

        if (timeString == null) {
            // No time selected, don't start timer
            return;
        }

        int hour;
        int minute;
        int second;
        try {
            String[] timeParts = timeString.split(":");
            if (timeParts.length != 3) {
                Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
                return;
            }
            hour = Integer.parseInt(timeParts[0].trim());
            minute = Integer.parseInt(timeParts[1].trim());
            second = Integer.parseInt(timeParts[2].trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hour < 0 || hour > 23) {
            Toast.makeText(this, "Invalid hour", Toast.LENGTH_SHORT).show();
            return;
        }
        if (minute < 0 || minute > 59) {
            Toast.makeText(this, "Invalid minute", Toast.LENGTH_SHORT).show();
            return;
        }
        if (second < 0 || second > 59) {
            Toast.makeText(this, "Invalid second", Toast.LENGTH_SHORT).show();
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inputTime = now.withHour(hour).withMinute(minute).withSecond(second);
        if (inputTime.isBefore(now)) {
            inputTime = inputTime.plusDays(1);
        }
        long inputMillis = inputTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        mTextViewCountDown.setTextColor(Color.BLACK);
        setTime(inputMillis);
        loadSounds();
        startTimer();
        updateTargetTimeLabel(inputTime);
        updateTargetTimeNameLabel();
    }

    private String formatTimeStr(int timeLeftInSecs) {
        String prefix = timeLeftInSecs < 0 ? "-" : "";
        int hours = java.lang.Math.abs(timeLeftInSecs / 3600);
        int minutes = java.lang.Math.abs(timeLeftInSecs % 3600 / 60);
        int seconds = java.lang.Math.abs(timeLeftInSecs % 60);
        String timeLeftFormatted;
        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
        return timeLeftFormatted;
    }

    private void loadFlagImages() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String classKey = prefs.getString("pref_class_flag", "flag_pennant_one");
        String prepKey = prefs.getString("pref_prep_flag", "flag_papa");
        mImageViewFlagClass.setImageResource(getFlagDrawable(classKey));
        mImageViewFlagPapa.setImageResource(getFlagDrawable(prepKey));
    }

    private int getFlagDrawable(String key) {
        switch (key) {
            // existing prep-flag assets
            case "flag_papa":        return R.drawable.flag_papa;
            case "flag_i":           return R.drawable.flag_i;
            case "flag_z":           return R.drawable.flag_z;
            case "flag_u":           return R.drawable.flag_u;
            case "flag_black":       return R.drawable.flag_black;
            // numeral pennants
            case "flag_pennant_0":   return R.drawable.flag_pennant_0;
            case "flag_pennant_2":   return R.drawable.flag_pennant_2;
            case "flag_pennant_3":   return R.drawable.flag_pennant_3;
            case "flag_pennant_4":   return R.drawable.flag_pennant_4;
            case "flag_pennant_5":   return R.drawable.flag_pennant_5;
            case "flag_pennant_6":   return R.drawable.flag_pennant_6;
            case "flag_pennant_7":   return R.drawable.flag_pennant_7;
            case "flag_pennant_8":   return R.drawable.flag_pennant_8;
            case "flag_pennant_9":   return R.drawable.flag_pennant_9;
            // letter flags A-Z
            case "flag_letter_a":    return R.drawable.flag_letter_a;
            case "flag_letter_b":    return R.drawable.flag_letter_b;
            case "flag_letter_c":    return R.drawable.flag_letter_c;
            case "flag_letter_d":    return R.drawable.flag_letter_d;
            case "flag_letter_e":    return R.drawable.flag_letter_e;
            case "flag_letter_f":    return R.drawable.flag_letter_f;
            case "flag_letter_g":    return R.drawable.flag_letter_g;
            case "flag_letter_h":    return R.drawable.flag_letter_h;
            case "flag_letter_i":    return R.drawable.flag_letter_i;
            case "flag_letter_j":    return R.drawable.flag_letter_j;
            case "flag_letter_k":    return R.drawable.flag_letter_k;
            case "flag_letter_l":    return R.drawable.flag_letter_l;
            case "flag_letter_m":    return R.drawable.flag_letter_m;
            case "flag_letter_n":    return R.drawable.flag_letter_n;
            case "flag_letter_o":    return R.drawable.flag_letter_o;
            case "flag_letter_p":    return R.drawable.flag_letter_p;
            case "flag_letter_q":    return R.drawable.flag_letter_q;
            case "flag_letter_r":    return R.drawable.flag_letter_r;
            case "flag_letter_s":    return R.drawable.flag_letter_s;
            case "flag_letter_t":    return R.drawable.flag_letter_t;
            case "flag_letter_u":    return R.drawable.flag_letter_u;
            case "flag_letter_v":    return R.drawable.flag_letter_v;
            case "flag_letter_w":    return R.drawable.flag_letter_w;
            case "flag_letter_x":    return R.drawable.flag_letter_x;
            case "flag_letter_y":    return R.drawable.flag_letter_y;
            case "flag_letter_z":    return R.drawable.flag_letter_z;
            // substitute flags
            case "flag_sub_1":       return R.drawable.flag_sub_1;
            case "flag_sub_2":       return R.drawable.flag_sub_2;
            case "flag_sub_3":       return R.drawable.flag_sub_3;
            default:                 return R.drawable.flag_pennant_one;
        }
    }

    private void updateFlags(int timeLeftinSecs) {
        if (timeLeftinSecs <= 300 && timeLeftinSecs > 0) {
            mImageViewFlagClass.setVisibility(View.VISIBLE);
        } else {
            mImageViewFlagClass.setVisibility(View.INVISIBLE);
        }
        if (timeLeftinSecs <= 240 && timeLeftinSecs > 60) {
            mImageViewFlagPapa.setVisibility(View.VISIBLE);
        } else {
            mImageViewFlagPapa.setVisibility(View.INVISIBLE);
        }
    }

    private void updateWatchInterface() {

    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        defaultPrefs.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);
        editor.apply();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        mTimerRunning = prefs.getBoolean("timerRunning", false);
        mEndTime = prefs.getLong("endTime", 0);
        
        // If no timer is running, check if we should start one from settings
        if (!mTimerRunning && mEndTime == 0) {
            startTimerFromSettings();
            // If no time was selected, just return without starting timer
            if (mEndTime == 0) {
                return;
            }
        }
        
        if (mEndTime > 0) {
            LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochMilli(mEndTime), ZoneId.systemDefault());
            updateTargetTimeLabel(t);
            updateTargetTimeNameLabel();
        }
        updateCountDownText();
        updateWatchInterface();
        if (mTimerRunning) {
            long timeLeftInMillis = mEndTime - System.currentTimeMillis();
            timeLeftInMillis = mEndTime - System.currentTimeMillis();
            if (timeLeftInMillis < 0) {
                timeLeftInMillis = 0;
                mTimerRunning = false;
                updateCountDownText();
                updateWatchInterface();
            } else {
                startTimer();
            }
        }
    }
}