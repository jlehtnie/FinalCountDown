package com.example.finalcountdown;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private EditText mEditTextHour;
    private EditText mEditTextMinute;
    private EditText mEditTextSecond;
    private TextView mTextViewCountDown;
    private Button mButtonSet;

    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditTextHour = findViewById(R.id.edit_text_hour);
        mEditTextMinute = findViewById(R.id.edit_text_minute);
        mEditTextSecond = findViewById(R.id.edit_text_second);

        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mButtonSet = findViewById(R.id.button_set);

        mButtonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputHour = mEditTextHour.getText().toString();
                String inputMinute = mEditTextMinute.getText().toString();
                String inputSecond = mEditTextSecond.getText().toString();

                if (inputHour.length() == 0 || inputMinute.length() == 0 || inputSecond.length() == 0) {
                    Toast.makeText(MainActivity.this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                int hour = Integer.parseInt(inputHour);
                int minute = Integer.parseInt(inputMinute);
                int second = Integer.parseInt(inputSecond);

                if (hour > 23) {
                    Toast.makeText(MainActivity.this, "Invalid hour", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (minute > 59) {
                    Toast.makeText(MainActivity.this, "Invalid minute", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (second > 59) {
                    Toast.makeText(MainActivity.this, "Invalid minute", Toast.LENGTH_SHORT).show();
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
                startTimer();
            }
        });
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
                updateWatchInterface();
            }
        }.start();
        mTimerRunning = true;
        updateWatchInterface();
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        updateWatchInterface();
    }

    private void updateCountDownText() {
        long timeLeftInMillis = mEndTime - System.currentTimeMillis();
        int timeLeftInSecs = (int) timeLeftInMillis / 1000;
        if (timeLeftInSecs < 0) {
            mTextViewCountDown.setTextColor(android.graphics.Color.RED);
            timeLeftInSecs = -timeLeftInSecs;
        }
        int hours = timeLeftInSecs / 3600;
        int minutes = timeLeftInSecs % 3600 / 60;
        int seconds = timeLeftInSecs % 60;
        String timeLeftFormatted;
        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
        mTextViewCountDown.setText(timeLeftFormatted);
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
        updateCountDownText();
        updateWatchInterface();
        if (mTimerRunning) {
            mEndTime = prefs.getLong("endTime",0);
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