package com.brahman.downloader;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        // Show version
        TextView tvVersion = findViewById(R.id.tv_version);
        if (tvVersion != null) {
            try {
                String version = getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName;
                tvVersion.setText("Version " + version);
            } catch (Exception e) {
                tvVersion.setText("Version 1.0.0");
            }
        }

        // Show save path
        TextView tvPath = findViewById(R.id.tv_save_path);
        if (tvPath != null) {
            tvPath.setText("/Download/Brahman/");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
