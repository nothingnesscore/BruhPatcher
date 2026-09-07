#!/system/bin/sh
# Bruh Patcher / KaoriOS - Late Start Service & Property Spoofing
MODDIR=${0%/*}

# Wait for boot completion
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done
sleep 1

# =========================================================================
# Locate resetprop tool
# =========================================================================
resetprop=$(find /data/adb -name "resetprop" 2>/dev/null | head -n 1)

if [ -z "$resetprop" ]; then
    if command -v resetprop >/dev/null 2>&1; then
        resetprop="resetprop"
    else
        resetprop="/data/adb/ksu/bin/resetprop"
        [ ! -x "$resetprop" ] && resetprop="/data/adb/ap/bin/resetprop"
        [ ! -x "$resetprop" ] && resetprop="/data/adb/magisk/resetprop"
    fi
fi

# =========================================================================
# Apply Verified Boot, Lock State, and Tamper-Flag Spoofing Properties
# =========================================================================
PROPERTIES="
ro.boot.verifiedbootstate=green
ro.boot.veritymode=enforcing
vendor.boot.vbmeta.device_state=locked
ro.crypto.state=encrypted
ro.secureboot.lockstate=locked
ro.boot.flash.locked=1
ro.boot.vbmeta.device_state=locked
ro.boot.selinux=enforcing
sys.oem_unlock_allowed=0
ro.boot.veritymode.managed=yes
ro.debuggable=0
ro.force.debuggable=0
ro.secure=1
ro.boot.realmebootstate=green
ro.boot.warranty_bit=0
ro.vendor.boot.warranty_bit=0
ro.vendor.warranty_bit=0
ro.warranty_bit=0
ro.boot.realme.lockstate=1
vendor.boot.verifiedbootstate=green
"

if [ -n "$resetprop" ] && [ -x "$resetprop" -o "$resetprop" = "resetprop" ]; then
    echo "=== SET PROPERTIES ==="
    for item in $PROPERTIES; do
        key="${item%%=*}"
        val="${item#*=}"
        $resetprop "$key" "$val" 2>/dev/null
    done
    echo "Done setting properties!"
fi

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
