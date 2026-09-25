#@name Settings Value Provider Filtering
#@description Intercepts and filters system and secure settings values via SettingsProvider.call (KaoriOS v3.0 Android 17 / HyperOS)
#@requires services.jar

SERVICES="$SERVICES_JAR"

echo "[*] Initializing Settings Value Provider Filtering patch..."

if [ -n "$SERVICES_WORKSPACE" ] && [ -d "$SERVICES_WORKSPACE" ]; then
    SVC_DIR="$SERVICES_WORKSPACE"
else
    SVC_DIR="$TMP/smali_workspaces/services"
fi

if [ -n "$FRAMEWORK_WORKSPACE" ] && [ -d "$FRAMEWORK_WORKSPACE" ]; then
    FW_DIR="$FRAMEWORK_WORKSPACE"
else
    FW_DIR="$TMP/smali_workspaces/framework"
fi

if [ ! -d "$SVC_DIR" ] && [ ! -d "$FW_DIR" ]; then
    echo "[!] ERROR: Neither framework nor services workspace directory found"
    return 1
fi

sp_hooked=0
for sp in $(find "$FW_DIR" "$SVC_DIR" -name "SettingsProvider.smali" -type f 2>/dev/null); do
    echo "  -> Hooking $sp (SettingsProvider.call)..."
    awk '
    BEGIN { in_target = 0; orig_regs = 0; new_regs = 0; target_reg = "" }
    /\.method.*call\(Ljava\/lang\/String;Ljava\/lang\/String;Landroid\/os\/Bundle;\)Landroid\/os\/Bundle;/ {
        in_target = 1
        print $0
        next
    }
    in_target && /^[ \t]*\.registers[ \t]+([0-9]+)/ {
        orig_regs = $2
        new_regs = orig_regs + 1
        target_reg = "v" (orig_regs - 4)
        print "    .registers " new_regs
        print "    if-eqz p1, :cond_kaorios_settings_stock"
        print "    if-eqz p2, :cond_kaorios_settings_stock"
        print "    invoke-static/range {p1 .. p2}, Landroid/security/kaorios/KaoriosHook;->filterSettingsCall(Ljava/lang/String;Ljava/lang/String;)Landroid/os/Bundle;"
        print "    move-result-object " target_reg
        print "    if-eqz " target_reg ", :cond_kaorios_settings_stock"
        print "    return-object " target_reg
        print "    :cond_kaorios_settings_stock"
        in_target = 0
        next
    }
    in_target && /^[ \t]*\.locals[ \t]+([0-9]+)/ {
        orig_locs = $2
        new_locs = orig_locs + 1
        target_reg = "v" orig_locs
        print "    .locals " new_locs
        print "    if-eqz p1, :cond_kaorios_settings_stock"
        print "    if-eqz p2, :cond_kaorios_settings_stock"
        print "    invoke-static/range {p1 .. p2}, Landroid/security/kaorios/KaoriosHook;->filterSettingsCall(Ljava/lang/String;Ljava/lang/String;)Landroid/os/Bundle;"
        print "    move-result-object " target_reg
        print "    if-eqz " target_reg ", :cond_kaorios_settings_stock"
        print "    return-object " target_reg
        print "    :cond_kaorios_settings_stock"
        in_target = 0
        next
    }
    in_target && /\.end method/ { in_target = 0 }
    { print $0 }
    ' "$sp" > "${sp}.tmp" && mv "${sp}.tmp" "$sp" 2>/dev/null || rm -f "${sp}.tmp"
    echo "    Hooked: $sp"
    sp_hooked=1
done

if [ -d "$SVC_DIR" ] && [ "$sp_hooked" -eq 1 ]; then
    command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "services.jar"
fi

if [ "$sp_hooked" -eq 0 ]; then
    echo "[*] Notice: SettingsProvider.smali not packaged in services.jar/framework.jar (resides in standalone SettingsProvider.apk). Skipping server-side call hook."
fi

echo "[+] Settings Value Provider Filtering applied successfully."
