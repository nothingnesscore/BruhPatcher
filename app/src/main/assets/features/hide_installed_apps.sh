#@name Hide Installed Apps
#@description Per-app caller package visibility isolation (HMA alternative) via AppsFilterBase / AppsFilterImpl hooks
#@requires services.jar

SERVICES="$SERVICES_JAR"

echo "[*] Initializing Hide Installed Apps patch..."

if [ -n "$SERVICES_WORKSPACE" ] && [ -d "$SERVICES_WORKSPACE" ]; then
    SVC_DIR="$SERVICES_WORKSPACE"
else
    SVC_DIR="$TMP/smali_workspaces/services"
fi

if [ ! -d "$SVC_DIR" ]; then
    echo "[!] ERROR: services workspace directory not found: $SVC_DIR"
    return 1
fi

for af in $(find "$SVC_DIR" \( -name "AppsFilterBase.smali" -o -name "AppsFilterImpl.smali" -o -name "AppsFilter.smali" \) -type f 2>/dev/null); do
    echo "  -> Hooking $af..."
    awk '
    BEGIN { in_target = 0 }
    /\.method.*shouldFilterApplication\(/ {
        in_target = 1
    }
    in_target && /invoke-interface.*getPackageName\(\)Ljava\/lang\/String;/ {
        print $0
        print "    move-result-object v2"
        print "    const/4 v3, 0x0"
        print "    invoke-static {v3, v2}, Landroid/security/kaorios/KaoriosHook;->shouldHideAppList(Landroid/content/ContentResolver;Ljava/lang/String;)Z"
        print "    move-result v0"
        print "    if-eqz v0, :cond_kaorios_hide_stock"
        print "    const/4 v0, 0x1"
        print "    return v0"
        print "    :cond_kaorios_hide_stock"
        in_target = 0
        next
    }
    in_target && /\.end method/ { in_target = 0 }
    { print $0 }
    ' "$af" > "${af}.tmp" && mv "${af}.tmp" "$af"
    echo "    Hooked: $af"
done

command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "services.jar"

echo "[+] Hide Installed Apps applied successfully."
