#!/system/bin/sh
# Bruh Patcher / KaoriOS - Late Start Service & Property Spoofing
MODDIR=${0%/*}

# Wait for boot completion (unless explicitly skipped during recovery/action flashing)
if [ -z "$SKIP_BOOT_WAIT" ]; then
    until [ "$(getprop sys.boot_completed)" = "1" ]; do
        sleep 1
    done
    sleep 1
fi

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
# Install Kaorios Toolbox APK if present and not yet registered
# =========================================================================
if ! pm path com.kousei.kaorios >/dev/null 2>&1; then
    if [ -f "$MODDIR/system/product/priv-app/KaoriosToolbox/KaoriosToolbox.apk" ]; then
        pm install -r "$MODDIR/system/product/priv-app/KaoriosToolbox/KaoriosToolbox.apk" >/dev/null 2>&1
    elif [ -f "$MODDIR/system/priv-app/KaoriosToolbox/KaoriosToolbox.apk" ]; then
        pm install -r "$MODDIR/system/priv-app/KaoriosToolbox/KaoriosToolbox.apk" >/dev/null 2>&1
    fi
fi
