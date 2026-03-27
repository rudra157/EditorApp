package com.rudra157.mediaeditor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.rudra157.mediaeditor.R;
import com.rudra157.mediaeditor.core.ads.AdManager;

/**
 * Main activity with grid menu for different editing options
 */
public class MainActivity extends AppCompatActivity {
    private LinearLayout bannerAdContainer;
    private CardView imageEditorCard;
    private CardView videoEditorCard;
    private CardView aiToolsCard;
    private AdManager adManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupClickListeners();
        loadBannerAd();
    }

    private void initViews() {
        bannerAdContainer = findViewById(R.id.bannerAdContainer);
        imageEditorCard = findViewById(R.id.imageEditorCard);
        videoEditorCard = findViewById(R.id.videoEditorCard);
        aiToolsCard = findViewById(R.id.aiToolsCard);
        
        adManager = AdManager.getInstance(this);
    }

    private void setupClickListeners() {
        imageEditorCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ImageEditorActivity.class);
            startActivity(intent);
        });

        videoEditorCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VideoEditorActivity.class);
            startActivity(intent);
        });

        aiToolsCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AIToolsActivity.class);
            startActivity(intent);
        });
    }

    private void loadBannerAd() {
        if (adManager != null) {
            adManager.loadBannerAd(bannerAdContainer);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        
        // Set dark mode toggle state
        MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
        boolean isNightMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
        darkModeItem.setTitle(isNightMode ? R.string.light_mode : R.string.dark_mode);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            // Open settings activity
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_dark_mode) {
            toggleDarkMode();
            return true;
        } else if (id == R.id.action_about) {
            // Show about dialog
            showAboutDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void toggleDarkMode() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        int newMode = (currentMode == AppCompatDelegate.MODE_NIGHT_YES) 
            ? AppCompatDelegate.MODE_NIGHT_NO 
            : AppCompatDelegate.MODE_NIGHT_YES;
        
        AppCompatDelegate.setDefaultNightMode(newMode);
        
        // Recreate activity to apply theme change
        recreate();
    }

    private void showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.about);
        builder.setMessage(R.string.about_message);
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload banner ad when activity resumes
        loadBannerAd();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up ad resources
        if (adManager != null) {
            adManager.destroy();
        }
    }
}
