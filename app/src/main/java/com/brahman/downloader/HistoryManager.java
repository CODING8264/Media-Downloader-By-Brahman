package com.brahman.downloader;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager {

    private static final String PREFS_NAME = "brahman_history";
    private static final String KEY_ITEMS = "download_items";
    private static final int MAX_ITEMS = 300;

    private static HistoryManager instance;
    private SharedPreferences prefs;
    private List<DownloadItem> items;

    private HistoryManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        items = loadItems();
    }

    public static synchronized HistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryManager(context);
        }
        return instance;
    }

    private List<DownloadItem> loadItems() {
        String json = prefs.getString(KEY_ITEMS, "[]");
        return DownloadItem.fromJson(json);
    }

    private void saveItems() {
        prefs.edit().putString(KEY_ITEMS, DownloadItem.toJson(items)).apply();
    }

    public void addItem(DownloadItem item) {
        items.add(0, item);
        if (items.size() > MAX_ITEMS) {
            items = items.subList(0, MAX_ITEMS);
        }
        saveItems();
    }

    public void updateItem(DownloadItem updated) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id.equals(updated.id)) {
                items.set(i, updated);
                saveItems();
                return;
            }
        }
    }

    public void removeItem(String id) {
        items.removeIf(item -> item.id.equals(id));
        saveItems();
    }

    public void clearAll() {
        items.clear();
        saveItems();
    }

    public List<DownloadItem> getAllItems() {
        return new ArrayList<>(items);
    }

    public List<DownloadItem> getAudioItems() {
        List<DownloadItem> result = new ArrayList<>();
        for (DownloadItem item : items) {
            if ("audio".equals(item.type)) result.add(item);
        }
        return result;
    }

    public List<DownloadItem> getVideoItems() {
        List<DownloadItem> result = new ArrayList<>();
        for (DownloadItem item : items) {
            if ("video".equals(item.type)) result.add(item);
        }
        return result;
    }
}
