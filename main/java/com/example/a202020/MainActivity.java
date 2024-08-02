package com.example.a202020;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.*;

import android.app.Notification;
import android.content.Context;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private TextView timerTextView;
    private CountDownTimer timer;
    private boolean isTimerRunning = false;
    private MediaPlayer mediaPlayer;
    private ProgressBar progressBar;
    private NotificationManager notificationManager;
    private int timeRemainingInSeconds = 0;
    private int timerDurationInSeconds;
    private SettingsManager settingsManager;
    private static final int TIMER_NOTIFICATION_ID = 1;
    private static final int TWENTY_SECOND_TIMER_NOTIFICATION_ID = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        // Initialize the SettingsManager
        settingsManager = new SettingsManager(findViewById(R.id.settingsContainer), this);

        button = findViewById(R.id.button);
        timerTextView = findViewById(R.id.timerTextView);
        mediaPlayer = MediaPlayer.create(this, R.raw.beep_sound);
        progressBar = findViewById(R.id.progressBar);

        // Check if the "Test Timer" switch is on
        boolean isTestModeOn = settingsManager.getTestModeSetting();
        timerDurationInSeconds = isTestModeOn ? 10 : 20 * 60; // 10 seconds for Test Mode, 20 minutes otherwise


        // Check if the activity was opened from the notification
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("NOTIFICATION_CLICKED")) {
                // Get the remaining time from the notification's intent
                timeRemainingInSeconds = intent.getIntExtra("TIME_REMAINING", 0);
                if (timeRemainingInSeconds > 0) {
                    // If there is an active timer, update the UI with the remaining time
                    startTimer(timeRemainingInSeconds);
                }
            }
        }

        // Initialize the SwitchCompat view
        SwitchCompat vibrateMuteSwitch = findViewById(R.id.vibrate_mute_switch);

        if (vibrateMuteSwitch == null) {
            // Handle the case where the view is not found
            Log.e("MainActivity", "vibrateMuteSwitch is null");
            // You might want to add some error handling here
        } else {
            // Assuming you have a saved switch state in SharedPreferences
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean switchState = preferences.getBoolean("vibrate_mute_state", false);

            // Set the switch state
            vibrateMuteSwitch.setChecked(switchState);

            // Set an OnCheckedChangeListener for the switch to handle user interactions
            vibrateMuteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Save the switch state to SharedPreferences when the user interacts with it
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean("vibrate_mute_state", isChecked);
                    editor.apply();

                    // Handle any other actions you want to perform when the switch state changes
                }
            });
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isTimerRunning) {
                    startTimer(timerDurationInSeconds);
                    isTimerRunning = true;
                } else {
                    stopTimer();
                }
            }
        });

        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the settings dialog and pass the settingsManager reference
                SettingsDialogFragment dialogFragment = new SettingsDialogFragment(settingsManager);
                dialogFragment.show(getSupportFragmentManager(), "SettingsDialogFragment");
            }
        });
    }

    private class TimerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                if (intent.getAction().equals("TIMER_TICK")) {
                    // Get the time remaining from the broadcast
                    long timeRemaining = intent.getLongExtra("TIME_REMAINING", 0);

                    // Update the notification with the new time remaining
                    updateNotification((int) timeRemaining);
                }
            }
        }
    }
    private TimerBroadcastReceiver timerBroadcastReceiver;

    @Override
    protected void onResume() {
        super.onResume();

        // Check if the activity was opened from the notification
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals("android.intent.action.MAIN")) {
                // Check if there is an active timer
                if (isTimerRunning) {
                    // If there is, update the UI with the remaining time
                    int seconds = timeRemainingInSeconds / 1000;
                    startTimer(seconds);
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Timer Channel";
            String description = "Channel for Timer Notifications";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("timer_channel_id", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void updateNotification(int seconds) {
        Notification notification = new NotificationCompat.Builder(this, "timer_channel_id")
                .setContentTitle("Timer Running")
                .setContentText(seconds + " seconds remaining")
                .setSmallIcon(R.drawable.ic_timer)
                .build();

        notificationManager.notify(1, notification);
    }


    private void startTimer(int seconds) {
        if (isTimerRunning != true){
        timeRemainingInSeconds = seconds;
        // Stop the MP3 file from playing, if it is currently playing
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.beep_sound);
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.prepareAsync();
        }

        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.setAction("start");
        serviceIntent.putExtra("timerValue", seconds);
        startService(serviceIntent);

        button.setText("Stop Timer");
        Toast.makeText(MainActivity.this, "Timer started", Toast.LENGTH_SHORT).show();
        isTimerRunning = true;

        timer = new CountDownTimer(timerDurationInSeconds * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;

                // Format the minutes and seconds with leading zeros if needed
                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

                timerTextView.setText(formattedTime);

                int progress = (int) (100 * millisUntilFinished / (10 * 1000)); // Calculate progress percentage
                progressBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                // Check if the notificationManager is not null and cancel the notification
                if (notificationManager != null) {
                    notificationManager.cancel(1);
                }

                button.setText("Start Timer");
                isTimerRunning = false;

                if (settingsManager.getVibrateMuteSetting()) {
                    // If vibrate and mute is enabled, vibrate the phone for 1 second
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // Create a vibration effect for one second
                            VibrationEffect vibrationEffect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE);
                            vibrator.vibrate(vibrationEffect);

                            // Schedule the second vibration after a one-second delay
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    vibrator.vibrate(vibrationEffect);
                                }
                            }, 2000);
                        } else {
                            // For older Android versions
                            vibrator.vibrate(1000); // Vibrate for 1 second

                            // Schedule the second vibration after a two-second delay (2000 milliseconds)
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    vibrator.vibrate(1000); // Vibrate for 1 second
                                }
                            }, 2000);
                        }
                    }
                } else {
                    // If vibrate and mute is disabled, play the beep sound
                    // Check if mediaPlayer is not null before trying to start it
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                    }
                }

                int newProgress = 0; // Set the new progress value
                ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), newProgress);
                animation.setDuration(300); // Set the animation duration (in milliseconds)
                animation.start();

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        start20SecondTimer();
                    }
                });
            }
        }.start();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isTimerRunning) {
                    startTimer(seconds);
                } else {
                    timer.cancel();
                    button.setText("Start Timer");
                    timerTextView.setText("");
                    Toast.makeText(MainActivity.this, "Timer stopped", Toast.LENGTH_SHORT).show();
                    isTimerRunning = false;
                }
            }
        });
    }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Set the new intent as the current intent
    }

    private void start20SecondTimer() {
        // Stop the MP3 file from playing, if it is currently playing
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.prepareAsync();
            mediaPlayer = MediaPlayer.create(this, R.raw.beep_sound);
        }

        Intent serviceIntent = new Intent(this, TimerService.class);
        serviceIntent.setAction("start");
        serviceIntent.putExtra("timerValue", 20); // Change this to 20 seconds or any other desired value
        startService(serviceIntent);

        button.setText("Stop Timer");
        Toast.makeText(MainActivity.this, "Timer started", Toast.LENGTH_SHORT).show();
        isTimerRunning = true;

        timer = new CountDownTimer(20 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                timerTextView.setText(String.format("%02d", seconds));
            }

            @Override
            public void onFinish() {
                // Check if the notificationManager is not null and cancel the notification
                if (notificationManager != null) {
                    notificationManager.cancel(2); // Use a different notification ID for the 20-second timer
                }

                Toast.makeText(MainActivity.this, "20 seconds are up", Toast.LENGTH_SHORT).show();
                button.setText("Start Timer");
                isTimerRunning = false;

                if (settingsManager.getVibrateMuteSetting()) {
                    // If vibrate and mute is enabled, vibrate the phone for 1 second
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // Create a vibration effect for one second
                            VibrationEffect vibrationEffect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE);
                            vibrator.vibrate(vibrationEffect);

                            // Schedule the second vibration after a two-second delay (2000 milliseconds)
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    vibrator.vibrate(vibrationEffect);
                                }
                            }, 2000);
                        } else {
                            // For older Android versions
                            vibrator.vibrate(1000); // Vibrate for 1 second

                            // Schedule the second vibration after a two-second delay (2000 milliseconds)
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    vibrator.vibrate(1000); // Vibrate for 1 second
                                }
                            }, 2000);
                        }
                    }
                } else {
                    // If vibrate and mute is disabled, play the beep sound
                    // Check if mediaPlayer is not null before trying to start it
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                    }
                }

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isTimerRunning) {
                            int seconds = 0;
                            startTimer(seconds); // Start the timer again
                        }
                    }
                });
            }
        }.start();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTimerRunning) {
                    timer.cancel();
                    button.setText("Start Timer");
                    timerTextView.setText("");
                    Toast.makeText(MainActivity.this, "Timer stopped", Toast.LENGTH_SHORT).show();
                    isTimerRunning = false;
                } else {
                    start20SecondTimer();
                }
            }
        });
    }

    private void stopTimer() {
        stopService(new Intent(this, TimerService.class));
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        notificationManager.cancel(1);
        updateNotification(0);
        button.setText("Start Timer");
        timerTextView.setText("");
        Toast.makeText(MainActivity.this, "Timer stopped", Toast.LENGTH_SHORT).show();
        isTimerRunning = false;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        // Unregister the BroadcastReceiver if needed
        // unregisterReceiver(stopSoundReceiver);

        Intent stopServiceIntent = new Intent(this, TimerService.class);
        stopService(stopServiceIntent);
        stopTimer(); // Make sure the timer is stopped
        stopService(new Intent(this, TimerService.class));
        notificationManager.cancel(1); // Remove the notification when the app is destroyed
    }
}