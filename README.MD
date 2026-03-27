# Media Editor - AI-Powered Android App

A complete Android application for online AI-powered media editing with modern UI, scalable architecture, and production-ready Gradle setup.

## 🚀 Features

### Core Features
- **Image Editor**: Crop, filters, brightness/contrast/saturation adjustments, text overlay, drawing tools
- **Video Editor**: Trim, cut, merge videos, add audio tracks
- **AI Tools**: Offline AI-powered image enhancement (upscale, background removal, enhance, sharpen, cartoon effect)
- **No Login Required**: Start editing immediately
- **Offline Processing**: All image/video editing and AI processing works offline
- **AdMob Integration**: Monetize with banner and interstitial ads

### Technical Features
- **Architecture**: MVVM (Model-View-ViewModel)
- **Language**: Java (Android Studio)
- **UI**: Material Design 3 with dark/light mode support
- **AI Models**: TensorFlow Lite (.tflite) stored locally in assets
- **Video Processing**: FFmpeg mobile library
- **File Management**: Direct export to device storage (Pictures/Movies folders)

## 📱 Screens

1. **Splash Screen**: Animated logo with model loading
2. **Home Screen**: Grid menu with Image Editor, Video Editor, AI Tools
3. **AI Tools Screen**: Grid layout of AI-powered tools
4. **Image Editor**: Full-screen canvas with editing tools
5. **Video Editor**: Video preview with timeline and editing tools

## 🛠 Tech Stack

- **Android SDK**: Latest stable (API 34)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Build Tools**: Gradle 8.1.2
- **Architecture**: MVVM with LiveData and ViewModel
- **UI Framework**: Material Design 3
- **AI Processing**: TensorFlow Lite
- **Video Processing**: FFmpeg Kit
- **Monetization**: Google AdMob
- **Image Loading**: Glide
- **Permissions**: Dexter

## 📦 Dependencies

### AndroidX & Material
- AndroidX Core Libraries
- Material Design Components
- Lifecycle Components (ViewModel, LiveData)
- Navigation Component

### AI & Processing
- TensorFlow Lite (with GPU support)
- FFmpeg Kit (full version)

### Media & UI
- Glide (Image loading)
- Image Picker
- Video Picker
- SpinKit (Loading animations)
- Photo Filters library

### Monetization
- Google Mobile Ads (AdMob)

### Utilities
- Dexter (Permissions)
- Multidex (for large apps)

## 🏗 Project Structure

```
app/
├── src/main/
│   ├── java/com/rudra157/mediaeditor/
│   │   ├── ui/                    # Activities and UI components
│   │   │   ├── SplashActivity.java
│   │   │   ├── MainActivity.java
│   │   │   ├── ImageEditorActivity.java
│   │   │   ├── VideoEditorActivity.java
│   │   │   ├── AIToolsActivity.java
│   │   │   ├── AIProcessActivity.java
│   │   │   ├── SettingsActivity.java
│   │   │   └── adapters/
│   │   │       └── AIToolAdapter.java
│   │   ├── core/                  # Core functionality
│   │   │   ├── ai/
│   │   │   │   └── TFLiteHelper.java
│   │   │   ├── ads/
│   │   │   │   └── AdManager.java
│   │   │   ├── video/
│   │   │   │   └── FFmpegProcessor.java
│   │   │   └── utils/
│   │   │       └── FileManager.java
│   │   └── MediaEditorApplication.java
│   ├── res/                       # Resources
│   │   ├── layout/                # XML layouts
│   │   ├── values/                # Strings, colors, themes
│   │   ├── menu/                  # Menu resources
│   │   └── xml/                   # XML configurations
│   └── AndroidManifest.xml
├── build.gradle                   # App-level build configuration
└── proguard-rules.pro           # ProGuard configuration
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or newer
- JDK 8 or higher
- Android SDK (API 24-34)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd EditorApp
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the `EditorApp` directory

3. **Configure AdMob (Optional)**
   - Replace test AdMob IDs in `AdManager.java` with your actual IDs
   - Update `AndroidManifest.xml` with your AdMob Application ID

4. **Add AI Models (Optional)**
   - Place your `.tflite` models in `app/src/main/assets/models/`
   - Supported models: `esrgan_lite.tflite`, `u2net_lite.tflite`, etc.

5. **Build and Run**
   - Connect Android device or start emulator
   - Click Run button in Android Studio

## 🔧 Build Configuration

### APK Build (for testing)
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing configuration)
./gradlew assembleRelease
```

