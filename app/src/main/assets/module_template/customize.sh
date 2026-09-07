##########################################################################################
#
# Bruh Patcher - Universal Patched Framework Module
# Compatible with: Magisk, KernelSU, APatch, and NoMount VFS Redirection
#
##########################################################################################

ui_print "*********************************************************"
ui_print "*             Bruh Patcher Framework Module            *"
ui_print "*          Universal Android 8 - 17 (HyperOS)          *"
ui_print "*             NoMount VFS Redirection Ready            *"
ui_print "*********************************************************"

# Extract module files if manager did not do so automatically
if [ ! -f "$MODPATH/module.prop" ]; then
    ui_print "- Extracting module files..."
    unzip -o "$ZIPFILE" -x 'META-INF/*' -d "$MODPATH" >&2
fi

# =========================================================================
# NoMount / VFS Redirection Detection & Auto-Heal Bootloop Guard
# =========================================================================
NOMOUNT_FOUND=false
if [ -d /data/adb/modules/nomount ] || [ -d /data/adb/modules/meta-nomount ] || \
   [ -d /data/adb/modules/NoMount-Suite ] || [ -d /data/adb/nomount ] || \
   command -v nm >/dev/null 2>&1 || command -v nomount >/dev/null 2>&1; then
    NOMOUNT_FOUND=true
    ui_print "- NoMount / VFS Redirection Engine detected!"

    # Check if NoMount was disabled by a previous bootloop guard trip
    TRIPPED=false
    if [ -f /data/adb/nomount/.booting ] || [ -f /data/adb/nomount/disabled ] || \
       [ -f /data/adb/modules/nomount/disable ] || [ -f /data/adb/modules/meta-nomount/disable ] || \
       [ -f /data/adb/modules/NoMount-Suite/disable ]; then
        TRIPPED=true
    fi

    if $TRIPPED; then
        ui_print "⚠️ Notice: NoMount metamodule was disabled by bootloop guard."
        ui_print "- Resetting NoMount bootloop guard and re-enabling metamodule..."
        
        # Clear lockfiles and disable triggers
        rm -f /data/adb/nomount/.booting
        rm -f /data/adb/nomount/disabled
        rm -f /data/adb/nomount/bootcount
        rm -f /data/adb/modules/nomount/disable
        rm -f /data/adb/modules/meta-nomount/disable
        rm -f /data/adb/modules/NoMount-Suite/disable

        # Restore module description in nomount's module.prop if modified by guard
        for prop_candidate in /data/adb/modules/nomount/module.prop /data/adb/modules/meta-nomount/module.prop /data/adb/modules/NoMount-Suite/module.prop; do
            if [ -f "$prop_candidate" ]; then
                sed -i "s|description=\[🚨 DISABLED.*\\\\n|description=|" "$prop_candidate" 2>/dev/null || true
                sed -i "s|description=\[❌ ERROR.*\\\\n|description=|" "$prop_candidate" 2>/dev/null || true
            fi
        done
        ui_print "[+] NoMount metamodule re-armed successfully!"
    fi
fi

# =========================================================================
# System Extension & Partition Path Convergence
# =========================================================================
# Ensure miui-services.jar is available under both /system/system_ext/ and /system_ext/
# for seamless VFS interception on all ROMs and kernels
if [ -f "$MODPATH/system/system_ext/framework/miui-services.jar" ] && [ ! -f "$MODPATH/system_ext/framework/miui-services.jar" ]; then
    mkdir -p "$MODPATH/system_ext/framework"
    cp "$MODPATH/system/system_ext/framework/miui-services.jar" "$MODPATH/system_ext/framework/miui-services.jar"
    chmod 0644 "$MODPATH/system_ext/framework/miui-services.jar"
fi

# =========================================================================
# Deploy Kaorios Configuration & Keybox Attestation
# =========================================================================
if [ -d "$MODPATH/data/adb/kaorios" ]; then
    mkdir -p /data/adb/kaorios
    cp -rf "$MODPATH/data/adb/kaorios/"* /data/adb/kaorios/ 2>/dev/null
    chmod 0755 /data/adb/kaorios
    chmod 0644 /data/adb/kaorios/* 2>/dev/null
    ui_print "[+] Kaorios configuration and Keybox deployed to /data/adb/kaorios"
fi

# =========================================================================
# File Permissions & Execution Bits
# =========================================================================
ui_print "- Setting permissions..."
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm_recursive "$MODPATH/system/framework" 0 0 0755 0644
[ -d "$MODPATH/system/system_ext/framework" ] && set_perm_recursive "$MODPATH/system/system_ext/framework" 0 0 0755 0644
[ -d "$MODPATH/system_ext/framework" ] && set_perm_recursive "$MODPATH/system_ext/framework" 0 0 0755 0644

# Ensure hook scripts are executable
[ -f "$MODPATH/action.sh" ] && chmod 0755 "$MODPATH/action.sh"
[ -f "$MODPATH/service.sh" ] && chmod 0755 "$MODPATH/service.sh"
[ -f "$MODPATH/post-fs-data.sh" ] && chmod 0755 "$MODPATH/post-fs-data.sh"
[ -f "$MODPATH/uninstall.sh" ] && chmod 0755 "$MODPATH/uninstall.sh"

# =========================================================================
# System Priv-App Permissions & App Setup
# =========================================================================
if [ -d "$MODPATH/system/priv-app" ]; then
    set_perm_recursive "$MODPATH/system/priv-app" 0 0 0755 0644
fi
if [ -d "$MODPATH/system/etc/permissions" ]; then
    set_perm_recursive "$MODPATH/system/etc/permissions" 0 0 0755 0644
fi

# =========================================================================
# Play Integrity & Package Cache Invalidation
# =========================================================================
ui_print "- Cleaning stale Play Integrity & package caches..."
rm -f /data/data/com.google.android.gms/cache/pif.prop /data/data/com.google.android.gms/pif.prop \
    /data/data/com.google.android.gms/cache/pif.json /data/data/com.google.android.gms/pif.json
rm -rf /data/system/package_cache/*

# Run service tasks immediately to stage properties
if [ -f "$MODPATH/service.sh" ]; then
    . "$MODPATH/service.sh" 2>/dev/null || true
fi

ui_print " "
ui_print "[+] Bruh Patcher module installation completed successfully."
ui_print " "

