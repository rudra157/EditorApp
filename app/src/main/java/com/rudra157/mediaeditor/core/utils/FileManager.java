package com.rudra157.mediaeditor.core.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manages file operations for saving edited media to device storage
 */
public class FileManager {
    private static final String TAG = "FileManager";
    private static final String APP_NAME = "MediaEditor";
    private static final int IMAGE_QUALITY = 90;
    private static final int VIDEO_QUALITY = 70;

    /**
     * Saves a bitmap to the device's Pictures folder
     * @param bitmap The bitmap to save
     * @param filename The filename without extension
     * @return The URI of the saved file, or null if failed
     */
    @Nullable
    public static Uri saveImageToGallery(@NonNull Context context, @NonNull Bitmap bitmap, @NonNull String filename) {
        try {
            String displayName = filename + "_" + getCurrentTimestamp() + ".jpg";
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return saveImageToMediaStore(context, bitmap, displayName);
            } else {
                return saveImageLegacy(context, bitmap, displayName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves a video file to the device's Movies folder
     * @param videoUri The URI of the video to save
     * @param filename The filename without extension
     * @return The URI of the saved file, or null if failed
     */
    @Nullable
    public static Uri saveVideoToGallery(@NonNull Context context, @NonNull Uri videoUri, @NonNull String filename) {
        try {
            String displayName = filename + "_" + getCurrentTimestamp() + ".mp4";
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return saveVideoToMediaStore(context, videoUri, displayName);
            } else {
                return saveVideoLegacy(context, videoUri, displayName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving video: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates app-specific directories
     */
    public static void createAppDirectories(@NonNull Context context) {
        // Create app-specific directories in external storage
        File picturesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), APP_NAME);
        File moviesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), APP_NAME);
        
        if (!picturesDir.exists()) {
            picturesDir.mkdirs();
        }
        
        if (!moviesDir.exists()) {
            moviesDir.mkdirs();
        }
        
        Log.d(TAG, "App directories created/verified");
    }

    /**
     * Gets the app's pictures directory
     */
    @NonNull
    public static File getAppPicturesDirectory() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), APP_NAME);
    }

    /**
     * Gets the app's movies directory
     */
    @NonNull
    public static File getAppMoviesDirectory() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), APP_NAME);
    }

    /**
     * Saves image using MediaStore API (Android 10+)
     */
    @Nullable
    private static Uri saveImageToMediaStore(@NonNull Context context, @NonNull Bitmap bitmap, @NonNull String displayName) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + APP_NAME);

        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        
        if (imageUri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(imageUri)) {
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream);
                    Log.d(TAG, "Image saved to MediaStore: " + imageUri.toString());
                    return imageUri;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error writing to MediaStore: " + e.getMessage());
                resolver.delete(imageUri, null, null);
            }
        }
        
        return null;
    }

    /**
     * Saves image using legacy method (pre-Android 10)
     */
    @Nullable
    private static Uri saveImageLegacy(@NonNull Context context, @NonNull Bitmap bitmap, @NonNull String displayName) {
        File picturesDir = getAppPicturesDirectory();
        File imageFile = new File(picturesDir, displayName);
        
        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream);
            Log.d(TAG, "Image saved using legacy method: " + imageFile.getAbsolutePath());
            return Uri.fromFile(imageFile);
        } catch (IOException e) {
            Log.e(TAG, "Error saving image using legacy method: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves video using MediaStore API (Android 10+)
     */
    @Nullable
    private static Uri saveVideoToMediaStore(@NonNull Context context, @NonNull Uri sourceUri, @NonNull String displayName) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/" + APP_NAME);

        Uri videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        
        if (videoUri != null) {
            try (InputStream inputStream = resolver.openInputStream(sourceUri);
                 OutputStream outputStream = resolver.openOutputStream(videoUri)) {
                
                if (inputStream != null && outputStream != null) {
                    copyStream(inputStream, outputStream);
                    Log.d(TAG, "Video saved to MediaStore: " + videoUri.toString());
                    return videoUri;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error writing video to MediaStore: " + e.getMessage());
                resolver.delete(videoUri, null, null);
            }
        }
        
        return null;
    }

    /**
     * Saves video using legacy method (pre-Android 10)
     */
    @Nullable
    private static Uri saveVideoLegacy(@NonNull Context context, @NonNull Uri sourceUri, @NonNull String displayName) {
        File moviesDir = getAppMoviesDirectory();
        File videoFile = new File(moviesDir, displayName);
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(videoFile)) {
            
            if (inputStream != null) {
                copyStream(inputStream, outputStream);
                Log.d(TAG, "Video saved using legacy method: " + videoFile.getAbsolutePath());
                return Uri.fromFile(videoFile);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving video using legacy method: " + e.getMessage());
            return null;
        }
        
        return null;
    }

    /**
     * Copies data from input stream to output stream
     */
    private static void copyStream(@NonNull InputStream inputStream, @NonNull OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Gets current timestamp for unique filenames
     */
    @NonNull
    private static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Deletes a file from storage
     */
    public static boolean deleteFile(@NonNull Context context, @NonNull Uri fileUri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            int deleted = resolver.delete(fileUri, null, null);
            return deleted > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets file size in human readable format
     */
    @NonNull
    public static String getReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return String.format(Locale.getDefault(), "%.1f %s", 
            size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Checks if external storage is available for read/write
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
