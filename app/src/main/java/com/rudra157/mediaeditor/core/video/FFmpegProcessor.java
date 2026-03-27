package com.rudra157.mediaeditor.core.video;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Handles video processing operations using FFmpeg
 */
public class FFmpegProcessor {
    private static final String TAG = "FFmpegProcessor";
    private static final int TIMEOUT_SECONDS = 120;
    
    private Context context;

    public FFmpegProcessor(Context context) {
        this.context = context.getApplicationContext();
        initializeFFmpeg();
    }

    /**
     * Initialize FFmpeg
     */
    private void initializeFFmpeg() {
        try {
            FFmpegKitConfig.init(context);
            Log.d(TAG, "FFmpeg initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing FFmpeg: " + e.getMessage());
        }
    }

    /**
     * Trim video from start to end time
     * @param inputUri Input video URI
     * @param outputUri Output video URI
     * @param startTimeMs Start time in milliseconds
     * @param endTimeMs End time in milliseconds
     * @param callback Progress callback
     */
    public void trimVideo(Uri inputUri, Uri outputUri, long startTimeMs, long endTimeMs, VideoProcessingCallback callback) {
        try {
            String inputPath = getPathFromUri(inputUri);
            String outputPath = getPathFromUri(outputUri);
            
            if (inputPath == null || outputPath == null) {
                callback.onError("Invalid file paths");
                return;
            }

            String startTime = formatTime(startTimeMs);
            String duration = formatTime(endTimeMs - startTimeMs);
            
            String command = String.format(
                "-i %s -ss %s -t %s -c:v libx264 -preset medium -crf 23 -c:a aac -b:a 128k %s",
                inputPath, startTime, duration, outputPath
            );

            executeFFmpegCommand(command, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error trimming video: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    /**
     * Cut video segment
     * @param inputUri Input video URI
     * @param outputUri Output video URI
     * @param startTimeMs Start time in milliseconds
     * @param endTimeMs End time in milliseconds
     * @param callback Progress callback
     */
    public void cutVideo(Uri inputUri, Uri outputUri, long startTimeMs, long endTimeMs, VideoProcessingCallback callback) {
        try {
            String inputPath = getPathFromUri(inputUri);
            String outputPath = getPathFromUri(outputUri);
            
            if (inputPath == null || outputPath == null) {
                callback.onError("Invalid file paths");
                return;
            }

            String startTime = formatTime(startTimeMs);
            String endTime = formatTime(endTimeMs);
            
            String command = String.format(
                "-i %s -ss %s -to %s -c:v libx264 -preset medium -crf 23 -c:a aac -b:a 128k %s",
                inputPath, startTime, endTime, outputPath
            );

            executeFFmpegCommand(command, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error cutting video: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    /**
     * Merge multiple videos
     * @param inputUris Array of input video URIs
     * @param outputUri Output video URI
     * @param callback Progress callback
     */
    public void mergeVideos(Uri[] inputUris, Uri outputUri, VideoProcessingCallback callback) {
        try {
            String outputPath = getPathFromUri(outputUri);
            
            if (outputPath == null) {
                callback.onError("Invalid output file path");
                return;
            }

            // Create temporary file list for concatenation
            File fileList = createFileList(inputUris);
            
            String command = String.format(
                "-f concat -safe 0 -i %s -c:v libx264 -preset medium -crf 23 -c:a aac -b:a 128k %s",
                fileList.getAbsolutePath(), outputPath
            );

            executeFFmpegCommand(command, callback);
            
            // Clean up temporary file
            fileList.delete();
            
        } catch (Exception e) {
            Log.e(TAG, "Error merging videos: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    /**
     * Add audio to video
     * @param videoUri Video URI
     * @param audioUri Audio URI
     * @param outputUri Output video URI
     * @param callback Progress callback
     */
    public void addAudioToVideo(Uri videoUri, Uri audioUri, Uri outputUri, VideoProcessingCallback callback) {
        try {
            String videoPath = getPathFromUri(videoUri);
            String audioPath = getPathFromUri(audioUri);
            String outputPath = getPathFromUri(outputUri);
            
            if (videoPath == null || audioPath == null || outputPath == null) {
                callback.onError("Invalid file paths");
                return;
            }

            String command = String.format(
                "-i %s -i %s -c:v copy -c:a aac -b:a 128k -map 0:v:0 -map 1:a:0 %s",
                videoPath, audioPath, outputPath
            );

            executeFFmpegCommand(command, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding audio to video: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    /**
     * Extract audio from video
     * @param videoUri Video URI
     * @param outputUri Output audio URI
     * @param callback Progress callback
     */
    public void extractAudio(Uri videoUri, Uri outputUri, VideoProcessingCallback callback) {
        try {
            String videoPath = getPathFromUri(videoUri);
            String outputPath = getPathFromUri(outputUri);
            
            if (videoPath == null || outputPath == null) {
                callback.onError("Invalid file paths");
                return;
            }

            String command = String.format(
                "-i %s -vn -c:a aac -b:a 128k %s",
                videoPath, outputPath
            );

            executeFFmpegCommand(command, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting audio: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    /**
     * Get video duration in milliseconds
     * @param videoUri Video URI
     * @return Duration in milliseconds, or -1 if error
     */
    public long getVideoDuration(Uri videoUri) {
        try {
            String videoPath = getPathFromUri(videoUri);
            
            if (videoPath == null) {
                return -1;
            }

            CountDownLatch latch = new CountDownLatch(1);
            final long[] duration = {-1};

            FFmpegSession session = FFmpegKit.execute(String.format("-i %s", videoPath));
            
            session.setState(new SessionState() {
                @Override
                public void onChanged(FFmpegSession session) {
                    if (session.getState() == com.arthenica.ffmpegkit.SessionState.COMPLETED) {
                        String log = session.getAllLogsAsString();
                        duration[0] = parseDurationFromLog(log);
                        latch.countDown();
                    } else if (session.getState() == com.arthenica.ffmpegkit.SessionState.FAILED) {
                        latch.countDown();
                    }
                }
            });

            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return duration[0];
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting video duration: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Execute FFmpeg command with progress tracking
     */
    private void executeFFmpegCommand(String command, VideoProcessingCallback callback) {
        try {
            Log.d(TAG, "Executing FFmpeg command: " + command);
            
            callback.onProgress(0);
            
            FFmpegSession session = FFmpegKit.execute(command);
            
            session.setState(new SessionState() {
                @Override
                public void onChanged(FFmpegSession session) {
                    if (session.getState() == com.arthenica.ffmpegkit.SessionState.RUNNING) {
                        // Parse progress from logs
                        String log = session.getAllLogsAsString();
                        int progress = parseProgressFromLog(log);
                        callback.onProgress(progress);
                    } else if (session.getState() == com.arthenica.ffmpegkit.SessionState.COMPLETED) {
                        ReturnCode returnCode = session.getReturnCode();
                        if (returnCode != null && returnCode.getValue() == ReturnCode.SUCCESS.getValue()) {
                            callback.onProgress(100);
                            callback.onSuccess();
                        } else {
                            callback.onError("FFmpeg failed with return code: " + returnCode);
                        }
                    } else if (session.getState() == com.arthenica.ffmpegkit.SessionState.FAILED) {
                        callback.onError("FFmpeg execution failed");
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error executing FFmpeg command: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }

    /**
     * Parse progress from FFmpeg log
     */
    private int parseProgressFromLog(String log) {
        try {
            // Look for "time=" in the log to extract progress
            String[] lines = log.split("\n");
            for (String line : lines) {
                if (line.contains("time=")) {
                    int timeIndex = line.indexOf("time=");
                    if (timeIndex != -1) {
                        String timeStr = line.substring(timeIndex + 5).trim();
                        // Parse time and calculate progress (simplified)
                        return Math.min(99, (int) (Math.random() * 100)); // Placeholder
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing progress: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Parse duration from FFmpeg log
     */
    private long parseDurationFromLog(String log) {
        try {
            // Look for "Duration:" in the log
            String[] lines = log.split("\n");
            for (String line : lines) {
                if (line.contains("Duration:")) {
                    int durationIndex = line.indexOf("Duration:");
                    if (durationIndex != -1) {
                        String durationStr = line.substring(durationIndex + 10).trim();
                        // Parse duration string (e.g., "00:01:23.45")
                        return parseTimeString(durationStr);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing duration: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Parse time string to milliseconds
     */
    private long parseTimeString(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length >= 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                double seconds = Double.parseDouble(parts[2].split(",")[0]);
                return (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing time string: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Format milliseconds to time string
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Get file path from URI
     */
    private String getPathFromUri(Uri uri) {
        try {
            if (uri.getScheme().equals("file")) {
                return uri.getPath();
            } else {
                // For content URIs, you might need to copy the file to a temporary location
                // This is a simplified implementation
                return uri.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting path from URI: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create file list for concatenation
     */
    private File createFileList(Uri[] inputUris) throws Exception {
        File fileList = new File(context.getCacheDir(), "filelist.txt");
        
        StringBuilder content = new StringBuilder();
        for (Uri uri : inputUris) {
            String path = getPathFromUri(uri);
            if (path != null) {
                content.append("file '").append(path).append("'\n");
            }
        }
        
        try (java.io.FileWriter writer = new java.io.FileWriter(fileList)) {
            writer.write(content.toString());
        }
        
        return fileList;
    }

    /**
     * Callback interface for video processing
     */
    public interface VideoProcessingCallback {
        void onProgress(int progress);
        void onSuccess();
        void onError(String error);
    }
}
