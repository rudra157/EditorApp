package com.rudra157.mediaeditor.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.github.AbedElazizN.video_picker.VideoPicker;
import com.rudra157.mediaeditor.R;
import com.rudra157.mediaeditor.core.ads.AdManager;
import com.rudra157.mediaeditor.core.utils.FileManager;
import com.rudra157.mediaeditor.core.video.FFmpegProcessor;

import java.io.File;
import java.util.ArrayList;

/**
 * Video editor activity with trim, cut, merge, and audio tools
 */
public class VideoEditorActivity extends AppCompatActivity {
    private static final int VIDEO_PICKER_REQUEST = 1001;
    private static final int AUDIO_PICKER_REQUEST = 1002;
    
    // Views
    private ImageView videoThumbnail;
    private SeekBar timelineSeekBar;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private LinearLayout toolsContainer;
    private LinearLayout trimControlsContainer;
    private Toolbar toolbar;
    private ProgressBar processingProgressBar;
    private TextView statusTextView;
    
    // Core components
    private FFmpegProcessor ffmpegProcessor;
    private AdManager adManager;
    
    // Video data
    private Uri inputVideoUri;
    private Uri outputVideoUri;
    private long videoDuration = -1;
    private long currentTime = 0;
    private boolean isProcessing = false;
    
