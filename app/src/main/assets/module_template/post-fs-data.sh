#!/system/bin/sh
# Bruh Patcher - post-fs-data Hook
# Handles early boot initialization and NoMount VFS redirection fallback
MODDIR=${0%/*}

# =========================================================================
# NoMount VFS Redirection Compatibility Engine
# =========================================================================
# If NoMount metamodule is active, it will automatically serve files from
# $MODDIR/system/ at post-fs-data. If the metamodule was disabled or if
# running in standalone VFS mode, this fallback registers the rules directly.

NM_BIN=""
if command -v nm >/dev/null 2>&1; then
    NM_BIN="$(command -v nm)"
elif [ -x /data/adb/nomount/bin/nm ]; then
    NM_BIN="/data/adb/nomount/bin/nm"
elif [ -x /data/adb/nomount/nm ]; then
    NM_BIN="/data/adb/nomount/nm"
elif [ -x /data/adb/modules/nomount/bin/nm ]; then
    NM_BIN="/data/adb/modules/nomount/bin/nm"
fi

if [ -n "$NM_BIN" ] && "$NM_BIN" version >/dev/null 2>&1; then
    # Check if framework.jar is already registered
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

# Support for NoMount-Suite CLI (nomount vfs)
if command -v nomount >/dev/null 2>&1; then
    if ! nomount vfs list 2>/dev/null | grep -q "framework.jar"; then
        [ -f "$MODDIR/system/framework/framework.jar" ] && \
            nomount vfs add /system/framework/framework.jar "$MODDIR/system/framework/framework.jar" 2>/dev/null
        [ -f "$MODDIR/system/framework/services.jar" ] && \
            nomount vfs add /system/framework/services.jar "$MODDIR/system/framework/services.jar" 2>/dev/null
        if [ -f "$MODDIR/system/system_ext/framework/miui-services.jar" ]; then
            nomount vfs add /system/system_ext/framework/miui-services.jar "$MODDIR/system/system_ext/framework/miui-services.jar" 2>/dev/null
            nomount vfs add /system_ext/framework/miui-services.jar "$MODDIR/system/system_ext/framework/miui-services.jar" 2>/dev/null
        fi
    fi
fi
