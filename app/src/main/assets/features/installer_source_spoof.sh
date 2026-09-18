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
        BEGIN { in_target = 0 }
        /\.method.*getInstallerPackageName\(Ljava\/lang\/String;I\)Ljava\/lang\/String;/ {
            in_target = 1
        }
        in_target && /return-object\s+(v[0-9]+)/ {
            ret_reg = $2
            print "    const/4 vNull, 0x0"
            print "    invoke-static {}, Landroid/os/Binder;->getCallingUid()I"
            print "    move-result vCallingUid"
            print "    invoke-static {vNull, vCallingUid, p2, p1, " ret_reg "}, Landroid/security/kaorios/KaoriosHook;->filterInstallerPackageName(Landroid/content/ContentResolver;IILjava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
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
