# AI Models Directory

This directory contains TensorFlow Lite (.tflite) models for offline AI processing.

## 📁 Required Models

Place the following models in this directory:

### Image Processing Models

1. **esrgan_lite.tflite**
   - **Purpose**: Image upscaling (ESRGAN lightweight)
   - **Input**: RGB image (256x256)
   - **Output**: Upscaled RGB image (512x512)
   - **Size**: ~2-5 MB

2. **u2net_lite.tflite**
   - **Purpose**: Background removal (U2Net lightweight)
   - **Input**: RGB image (256x256)
   - **Output**: Binary mask (256x256)
   - **Size**: ~1-3 MB

3. **enhance_lite.tflite**
   - **Purpose**: Image enhancement
   - **Input**: RGB image (256x256)
   - **Output**: Enhanced RGB image (256x256)
   - **Size**: ~1-2 MB

4. **sharpen_lite.tflite**
   - **Purpose**: Image sharpening
   - **Input**: RGB image (256x256)
   - **Output**: Sharpened RGB image (256x256)
   - **Size**: ~500 KB - 1 MB

5. **cartoon_lite.tflite**
   - **Purpose**: Cartoon effect
   - **Input**: RGB image (256x256)
   - **Output**: Cartoon-style RGB image (256x256)
   - **Size**: ~1-2 MB

## 🔄 Model Loading

Models are loaded lazily in the app:

1. **Initialization**: `TFLiteHelper` is initialized on app startup
2. **Lazy Loading**: Models are loaded when first used
3. **Memory Management**: Models are cached after first load
4. **GPU Acceleration**: GPU delegate is used when available

## 📊 Model Specifications

### Input Format
- **Type**: FLOAT32
- **Shape**: [1, 256, 256, 3] (batch, height, width, channels)
- **Normalization**: Mean=0.0, Std=255.0

### Output Format
- **Type**: Varies by model
- **Shape**: Typically [1, 256, 256, 3] or [1, 256, 256, 1]
- **Post-processing**: Applied in Java code

## 🚀 Performance Optimization

### Model Optimization
- **Quantization**: Use INT8 quantization for smaller models
- **Pruning**: Remove unnecessary weights
- **Knowledge Distillation**: Create smaller student models

### Runtime Optimization
- **GPU Delegate**: Use mobile GPU for faster inference
- **NNAPI Delegate**: Use Android Neural Networks API
- **Thread Count**: Configure optimal thread count (4 threads)
- **Memory Management**: Reuse tensors and buffers

## 📥 Model Sources

### Recommended Sources
1. **TensorFlow Hub**: Pre-trained models
2. **Hugging Face**: Community models
3. **Custom Training**: Train your own models

### Conversion to TFLite
```python
# Example conversion script
import tensorflow as tf

# Load model
model = tf.keras.models.load_model('model.h5')

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]

tflite_model = converter.convert()

# Save model
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
```

## 🔧 Model Testing

### Testing Framework
```java
// Test model inference
TFLiteHelper helper = new TFLiteHelper(context);
Bitmap testImage = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);
Bitmap result = helper.upscaleImage(testImage);

// Verify output
assert result != null;
assert result.getWidth() > testImage.getWidth();
```

### Performance Testing
- **Inference Time**: Should be < 1 second per image
- **Memory Usage**: Monitor with Android Profiler
- **Accuracy**: Compare with ground truth results

## 📝 Model Integration

### Adding New Models

1. **Add model file** to this directory
2. **Update `TFLiteHelper.java`**:
   ```java
   private static final String NEW_MODEL = "new_model.tflite";
   
   private void loadNewModel() {
       try {
           MappedByteBuffer modelBuffer = loadModelFile(NEW_MODEL);
           Interpreter.Options options = new Interpreter.Options();
           options.addDelegate(gpuDelegate);
           newModelInterpreter = new Interpreter(modelBuffer, options);
       } catch (Exception e) {
           Log.e(TAG, "Error loading new model: " + e.getMessage());
       }
   }
   ```

3. **Add processing method**:
   ```java
   public Bitmap processWithNewModel(Bitmap inputBitmap) {
       // Implementation
   }
   ```

4. **Update UI** to include new tool option

## 🐛 Troubleshooting

### Common Issues

1. **Model Not Found**
   - Check file name matches code exactly
   - Verify model is in correct directory
   - Clean and rebuild project

2. **Out of Memory**
   - Reduce input image size
   - Use quantized models
   - Increase heap size

3. **Slow Inference**
   - Enable GPU delegate
   - Optimize model size
   - Use background threads

4. **Poor Results**
   - Check model input preprocessing
   - Verify output postprocessing
   - Test with different images

## 📚 References

- **TensorFlow Lite Documentation**: [tensorflow.org/lite](https://tensorflow.org/lite)
- **Model Optimization Toolkit**: [tensorflow.org/model_optimization](https://tensorflow.org/model_optimization)
- **Android ML Kit**: [developers.google.com/ml-kit](https://developers.google.com/ml-kit)

---

**Note**: The app includes fallback implementations when models are not available. The app will still function without AI models, just without AI features.
