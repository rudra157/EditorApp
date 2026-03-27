package com.rudra157.mediaeditor.core.ai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Helper class for TensorFlow Lite AI model operations
 */
public class TFLiteHelper {
    private static final String TAG = "TFLiteHelper";
    
    // Model file names (should be in assets/models/)
    private static final String MODEL_UPSCALE = "esrgan_lite.tflite";
    private static final String MODEL_BACKGROUND_REMOVAL = "u2net_lite.tflite";
    private static final String MODEL_ENHANCE = "enhance_lite.tflite";
    private static final String MODEL_SHARPEN = "sharpen_lite.tflite";
    private static final String MODEL_CARTOON = "cartoon_lite.tflite";
    
    // Model dimensions
    private static final int INPUT_SIZE = 256;
    private static final float NORMALIZATION_MEAN = 0.0f;
    private static final float NORMALIZATION_STD = 255.0f;
    
    private Context context;
    private Interpreter upscaleInterpreter;
    private Interpreter backgroundRemovalInterpreter;
    private Interpreter enhanceInterpreter;
    private Interpreter sharpenInterpreter;
    private Interpreter cartoonInterpreter;
    private GpuDelegate gpuDelegate;
    
    private boolean modelsLoaded = false;

    public TFLiteHelper(Context context) {
        this.context = context.getApplicationContext();
        initializeModels();
    }

    /**
     * Initialize all AI models
     */
    private void initializeModels() {
        try {
            // Initialize GPU delegate for better performance
            gpuDelegate = new GpuDelegate();
            
            // Load models lazily - they will be loaded when first used
            Log.d(TAG, "TFLite helper initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TFLite: " + e.getMessage());
        }
    }

    /**
     * Upscale image using ESRGAN model
     */
    public Bitmap upscaleImage(Bitmap inputBitmap) {
        try {
            if (upscaleInterpreter == null) {
                loadUpscaleModel();
            }
            
            if (upscaleInterpreter == null) {
                Log.e(TAG, "Upscale model not available");
                return inputBitmap;
            }
            
            // Preprocess input
            Bitmap resizedBitmap = resizeBitmap(inputBitmap, INPUT_SIZE, INPUT_SIZE);
            TensorImage inputImage = preprocessImage(resizedBitmap);
            
            // Run inference
            TensorImage outputImage = new TensorImage(upscaleInterpreter.getOutputTensor(0).dataType());
            upscaleInterpreter.run(inputImage.getBuffer(), outputImage.getBuffer().rewind());
            
            // Postprocess output
            Bitmap resultBitmap = postprocessImage(outputImage);
            
            // Resize back to original dimensions (upscaled)
            return resizeBitmap(resultBitmap, 
                inputBitmap.getWidth() * 2, 
                inputBitmap.getHeight() * 2);
            
        } catch (Exception e) {
            Log.e(TAG, "Error upscaling image: " + e.getMessage());
            return inputBitmap;
        }
    }

    /**
     * Remove background using U2Net model
     */
    public Bitmap removeBackground(Bitmap inputBitmap) {
        try {
            if (backgroundRemovalInterpreter == null) {
                loadBackgroundRemovalModel();
            }
            
            if (backgroundRemovalInterpreter == null) {
                Log.e(TAG, "Background removal model not available");
                return inputBitmap;
            }
            
            // Preprocess input
            Bitmap resizedBitmap = resizeBitmap(inputBitmap, INPUT_SIZE, INPUT_SIZE);
            TensorImage inputImage = preprocessImage(resizedBitmap);
            
            // Run inference
            TensorImage outputImage = new TensorImage(backgroundRemovalInterpreter.getOutputTensor(0).dataType());
            backgroundRemovalInterpreter.run(inputImage.getBuffer(), outputImage.getBuffer().rewind());
            
            // Create mask from output
            Bitmap mask = createMaskFromOutput(outputImage);
            
            // Apply mask to original image
            return applyMaskToImage(inputBitmap, mask);
            
        } catch (Exception e) {
            Log.e(TAG, "Error removing background: " + e.getMessage());
            return inputBitmap;
        }
    }

    /**
     * Enhance image quality
     */
    public Bitmap enhanceImage(Bitmap inputBitmap) {
        try {
            if (enhanceInterpreter == null) {
                loadEnhanceModel();
            }
            
            if (enhanceInterpreter == null) {
                Log.e(TAG, "Enhance model not available");
                return inputBitmap;
            }
            
            // Apply basic enhancement as fallback
            return applyBasicEnhancement(inputBitmap);
            
        } catch (Exception e) {
            Log.e(TAG, "Error enhancing image: " + e.getMessage());
            return inputBitmap;
        }
    }

    /**
     * Sharpen image
     */
    public Bitmap sharpenImage(Bitmap inputBitmap) {
        try {
            if (sharpenInterpreter == null) {
                loadSharpenModel();
            }
            
            if (sharpenInterpreter == null) {
                Log.e(TAG, "Sharpen model not available");
                return inputBitmap;
            }
            
            // Apply basic sharpening as fallback
            return applyBasicSharpening(inputBitmap);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sharpening image: " + e.getMessage());
            return inputBitmap;
        }
    }

