package com.brahman.downloader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DownloadItem implements Serializable {

    public String id;
    public String url;
    public String title;
    public String uploader;
    public String thumbnail;
    public String type;        // "audio" or "video"
    public String format;      // mp3, mp4, m4a, webm etc
    public String quality;     // "best", "720", "1080", "2160" etc
    public String status;      // "queued", "downloading", "done", "failed", "cancelled"
    public String filename;
    public String filesize;
    public String filePath;
    public long date;
    public float progress;
    public String speed;
    public String eta;

    // Download options
    public boolean embedThumb;
    public boolean embedMetadata;
    public boolean embedSubs;
    public boolean embedChapters;
    public boolean sponsorBlock;

    // Playlist
    public boolean isPlaylist;
    public String playlistItems;

    public DownloadItem() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.date = System.currentTimeMillis();
        this.status = "queued";
        this.progress = 0f;
    }

    public boolean isAudio() {
        return "audio".equals(type);
    }

    public boolean isDone() {
        return "done".equals(status);
    }

    public boolean isFailed() {
        return "failed".equals(status);
    }

    public boolean isActive() {
        return "downloading".equals(status) || "queued".equals(status);
    }

    // Serialize list to JSON
    public static String toJson(List<DownloadItem> items) {
        return new Gson().toJson(items);
    }

    // Deserialize list from JSON
    public static List<DownloadItem> fromJson(String json) {
        try {
            Type type = new TypeToken<List<DownloadItem>>() {}.getType();
            List<DownloadItem> items = new Gson().fromJson(json, type);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
