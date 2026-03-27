package com.rudra157.mediaeditor.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rudra157.mediaeditor.R;
import com.rudra157.mediaeditor.core.ads.AdManager;
import com.rudra157.mediaeditor.ui.adapters.AIToolAdapter;

import java.util.Arrays;
import java.util.List;

/**
 * AI Tools screen with grid layout of AI-powered editing tools
 */
public class AIToolsActivity extends AppCompatActivity {
    private LinearLayout bannerAdContainer;
    private RecyclerView toolsRecyclerView;
    private AIToolAdapter toolAdapter;
    private AdManager adManager;

    // AI Tools data
    private List<AITool> aiTools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_tools);
        
        initViews();
        setupToolbar();
        setupAITools();
        setupRecyclerView();
        loadBannerAd();
    }

    private void initViews() {
        bannerAdContainer = findViewById(R.id.bannerAdContainer);
        toolsRecyclerView = findViewById(R.id.toolsRecyclerView);
        
        adManager = AdManager.getInstance(this);
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.ai_tools);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupAITools() {
        aiTools = Arrays.asList(
            new AITool(R.string.upscale_image, R.drawable.ic_image_upscale, AIToolType.UPSCALE),
            new AITool(R.string.remove_background, R.drawable.ic_background_remove, AIToolType.REMOVE_BACKGROUND),
            new AITool(R.string.enhance_image, R.drawable.ic_image_enhance, AIToolType.ENHANCE),
            new AITool(R.string.sharpen, R.drawable.ic_sharpen, AIToolType.SHARPEN),
            new AITool(R.string.cartoon_effect, R.drawable.ic_cartoon, AIToolType.CARTOON)
        );
    }

    private void setupRecyclerView() {
        toolAdapter = new AIToolAdapter(aiTools, this::onToolClicked);
        toolsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        toolsRecyclerView.setAdapter(toolAdapter);
    }

    private void loadBannerAd() {
        if (adManager != null) {
            adManager.loadBannerAd(bannerAdContainer);
        }
    }

    private void onToolClicked(AITool tool) {
        // Show interstitial ad before opening tool
        if (adManager != null) {
            adManager.showInterstitialAdIfConnected(this, () -> {
                openAITool(tool);
            });
        } else {
            openAITool(tool);
        }
    }

    private void openAITool(AITool tool) {
        Intent intent = new Intent(this, AIProcessActivity.class);
        intent.putExtra("tool_type", tool.getType().ordinal());
        intent.putExtra("tool_name", getString(tool.getNameResId()));
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBannerAd();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adManager != null) {
            adManager.destroy();
        }
    }

    /**
     * AI Tool data model
     */
    public static class AITool {
        private final int nameResId;
        private final int iconResId;
        private final AIToolType type;

        public AITool(int nameResId, int iconResId, AIToolType type) {
            this.nameResId = nameResId;
            this.iconResId = iconResId;
            this.type = type;
        }

        public int getNameResId() {
            return nameResId;
        }

        public int getIconResId() {
            return iconResId;
        }

        public AIToolType getType() {
            return type;
        }
    }

    /**
     * AI Tool types
     */
    public enum AIToolType {
        UPSCALE,
        REMOVE_BACKGROUND,
        ENHANCE,
        SHARPEN,
        CARTOON
    }
}
