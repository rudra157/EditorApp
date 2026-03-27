# Build Instructions - Media Editor App

This guide provides step-by-step instructions for building the Media Editor Android app.

## 📋 Prerequisites

### Required Software
- **Android Studio**: Hedgehog | 2023.1.1 or newer
- **JDK**: Java 8 or higher (Android Studio includes JDK)
- **Android SDK**: API levels 24-34
- **Git**: For cloning the repository

### Recommended Hardware
- **RAM**: 8GB or more (for smooth Android Studio performance)
- **Storage**: 10GB free space (for SDK and project files)
- **CPU**: Modern multi-core processor

## 🚀 Quick Start

### 1. Setup Development Environment

1. **Install Android Studio**
   - Download from [developer.android.com](https://developer.android.com/studio)
   - Run installer and follow setup wizard
   - Install required SDK platforms (API 24-34)

2. **Configure Android Studio**
   - Open Android Studio
   - Go to File → Settings → Appearance & Behavior → System Settings → Android SDK
   - Verify SDK platforms are installed
   - Update Android SDK Build Tools if needed

### 2. Clone and Open Project

```bash
# Clone the repository
git clone <repository-url>
cd EditorApp

# Open in Android Studio
# Option 1: Command line
studio .

# Option 2: GUI
# File → Open → Navigate to EditorApp folder
```

### 3. Initial Build

1. **Sync Project**
   - Android Studio will automatically sync Gradle
   - Wait for sync to complete (may take several minutes)

2. **Resolve Dependencies**
   - If prompted, accept any SDK component installations
   - Let Gradle download all dependencies

3. **Build Project**
   - Build → Make Project (Ctrl+F9)
   - Or click the hammer icon in toolbar

## 🔧 Build Variants

### Debug Build (Development)

```bash
# Command line
./gradlew assembleDebug

# Android Studio
# Build → Build Bundle(s)/APK(s) → Build APK(s)
```

**Output**: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build (Production)

#### Method 1: Using Android Studio GUI

1. **Build → Generate Signed Bundle/APK**
2. **Select Android App Bundle** (recommended for Play Store)
3. **Create or choose keystore**:
   - For first time: Create new keystore
   - Existing: Choose existing keystore
4. **Fill keystore information**:
   - Key store path
   - Passwords
   - Key alias
5. **Select build variant**: release
6. **Finish**

#### Method 2: Command Line

1. **Configure signing in `gradle.properties`**:
```properties
MYAPP_RELEASE_STORE_FILE=path/to/your/keystore.jks
MYAPP_RELEASE_STORE_PASSWORD=your_store_password
MYAPP_RELEASE_KEY_ALIAS=your_key_alias
MYAPP_RELEASE_KEY_PASSWORD=your_key_password
```

2. **Build release APK**:
```bash
./gradlew assembleRelease
```

3. **Build release AAB** (for Play Store):
```bash
./gradlew bundleRelease
```

**Outputs**:
- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

## 📱 Testing Builds

### Install Debug APK

```bash
# Using ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Or drag APK to Android Studio device file explorer
```

### Install Release APK

```bash
# Using ADB
adb install app/build/outputs/apk/release/app-release.apk
```

### Run on Emulator/Device

1. **Connect device or start emulator**
2. **Click Run button** in Android Studio (green play icon)
3. **Or use command line**:
```bash
./gradlew installDebug
```

## 🔐 Signing Configuration

### Creating a Keystore

1. **Using Android Studio**:
   - Build → Generate Signed Bundle/APK
   - Create new keystore
   - Fill in the required information

2. **Using command line**:
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
```

### Keystore Security

- **Never commit keystore files to version control**
- **Store keystore passwords securely**
- **Backup your keystore file**
- **Use different passwords for keystore and key**

## 🏗 Build Configuration Details

### Gradle Files Structure

```
EditorApp/
├── build.gradle                 # Project-level build
├── app/
│   ├── build.gradle            # App-level build
│   └── proguard-rules.pro      # ProGuard rules
└── gradle.properties           # Gradle properties
```

### Key Build Settings

**App-level build.gradle**:
```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.rudra157.mediaeditor"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
    
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### ProGuard Configuration

Release builds use ProGuard for:
- **Code obfuscation**: Makes reverse engineering difficult
- **Code shrinking**: Removes unused code
- **Resource shrinking**: Removes unused resources

## 📦 Publishing to Google Play Store

### Prepare AAB File

1. **Build release AAB**:
```bash
./gradlew bundleRelease
```

2. **Locate AAB file**:
`app/build/outputs/bundle/release/app-release.aab`

### Upload to Play Console

1. **Go to Google Play Console**
2. **Select your app** (or create new app)
3. **Navigate to Release → Production**
4. **Create new release**
5. **Upload AAB file**
6. **Fill release information**
7. **Submit for review**

### Play Store Requirements

- **App signing**: Use Play App Signing (recommended)
- **Target API**: Must target recent API level
- **Content rating**: Complete content rating questionnaire
- **Privacy policy**: Required if collecting user data
- **AdMob policy**: Comply with AdMob program policies

## 🐛 Common Build Issues

### Sync Issues

**Problem**: Gradle sync fails
**Solution**:
1. Check internet connection
2. Update Android Studio
3. Clear Gradle cache: `./gradlew clean`
4. Invalidate caches: File → Invalidate Caches

### Dependency Issues

**Problem**: Dependency resolution fails
**Solution**:
1. Check `build.gradle` versions
2. Update repository URLs
3. Clear Gradle cache
4. Check for conflicting dependencies

### Build Failures

**Problem**: Build compilation fails
**Solution**:
1. Check error logs in Build tab
2. Verify SDK versions
3. Check for syntax errors
4. Clean and rebuild project

### Signing Issues

**Problem**: Release build signing fails
**Solution**:
1. Verify keystore path exists
2. Check passwords are correct
3. Ensure keystore file permissions
4. Use correct key alias

## 📊 Build Optimization

### Speed Up Builds

1. **Enable Gradle daemon** (default enabled)
2. **Enable parallel builds**:
```properties
# gradle.properties
org.gradle.parallel=true
```

3. **Configure build cache**:
```properties
org.gradle.caching=true
```

4. **Increase memory allocation**:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m
```

### Reduce APK Size

1. **Enable ProGuard** (already enabled for release)
2. **Use vector drawables** instead of PNGs
3. **Compress images** before adding to project
4. **Remove unused resources**
5. **Enable App Bundle** for Play Store distribution

## 🔍 Build Verification

### Check APK Contents

```bash
# List APK contents
aapt list app/build/outputs/apk/debug/app-debug.apk

# Check permissions
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

### Test Installation

```bash
# Install and test
adb install -r app-debug.apk
adb shell am start -n com.rudra157.mediaeditor/.ui.SplashActivity
```

### Check Dependencies

```bash
# Dependency tree
./gradlew app:dependencies

# Dependency insights
./gradlew app:dependencyInsight --dependency <dependency-name>
```

## 📝 Build Checklist

Before releasing:

- [ ] App builds successfully without errors
- [ ] Debug APK installs and runs correctly
- [ ] Release APK/AAB builds and signs correctly
- [ ] ProGuard doesn't break functionality
- [ ] App size is reasonable
- [ ] All required permissions are declared
- [ ] AdMob configuration is correct (if using ads)
- [ ] AI models are included in assets (if using AI)
- [ ] App icon and metadata are correct
- [ ] Version code and name are updated

## 🆘 Getting Help

### Build Issues
- **Android Studio Help**: Help → Help Menu
- **Gradle Documentation**: [gradle.org](https://gradle.org/docs/)
- **Android Developer Guide**: [developer.android.com](https://developer.android.com/)

### Community Support
- **Stack Overflow**: Tag with `android` and `gradle`
- **Android Developer Community**: Official forums
- **GitHub Issues**: Project-specific issues

---

**Happy Building! 🚀**
