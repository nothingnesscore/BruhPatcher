#@name HyperOS CN Notification Fix
#@description Eliminates notification delays and wakes background push services on HyperOS and MIUI China ROMs
#@requires services.jar,miui-services.jar

SERVICES="$SERVICES_JAR"
MIUI_SERVICES="$MIUI_SERVICES_JAR"

echo "[*] Initializing HyperOS / MIUI China Notification Delay Fix..."

if [ -n "$SERVICES_WORKSPACE" ] && [ -d "$SERVICES_WORKSPACE" ]; then
    SVC_DIR="$SERVICES_WORKSPACE"
else
    SVC_DIR="$TMP/smali_workspaces/services"
fi

if [ -n "$MIUI_SERVICES_WORKSPACE" ] && [ -d "$MIUI_SERVICES_WORKSPACE" ]; then
    MIUI_DIR="$MIUI_SERVICES_WORKSPACE"
else
    MIUI_DIR="$TMP/smali_workspaces/miui-services"
fi

return_true='
    .locals 1
    const/4 v0, 0x1
    return v0
'

return_false='
    .locals 1
    const/4 v0, 0x0
    return v0
'

# 1. miui-services.jar patches
if [ -d "$MIUI_DIR" ]; then
    echo "[*] Patching miui-services.jar for notification optimization..."

    # Allow background start and push delivery by patching ONLY target methods
    find "$MIUI_DIR" -name "*ProcessManager*.smali" -type f | while read -r f; do
        if grep -q "isAllowStart" "$f"; then
            echo "  -> Patching isAllowStart in $(basename "$f")..."
            awk '
            BEGIN { in_m = 0 }
            /\.method.*isAllowStart/ { in_m = 1 }
            in_m && /return v[0-9]+/ {
                print "    const/4 v0, 0x1"
                print "    return v0"
                in_m = 0
                next
            }
            in_m && /\.end method/ { in_m = 0 }
            { print $0 }
            ' "$f" > "${f}.tmp" && mv "${f}.tmp" "$f"
        fi
    done

    find "$MIUI_DIR" -name "*PushService*.smali" -type f | while read -r f; do
        if grep -q "isServiceRunning" "$f"; then
            echo "  -> Patching isServiceRunning in $(basename "$f")..."
            awk '
            BEGIN { in_m = 0 }
            /\.method.*isServiceRunning/ { in_m = 1 }
            in_m && /return v[0-9]+/ {
                print "    const/4 v0, 0x1"
                print "    return v0"
                in_m = 0
                next
            }
            in_m && /\.end method/ { in_m = 0 }
            { print $0 }
            ' "$f" > "${f}.tmp" && mv "${f}.tmp" "$f"
        fi
    done
    command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "miui-services.jar"
fi

# 2. services.jar patches
if [ -d "$SVC_DIR" ]; then
    echo "[*] Optimizing notification priority in services.jar..."
    find "$SVC_DIR" -name "*NotificationManagerService*.smali" -type f | while read -r f; do
        if grep -q "canShowNotification" "$f"; then
            echo "  -> Ensuring notification visibility in $(basename "$f")..."
            awk '
            BEGIN { in_m = 0 }
            /\.method.*canShowNotification/ { in_m = 1 }
            in_m && /return v[0-9]+/ {
                print "    const/4 v0, 0x1"
                print "    return v0"
                in_m = 0
                next
            }
            in_m && /\.end method/ { in_m = 0 }
            { print $0 }
            ' "$f" > "${f}.tmp" && mv "${f}.tmp" "$f"
        fi
    done
    command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "services.jar"
fi

echo "[+] HyperOS CN Notification Fix applied."
