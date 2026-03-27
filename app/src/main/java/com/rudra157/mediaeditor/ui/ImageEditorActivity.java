package com.rudra157.mediaeditor.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.rudra157.mediaeditor.R;
import com.rudra157.mediaeditor.core.ads.AdManager;
import com.rudra157.mediaeditor.core.utils.FileManager;

import java.util.Stack;

/**
 * Image editor activity with various editing tools
 */
public class ImageEditorActivity extends AppCompatActivity {
    private static final int IMAGE_PICKER_REQUEST = 1001;
    
    // Views
    private ImageView imagePreview;
    private LinearLayout toolsContainer;
    private LinearLayout adjustmentContainer;
    private LinearLayout bannerAdContainer;
    private Toolbar toolbar;
    private SeekBar brightnessSeekBar;
    private SeekBar contrastSeekBar;
    private SeekBar saturationSeekBar;
    
    // Core components
    private AdManager adManager;
    
    // Image data
    private Bitmap originalBitmap;
    private Bitmap currentBitmap;
    private Stack<Bitmap> undoStack;
    private Stack<Bitmap> redoStack;
    
    // Editing state
    private boolean isDrawing = false;
    private boolean isTextMode = false;
    private ImageTool currentTool = ImageTool.NONE;
    private Paint drawingPaint;
    private Canvas drawingCanvas;
    