    // Editing state
    private VideoTool currentTool = VideoTool.NONE;
    private long trimStartTime = 0;
    private long trimEndTime = 0;
    private ArrayList<Uri> selectedVideos = new ArrayList<>();
    private Uri selectedAudioUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_editor);
        
        initViews();
        setupToolbar();
        setupTimeline();
        loadBannerAd();
        selectInitialVideo();
    }

    private void initViews() {
        videoThumbnail = findViewById(R.id.videoThumbnail);
        timelineSeekBar = findViewById(R.id.timelineSeekBar);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);
        toolsContainer = findViewById(R.id.toolsContainer);
        trimControlsContainer = findViewById(R.id.trimControlsContainer);
        toolbar = findViewById(R.id.toolbar);
        processingProgressBar = findViewById(R.id.processingProgressBar);
        statusTextView = findViewById(R.id.statusTextView);
        
        ffmpegProcessor = new FFmpegProcessor(this);
        adManager = AdManager.getInstance(this);
        
        // Initially hide processing UI
        processingProgressBar.setVisibility(View.GONE);
        trimControlsContainer.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.video_editor);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupTimeline() {
        timelineSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && videoDuration > 0) {
                    currentTime = (long) (progress * videoDuration / 100.0);
                    updateTimeDisplay();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadBannerAd() {
        LinearLayout bannerAdContainer = findViewById(R.id.bannerAdContainer);
        if (adManager != null) {
            adManager.loadBannerAd(bannerAdContainer);
        }
    }

    private void selectInitialVideo() {
        VideoPicker.create(this)
            .show()
            .start(VIDEO_PICKER_REQUEST);
    }

    public void onToolClick(View view) {
        int id = view.getId();
        
        // Hide all control containers initially
        trimControlsContainer.setVisibility(View.GONE);
        
        if (id == R.id.trimTool) {
            currentTool = VideoTool.TRIM;
            trimControlsContainer.setVisibility(View.VISIBLE);
            setupTrimControls();
        } else if (id == R.id.cutTool) {
            currentTool = VideoTool.CUT;
            showCutDialog();
        } else if (id == R.id.mergeTool) {
            currentTool = VideoTool.MERGE;
            showMergeDialog();
        } else if (id == R.id.addAudioTool) {
            currentTool = VideoTool.ADD_AUDIO;
            selectAudioFile();
        }
    }

    private void setupTrimControls() {
        Button trimStartButton = findViewById(R.id.trimStartButton);
        Button trimEndButton = findViewById(R.id.trimEndButton);
        Button applyTrimButton = findViewById(R.id.applyTrimButton);
        
        trimStartButton.setOnClickListener(v -> {
            trimStartTime = currentTime;
            Toast.makeText(this, getString(R.string.trim_start_set) + formatTime(trimStartTime), Toast.LENGTH_SHORT).show();
        });
        
        trimEndButton.setOnClickListener(v -> {
            trimEndTime = currentTime;
            Toast.makeText(this, getString(R.string.trim_end_set) + formatTime(trimEndTime), Toast.LENGTH_SHORT).show();
        });
        
        applyTrimButton.setOnClickListener(v -> {
            if (trimStartTime < trimEndTime) {
                performTrim();
            } else {
                Toast.makeText(this, R.string.invalid_trim_range, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCutDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_cut_video, null);
        
        SeekBar cutStartSeekBar = dialogView.findViewById(R.id.cutStartSeekBar);
        SeekBar cutEndSeekBar = dialogView.findViewById(R.id.cutEndSeekBar);
        TextView cutStartTextView = dialogView.findViewById(R.id.cutStartTextView);
        TextView cutEndTextView = dialogView.findViewById(R.id.cutEndTextView);
        
        cutStartSeekBar.setMax(100);
        cutEndSeekBar.setMax(100);
        cutEndSeekBar.setProgress(100);
        
        cutStartSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    long time = (long) (progress * videoDuration / 100.0);
                    cutStartTextView.setText(formatTime(time));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        cutEndSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    long time = (long) (progress * videoDuration / 100.0);
                    cutEndTextView.setText(formatTime(time));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        builder.setTitle(R.string.cut_video)
            .setView(dialogView)
            .setPositiveButton(R.string.cut, (dialog, which) -> {
                long startTime = (long) (cutStartSeekBar.getProgress() * videoDuration / 100.0);
                long endTime = (long) (cutEndSeekBar.getProgress() * videoDuration / 100.0);
                performCut(startTime, endTime);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showMergeDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.merge_videos)
            .setMessage(getString(R.string.merge_videos_description))
            .setPositiveButton(R.string.select_videos, (dialog, which) -> {
                selectMultipleVideos();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void selectMultipleVideos() {
        // For simplicity, we'll use the same video picker multiple times
        // In production, you might want to use a multi-select video picker
        selectedVideos.clear();
        selectedVideos.add(inputVideoUri);
        
        Toast.makeText(this, getString(R.string.first_video_selected) + "\n" + getString(R.string.select_more_videos), Toast.LENGTH_LONG).show();
        
        // Select second video
        VideoPicker.create(this)
            .show()
            .start(VIDEO_PICKER_REQUEST + 1);
    }

    private void selectAudioFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, AUDIO_PICKER_REQUEST);
    }

    private void performTrim() {
        if (inputVideoUri == null || isProcessing) return;
        
        File outputFile = new File(getCacheDir(), "trimmed_video.mp4");
        outputVideoUri = Uri.fromFile(outputFile);
        
        showProcessing(true);
        
        ffmpegProcessor.trimVideo(inputVideoUri, outputVideoUri, trimStartTime, trimEndTime, 
            new FFmpegProcessor.VideoProcessingCallback() {
                @Override
                public void onProgress(int progress) {
                    runOnUiThread(() -> updateProgress(progress));
                }

                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        showProcessing(false);
                        inputVideoUri = outputVideoUri;
                        Toast.makeText(VideoEditorActivity.this, R.string.trim_complete, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showProcessing(false);
                        Toast.makeText(VideoEditorActivity.this, getString(R.string.trim_failed) + ": " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    private void performCut(long startTime, long endTime) {
        if (inputVideoUri == null || isProcessing) return;
        
        File outputFile = new File(getCacheDir(), "cut_video.mp4");
        outputVideoUri = Uri.fromFile(outputFile);
        
        showProcessing(true);
        
        ffmpegProcessor.cutVideo(inputVideoUri, outputVideoUri, startTime, endTime,
            new FFmpegProcessor.VideoProcessingCallback() {
                @Override
                public void onProgress(int progress) {
                    runOnUiThread(() -> updateProgress(progress));
                }

                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        showProcessing(false);
                        inputVideoUri = outputVideoUri;
                        Toast.makeText(VideoEditorActivity.this, R.string.cut_complete, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showProcessing(false);
                        Toast.makeText(VideoEditorActivity.this, getString(R.string.cut_failed) + ": " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    private void performMerge() {
        if (selectedVideos.size() < 2 || isProcessing) return;
        
        File outputFile = new File(getCacheDir(), "merged_video.mp4");
        outputVideoUri = Uri.fromFile(outputFile);
        
        showProcessing(true);
        
        Uri[] videoUris = selectedVideos.toArray(new Uri[0]);
        ffmpegProcessor.mergeVideos(videoUris, outputVideoUri,
            new FFmpegProcessor.VideoProcessingCallback() {
                @Override
                public void onProgress(int progress) {
                    runOnUiThread(() -> updateProgress(progress));
                }

                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        showProcessing(false);
                        inputVideoUri = outputVideoUri;
                        Toast.makeText(VideoEditorActivity.this, R.string.merge_complete, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showProcessing(false);
                        Toast.makeText(VideoEditorActivity.this, getString(R.string.merge_failed) + ": " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    private void performAddAudio() {
        if (inputVideoUri == null || selectedAudioUri == null || isProcessing) return;
        
        File outputFile = new File(getCacheDir(), "video_with_audio.mp4");
        outputVideoUri = Uri.fromFile(outputFile);
        
        showProcessing(true);
        
        ffmpegProcessor.addAudioToVideo(inputVideoUri, selectedAudioUri, outputVideoUri,
            new FFmpegProcessor.VideoProcessingCallback() {
                @Override
                public void onProgress(int progress) {
                    runOnUiThread(() -> updateProgress(progress));
                }

                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        showProcessing(false);
                        inputVideoUri = outputVideoUri;
                        Toast.makeText(VideoEditorActivity.this, R.string.audio_added, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showProcessing(false);
                        Toast.makeText(VideoEditorActivity.this, getString(R.string.add_audio_failed) + ": " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    private void exportVideo() {
        if (inputVideoUri == null) {
            Toast.makeText(this, R.string.no_video_to_export, Toast.LENGTH_SHORT).show();
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
                Uri savedUri = FileManager.saveVideoToGallery(this, inputVideoUri, "edited_video");
                
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
            .setMessage(R.string.video_exported_successfully)
            .setPositiveButton(R.string.open_file, (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(savedUri, "video/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            })
            .setNegativeButton(R.string.share, (dialog, which) -> shareVideo())
            .setNeutralButton(R.string.ok, null)
            .show();
    }

    private void shareVideo() {
        if (inputVideoUri == null) return;
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("video/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, inputVideoUri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_edited_video));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_video)));
    }

    private void showProcessing(boolean show) {
        isProcessing = show;
        processingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            statusTextView.setText(R.string.processing);
        } else {
            statusTextView.setText("");
        }
    }

    private void updateProgress(int progress) {
        statusTextView.setText(getString(R.string.processing) + " " + progress + "%");
    }

    private void updateTimeDisplay() {
        currentTimeTextView.setText(formatTime(currentTime));
        totalTimeTextView.setText(formatTime(videoDuration));
    }

    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.video_editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_export) {
            exportVideo();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == VIDEO_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            inputVideoUri = data.getData();
            loadVideoInfo();
        } else if (requestCode == VIDEO_PICKER_REQUEST + 1 && resultCode == RESULT_OK && data != null) {
            // Second video for merging
            Uri secondVideoUri = data.getData();
            selectedVideos.add(secondVideoUri);
            performMerge();
        } else if (requestCode == AUDIO_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedAudioUri = data.getData();
            performAddAudio();
        } else if (resultCode == RESULT_CANCELED) {
            finish();
        }
    }

    private void loadVideoInfo() {
        if (inputVideoUri != null) {
            videoDuration = ffmpegProcessor.getVideoDuration(inputVideoUri);
            if (videoDuration > 0) {
                updateTimeDisplay();
                // Load video thumbnail
                loadVideoThumbnail();
            } else {
                Toast.makeText(this, R.string.error_loading_video, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void loadVideoThumbnail() {
        // Simple thumbnail loading - in production, use MediaMetadataRetriever
        try {
            videoThumbnail.setImageResource(R.drawable.ic_video_placeholder);
        } catch (Exception e) {
            videoThumbnail.setImageResource(R.drawable.ic_video_placeholder);
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
        
        // Clean up FFmpeg
        if (ffmpegProcessor != null) {
            FFmpegKit.shutdown();
        }
    }

    /**
     * Video tools enumeration
     */
    public enum VideoTool {
        NONE, TRIM, CUT, MERGE, ADD_AUDIO
    }
}
