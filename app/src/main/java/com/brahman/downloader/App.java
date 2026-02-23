package com.brahman.downloader;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

public class App extends Application {

    private static final String TAG = "BrahmanApp";
    public static final String CHANNEL_ID = "brahman_download_channel";
    public static final String CHANNEL_ID_PROGRESS = "brahman_progress_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        initYoutubeDL();
        createNotificationChannels();
    }

    private void initYoutubeDL() {
        try {
            YoutubeDL.getInstance().init(this);
            FFmpeg.getInstance().init(this);
            Log.d(TAG, "✅ yt-dlp + ffmpeg initialized successfully");
        } catch (YoutubeDLException e) {
            Log.e(TAG, "❌ Failed to initialize yt-dlp: " + e.getMessage());
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Main download channel
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Download Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Shows download completion notifications");
            manager.createNotificationChannel(channel);

            // Progress channel
            NotificationChannel progressChannel = new NotificationChannel(
                    CHANNEL_ID_PROGRESS,
                    "Download Progress",
                    NotificationManager.IMPORTANCE_LOW
            );
            progressChannel.setDescription("Shows download progress");
            manager.createNotificationChannel(progressChannel);
        }
    }
}
