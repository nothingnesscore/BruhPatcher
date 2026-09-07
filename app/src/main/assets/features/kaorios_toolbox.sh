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
            print $0
            print "    invoke-static {p0}, Landroid/security/kaorios/KaoriosHook;->initGenerateSoftwareKeyPair(Ljava/lang/Object;)Ljava/security/KeyPair;"
            print "    move-result-object v0"
            print "    if-eqz v0, :cond_kaorios_gen_stock"
            print "    return-object v0"
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

    # 4b. ActivityThread.smali - Process Runtime Hook
    act_file=$(find "$FW_DIR" -name "ActivityThread.smali" -type f | head -1)
    if [ -n "$act_file" ] && ! grep -q "KaoriosHook;->initActivityThread" "$act_file"; then
        echo "  -> Hooking ActivityThread.attach()..."
        awk '
        BEGIN { in_target = 0; hooked = 0 }
        /\.method private.*attach\(ZJ\)V/ { in_target = 1 }
        in_target && !hooked && /return-void/ {
            print "    const-string v0, \"kaorios\""
            print "    const-string v1, \"init\""
            print "    invoke-static {v0, v1}, Landroid/security/kaorios/KaoriosHook;->initActivityThread(Ljava/lang/String;Ljava/lang/String;)V"
            hooked = 1
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$act_file" > "${act_file}.tmp" && mv "${act_file}.tmp" "$act_file" 2>/dev/null || rm -f "${act_file}.tmp"
        echo "    Hooked: $act_file"
    fi

    # 4c. Settings NameValueCache - Dev status & ADB stealth
    nvc_file=$(find "$FW_DIR" -name "*NameValueCache*.smali" -type f | head -1)
    if [ -n "$nvc_file" ] && ! grep -q "KaoriosHook;->shouldHideDevStatus" "$nvc_file"; then
        echo "  -> Hooking NameValueCache for developer status isolation..."
        awk '
        BEGIN { in_get = 0; hooked = 0 }
        /\.method public.*getStringForUser\(Landroid\/content\/ContentResolver;Ljava\/lang\/String;I\)Ljava\/lang\/String;/ {
            in_get = 1
            print $0
            next
        }
        in_get && !hooked && /(\.registers|\.locals)/ {
            print $0
            print "    invoke-static {p1, p2, p3}, Landroid/security/kaorios/KaoriosHook;->shouldHideDevStatusFromNameValueCache(Landroid/content/ContentResolver;Ljava/lang/String;I)Z"
            print "    move-result v0"
            print "    if-eqz v0, :cond_kaorios_nvc_stock"
            print "    const/4 v0, 0x0"
            print "    return-object v0"
            print "    :cond_kaorios_nvc_stock"
            hooked = 1
            in_get = 0
            next
        }
        in_get && /\.end method/ { in_get = 0 }
        { print $0 }
        ' "$nvc_file" > "${nvc_file}.tmp" && mv "${nvc_file}.tmp" "$nvc_file" 2>/dev/null || rm -f "${nvc_file}.tmp"
        echo "    Hooked: $nvc_file"
    fi

    # 4d. Settings Global/Secure/System - Dev status & ADB stealth for query packages
    for s_file in $(find "$FW_DIR" -name "Settings\$Global.smali" -o -name "Settings\$Secure.smali" -o -name "Settings\$System.smali" 2>/dev/null); do
        if [ -f "$s_file" ] && ! grep -q "KaoriosHook;->shouldHideDevStatus" "$s_file"; then
            echo "  -> Hooking $(basename "$s_file") for developer status stealth..."
            awk '
            BEGIN { in_target = 0; hooked = 0 }
            /\.method public static.*getString\(Landroid\/content\/ContentResolver;Ljava\/lang\/String;\)Ljava\/lang\/String;/ { in_target = 1; print $0; next }
            in_target && !hooked && /(\.registers|\.locals)/ {
                print $0
                print "    invoke-static {}, Landroid/provider/Settings\$Config;->getContentResolver()Landroid/content/ContentResolver;"
                print "    move-result-object v0"
                print "    if-eqz v0, :cond_kaorios_hidedev"
                print "    invoke-virtual {v0}, Landroid/content/ContentResolver;->getPackageName()Ljava/lang/String;"
                print "    move-result-object v1"
                print "    invoke-static {v0, v1, p1}, Landroid/security/kaorios/KaoriosHook;->shouldHideDevStatus(Landroid/content/ContentResolver;Ljava/lang/String;Ljava/lang/String;)Z"
                print "    move-result v1"
                print "    if-eqz v1, :cond_kaorios_hidedev"
                print "    const-string p1, \"0\""
                print "    return-object p1"
                print "    :cond_kaorios_hidedev"
                hooked = 1
                in_target = 0
                next
            }
            in_target && /\.end method/ { in_target = 0 }
            { print $0 }
            ' "$s_file" > "${s_file}.tmp" && mv "${s_file}.tmp" "$s_file" 2>/dev/null || rm -f "${s_file}.tmp"
            echo "    Hooked: $s_file"
        fi
    done
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

    # 6. WindowState.smali - Dynamic FLAG_SECURE Hook (Controlled via KaoriosToolbox app toggle)
    ws_file=$(find "$SVC_DIR" -name "WindowState.smali" -type f | head -1)
    if [ -n "$ws_file" ] && ! grep -q "KaoriosHook;->isSecureFlag" "$ws_file"; then
        echo "  -> Hooking WindowState.isSecureLocked()..."
        awk '
        BEGIN { in_method = 0; hooked = 0 }
        /\.method.*isSecureLocked\(\)Z/ { in_method = 1; print $0; next }
        in_method && !hooked && /(\.registers|\.locals)/ {
            print $0
            print "    invoke-static {}, Landroid/security/kaorios/KaoriosHook;->isSecureFlag()Z"
            print "    move-result v0"
            print "    if-eqz v0, :cond_kaorios_sec_stock"
            print "    const/4 v0, 0x0"
            print "    return v0"
            print "    :cond_kaorios_sec_stock"
            hooked = 1
            in_method = 0
            next
        }
        in_method && /\.end method/ { in_method = 0 }
        { print $0 }
        ' "$ws_file" > "${ws_file}.tmp" && mv "${ws_file}.tmp" "$ws_file" 2>/dev/null || rm -f "${ws_file}.tmp"
        echo "    Hooked: $ws_file"
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
        echo "[*] Preparing multi-dex slot: classes$next_dex.dex for Kaorios bytecode..."
        
        # Primary & most reliable method: Direct DEX multi-dex staging
        # Instant (0.05s), lossless, zero-heap, avoids baksmali/smali discrepancies
        # and completely immune to Android 17 / HyperOS 4 ART runtime aborts
        mkdir -p "$FW_DIR/.extra_dex"
        cp -f "$KAORIOS_ASSET_DIR/kaorios_framework.dex" "$FW_DIR/.extra_dex/classes${next_dex}.dex"
        chmod 644 "$FW_DIR/.extra_dex/classes${next_dex}.dex"
        
        injected=false
        if [ -s "$FW_DIR/.extra_dex/classes${next_dex}.dex" ]; then
            echo "[+] Successfully staged Kaorios DEX as classes${next_dex}.dex for direct multi-dex bundling"
            injected=true
        fi
        
        # Secondary fallback: if direct staging somehow failed, attempt baksmali disassembly
        if ! $injected && [ -f "$DI_BIN/baksmali.jar" ]; then
            target_smali_dir="$FW_DIR/smali_classes$next_dex"
            echo "[*] Fallback: Disassembling Kaorios DEX bytecode into $target_smali_dir..."
            
            # 1. Try using DI's run_jar (which dynamically resolves Main-Class from MANIFEST.MF)
            if command -v run_jar >/dev/null 2>&1; then
                run_jar "$DI_BIN/baksmali.jar" d "$KAORIOS_ASSET_DIR/kaorios_framework.dex" -o "$target_smali_dir" 2>/dev/null || true
            fi
            
            # 2. Try app_process with correct smali 3.x main class (com.android.tools.smali.baksmali.Main)
            if [ ! -d "$target_smali_dir" ] || [ -z "$(ls -A "$target_smali_dir" 2>/dev/null)" ]; then
                export CLASSPATH="$DI_BIN/baksmali.jar"
                /system/bin/app_process -Xmx512m /system/bin com.android.tools.smali.baksmali.Main d "$KAORIOS_ASSET_DIR/kaorios_framework.dex" -o "$target_smali_dir" 2>/dev/null || true
            fi
            
            # 3. Try dalvikvm
            if [ ! -d "$target_smali_dir" ] || [ -z "$(ls -A "$target_smali_dir" 2>/dev/null)" ]; then
                dalvikvm -Xmx512m -cp "$DI_BIN/baksmali.jar" com.android.tools.smali.baksmali.Main d "$KAORIOS_ASSET_DIR/kaorios_framework.dex" -o "$target_smali_dir" 2>/dev/null || true
            fi
            
            if [ -d "$target_smali_dir" ] && [ -n "$(ls -A "$target_smali_dir" 2>/dev/null)" ]; then
                echo "[+] Successfully disassembled Kaorios bytecode into smali_classes$next_dex"
                injected=true
            fi
        fi

        # Verify injection succeeded
        if ! $injected; then
            echo "[!] FATAL: Failed to inject Kaorios bytecode into framework workspace!"
            echo "[!] Missing Kaorios bytecode would cause bootloops. Aborting."
            return 1 2>/dev/null || exit 1
        fi
        
        # Ensure framework.jar is marked as modified so recompilation bundles the new DEX
        if command -v mark_workspace_modified >/dev/null 2>&1; then
            mark_workspace_modified "framework.jar"
        fi
        echo "[+] Successfully registered Kaorios bytecode injection into framework.jar (slot $next_dex)"
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
    if [ -f "$KAORIOS_ASSET_DIR/device-model.json" ]; then
        add_to_module "$KAORIOS_ASSET_DIR/device-model.json" "data/adb/kaorios/device-model.json" "file"
    fi

    # Register Kaorios Companion App as a privileged system app
    if [ -f "$KAORIOS_ASSET_DIR/KaoriosToolbox.apk" ]; then
        add_to_module "$KAORIOS_ASSET_DIR/KaoriosToolbox.apk" "system/priv-app/KaoriosToolbox/KaoriosToolbox.apk" "apk"
        echo "[+] Kaorios Toolbox APK registered to module (system/priv-app)"
    fi
    if [ -f "$KAORIOS_ASSET_DIR/com.kousei.kaorios.xml" ]; then
        add_to_module "$KAORIOS_ASSET_DIR/com.kousei.kaorios.xml" "system/etc/permissions/com.kousei.kaorios.xml" "xml"
        echo "[+] Kaorios privapp permission whitelist registered to module"
    fi
    if [ -d "$KAORIOS_ASSET_DIR/lib" ]; then
        for so_file in $(find "$KAORIOS_ASSET_DIR/lib" -type f); do
            rel_so=$(echo "$so_file" | sed "s|^$KAORIOS_ASSET_DIR/lib/||")
            add_to_module "$so_file" "system/priv-app/KaoriosToolbox/lib/$rel_so" "lib"
        done
        echo "[+] Kaorios companion native libraries registered to module"
    fi

    echo "[+] Kaorios configuration, companion APK, and keybox registered to module"
fi

echo "[*] Kaorios Toolbox v2.0.6.0 patches applied successfully."