    // Adjustment values
    private float brightness = 0;
    private float contrast = 1;
    private float saturation = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_editor);
        
        initViews();
        setupToolbar();
        setupDrawingTools();
        setupAdjustments();
        loadBannerAd();
        selectInitialImage();
    }

    private void initViews() {
        imagePreview = findViewById(R.id.imagePreview);
        toolsContainer = findViewById(R.id.toolsContainer);
        adjustmentContainer = findViewById(R.id.adjustmentContainer);
        bannerAdContainer = findViewById(R.id.bannerAdContainer);
        toolbar = findViewById(R.id.toolbar);
        
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar);
        contrastSeekBar = findViewById(R.id.contrastSeekBar);
        saturationSeekBar = findViewById(R.id.saturationSeekBar);
        
        adManager = AdManager.getInstance(this);
        undoStack = new Stack<>();
        redoStack = new Stack<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.image_editor);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupDrawingTools() {
        drawingPaint = new Paint();
        drawingPaint.setColor(Color.BLACK);
        drawingPaint.setStrokeWidth(5);
        drawingPaint.setAntiAlias(true);
        drawingPaint.setStyle(Paint.Style.STROKE);
    }

    private void setupAdjustments() {
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    brightness = (progress - 50) * 2; // -100 to +100
                    applyAdjustments();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        contrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    contrast = progress / 50.0f; // 0 to 2
                    applyAdjustments();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        saturationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    saturation = progress / 50.0f; // 0 to 2
                    applyAdjustments();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadBannerAd() {
        if (adManager != null) {
            adManager.loadBannerAd(bannerAdContainer);
        }
    }

    private void selectInitialImage() {
        ImagePicker.with(this)
            .galleryOnly()
            .compress(1024)
            .maxResultSize(1080, 1920)
            .start(IMAGE_PICKER_REQUEST);
    }

    public void onToolClick(View view) {
        int id = view.getId();
        
        // Hide adjustment container initially
        adjustmentContainer.setVisibility(View.GONE);
        
        if (id == R.id.cropTool) {
            currentTool = ImageTool.CROP;
            showCropDialog();
        } else if (id == R.id.filterTool) {
            currentTool = ImageTool.FILTER;
            showFilterDialog();
        } else if (id == R.id.adjustTool) {
            currentTool = ImageTool.ADJUST;
            adjustmentContainer.setVisibility(View.VISIBLE);
        } else if (id == R.id.textTool) {
            currentTool = ImageTool.TEXT;
            isTextMode = !isTextMode;
            updateToolStates();
        } else if (id == R.id.drawTool) {
            currentTool = ImageTool.DRAW;
            isDrawing = !isDrawing;
            updateToolStates();
        }
    }

    private void showCropDialog() {
        // Simple crop dialog with preset ratios
        String[] ratios = {"Free", "1:1", "4:3", "16:9", "9:16"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.crop_image)
            .setItems(ratios, (dialog, which) -> {
                applyCrop(which);
            })
            .show();
    }

    private void applyCrop(int ratioIndex) {
        if (originalBitmap == null) return;
        
        saveToUndoStack();
        
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        int newWidth = width;
        int newHeight = height;
        
        switch (ratioIndex) {
            case 1: // 1:1
                newWidth = Math.min(width, height);
                newHeight = newWidth;
                break;
            case 2: // 4:3
                if (width > height) {
                    newHeight = (int) (width * 3.0f / 4.0f);
                } else {
                    newWidth = (int) (height * 4.0f / 3.0f);
                }
                break;
            case 3: // 16:9
                if (width > height) {
                    newHeight = (int) (width * 9.0f / 16.0f);
                } else {
                    newWidth = (int) (height * 16.0f / 9.0f);
                }
                break;
            case 4: // 9:16
                if (width > height) {
                    newHeight = (int) (width * 16.0f / 9.0f);
                } else {
                    newWidth = (int) (height * 9.0f / 16.0f);
                }
                break;
        }
        
        Bitmap croppedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
        currentBitmap = croppedBitmap;
        imagePreview.setImageBitmap(currentBitmap);
    }

    private void showFilterDialog() {
        String[] filters = {"Original", "Grayscale", "Sepia", "Vintage", "Cool", "Warm"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.apply_filter)
            .setItems(filters, (dialog, which) -> {
                applyFilter(which);
            })
            .show();
    }

    private void applyFilter(int filterIndex) {
        if (originalBitmap == null) return;
        
        saveToUndoStack();
        
        switch (filterIndex) {
            case 0: // Original
                currentBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
                break;
            case 1: // Grayscale
                currentBitmap = applyGrayscaleFilter(originalBitmap);
                break;
            case 2: // Sepia
                currentBitmap = applySepiaFilter(originalBitmap);
                break;
            case 3: // Vintage
                currentBitmap = applyVintageFilter(originalBitmap);
                break;
            case 4: // Cool
                currentBitmap = applyCoolFilter(originalBitmap);
                break;
            case 5: // Warm
                currentBitmap = applyWarmFilter(originalBitmap);
                break;
        }
        
        imagePreview.setImageBitmap(currentBitmap);
    }

    private void applyAdjustments() {
        if (originalBitmap == null) return;
        
        saveToUndoStack();
        
        // Apply brightness, contrast, and saturation adjustments
        currentBitmap = applyColorAdjustments(originalBitmap, brightness, contrast, saturation);
        imagePreview.setImageBitmap(currentBitmap);
    }

    private void updateToolStates() {
        // Update tool button states based on current selection
        // This would update UI to show which tool is active
    }

    private void saveToUndoStack() {
        if (currentBitmap != null) {
            undoStack.push(currentBitmap.copy(currentBitmap.getConfig(), true));
            redoStack.clear(); // Clear redo stack when new action is performed
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(currentBitmap.copy(currentBitmap.getConfig(), true));
            currentBitmap = undoStack.pop();
            imagePreview.setImageBitmap(currentBitmap);
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(currentBitmap.copy(currentBitmap.getConfig(), true));
            currentBitmap = redoStack.pop();
            imagePreview.setImageBitmap(currentBitmap);
        }
    }

    private void exportImage() {
        if (currentBitmap == null) {
            Toast.makeText(this, R.string.no_image_to_export, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show interstitial ad before export
        if (adManager != null) {
            adManager.showInterstitialAdIfConnected(this, this::performExport);
        } else {
            performExport();
        }
    }

    private void performExport() {
        new Thread(() -> {
            try {
                Uri savedUri = FileManager.saveImageToGallery(this, currentBitmap, "edited_image");
                
                runOnUiThread(() -> {
                    if (savedUri != null) {
                        showExportSuccessDialog(savedUri);
                    } else {
                        Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showExportSuccessDialog(Uri savedUri) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.export_success)
            .setMessage(R.string.image_exported_successfully)
            .setPositiveButton(R.string.open_file, (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(savedUri, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            })
            .setNegativeButton(R.string.share, (dialog, which) -> shareImage())
            .setNeutralButton(R.string.ok, null)
            .show();
    }

    private void shareImage() {
        if (currentBitmap == null) return;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        
        try {
            java.io.File cacheFile = new java.io.File(getCacheDir(), "share_edited_image.jpg");
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(cacheFile);
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.close();
            
            Uri imageUri = androidx.core.content.FileProvider.getUriForFile(
                this, 
                getApplicationContext().getPackageName() + ".fileprovider", 
                cacheFile
            );
            
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_edited_image));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_image)));
            
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_sharing, Toast.LENGTH_SHORT).show();
        }
    }

    // Filter methods
    private Bitmap applyGrayscaleFilter(Bitmap bitmap) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        
        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
        colorMatrix.setSaturation(0);
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(colorMatrix));
        
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return result;
    }

    private Bitmap applySepiaFilter(Bitmap bitmap) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        
        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
        colorMatrix.set(new float[] {
            0.393f, 0.769f, 0.189f, 0, 0,
            0.349f, 0.686f, 0.168f, 0, 0,
            0.272f, 0.534f, 0.131f, 0, 0,
            0, 0, 0, 1, 0
        });
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(colorMatrix));
        
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return result;
    }

    private Bitmap applyVintageFilter(Bitmap bitmap) {
        // Apply sepia then reduce brightness
        Bitmap sepia = applySepiaFilter(bitmap);
        return applyColorAdjustments(sepia, -20, 0.9f, 0.8f);
    }

    private Bitmap applyCoolFilter(Bitmap bitmap) {
        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
        colorMatrix.set(new float[] {
            0.8f, 0, 0, 0, 20,
            0, 0.8f, 0, 0, 20,
            0, 0, 1.2f, 0, 0,
            0, 0, 0, 1, 0
        });
        
        return applyColorMatrix(bitmap, colorMatrix);
    }

    private Bitmap applyWarmFilter(Bitmap bitmap) {
        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
        colorMatrix.set(new float[] {
            1.2f, 0, 0, 0, 0,
            0, 1.1f, 0, 0, 0,
            0, 0, 0.8f, 0, 20,
            0, 0, 0, 1, 0
        });
        
        return applyColorMatrix(bitmap, colorMatrix);
    }

    private Bitmap applyColorAdjustments(Bitmap bitmap, float brightness, float contrast, float saturation) {
        android.graphics.ColorMatrix colorMatrix = new android.graphics.ColorMatrix();
        
        // Apply brightness
        colorMatrix.postTranslate(brightness, brightness, brightness, 0);
        
        // Apply contrast
        float[] contrastArray = new float[] {
            contrast, 0, 0, 0, 0,
            0, contrast, 0, 0, 0,
            0, 0, contrast, 0, 0,
            0, 0, 0, 1, 0
        };
        android.graphics.ColorMatrix contrastMatrix = new android.graphics.ColorMatrix(contrastArray);
        colorMatrix.postConcat(contrastMatrix);
        
        // Apply saturation
        colorMatrix.setSaturation(saturation);
        
        return applyColorMatrix(bitmap, colorMatrix);
    }

    private Bitmap applyColorMatrix(Bitmap bitmap, android.graphics.ColorMatrix colorMatrix) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_undo) {
            undo();
            return true;
        } else if (id == R.id.action_redo) {
            redo();
            return true;
        } else if (id == R.id.action_download) {
            exportImage();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            
            try {
                originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                currentBitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
                imagePreview.setImageBitmap(currentBitmap);
                
                // Clear undo/redo stacks
                undoStack.clear();
                redoStack.clear();
                
            } catch (Exception e) {
                Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (resultCode == RESULT_CANCELED) {
            finish();
        }
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
        
        // Clean up bitmaps
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
        }
        
        // Clear undo/redo stacks
        while (!undoStack.isEmpty()) {
            Bitmap bitmap = undoStack.pop();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        while (!redoStack.isEmpty()) {
            Bitmap bitmap = redoStack.pop();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /**
     * Image tools enumeration
     */
    public enum ImageTool {
        NONE, CROP, FILTER, ADJUST, TEXT, DRAW
    }
}
