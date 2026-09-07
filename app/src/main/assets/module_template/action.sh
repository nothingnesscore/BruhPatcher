#!/system/bin/sh
# KernelSU / APatch / Magisk Module Action Button Script
# Provides on-demand Play Integrity Fix cache clearing and property reset without rebooting

MODDIR="${0%/*}"
[ ! -f "$MODDIR/service.sh" ] && MODDIR="/data/adb/modules/bruhpatcher_mod"
[ ! -f "$MODDIR/service.sh" ] && MODDIR="/data/adb/modules/kaoriostoolbox"

echo "=== Bruh Patcher / KaoriOS Action ==="

if [ -f "$MODDIR/service.sh" ]; then
    echo "[*] Triggering property reset..."
    . "$MODDIR/service.sh"
else
    echo "[-] Warning: service.sh not found in $MODDIR"
fi

echo "[*] Purging GMS Play Integrity cache..."
rm -f /data/data/com.google.android.gms/cache/pif.prop /data/data/com.google.android.gms/pif.prop \
    /data/data/com.google.android.gms/cache/pif.json /data/data/com.google.android.gms/pif.json

echo "[*] Clearing package cache..."
rm -rf /data/system/package_cache/*

echo "[+] Clean PIF & Reset Properties done!"
