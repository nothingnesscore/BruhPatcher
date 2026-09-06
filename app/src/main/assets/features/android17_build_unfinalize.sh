#@name Android 17 Build Reflection Patch
#@description Unfinalizes Build and Build$VERSION fields on Android 17 (Baklava / HyperOS 4) so PIF and device spoofing work
#@requires framework.jar

FRAMEWORK="$FRAMEWORK_JAR"

echo "[*] Initializing Android 17 / HyperOS 4 Build Reflection Patch..."

if [ -n "$FRAMEWORK_WORKSPACE" ] && [ -d "$FRAMEWORK_WORKSPACE" ]; then
    FW_DIR="$FRAMEWORK_WORKSPACE"
else
    FW_DIR="$TMP/smali_workspaces/framework"
fi

if [ ! -d "$FW_DIR" ]; then
    echo "[!] ERROR: framework workspace directory not found"
    return 1
fi

build_file=$(find "$FW_DIR" -name "Build.smali" -type f | head -1)
if [ -n "$build_file" ]; then
    echo "[*] Patching Build.smali..."
    # Target fields in Build.smali: remove final and append = null
    awk '
    /\.field public static final.*(BRAND|BRAND_FOR_ATTESTATION|DEVICE|DEVICE_FOR_ATTESTATION|FINGERPRINT|HARDWARE|ID|MANUFACTURER|MANUFACTURER_FOR_ATTESTATION|MODEL|MODEL_FOR_ATTESTATION|PRODUCT|PRODUCT_FOR_ATTESTATION|TAGS|TIME|TYPE|USER):/ {
        sub(" static final ", " static ")
        if (!/=/) {
            $0 = $0 " = null"
        }
        print $0
        next
    }
    { print $0 }
    ' "$build_file" > "${build_file}.tmp" && mv "${build_file}.tmp" "$build_file"
    echo "[+] Patched: $build_file"
else
    echo "[!] Warning: Build.smali not found"
fi

version_file=$(find "$FW_DIR" -name "Build\$VERSION.smali" -type f | head -1)
if [ -n "$version_file" ]; then
    echo "[*] Patching Build\$VERSION.smali..."
    # Target fields in Build$VERSION: remove final from RELEASE, RELEASE_OR_CODENAME, RELEASE_OR_PREVIEW_DISPLAY, SECURITY_PATCH, DEVICE_INITIAL_SDK_INT
    # Note: explicitly preserve SDK_INT!
    awk '
    /\.field public static final.*(RELEASE|RELEASE_OR_CODENAME|RELEASE_OR_PREVIEW_DISPLAY|SECURITY_PATCH|DEVICE_INITIAL_SDK_INT):/ {
        if ($0 !~ /SDK_INT/) {
            sub(" static final ", " static ")
        }
        print $0
        next
    }
    { print $0 }
    ' "$version_file" > "${version_file}.tmp" && mv "${version_file}.tmp" "$version_file"
    echo "[+] Patched: $version_file"
else
    echo "[!] Warning: Build\$VERSION.smali not found"
fi

echo "[*] Android 17 / HyperOS 4 Build Reflection Patch complete."
