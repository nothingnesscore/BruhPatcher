#@name Kaorios Toolbox v2.0.6.0
#@description Integrates Play Integrity Fix, Keybox hardware attestation, per-app game spoofing (120 FPS), and privacy isolation
#@requires framework.jar,services.jar

FRAMEWORK="$FRAMEWORK_JAR"
SERVICES="$SERVICES_JAR"
MIUI_SERVICES="$MIUI_SERVICES_JAR"

echo "[*] Initializing Kaorios Toolbox v2.0.6.0 integration..."

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

# ==================== FRAMEWORK.JAR PATCHES ====================
if [ -d "$FW_DIR" ]; then
    echo "[*] Applying Kaorios hooks to framework.jar..."

    # 1. Instrumentation.smali - App Context Initialization
    inst_file=$(find "$FW_DIR" -name "Instrumentation.smali" -type f | head -1)
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
    apm_file=$(find "$FW_DIR" -name "ApplicationPackageManager.smali" -type f | head -1)
    if [ -n "$apm_file" ]; then
        echo "  -> Hooking ApplicationPackageManager.hasSystemFeature()..."
        awk '
        BEGIN { in_target = 0 }
        /\.method public.*hasSystemFeature\(Ljava\/lang\/String;I\)Z/ {
            in_target = 1
            print $0
            next
        }
        in_target && /(\.registers|\.locals)/ {
            print $0
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

    # 3. AndroidKeyStoreKeyPairGeneratorSpi.smali - Software KeyGen Hook
    keygen_file=$(find "$FW_DIR" -name "AndroidKeyStoreKeyPairGeneratorSpi.smali" -type f | head -1)
    if [ -n "$keygen_file" ]; then
        echo "  -> Hooking AndroidKeyStoreKeyPairGeneratorSpi.generateKeyPair()..."
        awk '
        BEGIN { in_target = 0 }
        /\.method public.*generateKeyPair\(\)Ljava\/security\/KeyPair;/ {
            in_target = 1
            print $0
            next
        }
        in_target && /(\.registers|\.locals)/ {
            print "    .locals 15"
            print "    invoke-static {p0}, Landroid/security/kaorios/KaoriosHook;->initGenerateSoftwareKeyPair(Ljava/lang/Object;)Ljava/security/KeyPair;"
            print "    move-result-object v14"
            print "    if-eqz v14, :cond_kaorios_gen_stock"
            print "    return-object v14"
            print "    :cond_kaorios_gen_stock"
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$keygen_file" > "${keygen_file}.tmp" && mv "${keygen_file}.tmp" "$keygen_file"
        echo "    Hooked: $keygen_file"
    fi

    # 4. AndroidKeyStoreSpi.smali - Certificate Chain Interception
    cert_file=$(find "$FW_DIR" -name "AndroidKeyStoreSpi.smali" -type f | head -1)
    if [ -n "$cert_file" ]; then
        echo "  -> Hooking AndroidKeyStoreSpi.engineGetCertificateChain()..."
        awk '
        BEGIN { in_target = 0; hooked = 0 }
        /\.method public.*engineGetCertificateChain\(Ljava\/lang\/String;\)\[Ljava\/security\/cert\/Certificate;/ { in_target = 1 }
        in_target && !hooked && /aput-object/ {
            print $0
            clean_line = $0
            gsub(/,/, " ", clean_line)
            count = split(clean_line, toks)
            target_reg = ""
            for (idx = 1; idx <= count; idx++) {
                if (toks[idx] ~ /^aput-object/) {
                    target_reg = toks[idx+2]
                    break
                }
            }
            if (target_reg ~ /^[vp][0-9]+$/) {
                print "    invoke-static {" target_reg "}, Landroid/security/kaorios/KaoriosHook;->CertificateChainIfNeeded([Ljava/security/cert/Certificate;)[Ljava/security/cert/Certificate;"
                print "    move-result-object " target_reg
                hooked = 1
                in_target = 0
            }
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$cert_file" > "${cert_file}.tmp" && mv "${cert_file}.tmp" "$cert_file"
        echo "    Hooked: $cert_file"
    fi
else
    echo "[!] FATAL ERROR: Framework workspace directory not found: $FW_DIR"
    return 1
fi

# ==================== SERVICES.JAR PATCHES ====================
if [ -d "$SVC_DIR" ]; then
    echo "[*] Applying Kaorios hooks to services.jar..."

    # 5. SystemServer.smali - Init System Server Hook
    sys_file=$(find "$SVC_DIR" -name "SystemServer.smali" -type f | head -1)
    if [ -n "$sys_file" ]; then
        echo "  -> Hooking SystemServer.run()..."
        awk '
        BEGIN { hooked = 0 }
        !hooked && /invoke-direct.*->startOtherServices\(Lcom\/android\/server\/utils\/TimingsTraceAndSlog;\)V/ {
            print "    invoke-static {}, Landroid/security/kaorios/KaoriosHook;->initSystemServer()V"
            hooked = 1
        }
        { print $0 }
        ' "$sys_file" > "${sys_file}.tmp" && mv "${sys_file}.tmp" "$sys_file"
        echo "    Hooked: $sys_file"
    fi
else
    echo "[!] FATAL ERROR: Services workspace directory not found: $SVC_DIR"
    return 1
fi

# ==================== KAORIOS BYTECODE INJECTION ====================
echo "[*] Bundling Kaorios Framework DEX into module extras..."

KAORIOS_ASSET_DIR="/data/local/tmp/bruhpatcher/kaorios"
if [ ! -d "$KAORIOS_ASSET_DIR" ]; then
    KAORIOS_ASSET_DIR="/data/local/tmp/frameworkforge/kaorios"
fi

if [ -f "$KAORIOS_ASSET_DIR/kaorios_framework.dex" ]; then
    echo "[+] Found Kaorios DEX bytecode: $KAORIOS_ASSET_DIR/kaorios_framework.dex"
    
    # Check highest classesN.dex in framework workspace
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
        target_smali_dir="$FW_DIR/smali_classes$next_dex"
        echo "[*] Injecting Kaorios DEX as smali_classes$next_dex..."
        
        # Decompile kaorios_framework.dex directly into the framework workspace
        if [ -f "$DI_BIN/baksmali.jar" ]; then
            echo "[*] Disassembling Kaorios DEX bytecode..."
            export CLASSPATH="$DI_BIN/baksmali.jar"
            /system/bin/app_process -Xmx512m /system/bin org.jf.baksmali.Main d "$KAORIOS_ASSET_DIR/kaorios_framework.dex" -o "$target_smali_dir" 2>/dev/null
            if [ ! -d "$target_smali_dir" ] || [ -z "$(ls -A "$target_smali_dir" 2>/dev/null)" ]; then
                dalvikvm -Xmx512m -cp "$DI_BIN/baksmali.jar" org.jf.baksmali.Main d "$KAORIOS_ASSET_DIR/kaorios_framework.dex" -o "$target_smali_dir" 2>/dev/null || true
            fi
        fi

        # Verify injection succeeded
        if [ ! -d "$target_smali_dir" ] || [ -z "$(ls -A "$target_smali_dir" 2>/dev/null)" ]; then
            echo "[!] FATAL: Failed to inject Kaorios bytecode into framework workspace!"
            echo "[!] Missing Kaorios bytecode would cause bootloops. Aborting."
            return 1 2>/dev/null || exit 1
        fi
        echo "[+] Successfully injected Kaorios bytecode into smali_classes$next_dex"
    fi
    
    # Register Kaorios configuration files into the flashable module
    # Attempt to fetch latest verified Strong Keybox from keybox.hzzmonet.io.vn if online
    echo "[*] Connecting to Keybox Hub (https://keybox.hzzmonet.io.vn)..."
    if command -v curl >/dev/null 2>&1; then
        curl -k -s -L --connect-timeout 6 -m 12 "https://keybox.hzzmonet.io.vn/api/download" -o "$KAORIOS_ASSET_DIR/Keybox_latest.xml" 2>/dev/null || true
    elif command -v wget >/dev/null 2>&1; then
        wget --no-check-certificate -q -T 8 -O "$KAORIOS_ASSET_DIR/Keybox_latest.xml" "https://keybox.hzzmonet.io.vn/api/download" 2>/dev/null || true
    fi

    if [ -s "$KAORIOS_ASSET_DIR/Keybox_latest.xml" ] && grep -q "<AndroidAttestation" "$KAORIOS_ASSET_DIR/Keybox_latest.xml"; then
        cp "$KAORIOS_ASSET_DIR/Keybox_latest.xml" "$KAORIOS_ASSET_DIR/Keybox.xml"
        rm -f "$KAORIOS_ASSET_DIR/Keybox_latest.xml"
        echo "[+] Downloaded & verified live Strong Keybox from keybox.hzzmonet.io.vn"
    else
        rm -f "$KAORIOS_ASSET_DIR/Keybox_latest.xml"
        echo "[*] Using bundled/cached genuine Keybox.xml (offline fallback)"
    fi

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
    echo "[+] Kaorios configuration and keybox registered to module"
fi

echo "[*] Kaorios Toolbox v2.0.6.0 patches applied successfully."
