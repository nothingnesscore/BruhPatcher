#@name Installer-Source Spoofing
#@description Spoofs installer package name (e.g. Google Play Store) in ComputerEngine.getInstallerPackageName
#@requires services.jar

SERVICES="$SERVICES_JAR"

echo "[*] Initializing Installer-Source Spoofing patch..."

if [ -n "$SERVICES_WORKSPACE" ] && [ -d "$SERVICES_WORKSPACE" ]; then
    SVC_DIR="$SERVICES_WORKSPACE"
else
    SVC_DIR="$TMP/smali_workspaces/services"
fi

if [ ! -d "$SVC_DIR" ]; then
    echo "[!] ERROR: services workspace directory not found: $SVC_DIR"
    return 1
fi

for ce in $(find "$SVC_DIR" -name "ComputerEngine*.smali" -type f 2>/dev/null); do
    if grep -q "getInstallerPackageName" "$ce" 2>/dev/null; then
        echo "  -> Hooking $ce..."
        awk '
        BEGIN { in_target = 0; orig_regs = 0; new_regs = 0; v_null = ""; v_uid = "" }
        /\.method.*getInstallerPackageName\(Ljava\/lang\/String;I\)Ljava\/lang\/String;/ {
            in_target = 1
            print $0
            next
        }
        in_target && /^\s*\.registers\s+([0-9]+)/ {
            orig_regs = $2
            new_regs = orig_regs + 2
            v_null = "v" (orig_regs - 3)
            v_uid = "v" (orig_regs - 2)
            print "    .registers " new_regs
            next
        }
        in_target && /^\s*\.locals\s+([0-9]+)/ {
            orig_locs = $2
            new_locs = orig_locs + 2
            v_null = "v" orig_locs
            v_uid = "v" (orig_locs + 1)
            print "    .locals " new_locs
            next
        }
        in_target && /return-object\s+(v[0-9]+)/ {
            ret_reg = $2
            if (v_null == "") {
                v_null = "v0"
                v_uid = "v1"
            }
            print "    const/16 " v_null ", 0x0"
            print "    invoke-static {}, Landroid/os/Binder;->getCallingUid()I"
            print "    move-result " v_uid
            print "    invoke-static {" v_null ", " v_uid ", p2, p1, " ret_reg "}, Landroid/security/kaorios/KaoriosHook;->filterInstallerPackageName(Landroid/content/ContentResolver;IILjava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
            print "    move-result-object " ret_reg
            print $0
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$ce" > "${ce}.tmp" && mv "${ce}.tmp" "$ce"
        echo "    Hooked: $ce"
    fi
done

command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "services.jar"

echo "[+] Installer-Source Spoofing applied successfully."
