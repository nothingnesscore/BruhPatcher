##########################################################################################
#
# Bruh Patcher - Universal Patched Framework Module
# Compatible with: Magisk, KernelSU, APatch
#
##########################################################################################

ui_print "*********************************************************"
ui_print "*             Bruh Patcher Framework Module            *"
ui_print "*          Universal Android 8 - 17 (HyperOS)          *"
ui_print "*********************************************************"

# Extract module files if manager did not do so automatically
if [ ! -f "$MODPATH/module.prop" ]; then
    ui_print "- Extracting module files..."
    unzip -o "$ZIPFILE" -x 'META-INF/*' -d "$MODPATH" >&2
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

# Run service tasks immediately to stage properties (skip boot wait during flashing)
if [ -f "$MODPATH/service.sh" ]; then
    SKIP_BOOT_WAIT=1 . "$MODPATH/service.sh" 2>/dev/null || true
fi

ui_print " "
ui_print "[+] Bruh Patcher module installation completed successfully."
ui_print " "
