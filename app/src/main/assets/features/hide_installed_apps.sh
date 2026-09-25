#@name Hide Installed Apps
#@description Per-app caller package visibility isolation (HMA alternative) via ComputerEngine and AppsFilterBase hooks
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
    BEGIN { in_target = 0; seen_pkg = 0 }
    /\.method.*shouldFilterApplication\(/ {
        in_target = 1
    }
    in_target && /invoke-interface.*getPackageName\(\)Ljava\/lang\/String;/ {
        seen_pkg = 1
        print $0
        next
    }
    in_target && seen_pkg && /move-result-object[ \t]+(v[0-9]+)/ {
        pkg_str_reg = $2
        print $0
        print "    const/4 v0, 0x0"
        print "    invoke-static {v0, " pkg_str_reg "}, Landroid/security/kaorios/KaoriosHook;->shouldHideAppList(Landroid/content/ContentResolver;Ljava/lang/String;)Z"
        print "    move-result v0"
        print "    if-eqz v0, :cond_kaorios_hide_stock"
        print "    const/4 v0, 0x1"
        print "    return v0"
        print "    :cond_kaorios_hide_stock"
        seen_pkg = 0
        in_target = 0
        next
    }
    in_target && /\.end method/ { in_target = 0; seen_pkg = 0 }
    { print $0 }
    ' "$af" > "${af}.tmp" && mv "${af}.tmp" "$af" 2>/dev/null || rm -f "${af}.tmp"
    echo "    Hooked: $af"
done

for ce in $(find "$SVC_DIR" -name "ComputerEngine*.smali" -type f 2>/dev/null); do
    if grep -q "shouldFilterApplication" "$ce" 2>/dev/null; then
        echo "  -> Hooking $ce (ComputerEngine.shouldFilterApplication)..."
        awk '
        BEGIN { in_target = 0; hook_id = 0; is_pkg_state = 0; uid_reg = ""; pkg_reg = ""; user_reg = ""; target_reg = ""; param_count = 0 }
        /\.method.*shouldFilterApplication\(/ {
            in_target = 1
            hook_id++
            is_pkg_state = 0
            if ($0 ~ /shouldFilterApplication\(Lcom\/android\/server\/pm\/pkg\/PackageStateInternal;ILandroid\/content\/ComponentName;IIZZ\)Z/) {
                pkg_reg = "p1"
                uid_reg = "p2"
                user_reg = "p5"
                is_pkg_state = 1
                param_count = 7
            } else if ($0 ~ /shouldFilterApplication\(Lcom\/android\/server\/pm\/pkg\/PackageStateInternal;II\)Z/) {
                pkg_reg = "p1"
                uid_reg = "p2"
                user_reg = "p3"
                is_pkg_state = 1
                param_count = 4
            } else if ($0 ~ /shouldFilterApplication\(ILcom\/android\/server\/pm\/pkg\/PackageStateInternal;I\)Z/) {
                uid_reg = "p1"
                pkg_reg = "p2"
                user_reg = "p3"
                is_pkg_state = 1
                param_count = 4
            } else if ($0 ~ /shouldFilterApplication\(.*Ljava\/lang\/Object;.*PackageStateInternal.*\)Z/) {
                uid_reg = "p1"
                pkg_reg = "p3"
                user_reg = "p4"
                is_pkg_state = 1
                param_count = 5
            } else {
                uid_reg = "p1"
                pkg_reg = "p2"
                user_reg = "p3"
                is_pkg_state = 0
                param_count = 4
            }
            print $0
            next
        }
        in_target && /^[ \t]*\.registers[ \t]+([0-9]+)/ {
            orig_regs = $2
            new_regs = orig_regs + 1
            target_reg = "v" (orig_regs - param_count)
            print "    .registers " new_regs
            cond_lbl = (hook_id == 1) ? ":cond_kaorios_hide_stock" : (":cond_kaorios_hide_stock_" hook_id)
            print "    if-eqz " pkg_reg ", " cond_lbl
            if (is_pkg_state == 1) {
                print "    invoke-interface {" pkg_reg "}, Lcom/android/server/pm/pkg/PackageStateInternal;->getPackageName()Ljava/lang/String;"
                print "    move-result-object " target_reg
                print "    if-eqz " target_reg ", " cond_lbl
                print "    invoke-static {" uid_reg ", " target_reg ", " user_reg "}, Landroid/security/kaorios/KaoriosHook;->shouldHideAppListForCaller(ILjava/lang/String;I)Z"
            } else {
                print "    invoke-static {" uid_reg ", " pkg_reg ", " user_reg "}, Landroid/security/kaorios/KaoriosHook;->shouldHideAppListForCaller(ILjava/lang/String;I)Z"
            }
            print "    move-result " target_reg
            print "    if-eqz " target_reg ", " cond_lbl
            print "    const/4 " target_reg ", 0x1"
            print "    return " target_reg
            print "    " cond_lbl
            in_target = 0
            next
        }
        in_target && /^[ \t]*\.locals[ \t]+([0-9]+)/ {
            orig_locs = $2
            new_locs = orig_locs + 1
            target_reg = "v" orig_locs
            print "    .locals " new_locs
            cond_lbl = (hook_id == 1) ? ":cond_kaorios_hide_stock" : (":cond_kaorios_hide_stock_" hook_id)
            print "    if-eqz " pkg_reg ", " cond_lbl
            if (is_pkg_state == 1) {
                print "    invoke-interface {" pkg_reg "}, Lcom/android/server/pm/pkg/PackageStateInternal;->getPackageName()Ljava/lang/String;"
                print "    move-result-object " target_reg
                print "    if-eqz " target_reg ", " cond_lbl
                print "    invoke-static {" uid_reg ", " target_reg ", " user_reg "}, Landroid/security/kaorios/KaoriosHook;->shouldHideAppListForCaller(ILjava/lang/String;I)Z"
            } else {
                print "    invoke-static {" uid_reg ", " pkg_reg ", " user_reg "}, Landroid/security/kaorios/KaoriosHook;->shouldHideAppListForCaller(ILjava/lang/String;I)Z"
            }
            print "    move-result " target_reg
            print "    if-eqz " target_reg ", " cond_lbl
            print "    const/4 " target_reg ", 0x1"
            print "    return " target_reg
            print "    " cond_lbl
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$ce" > "${ce}.tmp" && mv "${ce}.tmp" "$ce" 2>/dev/null || rm -f "${ce}.tmp"
        echo "    Hooked: $ce"
    fi
done

command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "services.jar"

echo "[+] Hide Installed Apps applied successfully."
