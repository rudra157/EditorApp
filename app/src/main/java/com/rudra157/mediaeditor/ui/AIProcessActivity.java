package com.rudra157.mediaeditor.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.rudra157.mediaeditor.R;
import com.rudra157.mediaeditor.core.ai.TFLiteHelper;
import com.rudra157.mediaeditor.core.ads.AdManager;
import com.rudra157.mediaeditor.core.utils.FileManager;
import com.rudra157.mediaeditor.ui.AIToolsActivity.AIToolType;

/**
 * Activity for processing AI tools with image selection and result display
 */
public class AIProcessActivity extends AppCompatActivity {
    private static final int IMAGE_PICKER_REQUEST = 1001;
    
    private ImageView inputImageView;
    private ImageView outputImageView;
    private Button selectImageButton;
    private Button processButton;
    private Button saveButton;
    private Button shareButton;
    private ProgressBar progressBar;
    private TextView statusTextView;
    
    private TFLiteHelper tfliteHelper;
    private AdManager adManager;
    private Bitmap inputBitmap;
    private Bitmap outputBitmap;
    private AIToolType currentToolType;
    private String currentToolName;
    
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_process);
        
        initViews();
        setupToolbar();
        getToolType();
        setupClickListeners();
        initializeComponents();
    }

    private void initViews() {
        inputImageView = findViewById(R.id.inputImageView);
        outputImageView = findViewById(R.id.outputImageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        processButton = findViewById(R.id.processButton);
        saveButton = findViewById(R.id.saveButton);
        shareButton = findViewById(R.id.shareButton);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);
        
        tfliteHelper = new TFLiteHelper(this);
        adManager = AdManager.getInstance(this);
        
        // Initially hide output and action buttons
        outputImageView.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        shareButton.setVisibility(View.GONE);
        processButton.setEnabled(false);
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentToolName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void getToolType() {
        int toolTypeOrdinal = getIntent().getIntExtra("tool_type", 0);
        currentToolType = AIToolType.values()[toolTypeOrdinal];
        currentToolName = getIntent().getStringExtra("tool_name");
    }

    private void setupClickListeners() {
        selectImageButton.setOnClickListener(v -> selectImage());
        processButton.setOnClickListener(v -> processImage());
        saveButton.setOnClickListener(v -> saveResult());
        shareButton.setOnClickListener(v -> shareResult());
    }

    private void initializeComponents() {
        // Set initial status
        updateStatus(getString(R.string.select_image_to_start));
    }

    private void selectImage() {
        ImagePicker.with(this)
            .galleryOnly()
            .compress(1024)
            .maxResultSize(1080, 1920)
            .start(IMAGE_PICKER_REQUEST);
    }

    private void processImage() {
        if (inputBitmap == null || isProcessing) {
            return;
        }
        
        isProcessing = true;
        updateStatus(getString(R.string.processing));
        showProgress(true);
        
        // Process image in background thread
        new Thread(() -> {
            try {
                Bitmap result = processImageWithAI(inputBitmap, currentToolType);
                
                runOnUiThread(() -> {
                    outputBitmap = result;
                    showResult();
                    isProcessing = false;
                    showProgress(false);
                    updateStatus(getString(R.string.processing_complete));
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    isProcessing = false;
                    showProgress(false);
                    updateStatus(getString(R.string.processing_failed));
                    Toast.makeText(this, getString(R.string.error_processing) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private Bitmap processImageWithAI(Bitmap bitmap, AIToolType toolType) {
        switch (toolType) {
            case UPSCALE:
                return tfliteHelper.upscaleImage(bitmap);
            case REMOVE_BACKGROUND:
                return tfliteHelper.removeBackground(bitmap);
            case ENHANCE:
                return tfliteHelper.enhanceImage(bitmap);
            case SHARPEN:
                return tfliteHelper.sharpenImage(bitmap);
            case CARTOON:
                return tfliteHelper.applyCartoonEffect(bitmap);
            default:
                return bitmap;
        }
    }

    private void showResult() {
        if (outputBitmap != null) {
            outputImageView.setImageBitmap(outputBitmap);
            outputImageView.setVisibility(View.VISIBLE);
            saveButton.setVisibility(View.VISIBLE);
            shareButton.setVisibility(View.VISIBLE);
            
            // Animate result appearance
            outputImageView.setAlpha(0f);
            outputImageView.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
        }
    }

    private void saveResult() {
        if (outputBitmap == null) {
            return;
        }
        
        // Show interstitial ad before saving
        if (adManager != null) {
            adManager.showInterstitialAdIfConnected(this, this::performSave);
        } else {
            performSave();
        }
    }

    private void performSave() {
        new Thread(() -> {
            try {
                Uri savedUri = FileManager.saveImageToGallery(this, outputBitmap, currentToolName.toLowerCase());
                
                runOnUiThread(() -> {
                    if (savedUri != null) {
                        updateStatus(getString(R.string.image_saved));
                        showSaveShareOptions(savedUri);
                    } else {
                        updateStatus(getString(R.string.save_failed));
                        Toast.makeText(this, getString(R.string.error_saving), Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    updateStatus(getString(R.string.save_failed));
                    Toast.makeText(this, getString(R.string.error_saving) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void shareResult() {
        if (outputBitmap == null) {
            return;
        }
        
        // Share bitmap directly
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        
        // Save bitmap to cache and share
        try {
            java.io.File cacheFile = new java.io.File(getCacheDir(), "share_image.jpg");
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(cacheFile);
            outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.close();
            
            Uri imageUri = androidx.core.content.FileProvider.getUriForFile(
                this, 
                getApplicationContext().getPackageName() + ".fileprovider", 
                cacheFile
            );
            
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message) + " " + currentToolName);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_image)));
            
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_sharing), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSaveShareOptions(Uri savedUri) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.image_saved))
            .setMessage(getString(R.string.image_saved_success))
            .setPositiveButton(getString(R.string.open_file), (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(savedUri, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            })
            .setNegativeButton(getString(R.string.share), (dialog, which) -> shareResult())
            .setNeutralButton(getString(R.string.ok), null)
            .show();
    }

    private void updateStatus(String status) {
        statusTextView.setText(status);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        processButton.setEnabled(!show);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            
            try {
                inputBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                inputImageView.setImageBitmap(inputBitmap);
                processButton.setEnabled(true);
                updateStatus(getString(R.string.image_selected));
                
                // Hide previous result
                outputImageView.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);
                shareButton.setVisibility(View.GONE);
                
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tfliteHelper != null) {
            tfliteHelper.cleanup();
        }
        if (adManager != null) {
            adManager.destroy();
        }
        
        // Clean up bitmaps
        if (inputBitmap != null && !inputBitmap.isRecycled()) {
            inputBitmap.recycle();
        }
        if (outputBitmap != null && !outputBitmap.isRecycled()) {
            outputBitmap.recycle();
        }
    }
}
