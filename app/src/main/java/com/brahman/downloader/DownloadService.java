package com.brahman.downloader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import io.github.junkfood02.youtubedl_android.YoutubeDL;
import io.github.junkfood02.youtubedl_android.YoutubeDLRequest;
import io.github.junkfood02.youtubedl_android.YoutubeDLResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    public static final String ACTION_DOWNLOAD = "action_download";
    public static final String ACTION_CANCEL = "action_cancel";
    public static final String EXTRA_ITEM = "extra_item";
    public static final String BROADCAST_PROGRESS = "com.brahman.downloader.PROGRESS";
    public static final String BROADCAST_DONE = "com.brahman.downloader.DONE";
    public static final String BROADCAST_ERROR = "com.brahman.downloader.ERROR";

    private ExecutorService executor;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private NotificationManager notificationManager;
    private static final int NOTIF_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        notificationManager = getSystemService(NotificationManager.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();

        if (ACTION_CANCEL.equals(action)) {
            isCancelled.set(true);
            YoutubeDL.getInstance().destroyProcessById("brahman_dl");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_DOWNLOAD.equals(action)) {
            DownloadItem item = (DownloadItem) intent.getSerializableExtra(EXTRA_ITEM);
            if (item != null) {
                isCancelled.set(false);
                startForeground(NOTIF_ID, buildNotification(item.title, "Starting...", 0));
                executor.execute(() -> performDownload(item));
            }
        }

        return START_NOT_STICKY;
    }

    private void performDownload(DownloadItem item) {
        try {
            // Setup output directory
            String baseDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Brahman";
            String outputDir = item.isAudio()
                    ? baseDir + "/Audio"
                    : baseDir + "/Video";

            File dir = new File(outputDir);
            if (!dir.exists()) dir.mkdirs();

            // Build yt-dlp request
            YoutubeDLRequest request = new YoutubeDLRequest(item.url);

            // Output template
            String nameTemplate = (item.title != null && !item.title.isEmpty())
                    ? item.title.replaceAll("[/\\\\:*?\"<>|]", "_")
                    : "%(title)s";
            request.addOption("-o", outputDir + "/" + nameTemplate + ".%(ext)s");

            if (item.isAudio()) {
                // Audio download
                request.addOption("-x");
                request.addOption("--audio-format", item.format != null ? item.format : "mp3");
                request.addOption("--audio-quality", "0");
            } else {
                // Video download
                if ("best".equals(item.quality) || item.quality == null) {
                    request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best");
                } else {
                    request.addOption("-f",
                            "bestvideo[height<=" + item.quality + "][ext=mp4]+bestaudio[ext=m4a]" +
                            "/bestvideo[height<=" + item.quality + "]+bestaudio/best");
                }
                request.addOption("--merge-output-format", item.format != null ? item.format : "mp4");
            }

            // Options
            if (item.embedThumb) request.addOption("--embed-thumbnail");
            if (item.embedMetadata) request.addOption("--add-metadata");
            if (item.embedSubs) {
                request.addOption("--embed-subs");
                request.addOption("--sub-langs", "all");
            }
            if (item.embedChapters) request.addOption("--add-chapters");
            if (item.sponsorBlock) {
                request.addOption("--sponsorblock-remove", "sponsor,intro,outro,selfpromo");
            }

            // No playlist by default
            if (!item.isPlaylist) {
                request.addOption("--no-playlist");
            } else if (item.playlistItems != null && !item.playlistItems.isEmpty()) {
                request.addOption("--playlist-items", item.playlistItems);
            }

            // Misc
            request.addOption("--no-warnings");
            request.addOption("--retries", "3");

            Log.d(TAG, "Starting download: " + item.url);

            // Execute with progress callback
            YoutubeDLResponse response = YoutubeDL.getInstance().execute(
                    request,
                    "brahman_dl",
                    (progress, etaInSeconds, line) -> {
                        if (isCancelled.get()) return;

                        // Parse speed from line
                        String speed = "";
                        String eta = etaInSeconds > 0 ? formatEta(etaInSeconds) : "";

                        if (line != null) {
                            if (line.contains("EiB/s") || line.contains("KiB/s") ||
                                line.contains("MiB/s") || line.contains("GiB/s")) {
                                String[] parts = line.split("\\s+");
                                for (int i = 0; i < parts.length - 1; i++) {
                                    if (parts[i + 1].contains("iB/s")) {
                                        speed = parts[i] + " " + parts[i + 1];
                                        break;
                                    }
                                }
                            }
                        }

                        // Update notification
                        notificationManager.notify(NOTIF_ID,
                                buildNotification(item.title, speed + " · " + (int) progress + "%", (int) progress));

                        // Broadcast progress
                        Intent progressIntent = new Intent(BROADCAST_PROGRESS);
                        progressIntent.putExtra("id", item.id);
                        progressIntent.putExtra("progress", progress);
                        progressIntent.putExtra("speed", speed);
                        progressIntent.putExtra("eta", eta);
                        sendBroadcast(progressIntent);
                    }
            );

            if (isCancelled.get()) {
                broadcastError(item.id, "Cancelled");
                return;
            }

            // Find downloaded file
            File[] files = dir.listFiles();
            String filename = "";
            long filesize = 0;
            String filePath = "";
            long latestTime = 0;

            if (files != null) {
                for (File f : files) {
                    if (f.lastModified() > latestTime) {
                        latestTime = f.lastModified();
                        filename = f.getName();
                        filesize = f.length();
                        filePath = f.getAbsolutePath();
                    }
                }
            }

            // Broadcast success
            Intent doneIntent = new Intent(BROADCAST_DONE);
            doneIntent.putExtra("id", item.id);
            doneIntent.putExtra("filename", filename);
            doneIntent.putExtra("filesize", formatSize(filesize));
            doneIntent.putExtra("filePath", filePath);
            sendBroadcast(doneIntent);

            // Success notification
            notificationManager.notify(NOTIF_ID + 1,
                    buildDoneNotification("Download Complete", filename));

            Log.d(TAG, "✅ Download complete: " + filename);

        } catch (Exception e) {
            Log.e(TAG, "❌ Download failed: " + e.getMessage());
            broadcastError(item.id, e.getMessage() != null ? e.getMessage() : "Unknown error");
        } finally {
            stopForeground(true);
            stopSelf();
        }
    }

    private void broadcastError(String id, String error) {
        Intent errorIntent = new Intent(BROADCAST_ERROR);
        errorIntent.putExtra("id", id);
        errorIntent.putExtra("error", error);
        sendBroadcast(errorIntent);
    }

    private Notification buildNotification(String title, String content, int progress) {
        Intent cancelIntent = new Intent(this, DownloadService.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent cancelPending = PendingIntent.getService(this, 0, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, App.CHANNEL_ID_PROGRESS)
                .setContentTitle(title != null ? title : "Downloading...")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, progress, progress == 0)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_delete, "Cancel", cancelPending)
                .build();
    }

    private Notification buildDoneNotification(String title, String filename) {
        Intent openIntent = new Intent(this, HistoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(filename)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private String formatEta(long seconds) {
        if (seconds <= 0) return "";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1073741824) return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdownNow();
    }
}