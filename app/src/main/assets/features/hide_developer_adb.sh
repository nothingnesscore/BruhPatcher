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
    BEGIN { in_target = 0; orig_regs = 0; new_regs = 0; target_reg = "" }
    /\.method.*getStringForUser\(Landroid\/content\/ContentResolver;Ljava\/lang\/String;I\)Ljava\/lang\/String;/ {
        in_target = 1
        print $0
        next
    }
    in_target && /^[ \t]*\.registers[ \t]+([0-9]+)/ {
        orig_regs = $2
        new_regs = orig_regs + 1
        target_reg = "v" (orig_regs - 4)
        print "    .registers " new_regs
        print "    if-eqz p2, :cond_kaorios_dev_stock"
        print "    invoke-static/range {p1 .. p3}, Landroid/security/kaorios/KaoriosHook;->shouldHideDevStatusFromNameValueCache(Landroid/content/ContentResolver;Ljava/lang/String;I)Z"
        print "    move-result " target_reg
        print "    if-eqz " target_reg ", :cond_kaorios_dev_stock"
        print "    const-string " target_reg ", \"0\""
        print "    return-object " target_reg
        print "    :cond_kaorios_dev_stock"
        in_target = 0
        next
    }
    in_target && /^[ \t]*\.locals[ \t]+([0-9]+)/ {
        orig_locs = $2
        new_locs = orig_locs + 1
        target_reg = "v" orig_locs
        print "    .locals " new_locs
        print "    if-eqz p2, :cond_kaorios_dev_stock"
        print "    invoke-static/range {p1 .. p3}, Landroid/security/kaorios/KaoriosHook;->shouldHideDevStatusFromNameValueCache(Landroid/content/ContentResolver;Ljava/lang/String;I)Z"
        print "    move-result " target_reg
        print "    if-eqz " target_reg ", :cond_kaorios_dev_stock"
        print "    const-string " target_reg ", \"0\""
        print "    return-object " target_reg
        print "    :cond_kaorios_dev_stock"
        in_target = 0
        next
    }
    in_target && /\.end method/ { in_target = 0 }
    { print $0 }
    ' "$nvc" > "${nvc}.tmp" && mv "${nvc}.tmp" "$nvc" 2>/dev/null || rm -f "${nvc}.tmp"
    echo "    Hooked: $nvc"
done

command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "framework.jar"

echo "[+] Hide Developer & ADB State applied successfully."
