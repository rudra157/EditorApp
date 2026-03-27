package com.rudra157.mediaeditor.core.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages all AdMob advertisements in the app
 */
public class AdManager {
    private static final String TAG = "AdManager";
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"; // Test ID
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // Test ID
    
    private static AdManager instance;
    private final Context context;
    private InterstitialAd interstitialAd;
    private final AtomicBoolean isInterstitialLoading = new AtomicBoolean(false);
    private final AtomicBoolean isInterstitialShowing = new AtomicBoolean(false);

    private AdManager(Context context) {
        this.context = context.getApplicationContext();
        initializeMobileAds();
    }

    public static synchronized AdManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdManager(context);
        }
        return instance;
    }

    private void initializeMobileAds() {
        MobileAds.initialize(context, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                Log.d(TAG, "AdMob initialized successfully");
                loadInterstitialAd();
            }
        });
    }

    /**
     * Loads a banner ad into the provided container
     */
    public void loadBannerAd(LinearLayout bannerContainer) {
        if (bannerContainer == null) return;

        AdView adView = new AdView(context);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);

        bannerContainer.removeAllViews();
        bannerContainer.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        adView.setAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Log.e(TAG, "Banner ad failed to load: " + adError.getMessage());
                bannerContainer.removeAllViews();
            }
        });
    }

    /**
     * Loads an interstitial ad
     */
    public void loadInterstitialAd() {
        if (isInterstitialLoading.get()) return;

        isInterstitialLoading.set(true);

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, 
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    interstitialAd = ad;
                    isInterstitialLoading.set(false);
                    Log.d(TAG, "Interstitial ad loaded successfully");
                    
                    // Set full screen content callback
                    ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            isInterstitialShowing.set(false);
                            loadInterstitialAd(); // Preload next ad
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            isInterstitialShowing.set(false);
                            Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isInterstitialShowing.set(true);
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                    interstitialAd = null;
                    isInterstitialLoading.set(false);
                    Log.e(TAG, "Interstitial ad failed to load: " + adError.getMessage());
                }
            });
    }

    /**
     * Shows an interstitial ad if available
     * @param onAdDismissed Callback called when ad is dismissed
     */
    public void showInterstitialAd(Runnable onAdDismissed) {
        if (interstitialAd == null || isInterstitialShowing.get()) {
            Log.d(TAG, "Interstitial ad not ready or already showing");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }

        interstitialAd.show((Activity) context);
        
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                isInterstitialShowing.set(false);
                if (onAdDismissed != null) {
                    onAdDismissed.run();
                }
                loadInterstitialAd(); // Preload next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                isInterstitialShowing.set(false);
                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                if (onAdDismissed != null) {
                    onAdDismissed.run();
                }
            }

            @Override
            public void onAdShowedFullScreenContent() {
                isInterstitialShowing.set(true);
            }
        });
    }

    /**
     * Shows an interstitial ad if available and internet is connected
     */
    public void showInterstitialAdIfConnected(Activity activity, Runnable onAdDismissed) {
        if (!isInternetConnected()) {
            Log.d(TAG, "No internet connection, skipping ad");
            if (onAdDismissed != null) {
                onAdDismissed.run();
            }
            return;
        }

        showInterstitialAd(onAdDismissed);
    }

    /**
     * Checks if internet is available
     */
    private boolean isInternetConnected() {
        // Simple connectivity check
        return true; // In production, implement proper connectivity check
    }

    /**
     * Check if interstitial ad is ready to show
     */
    public boolean isInterstitialAdReady() {
        return interstitialAd != null && !isInterstitialShowing.get();
    }

    /**
     * Clean up resources
     */
    public void destroy() {
        interstitialAd = null;
    }
}
