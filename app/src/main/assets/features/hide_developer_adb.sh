#@name Hide Developer & ADB State
#@description Hides developer options, USB debugging, and ADB status from root detectors and banking apps via Settings$NameValueCache
#@requires framework.jar

FRAMEWORK="$FRAMEWORK_JAR"

echo "[*] Initializing Hide Developer Options & ADB State patch..."

if [ -n "$FRAMEWORK_WORKSPACE" ] && [ -d "$FRAMEWORK_WORKSPACE" ]; then
    FW_DIR="$FRAMEWORK_WORKSPACE"
else
    FW_DIR="$TMP/smali_workspaces/framework"
fi

if [ ! -d "$FW_DIR" ]; then
    echo "[!] ERROR: framework workspace directory not found: $FW_DIR"
    return 1
fi

for nvc in $(find "$FW_DIR" \( -name "Settings\$NameValueCache.smali" -o -name "Settings_NameValueCache.smali" \) -type f 2>/dev/null); do
    echo "  -> Hooking $nvc..."
    awk '
    BEGIN { in_target = 0 }
    /\.method.*getStringForUser\(Landroid\/content\/ContentResolver;Ljava\/lang\/String;I\)Ljava\/lang\/String;/ {
        in_target = 1
        print $0
        next
    }
    in_target && /^\s*\.registers\s+([0-9]+)/ {
        print $0
        print "    if-eqz p2, :cond_kaorios_dev_stock"
        print "    invoke-static/range {p1 .. p3}, Landroid/security/kaorios/KaoriosHook;->shouldHideDevStatusFromNameValueCache(Landroid/content/ContentResolver;Ljava/lang/String;I)Z"
        print "    move-result v0"
        print "    if-eqz v0, :cond_kaorios_dev_stock"
        print "    const-string v0, \"0\""
        print "    return-object v0"
        print "    :cond_kaorios_dev_stock"
        in_target = 0
        next
    }
    in_target && /\.end method/ { in_target = 0 }
    { print $0 }
    ' "$nvc" > "${nvc}.tmp" && mv "${nvc}.tmp" "$nvc"
    echo "    Hooked: $nvc"
done

command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "framework.jar"

echo "[+] Hide Developer & ADB State applied successfully."
