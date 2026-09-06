#@name Google Photos Unlimited Backup
#@description Unlocks unlimited original-quality cloud backup in Google Photos by spoofing Pixel XL
#@requires framework.jar

FRAMEWORK="$FRAMEWORK_JAR"

echo "[*] Initializing Google Photos Unlimited Backup patch..."

if [ -n "$FRAMEWORK_WORKSPACE" ] && [ -d "$FRAMEWORK_WORKSPACE" ]; then
    FW_DIR="$FRAMEWORK_WORKSPACE"
else
    FW_DIR="$TMP/smali_workspaces/framework"
fi

if [ ! -d "$FW_DIR" ]; then
    echo "[!] ERROR: framework workspace directory not found"
    return 1
fi

# In framework.jar, patch ApplicationPackageManager or register Pixel feature flags
echo "[*] Patching Google Photos Pixel feature flags..."

apm_file=$(find "$FW_DIR" -name "ApplicationPackageManager.smali" -type f | head -1)
if [ -n "$apm_file" ]; then
    echo "  -> Adding Google Photos Pixel capability hooks..."
    # If not already hooked by Kaorios, inject check
    if ! grep -q "nexus_preload" "$apm_file"; then
        awk '
        BEGIN { in_feature = 0 }
        /\.method public.*hasSystemFeature\(Ljava\/lang\/String;I\)Z/ { in_feature = 1 }
        in_feature && /return v[0-9]+/ {
            print "    const-string v1, \"com.google.android.apps.photos.NEXUS_PRELOAD\""
            print "    invoke-virtual {p1, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z"
            print "    move-result v1"
            print "    if-eqz v1, :cond_photos_stock"
            print "    const/4 v0, 0x1"
            print "    return v0"
            print "    :cond_photos_stock"
            in_feature = 0
        }
        { print $0 }
        ' "$apm_file" > "${apm_file}.tmp" && mv "${apm_file}.tmp" "$apm_file"
        echo "[+] Injected Nexus preload capability"
    fi
fi

# Register system prop for Google Photos
echo "persist.sys.pixel.photos=1" >> "$OUTPUT_DIR/system.prop.temp"
if [ -f "$OUTPUT_DIR/system.prop.temp" ]; then
    add_to_module "$OUTPUT_DIR/system.prop.temp" "system.prop" "props"
fi

echo "[+] Google Photos Unlimited Backup patch complete."
