#@name Disable FLAG_SECURE
#@description Allows screenshots and screen recording in secure/protected apps on AOSP & HyperOS
#@requires services.jar,miui-services.jar

SERVICES="$SERVICES_JAR"
MIUI_SERVICES="$MIUI_SERVICES_JAR"

if [ -z "$SERVICES" ]; then
    echo "[!] ERROR: services.jar not found"
    return 1
fi

if [ -n "$SERVICES_WORKSPACE" ] && [ -d "$SERVICES_WORKSPACE" ]; then
    SVC_WORK_DIR="$SERVICES_WORKSPACE"
    SVC_STANDALONE=0
else
    SVC_WORK_DIR="$TMP/svc_dc"
    SVC_STANDALONE=1
fi

if [ -n "$MIUI_SERVICES_WORKSPACE" ] && [ -d "$MIUI_SERVICES_WORKSPACE" ]; then
    MIUI_WORK_DIR="$MIUI_SERVICES_WORKSPACE"
    MIUI_STANDALONE=0
else
    MIUI_WORK_DIR="$TMP/miui_dc"
    MIUI_STANDALONE=1
fi

return_false='
    .locals 1
    const/4 v0, 0x0
    return v0
'

return_true='
    .locals 1
    const/4 v0, 0x1
    return v0
'

# ==================== SERVICES.JAR ====================
if [ "$SVC_STANDALONE" -eq 1 ]; then
    echo "[*] Decompiling services.jar..."
    dynamic_apktool -decompile "$SERVICES" -o "$SVC_WORK_DIR"
fi

if [ -d "$SVC_WORK_DIR" ]; then
    echo "[*] Applying FLAG_SECURE patches to services.jar..."

    echo "[*] Patching WindowState.isSecureLocked()..."
    smali_kit -c -m "isSecureLocked" -re "$return_false" -d "$SVC_WORK_DIR" -name "WindowState.smali"
    smali_kit -c -m "isSecureLocked" -re "$return_false" -d "$SVC_WORK_DIR" -name "WindowStateAnimator.smali"

    echo "[*] Patching notAllowCaptureDisplay()..."
    smali_kit -c -m "notAllowCaptureDisplay" -re "$return_false" -d "$SVC_WORK_DIR" -name "WindowManagerService*.smali"

    echo "[*] Patching DevicePolicyCacheImpl.isScreenCaptureAllowed()..."
    smali_kit -c -m "isScreenCaptureAllowed" -re "$return_true" -d "$SVC_WORK_DIR" -name "DevicePolicyCacheImpl.smali"

    echo "[*] Patching preventTakingScreenshotToTargetWindow()..."
    smali_kit -c -m "preventTakingScreenshotToTargetWindow" -re "$return_false" -d "$SVC_WORK_DIR" -name "ScreenshotController*.smali"

    if [ "$SVC_STANDALONE" -eq 1 ]; then
        echo "[*] Recompiling services.jar..."
        dynamic_apktool -recompile "$SVC_WORK_DIR" -o "$SERVICES"
        delete_recursive "$SVC_WORK_DIR"
    fi
fi

# ==================== MIUI-SERVICES.JAR (HYPEROS) ====================
if [ -n "$MIUI_SERVICES" ]; then
    if [ "$MIUI_STANDALONE" -eq 1 ]; then
        echo "[*] Decompiling miui-services.jar..."
        dynamic_apktool -decompile "$MIUI_SERVICES" -o "$MIUI_WORK_DIR"
    fi

    if [ -d "$MIUI_WORK_DIR" ]; then
        echo "[*] Applying FLAG_SECURE patches to miui-services.jar..."

        echo "[*] Patching WindowManagerServiceImpl.notAllowCaptureDisplay()..."
        smali_kit -c -m "notAllowCaptureDisplay" -re "$return_false" -d "$MIUI_WORK_DIR" -name "WindowManagerServiceImpl.smali"

        if [ "$MIUI_STANDALONE" -eq 1 ]; then
            echo "[*] Recompiling miui-services.jar..."
            dynamic_apktool -recompile "$MIUI_WORK_DIR" -o "$MIUI_SERVICES"
            delete_recursive "$MIUI_WORK_DIR"
        fi
    fi
fi

echo "[*] FLAG_SECURE patch complete."
