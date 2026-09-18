#@name Settings Value Provider Filtering
#@description Intercepts and filters system and secure settings values, allowing per-app override or removal of sensitive keys
#@requires framework.jar

FRAMEWORK="$FRAMEWORK_JAR"

echo "[*] Initializing Settings Value Provider Filtering patch..."

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
    echo "  -> Hooking $nvc for settings filtering..."
    awk '
    BEGIN { in_target = 0 }
    /\.method.*getStringForUser\(Landroid\/content\/ContentResolver;Ljava\/lang\/String;I\)Ljava\/lang\/String;/ {
        in_target = 1
    }
    in_target && /return-object\s+(v[0-9]+)/ {
        ret_reg = $2
        print "    const/4 vNull, 0x0"
        print "    const-string vNamespace, \"system\""
        print "    invoke-static {p1, vNamespace, p2, " ret_reg "}, Landroid/security/kaorios/KaoriosHook;->filterSettingValue(Landroid/content/ContentResolver;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
        print "    move-result-object " ret_reg
        print $0
        in_target = 0
        next
    }
    in_target && /\.end method/ { in_target = 0 }
    { print $0 }
    ' "$nvc" > "${nvc}.tmp" && mv "${nvc}.tmp" "$nvc"
    echo "    Hooked: $nvc"
done

command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "framework.jar"

echo "[+] Settings Value Provider Filtering applied successfully."
