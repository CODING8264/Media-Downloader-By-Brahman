package com.brahman.downloader;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 100;

    private TextInputEditText etUrl;
    private MaterialButton btnSearch;
    private LinearLayout layoutLoading;
    private LinearLayout layoutEmpty;

    private DownloadItem currentItem;
    private BottomSheetDialog bottomSheet;

    private ProgressBar currentProgressBar;
    private TextView currentTvProgress;
    private TextView currentTvSpeed;
    private MaterialButton currentBtnDownload;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            runOnUiThread(() -> {
                switch (intent.getAction()) {
                    case DownloadService.BROADCAST_PROGRESS: handleProgress(intent); break;
                    case DownloadService.BROADCAST_DONE:     handleDone(intent);     break;
                    case DownloadService.BROADCAST_ERROR:    handleError(intent);    break;
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar();
        setupViews();
        requestStoragePermissions();
        handleSharedIntent(getIntent());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    private void setupViews() {
        etUrl         = findViewById(R.id.et_url);
        btnSearch     = findViewById(R.id.btn_search);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutEmpty   = findViewById(R.id.layout_empty);

        btnSearch.setOnClickListener(v -> {
            String url = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
            if (url.isEmpty()) {
                Toast.makeText(this, "Please paste a link first", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchVideoInfo(url);
        });
    }

    private void fetchVideoInfo(String url) {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        btnSearch.setEnabled(false);

        executor.execute(() -> {
            try {
                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("--dump-json");
                request.addOption("--no-playlist");
                request.addOption("--no-warnings");

                YoutubeDLResponse response = YoutubeDL.getInstance().execute(request, null, null);
                JSONObject info = new JSONObject(response.getOut());

                String title       = info.optString("title", "Unknown Title");
                String uploader    = info.optString("uploader", info.optString("channel", "Unknown"));
                String thumbnail   = info.optString("thumbnail", "");
                long   duration    = info.optLong("duration", 0);
                String platform    = info.optString("extractor_key", "Unknown");
                int subtitleCount  = info.optJSONObject("subtitles") != null
                        ? info.getJSONObject("subtitles").length() : 0;
                int chapterCount   = info.optJSONArray("chapters") != null
                        ? info.getJSONArray("chapters").length() : 0;

                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    btnSearch.setEnabled(true);
                    showDownloadSheet(url, title, uploader, thumbnail,
                            duration, platform, subtitleCount, chapterCount);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                    btnSearch.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showDownloadSheet(String url, String title, String uploader,
                                   String thumbnail, long duration, String platform,
                                   int subtitleCount, int chapterCount) {

        bottomSheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        bottomSheet.setContentView(R.layout.sheet_download);

        ImageView      imgThumb         = bottomSheet.findViewById(R.id.img_thumbnail);
        TextView       tvPlatform       = bottomSheet.findViewById(R.id.tv_platform);
        TextView       tvDuration       = bottomSheet.findViewById(R.id.tv_duration);
        TextView       tvTitle          = bottomSheet.findViewById(R.id.tv_title);
        TextView       tvUploader       = bottomSheet.findViewById(R.id.tv_uploader);
        RadioGroup     tabGroup         = bottomSheet.findViewById(R.id.tab_group);
        LinearLayout   layoutAudio      = bottomSheet.findViewById(R.id.layout_audio);
        LinearLayout   layoutVideo      = bottomSheet.findViewById(R.id.layout_video);
        Spinner        spinnerAudioFmt  = bottomSheet.findViewById(R.id.spinner_audio_format);
        Spinner        spinnerVideoFmt  = bottomSheet.findViewById(R.id.spinner_video_format);
        ChipGroup      chipGroupQuality = bottomSheet.findViewById(R.id.chip_group_quality);
        Chip           chipThumb        = bottomSheet.findViewById(R.id.chip_thumb);
        Chip           chipMeta         = bottomSheet.findViewById(R.id.chip_meta);
        Chip           chipSubs         = bottomSheet.findViewById(R.id.chip_subs);
        Chip           chipChapters     = bottomSheet.findViewById(R.id.chip_chapters);
        Chip           chipSponsor      = bottomSheet.findViewById(R.id.chip_sponsor);
        LinearLayout   layoutProgress   = bottomSheet.findViewById(R.id.layout_progress);
        ProgressBar    progressBar      = bottomSheet.findViewById(R.id.progress_bar);
        TextView       tvProgress       = bottomSheet.findViewById(R.id.tv_progress);
        TextView       tvSpeed          = bottomSheet.findViewById(R.id.tv_speed);
        MaterialButton btnDownload      = bottomSheet.findViewById(R.id.btn_download);
        MaterialButton btnCancel        = bottomSheet.findViewById(R.id.btn_cancel);

        if (imgThumb != null && !thumbnail.isEmpty())
            Glide.with(this).load(thumbnail).into(imgThumb);
        if (tvPlatform != null)  tvPlatform.setText(platform);
        if (tvDuration != null)  tvDuration.setText(formatDuration(duration));
        if (tvTitle    != null)  tvTitle.setText(title);
        if (tvUploader != null)  tvUploader.setText(uploader);

        if (chipSubs != null && subtitleCount > 0)
            chipSubs.setText("💬 Subtitles (" + subtitleCount + ")");
        if (chipChapters != null && chapterCount > 0)
            chipChapters.setText("📚 Chapters (" + chapterCount + ")");

        if (tabGroup != null && layoutAudio != null && layoutVideo != null) {
            tabGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.tab_audio) {
                    layoutAudio.setVisibility(View.VISIBLE);
                    layoutVideo.setVisibility(View.GONE);
                } else {
                    layoutAudio.setVisibility(View.GONE);
                    layoutVideo.setVisibility(View.VISIBLE);
                }
            });
        }

        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                currentItem            = new DownloadItem();
                currentItem.url        = url;
                currentItem.title      = title;
                currentItem.uploader   = uploader;
                currentItem.thumbnail  = thumbnail;

                boolean isAudio = tabGroup != null
                        && tabGroup.getCheckedRadioButtonId() == R.id.tab_audio;
                currentItem.type = isAudio ? "audio" : "video";

                if (isAudio) {
                    currentItem.format = spinnerAudioFmt != null
                            ? spinnerAudioFmt.getSelectedItem().toString().toLowerCase() : "mp3";
                } else {
                    currentItem.format = spinnerVideoFmt != null
                            ? spinnerVideoFmt.getSelectedItem().toString().toLowerCase() : "mp4";
                    if (chipGroupQuality != null) {
                        int cid = chipGroupQuality.getCheckedChipId();
                        if      (cid == R.id.chip_2160p) currentItem.quality = "2160";
                        else if (cid == R.id.chip_1440p) currentItem.quality = "1440";
                        else if (cid == R.id.chip_1080p) currentItem.quality = "1080";
                        else if (cid == R.id.chip_720p)  currentItem.quality = "720";
                        else if (cid == R.id.chip_480p)  currentItem.quality = "480";
                        else if (cid == R.id.chip_360p)  currentItem.quality = "360";
                        else                              currentItem.quality = "best";
                    }
                }

                currentItem.embedThumb    = chipThumb    != null && chipThumb.isChecked();
                currentItem.embedMetadata = chipMeta     != null && chipMeta.isChecked();
                currentItem.embedSubs     = chipSubs     != null && chipSubs.isChecked();
                currentItem.embedChapters = chipChapters != null && chipChapters.isChecked();
                currentItem.sponsorBlock  = chipSponsor  != null && chipSponsor.isChecked();

                HistoryManager.getInstance(this).addItem(currentItem);
                startDownload(currentItem);

                if (layoutProgress != null) layoutProgress.setVisibility(View.VISIBLE);
                btnDownload.setEnabled(false);
                btnDownload.setText("Downloading...");
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                Intent i = new Intent(this, DownloadService.class);
                i.setAction(DownloadService.ACTION_CANCEL);
                startService(i);
                bottomSheet.dismiss();
            });
        }

        currentProgressBar = progressBar;
        currentTvProgress  = tvProgress;
        currentTvSpeed     = tvSpeed;
        currentBtnDownload = btnDownload;

        bottomSheet.show();
    }

    private void handleProgress(Intent intent) {
        float  progress = intent.getFloatExtra("progress", 0);
        String speed    = intent.getStringExtra("speed");
        String eta      = intent.getStringExtra("eta");
        if (currentProgressBar != null) currentProgressBar.setProgress((int) progress);
        if (currentTvProgress  != null)
            currentTvProgress.setText(String.format("%.0f%%  ETA: %s", progress, eta != null ? eta : "--"));
        if (currentTvSpeed     != null) currentTvSpeed.setText(speed != null ? speed : "");
    }

    private void handleDone(Intent intent) {
        String filename = intent.getStringExtra("filename");
        String filesize = intent.getStringExtra("filesize");
        if (currentProgressBar != null) currentProgressBar.setProgress(100);
        if (currentTvProgress  != null) currentTvProgress.setText("✅ Complete!");
        if (currentTvSpeed     != null) currentTvSpeed.setText(filesize != null ? filesize : "");
        if (currentBtnDownload != null) { currentBtnDownload.setText("Done ✅"); currentBtnDownload.setEnabled(false); }
        if (currentItem != null) {
            currentItem.status = "done"; currentItem.filename = filename; currentItem.filesize = filesize;
            HistoryManager.getInstance(this).updateItem(currentItem);
        }
        Toast.makeText(this, "Downloaded: " + filename, Toast.LENGTH_LONG).show();
    }

    private void handleError(Intent intent) {
        String error = intent.getStringExtra("error");
        if (currentTvProgress  != null) currentTvProgress.setText("❌ Failed");
        if (currentBtnDownload != null) { currentBtnDownload.setText("Retry"); currentBtnDownload.setEnabled(true); }
        if (currentItem != null) { currentItem.status = "failed"; HistoryManager.getInstance(this).updateItem(currentItem); }
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
    }

    private void startDownload(DownloadItem item) {
        Intent si = new Intent(this, DownloadService.class);
        si.setAction(DownloadService.ACTION_DOWNLOAD);
        si.putExtra(DownloadService.EXTRA_ITEM, item);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(si);
        else startService(si);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.BROADCAST_PROGRESS);
        filter.addAction(DownloadService.BROADCAST_DONE);
        filter.addAction(DownloadService.BROADCAST_ERROR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(downloadReceiver); } catch (Exception ignored) {}
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSharedIntent(intent);
    }

    private void handleSharedIntent(Intent intent) {
        if (intent == null) return;
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null && !text.isEmpty()) {
                if (etUrl != null) etUrl.setText(text.trim());
                fetchVideoInfo(text.trim());
            }
        }
    }

    private void requestStoragePermissions() {
        List<String> perms = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        }
        if (!perms.isEmpty())
            ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), PERMISSION_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_history) {
            startActivity(new Intent(this, HistoryActivity.class)); return true;
        } else if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class)); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String formatDuration(long s) {
        if (s <= 0) return "";
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%d:%02d", m, sec);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdownNow();
    }
}
