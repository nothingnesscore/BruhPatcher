#@name KaoriOS Framework v3.0 Core
#@description Essential KaoriOS Core: payload DEX injection, Hardware Attestation Keybox hooks, SystemServer and ApplicationPackageManager integration
#@requires framework.jar,services.jar

FRAMEWORK="$FRAMEWORK_JAR"
SERVICES="$SERVICES_JAR"

echo "[*] Initializing KaoriOS Framework v3.0 Core integration..."

# Determine workspace directories from smali_workspace or fallback
if [ -n "$FRAMEWORK_WORKSPACE" ] && [ -d "$FRAMEWORK_WORKSPACE" ]; then
    FW_DIR="$FRAMEWORK_WORKSPACE"
else
    FW_DIR="$TMP/smali_workspaces/framework"
fi

if [ -n "$SERVICES_WORKSPACE" ] && [ -d "$SERVICES_WORKSPACE" ]; then
    SVC_DIR="$SERVICES_WORKSPACE"
else
    SVC_DIR="$TMP/smali_workspaces/services"
fi

# ==================== FRAMEWORK.JAR HOOKS ====================
if [ -d "$FW_DIR" ]; then
    echo "[*] Applying KaoriOS Core hooks to framework.jar..."

    # 1. Instrumentation.smali - App Context Initialization
    inst_file=$(find "$FW_DIR" -name "Instrumentation.smali" -type f 2>/dev/null | head -n 1)
    if [ -n "$inst_file" ]; then
        echo "  -> Hooking Instrumentation.newApplication()..."
        awk '
        BEGIN { in_method = 0 }
        /\.method public static.*newApplication\(Ljava\/lang\/Class;Landroid\/content\/Context;\)Landroid\/app\/Application;/ { in_method = 1 }
        /\.method public.*newApplication\(Ljava\/lang\/ClassLoader;Ljava\/lang\/String;Landroid\/content\/Context;\)Landroid\/app\/Application;/ { in_method = 2 }
        in_method == 1 && /return-object/ {
            print "    invoke-static {p1}, Landroid/security/kaorios/KaoriosHook;->initContext(Landroid/content/Context;)V"
            in_method = 0
        }
        in_method == 2 && /return-object/ {
            print "    invoke-static {p3}, Landroid/security/kaorios/KaoriosHook;->initContext(Landroid/content/Context;)V"
            in_method = 0
        }
        { print $0 }
        ' "$inst_file" > "${inst_file}.tmp" && mv "${inst_file}.tmp" "$inst_file"
        echo "    Hooked: $inst_file"
    fi

    # 2. ApplicationPackageManager.smali - System Feature Hook
    apm_file=$(find "$FW_DIR" -name "ApplicationPackageManager.smali" -type f 2>/dev/null | head -n 1)
    if [ -n "$apm_file" ]; then
        echo "  -> Hooking ApplicationPackageManager.hasSystemFeature()..."
        awk '
        BEGIN { in_target = 0 }
        /\.method public.*hasSystemFeature\(Ljava\/lang\/String;I\)Z/ {
            in_target = 1
            print $0
            next
        }
        in_target && /^\s*\.locals\s+([0-9]+)/ {
            loc = $2
            if (loc < 1) loc = 1
            print "    .locals " loc
            print "    invoke-static {p1, p2}, Landroid/security/kaorios/KaoriosHook;->hasSystemFeature(Ljava/lang/String;I)Ljava/lang/Boolean;"
            print "    move-result-object v0"
            print "    if-eqz v0, :cond_kaorios_feature_stock"
            print "    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z"
            print "    move-result v0"
            print "    return v0"
            print "    :cond_kaorios_feature_stock"
            in_target = 0
            next
        }
        in_target && /^\s*\.registers\s+([0-9]+)/ {
            regs = $2
            if (regs < 4) regs = 4
            print "    .registers " regs
            print "    invoke-static {p1, p2}, Landroid/security/kaorios/KaoriosHook;->hasSystemFeature(Ljava/lang/String;I)Ljava/lang/Boolean;"
            print "    move-result-object v0"
            print "    if-eqz v0, :cond_kaorios_feature_stock"
            print "    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z"
            print "    move-result v0"
            print "    return v0"
            print "    :cond_kaorios_feature_stock"
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$apm_file" > "${apm_file}.tmp" && mv "${apm_file}.tmp" "$apm_file"
        echo "    Hooked: $apm_file"
    fi

    # 3. AndroidKeyStoreKeyPairGeneratorSpi.smali - Software KeyGen Hook (Upstream specification)
    keygen_file=$(find "$FW_DIR" -name "AndroidKeyStoreKeyPairGeneratorSpi.smali" -type f 2>/dev/null | head -n 1)
    if [ -n "$keygen_file" ]; then
        echo "  -> Hooking AndroidKeyStoreKeyPairGeneratorSpi.generateKeyPair()..."
        awk '
        BEGIN { in_target = 0; orig_regs = 0; new_regs = 0; target_reg = "" }
        /\.method public.*generateKeyPair\(\)Ljava\/security\/KeyPair;/ {
            in_target = 1
            print $0
            next
        }
        in_target && /^\s*\.registers\s+([0-9]+)/ {
            orig_regs = $2
            new_regs = orig_regs + 1
            target_reg = "v" (new_regs - 2)
            print "    .registers " new_regs
            print "    invoke-static {p0}, Landroid/security/kaorios/KaoriosHook;->initGenerateSoftwareKeyPair(Ljava/lang/Object;)Ljava/security/KeyPair;"
            print "    move-result-object " target_reg
            print "    if-eqz " target_reg ", :cond_kaorios_gen_stock"
            print "    return-object " target_reg
            print "    :cond_kaorios_gen_stock"
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$keygen_file" > "${keygen_file}.tmp" && mv "${keygen_file}.tmp" "$keygen_file"
        echo "    Hooked: $keygen_file"
    fi

    # 4. AndroidKeyStoreSpi.smali - Certificate Chain Hook
    chain_file=$(find "$FW_DIR" -name "AndroidKeyStoreSpi.smali" -type f 2>/dev/null | head -n 1)
    if [ -n "$chain_file" ]; then
        echo "  -> Hooking AndroidKeyStoreSpi.engineGetCertificateChain()..."
        awk '
        BEGIN { in_target = 0 }
        /\.method public.*engineGetCertificateChain\(Ljava\/lang\/String;\)\[Ljava\/security\/cert\/Certificate;/ {
            in_target = 1
        }
        in_target && /aput-object\s+([^,]+),\s*([^,]+),\s*([^,]+)/ {
            cert_reg = $2
            arr_reg = $3
            idx_reg = $4
            gsub(/,/, "", cert_reg)
            gsub(/,/, "", arr_reg)
            gsub(/,/, "", idx_reg)
            print $0
            print "    invoke-static {" arr_reg "}, Landroid/security/kaorios/KaoriosHook;->CertificateChainIfNeeded([Ljava/security/cert/Certificate;)[Ljava/security/cert/Certificate;"
            print "    move-result-object " arr_reg
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$chain_file" > "${chain_file}.tmp" && mv "${chain_file}.tmp" "$chain_file"
        echo "    Hooked: $chain_file"
    fi

    # 5. ActivityThread.smali - Preload ActivityThread context
    act_file=$(find "$FW_DIR" -name "ActivityThread.smali" -type f 2>/dev/null | head -n 1)
    if [ -n "$act_file" ]; then
        echo "  -> Hooking ActivityThread.handleBindApplication()..."
        awk '
        BEGIN { in_target = 0; hooked = 0 }
        /\.method private.*handleBindApplication\(Landroid\/app\/ActivityThread\$AppBindData;\)V/ { in_target = 1 }
        in_target && !hooked && /iput-object\s+p1,\s*p0,\s*Landroid\/app\/ActivityThread;->mBoundApplication:/ {
            print $0
            print "    invoke-static {p1}, Landroid/security/kaorios/KaoriosHook;->initActivityThread(Ljava/lang/Object;)V"
            hooked = 1
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$act_file" > "${act_file}.tmp" && mv "${act_file}.tmp" "$act_file" 2>/dev/null || rm -f "${act_file}.tmp"
        echo "    Hooked: $act_file"
    fi

else
    echo "[!] FATAL ERROR: Framework workspace directory not found: $FW_DIR"
    return 1
fi

# ==================== SERVICES.JAR HOOKS ====================
if [ -d "$SVC_DIR" ]; then
    echo "[*] Applying KaoriOS Core hooks to services.jar..."

    # SystemServer.smali - Server Initialization Hook
    sys_server=$(find "$SVC_DIR" -name "SystemServer.smali" -type f 2>/dev/null | head -n 1)
    if [ -n "$sys_server" ]; then
        echo "  -> Hooking SystemServer.run()..."
        awk '
        BEGIN { in_run = 0; hooked = 0 }
        /\.method.*run\(\)V/ { in_run = 1 }
        in_run && !hooked && /invoke-static.*Landroid\/os\/Looper;->loop\(\)V/ {
            print "    invoke-static {}, Landroid/security/kaorios/KaoriosHook;->initSystemServer()V"
            hooked = 1
            print $0
            in_run = 0
            next
        }
        in_run && /\.end method/ { in_run = 0 }
        { print $0 }
        ' "$sys_server" > "${sys_server}.tmp" && mv "${sys_server}.tmp" "$sys_server"
        echo "    Hooked: $sys_server"
    fi

    command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "services.jar"
else
    echo "[!] FATAL ERROR: Services workspace directory not found: $SVC_DIR"
    return 1
fi

# ==================== KAORIOS BYTECODE & PAYLOAD INJECTION ====================
echo "[*] Bundling KaoriOS Framework DEX into framework.jar multi-dex..."

KAORIOS_ASSET_DIR="/data/local/tmp/bruhpatcher/kaorios"

if [ -f "$KAORIOS_ASSET_DIR/kaorios_framework.dex" ]; then
    echo "[+] Found KaoriOS DEX bytecode: $KAORIOS_ASSET_DIR/kaorios_framework.dex"

    if [ -d "$FW_DIR" ]; then
        max_dex=1
        for d in "$FW_DIR"/smali*; do
            [ -d "$d" ] || continue
            num=$(basename "$d" | sed 's/smali_classes//;s/smali//')
            [ -z "$num" ] && num=1
            if [ "$num" -gt "$max_dex" ] 2>/dev/null; then
                max_dex=$num
            fi
        done
        next_dex=$((max_dex + 1))
        echo "[*] Staging KaoriOS bytecode as classes${next_dex}.dex..."

        mkdir -p "$FW_DIR/.extra_dex"
        cp -f "$KAORIOS_ASSET_DIR/kaorios_framework.dex" "$FW_DIR/.extra_dex/classes${next_dex}.dex"
        chmod 644 "$FW_DIR/.extra_dex/classes${next_dex}.dex"

        if [ -s "$FW_DIR/.extra_dex/classes${next_dex}.dex" ]; then
            echo "[+] Successfully staged KaoriOS DEX as classes${next_dex}.dex"
        else
            echo "[!] FATAL: Failed to stage KaoriOS DEX payload!"
            return 1
        fi

        command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "framework.jar"
    fi

    # Register module configuration files
    if [ -f "$KAORIOS_ASSET_DIR/Keybox.xml" ]; then
        add_to_module "$KAORIOS_ASSET_DIR/Keybox.xml" "data/adb/kaorios/Keybox.xml" "file"
        echo "[+] Keybox hardware attestation registered to module"
    fi
    if [ -f "$KAORIOS_ASSET_DIR/Pif-props.json" ]; then
        add_to_module "$KAORIOS_ASSET_DIR/Pif-props.json" "data/adb/kaorios/Pif-props.json" "file"
    fi
    if [ -f "$KAORIOS_ASSET_DIR/app-props.json" ]; then
        add_to_module "$KAORIOS_ASSET_DIR/app-props.json" "data/adb/kaorios/app-props.json" "file"
    fi
    if [ -f "$KAORIOS_ASSET_DIR/device-model.json" ]; then
        add_to_module "$KAORIOS_ASSET_DIR/device-model.json" "data/adb/kaorios/device-model.json" "file"
    fi

    # Register KaoriOS Companion App as privileged system app
    if [ -f "$KAORIOS_ASSET_DIR/KaoriosToolbox.apk" ]; then
        add_to_module "$KAORIOS_ASSET_DIR/KaoriosToolbox.apk" "system/priv-app/KaoriosToolbox/KaoriosToolbox.apk" "apk"
        echo "[+] KaoriOS Toolbox APK registered to module (system/priv-app)"
    fi
    if [ -f "$KAORIOS_ASSET_DIR/com.kousei.kaorios.xml" ]; then
        add_to_module "$KAORIOS_ASSET_DIR/com.kousei.kaorios.xml" "system/etc/permissions/com.kousei.kaorios.xml" "xml"
        echo "[+] KaoriOS privapp permission whitelist registered to module"
    fi
    if [ -d "$KAORIOS_ASSET_DIR/lib" ]; then
        for so_file in $(find "$KAORIOS_ASSET_DIR/lib" -type f); do
            rel_so=$(echo "$so_file" | sed "s|^$KAORIOS_ASSET_DIR/lib/||")
            add_to_module "$so_file" "system/priv-app/KaoriosToolbox/lib/$rel_so" "lib"
        done
        echo "[+] KaoriOS companion native libraries registered to module"
    fi
fi

echo "[+] KaoriOS Framework v3.0 Core patches applied successfully."