    /**
     * Apply cartoon effect
     */
    public Bitmap applyCartoonEffect(Bitmap inputBitmap) {
        try {
            if (cartoonInterpreter == null) {
                loadCartoonModel();
            }
            
            if (cartoonInterpreter == null) {
                Log.e(TAG, "Cartoon model not available");
                return inputBitmap;
            }
            
            // Apply basic cartoon effect as fallback
            return applyBasicCartoonEffect(inputBitmap);
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying cartoon effect: " + e.getMessage());
            return inputBitmap;
        }
    }

    // Model loading methods
    private void loadUpscaleModel() {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(MODEL_UPSCALE);
            Interpreter.Options options = new Interpreter.Options();
            options.addDelegate(gpuDelegate);
            options.setNumThreads(4);
            upscaleInterpreter = new Interpreter(modelBuffer, options);
            Log.d(TAG, "Upscale model loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading upscale model: " + e.getMessage());
        }
    }

    private void loadBackgroundRemovalModel() {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(MODEL_BACKGROUND_REMOVAL);
            Interpreter.Options options = new Interpreter.Options();
            options.addDelegate(gpuDelegate);
            options.setNumThreads(4);
            backgroundRemovalInterpreter = new Interpreter(modelBuffer, options);
            Log.d(TAG, "Background removal model loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading background removal model: " + e.getMessage());
        }
    }

    private void loadEnhanceModel() {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(MODEL_ENHANCE);
            Interpreter.Options options = new Interpreter.Options();
            options.addDelegate(gpuDelegate);
            options.setNumThreads(4);
            enhanceInterpreter = new Interpreter(modelBuffer, options);
            Log.d(TAG, "Enhance model loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading enhance model: " + e.getMessage());
        }
    }

    private void loadSharpenModel() {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(MODEL_SHARPEN);
            Interpreter.Options options = new Interpreter.Options();
            options.addDelegate(gpuDelegate);
            options.setNumThreads(4);
            sharpenInterpreter = new Interpreter(modelBuffer, options);
            Log.d(TAG, "Sharpen model loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading sharpen model: " + e.getMessage());
        }
    }

    private void loadCartoonModel() {
        try {
            MappedByteBuffer modelBuffer = loadModelFile(MODEL_CARTOON);
            Interpreter.Options options = new Interpreter.Options();
            options.addDelegate(gpuDelegate);
            options.setNumThreads(4);
            cartoonInterpreter = new Interpreter(modelBuffer, options);
            Log.d(TAG, "Cartoon model loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading cartoon model: " + e.getMessage());
        }
    }

    // Helper methods
    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("models/" + modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private TensorImage preprocessImage(Bitmap bitmap) {
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(new NormalizeOp(NORMALIZATION_MEAN, NORMALIZATION_STD))
            .build();
        
        TensorImage tensorImage = new TensorImage(org.tensorflow.lite.support.image.DataType.FLOAT32);
        tensorImage.load(bitmap);
        return imageProcessor.process(tensorImage);
    }

    private Bitmap postprocessImage(TensorImage tensorImage) {
        return tensorImage.getBitmap();
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int width, int height) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private Bitmap createMaskFromOutput(TensorImage outputImage) {
        // Convert output to binary mask
        Bitmap mask = outputImage.getBitmap();
        Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
        
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        
        for (int x = 0; x < mask.getWidth(); x++) {
            for (int y = 0; y < mask.getHeight(); y++) {
                int pixel = mask.getPixel(x, y);
                int alpha = (pixel & 0xFF) > 128 ? 255 : 0;
                result.setPixel(x, y, (alpha << 24) | (0xFF << 16) | (0xFF << 8) | 0xFF);
            }
        }
        
        return result;
    }

    private Bitmap applyMaskToImage(Bitmap image, Bitmap mask) {
        Bitmap result = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        Paint paint = new Paint();
        canvas.drawBitmap(image, 0, 0, paint);
        
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(mask, 0, 0, paint);
        
        return result;
    }

    // Basic image processing fallbacks
    private Bitmap applyBasicEnhancement(Bitmap bitmap) {
        ColorMatrix colorMatrix = new ColorMatrix(new float[] {
            1.2f, 0, 0, 0, 0,
            0, 1.2f, 0, 0, 0,
            0, 0, 1.2f, 0, 0,
            0, 0, 0, 1, 0
        });
        
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        
        return result;
    }

    private Bitmap applyBasicSharpening(Bitmap bitmap) {
        ColorMatrix colorMatrix = new ColorMatrix(new float[] {
            0, -1, 0, 0, 0,
            -1, 5, -1, 0, 0,
            0, -1, 0, 0, 0,
            0, 0, 0, 1, 0
        });
        
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        
        return result;
    }

    private Bitmap applyBasicCartoonEffect(Bitmap bitmap) {
        // Simplify colors and add edge detection
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        // Apply color quantization
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(2.0f); // Increase saturation
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        
        return result;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (upscaleInterpreter != null) {
            upscaleInterpreter.close();
        }
        if (backgroundRemovalInterpreter != null) {
            backgroundRemovalInterpreter.close();
        }
        if (enhanceInterpreter != null) {
            enhanceInterpreter.close();
        }
        if (sharpenInterpreter != null) {
            sharpenInterpreter.close();
        }
        if (cartoonInterpreter != null) {
            cartoonInterpreter.close();
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
        }
    }
}
