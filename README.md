# Bruh Patcher ⚡

> **The Universal Android Framework Patcher**  
> Supporting **Android 8.0 through Android 17 (API 37 / Baklava)** across **AOSP, Pixel, Xiaomi MIUI & HyperOS 1–4, Samsung OneUI, ColorOS, OxygenOS & all OEM ROMs**.  
> Features modern **MIUIX Liquid Glass UI** with HyperOS Alive Design theme, permanent release signature for seamless in-place updates, and integrated **Kaorios Toolbox v2.0.6.0**!

---

## 🌟 Universal Compatibility (Not Limited to Any Single OS)

**Bruh Patcher is an all-in-one universal framework modification suite.**  
It is **not limited to Android 17 or HyperOS 4**—it is engineered to patch **all available features across any supported device, OEM skin, and Android release**:

- **Supported Android Versions**: Android 8.0, 8.1, 9, 10, 11, 12, 12L, 13, 14, 15, 16, and **Android 17 (Baklava / API 37)**.
- **Supported ROM Ecosystems**: Pure AOSP, Google Pixel, Xiaomi (MIUI 12/13/14, HyperOS 1, 2, 3, 4), Samsung OneUI, OnePlus/Oppo (OxygenOS/ColorOS), Realme UI, Motorola, LineageOS, and custom ROMs.
- **Root Managers Supported**: Magisk (v24+), KernelSU, KernelSU Next, APatch, and SUFS. Also supports manual file extraction without root.

---

## ✨ Key Features & Patches

### 🪞 1. MIUIX Liquid Glass Floating Dock
- **Optical Light Refraction**: Custom shader reproducing ambient light bending through a curved convex glass lens with dynamic diagonal glare and top-rim Fresnel lens reflection.
- **Prismatic Chromatic Dispersion**: Outer border features iridescent spectral refraction (cyan `#00C7BE` to electric blue `#007AFF` to violet `#7C3AED`).
- **Dynamic Contrast Support**: Intelligently adjusts surface opacity, border luminance, and active item bloom according to theme and ambient backgrounds for crisp WCAG contrast.
- **Fluid Spring Dock Navigation**: Four smooth interactive tabs (**Status**, **Patches**, **Terminal**, **Settings**) with spring physics and live pulse indicators.

### 🛡️ 2. Integrated Kaorios Toolbox Engine (v2.0.6.0)
- **Play Integrity Fix & Hardware Attestation**:
  - Injects `kaorios_framework.dex` directly into `framework.jar`.
  - Provisions `Pif-props.json`, `app-props.json`, and `device-model.json` into the flashable Magisk/KSU module.
  - Supports custom user `Keybox.xml` hardware attestation files.
  - Automated smali hooks into `Instrumentation`, `ApplicationPackageManager`, `AndroidKeyStoreSpi`, and `SystemServer`.
- **Per-App High Refresh Rate Spoofing**:
  - Unlock 120 FPS in games (Honor of Kings, Genshin Impact, PUBG Mobile, etc.).
- **Privacy Isolation & Caller-Aware Stealth**:
  - `AppsFilterBase` and `ComputerEngine` hooks ensure target apps are completely invisible to querying packages without breaking IPC.
- **Installer Source Spoofing**:
  - Bypasses marketplace-restricted installation checks.

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
