# Virtual Background Android App

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)
[![TensorFlow](https://img.shields.io/badge/TensorFlow_Lite-2.5.0-orange.svg)](https://tensorflow.org/lite)
[![OpenCV](https://img.shields.io/badge/OpenCV-4.x-blue.svg)](https://opencv.org/android/)
[![AGSL](https://img.shields.io/badge/AGSL-GPU_Accelerated-red.svg)](https://developer.android.com/develop/ui/views/graphics/agsl)

A real-time virtual background Android application that uses **TensorFlow Lite**, **OpenCV**, and **AGSL (Android Graphics Shading Language)** for GPU-accelerated person segmentation and background replacement. Built with Camera2 API for professional-grade camera integration.

## ✨ Key Features

- 🎥 **Real-time Processing**: Live background replacement using Camera2 API
- 🧠 **On-device AI**: TensorFlow Lite models for fast person segmentation
- ⚡ **GPU Acceleration**: AGSL shaders for hardware-accelerated blending (Android 13+)
- 📱 **Modern Architecture**: Fragment-based design with proper lifecycle management
- 🔧 **OpenCV Integration**: Advanced image processing capabilities
- 🎯 **High Performance**: Optimized for smooth 30fps processing
- 📷 **Professional Camera**: Camera2 API with autofocus and front/rear camera support

## 🛠️ Technical Stack

### Core Technologies
- **Languages**: Kotlin + Java
- **ML Framework**: TensorFlow Lite 2.5.0
- **Computer Vision**: OpenCV 4.x
- **Graphics**: AGSL (Android Graphics Shading Language)
- **Camera**: Camera2 API
- **Architecture**: Fragment-based with proper lifecycle management

### Key Components
- **ImageProcessorAGSL**: GPU-accelerated image blending using AGSL shaders
- **CameraActivity**: Main activity with fragment management
- **Camera2BasicFragment**: Camera handling and real-time processing
- **TensorFlow Lite**: Person segmentation model
- **OpenCV**: Image preprocessing and post-processing

## 📋 Requirements

### Device Requirements
- **Android Version**: API 26+ (Android 8.0)
- **For AGSL Features**: API 33+ (Android 13) - Tiramisu
- **RAM**: Minimum 4GB (6GB+ recommended)
- **Storage**: ~150MB for app + models
- **Camera**: Front/rear camera with autofocus support
- **GPU**: Adreno/Mali/PowerVR with OpenGL ES 3.0+

### Development Requirements
- **Android Studio**: Flamingo or later
- **NDK**: For OpenCV native libraries
- **CMake**: For building native modules
- **Gradle**: 8.0+
- **JDK**: 11+

## 🚀 Installation & Setup

### For Users
1. Download APK from [Releases](https://github.com/sudhakar-r08/VirtualBackground/releases)
2. Enable "Install from Unknown Sources" in device settings
3. Install the APK
4. Grant camera permissions when prompted
5. Start using real-time virtual backgrounds!

### For Developers

#### 1. Clone Repository
```bash
git clone https://github.com/sudhakar-r08/VirtualBackground.git
cd VirtualBackground
```

#### 2. Open in Android Studio
- Import project in Android Studio
- Sync Gradle files
- Ensure NDK and CMake are installed

#### 3. Dependencies Setup
The project uses these key dependencies:
```gradle
// TensorFlow Lite
implementation 'org.tensorflow:tensorflow-lite:2.5.0'
implementation 'org.tensorflow:tensorflow-lite-gpu:2.3.0'

// OpenCV (local module)
implementation project(':opencv')

// Image Processing
implementation 'com.github.bumptech.glide:glide:4.12.0'
implementation 'jp.co.cyberagent.android:gpuimage:2.1.0'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

#### 4. Build and Run
```bash
./gradlew assembleDebug
```

## 📂 Project Structure

```
VirtualBackground/
├── app/
│   ├── src/main/
│   │   ├── java/com/sudhakar/backgroundchangerapp/
│   │   │   ├── CameraActivity.java           # Main activity
│   │   │   ├── Camera2BasicFragment.java     # Camera fragment
│   │   │   ├── ImageProcessorAGSL.kt         # AGSL GPU processing
│   │   │   └── VBApp.java                    # Application class
│   │   ├── assets/
│   │   │   └── models/                       # TensorFlow Lite models
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_camera.xml
│   │   │   └── values/
│   │   └── AndroidManifest.xml
│   ├── jni/                                  # Native libraries
│   └── build.gradle
├── opencv/                                   # OpenCV module
└── README.md
```

## 🎨 AGSL Shader Implementation

The app uses **Android Graphics Shading Language (AGSL)** for GPU-accelerated background blending:

```kotlin
const val AGSL_SHADER_CODE = """
    uniform shader inputShader;    // Background image
    uniform shader fgdShader;      // Foreground (person)
    uniform shader maskShader;     // Segmentation mask

    half4 main(float2 coords) {
        half4 bgd = inputShader.eval(coords);
        half4 fgd = fgdShader.eval(coords);
        half4 msk = maskShader.eval(coords);

        half4 outColor = mix(bgd, fgd, msk);
        return outColor;
    }
"""
```

### Key AGSL Features:
- **Hardware Acceleration**: Runs directly on GPU
- **Real-time Blending**: Smooth 30fps processing
- **Memory Efficient**: Minimal CPU-GPU data transfer
- **Modern Graphics**: Leverages Android 13+ graphics pipeline

## 🎮 Usage & Controls

### App Flow
1. **Launch App**: Opens directly to camera view
2. **Grant Permissions**: Camera access required
3. **Real-time Processing**: Automatic background replacement
4. **Background Selection**: Choose from predefined backgrounds
5. **Capture/Record**: Save photos or videos with virtual backgrounds

### Technical Implementation
```java
// Main Activity Setup
public class CameraActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        // Load OpenCV
        System.loadLibrary("opencv_java4");
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCv", "Unable to load OpenCV");
        }
        
        // Setup Camera Fragment
        Fragment fragment = Camera2BasicFragment.newInstance();
        switchFragment(fragment, false);
    }
}
```

## ⚙️ Configuration

### Build Configuration
```gradle
android {
    compileSdk 36
    minSdk 26
    targetSdk 36
    
    buildFeatures {
        shaders = true  // Enable AGSL compilation
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    
    packagingOptions {
        pickFirst 'lib/armeabi-v7a/libRSSupport.so'
    }
}
```

### Permissions Required
```xml
<uses-permission android:name="android.permission.CAMERA" />

<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
<uses-feature android:name="android.hardware.camera.front" android:required="false" />
```

## 🔧 Core Components

### 1. ImageProcessorAGSL
GPU-accelerated image processing using AGSL:
- **Hardware Rendering**: Uses `HardwareRenderer` for off-screen processing
- **Shader Blending**: Custom AGSL shader for real-time compositing
- **Memory Management**: Efficient bitmap handling with proper cleanup
- **Android 13+ Only**: Requires API level 33 (Tiramisu) or higher

### 2. Camera Integration
- **Camera2 API**: Professional camera control
- **Fragment Architecture**: Proper lifecycle management
- **Real-time Preview**: Live camera feed with processing overlay
- **Multi-camera Support**: Front and rear camera switching

### 3. TensorFlow Lite Integration
- **Person Segmentation**: Real-time human detection
- **GPU Acceleration**: TensorFlow Lite GPU delegate
- **Optimized Models**: Compressed models for mobile deployment
- **Efficient Inference**: <50ms processing time on modern devices

## 🏆 Performance Optimizations

### AGSL Benefits:
- **GPU Processing**: Offloads blending to graphics hardware
- **Parallel Processing**: Concurrent pixel operations
- **Memory Efficiency**: Direct GPU memory access
- **Reduced Latency**: Minimal CPU-GPU data transfer

### Performance Benchmarks:
| Device | Processing Time | FPS | Memory Usage |
|--------|----------------|-----|--------------|
| Pixel 6 Pro | ~25ms | 30+ | ~180MB |
| Galaxy S22 | ~20ms | 30+ | ~160MB |
| OnePlus 9 | ~30ms | 25+ | ~200MB |

## 🐛 Troubleshooting

### Common Issues

**1. AGSL Not Working**
- Ensure device runs Android 13+ (API 33)
- Check GPU compatibility
- Fallback to CPU processing if needed

**2. OpenCV Loading Failed**
```java
// Check OpenCV initialization
if (!OpenCVLoader.initDebug()) {
    Log.e("OpenCV", "Unable to load OpenCV");
    // Implement fallback or show error
}
```

**3. Camera Permissions**
- Grant camera permission in device settings
- Check manifest permissions are correctly declared
- Handle runtime permission requests

**4. Performance Issues**
- Reduce camera resolution
- Disable GPU acceleration if causing crashes
- Optimize TensorFlow Lite model size

### Development Tips
- Use Android Studio profiler for performance analysis
- Test on multiple device architectures (ARM64, ARM32)
- Implement proper error handling for GPU operations
- Add fallback processing for older devices

## 🔮 Roadmap

### Planned Features
- [ ] **Real-time Effects**: Add filters and color adjustments
- [ ] **Video Backgrounds**: Animated background support
- [ ] **Edge Refinement**: Improved segmentation boundaries
- [ ] **Background Library**: Downloadable background packs
- [ ] **Social Sharing**: Direct integration with social platforms
- [ ] **Custom Models**: Support for custom TensorFlow Lite models

### Technical Improvements
- [ ] **Vulkan API**: Next-gen graphics API support
- [ ] **MediaPipe**: Alternative to TensorFlow Lite
- [ ] **CameraX**: Migration from Camera2 API
- [ ] **Jetpack Compose**: Modern UI framework

## 🏗️ Architecture

### Design Pattern
```
CameraActivity (Main)
    ↓
Camera2BasicFragment (Camera Logic)
    ↓
ImageProcessorAGSL (GPU Processing)
    ↓
TensorFlow Lite (ML Inference)
    ↓
OpenCV (Image Processing)
```

### Key Classes
- **CameraActivity**: Fragment management and OpenCV initialization
- **Camera2BasicFragment**: Camera2 API integration and real-time processing
- **ImageProcessorAGSL**: AGSL shader-based GPU acceleration
- **VBApp**: Application class for global initialization

## 🤝 Contributing

1. **Fork** the repository
2. **Create feature branch**: `git checkout -b feature-agsl-improvements`
3. **Implement changes** with proper testing
4. **Test on multiple devices** and Android versions
5. **Submit pull request** with detailed description

### Development Guidelines
- Follow Android architecture best practices
- Test AGSL features on Android 13+ devices
- Maintain backward compatibility for older Android versions
- Document GPU-specific implementations
- Include performance benchmarks for new features

## 📄 License

This project is licensed under the MIT License:

```
MIT License

Copyright (c) 2025 Sudhakar Raju

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🙏 Acknowledgments

- **TensorFlow Team**: For TensorFlow Lite mobile AI framework
- **OpenCV Foundation**: For computer vision libraries
- **Android Team**: For AGSL and Camera2 API
- **Google AI**: For person segmentation research and models

## 📞 Support

- 🐛 **Issues**: [GitHub Issues](https://github.com/sudhakar-r08/VirtualBackground/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/sudhakar-r08/VirtualBackground/discussions)
- 📧 **Email**: [sudhakar.r08@gmail.com](sudhakar.r08@gmail.com)

---

⭐ **If this project helped you, please give it a star!** ⭐

<div align="center">
  <strong>Built with cutting-edge Android graphics technology</strong><br>
  Made with ❤️ by <a href="https://github.com/sudhakar-r08">Sudhakar Raju</a>
</div>