### AAB Build (for Play Store)
```bash
# Debug AAB
./gradlew bundleDebug

# Release AAB (requires signing configuration)
./gradlew bundleRelease
```

### Signing Configuration

For release builds, configure signing in `gradle.properties`:

```properties
MYAPP_RELEASE_STORE_FILE=path/to/your/keystore.jks
MYAPP_RELEASE_STORE_PASSWORD=your_store_password
MYAPP_RELEASE_KEY_ALIAS=your_key_alias
MYAPP_RELEASE_KEY_PASSWORD=your_key_password
```

Or use Android Studio:
1. Build → Generate Signed Bundle/APK
2. Select Android App Bundle
3. Create or choose keystore
4. Follow the wizard

## 📱 Permissions

The app requires the following permissions:

- `INTERNET` - For AdMob ads only
- `ACCESS_NETWORK_STATE` - Check internet connectivity for ads
- `READ_EXTERNAL_STORAGE` - Read media files for editing
- `WRITE_EXTERNAL_STORAGE` - Save edited media (API < 29)
- `CAMERA` (optional) - Capture photos/videos
- `RECORD_AUDIO` (optional) - Record audio for videos

## 🤖 AI Models

The app supports offline AI processing using TensorFlow Lite models:

### Supported Models
- **ESRGAN Lite**: Image upscaling
- **U2Net Lite**: Background removal
- **Enhancement Lite**: Image enhancement
- **Sharpen Lite**: Image sharpening
- **Cartoon Lite**: Cartoon effect

### Model Placement
Place `.tflite` models in `app/src/main/assets/models/`:
```
assets/models/
├── esrgan_lite.tflite
├── u2net_lite.tflite
├── enhance_lite.tflite
├── sharpen_lite.tflite
└── cartoon_lite.tflite
```

### Model Optimization
- Models are loaded lazily to improve startup performance
- GPU delegate is used for faster inference
- Background threads prevent UI blocking

## 💰 AdMob Integration

### Ad Types
- **Banner Ads**: Home Screen, AI Tools Screen
- **Interstitial Ads**: Before AI tool usage, before export

### Test IDs (replace with production IDs)
```java
// Banner Ad Unit ID
"ca-app-pub-3940256099942544/6300978111"

// Interstitial Ad Unit ID  
"ca-app-pub-3940256099942544/1033173712"

// Application ID
"ca-app-pub-3940256099942544~3347511713"
```

## 📤 File Export

Edited media is saved directly to device storage:

- **Images**: `/Pictures/MediaEditor/`
- **Videos**: `/Movies/MediaEditor/`

Files are saved with timestamps to avoid overwriting:
- `edited_image_20231227_143022.jpg`
- `edited_video_20231227_143022.mp4`

## 🔒 Security & Privacy

- No user data is collected or stored
- No backend servers required
- All processing happens locally on device
- Internet is only used for AdMob ads
- Files are saved to standard Android directories

## 🐛 Troubleshooting

### Common Issues

1. **Build Errors**
   - Update Android Studio and SDK
   - Clean and rebuild project
   - Check `gradle.properties` for signing configuration

2. **AI Models Not Working**
   - Verify models are in `assets/models/`
   - Check model file names match code
   - Ensure models are compatible with TensorFlow Lite

3. **AdMob Issues**
   - Check internet connectivity
   - Verify AdMob configuration
   - Test with test IDs first

4. **Video Processing Issues**
   - Ensure sufficient storage space
   - Check video file format compatibility
   - Verify FFmpeg library is properly initialized

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📞 Support

For issues and questions:
- Create an issue on GitHub
- Check the troubleshooting section
- Review the documentation

---

**Media Editor v1.0** - Complete AI-powered media editing solution for Android
