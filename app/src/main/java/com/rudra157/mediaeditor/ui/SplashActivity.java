package com.rudra157.mediaeditor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.rudra157.mediaeditor.R;
import com.rudra157.mediaeditor.core.ai.TFLiteHelper;
import com.rudra157.mediaeditor.core.ads.AdManager;
import com.rudra157.mediaeditor.core.utils.FileManager;

/**
 * Splash screen activity with animated logo and initialization
 */
public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION = 3000; // 3 seconds
    private static final long ANIMATION_DURATION = 1500; // 1.5 seconds
    
    private ImageView logoImageView;
    private TextView appNameTextView;
    private TFLiteHelper tfliteHelper;
    private boolean initializationComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        initViews();
        startLogoAnimation();
        initializeApp();
    }

    private void initViews() {
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
    }

    private void startLogoAnimation() {
        // Scale animation for logo
        ScaleAnimation scaleAnimation = new ScaleAnimation(
            0.5f, 1.0f, 0.5f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );
        
        scaleAnimation.setDuration(ANIMATION_DURATION);
        scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnimation.setFillAfter(true);
        
        logoImageView.startAnimation(scaleAnimation);
        
        // Fade in animation for app name
        appNameTextView.setAlpha(0f);
        appNameTextView.animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .setStartDelay(500)
            .start();
    }

    private void initializeApp() {
        // Initialize components in background
        new Thread(() -> {
            try {
                // Initialize AdMob
                AdManager.getInstance(this);
                
                // Initialize TFLite helper (models will be loaded lazily)
                tfliteHelper = new TFLiteHelper(this);
                
                // Create app directories
                FileManager.createAppDirectories(this);
                
                // Simulate additional loading time for better UX
                Thread.sleep(1000);
                
                // Mark initialization as complete
                initializationComplete = true;
                
                // Navigate to main activity after splash duration
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (initializationComplete) {
                        navigateToMainActivity();
                    }
                });
                
            } catch (Exception e) {
                // If initialization fails, still proceed to main activity
                initializationComplete = true;
                new Handler(Looper.getMainLooper()).post(this::navigateToMainActivity);
            }
        }).start();
        
        // Fallback timer to ensure we don't get stuck
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!initializationComplete) {
                initializationComplete = true;
                navigateToMainActivity();
            }
        }, SPLASH_DURATION);
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        
        // Add transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up TFLite resources
        if (tfliteHelper != null) {
            tfliteHelper.cleanup();
        }
    }
}
