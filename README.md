# Bruh Patcher ⚡

> **The Universal Android Framework Patcher**  
> Supporting **Android 8.0 through Android 17 (API 37 / Baklava)** across **AOSP, Pixel, Xiaomi MIUI & HyperOS 1–4, Samsung OneUI, ColorOS, OxygenOS & all OEM ROMs**.  
> Features modern **Monet Dynamic Color & MIUIX Alive Design**, 1-Tap **AutoPatcher** for Android 17 / HyperOS 4 (specifically Redmi Turbo 3 / Poco F6 `peridot`), seamless toggle between **Liquid Glass Floating Navbar** and **MIUIX Docked Navigation Bar**, and complete **Kaorios Toolbox v2.0.6.0** automated hooks!

---

## 🌟 Universal Compatibility & AutoPatcher Engine

**Bruh Patcher is an all-in-one universal framework modification suite.**  
It is **not limited to Android 17 or HyperOS 4**—it is engineered to patch **all available features across any supported device, OEM skin, and Android release**, while featuring a dedicated **1-Tap AutoPatcher Preset** for latest-gen hardware:

- **1-Tap AutoPatcher**: Automatically detects Xiaomi Redmi Turbo 3 / Poco F6 (`peridot`, 24069RA21C, Snapdragon 8s Gen 3, Android 17 API 37, HyperOS 4.0), selects all 6 core patches, enables automated extraction, and starts local patching with a single tap.
- **Supported Android Versions**: Android 8.0, 8.1, 9, 10, 11, 12, 12L, 13, 14, 15, 16, and **Android 17 (Baklava / API 37)**.
- **Supported ROM Ecosystems**: Pure AOSP, Google Pixel, Xiaomi (MIUI 12/13/14, HyperOS 1, 2, 3, 4), Samsung OneUI, OnePlus/Oppo (OxygenOS/ColorOS), Realme UI, Motorola, LineageOS, and custom ROMs.
- **Root Managers Supported**: Magisk (v24+), KernelSU, KernelSU Next, APatch, and SUFS. Also supports manual file extraction without root.

---

## ✨ Key Features & Patches

### 🪞 1. Monet Dynamic Color & Dual Navigation Architecture
- **Material You Monet Theming**: Adapts seamlessly to system wallpaper dynamic palette across all UI cards, buttons, switches, and elevated surfaces.
- **Liquid Glass Navbar Toggle**: Switch effortlessly between the authentic iOS-style refractive floating navbar and the standard docked MIUIX navigation bar in **Settings > Appearance & Navigation**.
- **Authentic iOS-Style Liquid Glass Floating Navbar**:
  - **Architecture & Lineage**: Kyant0/AndroidLiquidGlass SDF optical distance fields, compose-miuix-ui multiplatform port, and SukiSU-Ultra production-hardened physics.
  - **Damped Spring Drag Physics**: Features `DampedDragAnimation` (`stiffness = 650f, damping = 0.82f`) with 78/56 horizontal press expansion ratio, fling velocity inertia deformation, and non-linear rubber-band edge elasticity.
  - **Sliding Frosted Indicator Pill**: Minimalist elevated frosted glass pill (`Color.White.copy(alpha = 0.11f)`) with 0.5dp top-lit specular edge sliding smoothly beneath active tabs.
  - **Interactive Specular Bloom**: Touch-driven specular highlight that ONLY activates under active touch/drag coordinates via hardware AGSL RuntimeShader on Android 13+ and radial gradient fallback on API 26–32 (zero GPU/CPU idle overhead).
- **Docked MIUIX Navigation Bar**: Clean, classic docked bottom bar with squircle selection indicators, terminal active status dots, and tactile haptic feedback.

### 🛡️ 2. Integrated Kaorios Toolbox Engine (v2.0.6.0+)
- **Full Smali Hook Suite**:
  - **Process Runtime Hook**: Injects `KaoriosHook.initActivityThread(String, String)` directly into `ActivityThread.attach()` for early runtime spoofing.
  - **Developer Options & ADB Stealth**: Hooks `Settings$NameValueCache.getStringForUser` to hide developer mode and ADB debugging status from banking apps.
  - **Caller-Aware Privacy Isolation**: Process runtime isolation and context filtering to protect installed package lists from inspection without breaking system IPC.
  - **Play Integrity & Hardware Attestation**: Injects `kaorios_framework.dex` into `framework.jar`, provisions `Pif-props.json`, `app-props.json`, `device-model.json`, and integrates with user `Keybox.xml`.
  - **ApplicationPackageManager & KeyStore**: Complete hooks into `ApplicationPackageManager.hasSystemFeature`, `AndroidKeyStoreKeyPairGeneratorSpi.generateKeyPair`, and `AndroidKeyStoreSpi.engineGetCertificateChain`.
