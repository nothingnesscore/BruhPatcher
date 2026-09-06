#!/system/bin/sh
# Bruh Patcher - Late Start Service
MODDIR=${0%/*}

# Wait for boot completion
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
done
sleep 2

# =========================================================================
# NoMount VFS Verification
# =========================================================================
NM_BIN=""
if command -v nm >/dev/null 2>&1; then
    NM_BIN="$(command -v nm)"
elif [ -x /data/adb/nomount/bin/nm ]; then
    NM_BIN="/data/adb/nomount/bin/nm"
elif [ -x /data/adb/nomount/nm ]; then
    NM_BIN="/data/adb/nomount/nm"
fi

if [ -n "$NM_BIN" ] && "$NM_BIN" version >/dev/null 2>&1; then
    if ! "$NM_BIN" rule list 2>/dev/null | grep -q "framework.jar"; then
        [ -f "$MODDIR/system/framework/framework.jar" ] && \
            "$NM_BIN" rule add /system/framework/framework.jar "$MODDIR/system/framework/framework.jar" 2>/dev/null
        [ -f "$MODDIR/system/framework/services.jar" ] && \
            "$NM_BIN" rule add /system/framework/services.jar "$MODDIR/system/framework/services.jar" 2>/dev/null
        if [ -f "$MODDIR/system/system_ext/framework/miui-services.jar" ]; then
            "$NM_BIN" rule add /system/system_ext/framework/miui-services.jar "$MODDIR/system/system_ext/framework/miui-services.jar" 2>/dev/null
            "$NM_BIN" rule add /system_ext/framework/miui-services.jar "$MODDIR/system/system_ext/framework/miui-services.jar" 2>/dev/null
        fi
    fi
fi

# =========================================================================
# Install Kaorios Toolbox APK if present as user app
# =========================================================================
if [ -f "$MODDIR/system/product/priv-app/KaoriosToolbox/KaoriosToolbox.apk" ]; then
    pm install -r "$MODDIR/system/product/priv-app/KaoriosToolbox/KaoriosToolbox.apk" >/dev/null 2>&1
elif [ -f "$MODDIR/system/priv-app/KaoriosToolbox/KaoriosToolbox.apk" ]; then
    pm install -r "$MODDIR/system/priv-app/KaoriosToolbox/KaoriosToolbox.apk" >/dev/null 2>&1
fi
