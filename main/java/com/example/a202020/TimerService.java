package com.example.a202020;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TimerService extends Service {

    private Handler handler;
    private Runnable timerRunnable;
    private boolean isTimerRunning = false;
    private int timeRemainingInSeconds = 0;
    private NotificationManagerCompat notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = NotificationManagerCompat.from(this);
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals("start")) {
                int seconds = intent.getIntExtra("timerValue", 0);
                if (!isTimerRunning) {
                    startTimer(seconds);
                }
            } else if (action != null && action.equals("stop")) {
                stopForegroundService();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer(); // Make sure the timer is stopped
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startTimer(int seconds) {
        timeRemainingInSeconds = seconds;
        updateNotification(timeRemainingInSeconds);
        isTimerRunning = true;
        startBackgroundTimer();
    }

    private void startBackgroundTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeRemainingInSeconds > 0) {
                    timeRemainingInSeconds--;
                    updateNotification(timeRemainingInSeconds);
                    handler.postDelayed(this, 1000); // Update timer every second
                } else {
                    stopForegroundService();
                }
            }
        };
        handler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        isTimerRunning = false;
        handler.removeCallbacks(timerRunnable);
        updateNotification(0); // Update notification with 0 seconds remaining
        stopForegroundService(); // Add this line to stop the notification timer
    }

    private void stopForegroundService() {
        stopForeground(true);
        notificationManager.cancel(1); // Remove the notification
        stopSelf();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopForeground(true);
        notificationManager.cancel(1); // Remove the notification
        stopSelf();
    }

    private void updateNotification(int timeRemaining) {
        // Create a pending intent for the MainActivity
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction("NOTIFICATION_CLICKED");
        notificationIntent.putExtra("TIME_REMAINING", timeRemaining);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Replace R.drawable.ic_timer with the "info" icon
        int iconResourceId = android.R.drawable.ic_dialog_info;

        // Format the timeRemaining as "mm:ss"
        String formattedTime = String.format("%02d:%02d", timeRemaining / 60, timeRemaining % 60);

        // Build the notification
        Notification notification = new NotificationCompat.Builder(this, "timer_channel_id")
                .setContentTitle("Timer Running")
                .setContentText(formattedTime + " remaining")
                .setSmallIcon(iconResourceId)
                .setContentIntent(pendingIntent)
                .build();

        // Start the service in the foreground with the notification
        startForeground(1, notification);
    }
}