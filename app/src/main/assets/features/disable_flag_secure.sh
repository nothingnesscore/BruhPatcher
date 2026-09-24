#@name Disable FLAG_SECURE
#@description Dynamic FLAG_SECURE bypass allowing screenshots, screen recording, and mirroring in secure banking and DRM apps
#@requires services.jar,miui-services.jar

SERVICES="$SERVICES_JAR"
MIUI_SERVICES="$MIUI_SERVICES_JAR"

echo "[*] Initializing Disable FLAG_SECURE (Dynamic bypass per Disable_Secure_Flag.md)..."

if [ -n "$SERVICES_WORKSPACE" ] && [ -d "$SERVICES_WORKSPACE" ]; then
    SVC_DIR="$SERVICES_WORKSPACE"
else
    SVC_DIR="$TMP/smali_workspaces/services"
fi

if [ -n "$MIUI_SERVICES_WORKSPACE" ] && [ -d "$MIUI_SERVICES_WORKSPACE" ]; then
    MIUI_DIR="$MIUI_SERVICES_WORKSPACE"
else
    MIUI_DIR="$TMP/smali_workspaces/miui-services"
fi

# ==================== SERVICES.JAR ====================
if [ -d "$SVC_DIR" ]; then
    echo "[*] Applying FLAG_SECURE hooks to services.jar..."

    # 1. DevicePolicyCacheImpl.smali
    dpci=$(find "$SVC_DIR" -name "DevicePolicyCacheImpl.smali" -type f 2>/dev/null | head -n 1)
    if [ -n "$dpci" ]; then
        echo "  -> Hooking DevicePolicyCacheImpl.isScreenCaptureAllowed()..."
        awk '
        BEGIN { in_target = 0 }
        /\.method public.*isScreenCaptureAllowed\(I\)Z/ {
            in_target = 1; print $0; next
        }
        in_target && /^[ \t]*\.registers[ \t]+([0-9]+)/ {
            regs = $2; if (regs < 3) regs = 3
            print "    .registers " regs
            print "    invoke-static {}, Landroid/security/kaorios/KaoriosHook;->isSecureFlag()Z"
            print "    move-result v0"
            print "    if-eqz v0, :cond_kaorios_dpc"
            print "    const/4 v0, 0x1"
            print "    return v0"
            print "    :cond_kaorios_dpc"
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$dpci" > "${dpci}.tmp" && mv "${dpci}.tmp" "$1" 2>/dev/null || rm -f "${dpci}.tmp"
    fi

    # 2. WindowState.smali & WindowStateAnimator.smali
    for ws in $(find "$SVC_DIR" \( -name "WindowState.smali" -o -name "WindowStateAnimator.smali" \) -type f 2>/dev/null); do
        echo "  -> Hooking $ws..."
        awk '
        BEGIN { in_secure = 0; in_set = 0 }
        /\.method.*isSecureLocked\(\)Z/ {
            in_secure = 1; print $0; next
        }
        /\.method.*setSecureLocked\(Z\)V/ {
            in_set = 1; print $0; next
        }
        in_secure && /^[ \t]*\.registers[ \t]+([0-9]+)/ {
            regs = $2; if (regs < 3) regs = 3
            print "    .registers " regs
            print "    invoke-static {}, Landroid/security/kaorios/KaoriosHook;->isSecureFlag()Z"
            print "    move-result v0"
            print "    if-eqz v0, :cond_kaorios_sec"
            print "    const/4 v0, 0x0"
            print "    return v0"
            print "    :cond_kaorios_sec"
            in_secure = 0
            next
        }
        in_set && /^[ \t]*\.registers[ \t]+([0-9]+)/ {
            regs = $2; if (regs < 3) regs = 3
            print "    .registers " regs
            print "    invoke-static {}, Landroid/security/kaorios/KaoriosHook;->isSecureFlag()Z"
            print "    move-result v0"
            print "    if-eqz v0, :cond_kaorios_set"
            print "    return-void"
            print "    :cond_kaorios_set"
            in_set = 0
            next
        }
        in_secure && /\.end method/ { in_secure = 0 }
        in_set && /\.end method/ { in_set = 0 }
        { print $0 }
        ' "$ws" > "${ws}.tmp" && mv "${ws}.tmp" "$1" 2>/dev/null || rm -f "${ws}.tmp"
    done

    # 3. WindowManagerService.smali
    for wms in $(find "$SVC_DIR" -name "WindowManagerService*.smali" -type f 2>/dev/null); do
        if grep -q "notAllowCaptureDisplay" "$wms" 2>/dev/null; then
            echo "  -> Hooking notAllowCaptureDisplay in $wms..."
            awk '
            /->notAllowCaptureDisplay\(/ {
                print $0
                print "    invoke-static {}, Landroid/security/kaorios/KaoriosHook;->isSecureFlag()Z"
                print "    move-result v0"
                print "    if-eqz v0, :cond_kaorios_wms"
                print "    const/4 v0, 0x0"
                print "    :cond_kaorios_wms"
                next
            }
            { print $0 }
            ' "$wms" > "${wms}.tmp" && mv "${wms}.tmp" "$1" 2>/dev/null || rm -f "${wms}.tmp"
        fi
    done

    command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "services.jar"
fi

# ==================== MIUI-SERVICES.JAR ====================
if [ -d "$MIUI_DIR" ]; then
    echo "[*] Applying FLAG_SECURE hooks to miui-services.jar..."
    for mwms in $(find "$MIUI_DIR" -name "WindowManagerServiceImpl.smali" -type f 2>/dev/null); do
        echo "  -> Hooking $mwms..."
        awk '
        BEGIN { in_target = 0 }
        /\.method.*notAllowCaptureDisplay\(/ {
            in_target = 1; print $0; next
        }
        in_target && /^[ \t]*\.registers[ \t]+([0-9]+)/ {
            regs = $2; if (regs < 3) regs = 3
            print "    .registers " regs
            print "    const/4 v0, 0x0"
            print "    return v0"
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$mwms" > "${mwms}.tmp" && mv "${mwms}.tmp" "$1" 2>/dev/null || rm -f "${mwms}.tmp"
    done
    command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "miui-services.jar"
fi

echo "[+] Disable FLAG_SECURE applied successfully."