- **Per-App High Refresh Rate Spoofing**: Unlock 120 FPS in games (Honor of Kings, Genshin Impact, PUBG Mobile, etc.).
- **Installer Source Spoofing**: Bypasses marketplace-restricted installation checks.

### 🚀 3. Android 17 (Baklava) & HyperOS 4 Extended Support
- **Android 17 Build Static-Final Unfinalize**:
  - Unfinalizes static final fields on `android.os.Build` and `android.os.Build$VERSION` to enable dynamic reflection and device spoofing on Android 17.
- **Dual-Framework Handling**:
  - Automatic detection and support for `miui-framework.jar` and `miui-services.jar` alongside `framework.jar` and `services.jar`.
- **HyperOS / MIUI China Push Notification Optimizer**:
  - Unfreezes background push daemons and eliminates delay loops on China ROMs.

### 🔓 4. Universal CorePatch Signature Bypass
- Disables APK signature verification, digest mismatch checks, and package downgrade restrictions across **Android 13, 14, 15, 16, 17, and HyperOS 1–4**.

### 📸 5. Media & Security Tweaks
- **Disable Secure Flag**: Enables screenshots, screen recording, and display mirroring in DRM and banking apps.
- **Google Photos Unlimited**: Enables original-quality cloud photo and video backup by spoofing Pixel XL.
- **Robust Local Packaging**: Fixed `EACCES (Permission denied)` log creation on restricted storage; logs are staged in context cache before atomic root copy into flashable module.

---

## ⚡ Execution Modes

1. **Local DynamicInstaller (On-Device)**:
   - Uses embedded BusyBox, Smali, Baksmali, and ZipAlign.
   - Decompiles, applies shell-based AST and smali hooks, recompiles, and packages a ready-to-flash module directly to `/sdcard/Download/`. Fast, offline, and completely private.
2. **Cloud Mode**:
   - Uploads framework JARs to secure staging and triggers GitHub Actions workflow for remote compilation on high-performance runners.

---

## 📱 Framework Files Overview

| File | Standard Location | Purpose |
| :--- | :--- | :--- |
| `framework.jar` | `/system/framework/framework.jar` | Core Android application framework & Kaorios runtime |
| `services.jar` | `/system/framework/services.jar` | System Server services & signature verification checks |
| `miui-services.jar` | `/system_ext/framework/miui-services.jar` | HyperOS / MIUI security and package managers |
| `miui-framework.jar` | `/system_ext/framework/miui-framework.jar` | Xiaomi HyperOS extended runtime classes |

---

## 🚀 Getting Started

### Prerequisites
- Root access (Magisk v24+, KernelSU, KernelSU Next, APatch) **OR** manual framework extraction.
- Android 8.0+ (API 26+) minimum, up to Android 17 (API 37).

### Usage Guide
1. Launch **Bruh Patcher**.
2. Check device status on the **Status** tab (detects Android version, API level, ROM type, and framework JARs).
3. Navigate to the **Patches** tab:
   - Choose **Local Patching (DynamicInstaller)** or **Cloud Patching**.
   - Select your desired features (Signature Bypass, Kaorios v2.0.6.0, A17 Fix, Secure Flag, etc.).
4. Tap **Start Local Patching**.
5. Monitor real-time progress in the **Terminal** tab.
6. Flash the generated ZIP module in Magisk / KernelSU / APatch from your `Downloads` folder and reboot!

---

## 👨‍💻 Credits & Attributions

- **Bruh Patcher**: [nothingnesscore](https://github.com/nothingnesscore)
- **Kaorios Toolbox**: [hzzmonetvn/Kaorios-Toolbox](https://github.com/hzzmonetvn/Kaorios-Toolbox)
- **FrameworkPatcher Base**: [Jefino9488/FrameworkPatcherApp](https://github.com/FrameworksForge/FrameworkPatcherApp)

---

## 📄 License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
