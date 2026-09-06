#!/system/bin/sh
# Do NOT assume where your module will be located.
# ALWAYS use $MODDIR if you need to know where this script and module is placed.
# This will make sure your module will still work
# if Magisk change its mount point in the future
MODDIR=${0%/*}

# Wait for boot to complete
while [ "$(getprop sys.boot_completed)" != "1" ]; do
  sleep 1
done

# Install Kaorios Toolbox as user app (update) if present
if [ -f "$MODDIR/system/product/priv-app/KaoriosToolbox/KaoriosToolbox.apk" ]; then
  pm install -r "$MODDIR/system/product/priv-app/KaoriosToolbox/KaoriosToolbox.apk" >/dev/null 2>&1
fi
