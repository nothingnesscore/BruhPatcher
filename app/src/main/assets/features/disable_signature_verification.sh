#@name Disable Signature Verification (CorePatch)
#@description Bypasses APK signature verification, downgrade checks, and digest validation on Android 13-17 & HyperOS 4
#@requires framework.jar,services.jar,miui-services.jar

FRAMEWORK="$FRAMEWORK_JAR"
SERVICES="$SERVICES_JAR"
MIUI_SERVICES="$MIUI_SERVICES_JAR"

echo "[*] Initializing CorePatch (Signature Verification Bypass)..."

# Use centralized smali workspace if available, otherwise decompile
if [ -n "$FRAMEWORK_WORKSPACE" ] && [ -d "$FRAMEWORK_WORKSPACE" ]; then
    FW_WORK_DIR="$FRAMEWORK_WORKSPACE"
    FW_STANDALONE=0
else
    FW_WORK_DIR="$TMP/fw_dc"
    FW_STANDALONE=1
fi

if [ -n "$SERVICES_WORKSPACE" ] && [ -d "$SERVICES_WORKSPACE" ]; then
    SVC_WORK_DIR="$SERVICES_WORKSPACE"
    SVC_STANDALONE=0
else
    SVC_WORK_DIR="$TMP/svc_dc"
    SVC_STANDALONE=1
fi

if [ -n "$MIUI_SERVICES_WORKSPACE" ] && [ -d "$MIUI_SERVICES_WORKSPACE" ]; then
    MIUI_WORK_DIR="$MIUI_SERVICES_WORKSPACE"
    MIUI_STANDALONE=0
else
    MIUI_WORK_DIR="$TMP/miui_dc"
    MIUI_STANDALONE=1
fi

# Method body replacements
return_true='
    .registers 8
    const/4 v0, 0x1
    return v0
'
return_false='
    .registers 8
    const/4 v0, 0x0
    return v0
'
return_void='
    .registers 8
    return-void
'

# ==================== FRAMEWORK.JAR ====================
if [ -n "$FRAMEWORK" ]; then
    if [ "$FW_STANDALONE" -eq 1 ]; then
        echo "[*] Decompiling framework.jar..."
        dynamic_apktool -decompile "$FRAMEWORK" -o "$FW_WORK_DIR"
    fi

    if [ -d "$FW_WORK_DIR" ]; then
        echo "[*] Applying patches to framework.jar..."

        # PackageParser patches
        echo "[*] Patch 1.1: Bypass Certificate Verification..."
        smali_kit -c -m "collectCertificates" -bl "ApkSignatureVerifier;->unsafeGetCertsWithoutVerification" "    const/4 v1, 0x1" -d "$FW_WORK_DIR" -name "PackageParser.smali"

        echo "[*] Patch 1.2: Bypass Shared User ID Validation..."
        smali_kit -c -m "parseBaseApk" -bl "if-nez v14," "    const/4 v14, 0x1" -d "$FW_WORK_DIR" -name "PackageParser.smali"

        echo "[*] Patch 2.1: Suppress Parse Errors..."
        smali_kit -c -m "<init>" -bl "iput p1, p0" "    const/4 p1, 0x0" -d "$FW_WORK_DIR" -name 'PackageParser$PackageParserException.smali'

        # SigningDetails patches
        echo "[*] Patch 3.1: Force checkCapability (P\$SD)..."
        smali_kit -c -m "checkCapability" -re "$return_true" -d "$FW_WORK_DIR" -name 'PackageParser$SigningDetails.smali'

        echo "[*] Patch 3.2: Force checkCapability (SD)..."
        smali_kit -c -m "checkCapability" -re "$return_true" -d "$FW_WORK_DIR" -name "SigningDetails.smali"
        
        echo "[*] Patch 3.x: Force checkCapabilityRecover..."
        smali_kit -c -m "checkCapabilityRecover" -re "$return_true" -d "$FW_WORK_DIR" -name "SigningDetails.smali"
        
        echo "[*] Patch 3.3: Bypass Ancestor Verification..."
        smali_kit -c -m "hasAncestorOrSelf" -re "$return_true" -d "$FW_WORK_DIR" -name "SigningDetails.smali"

        # V2 Signature Verification
        echo "[*] Patch 4.1: Bypass V2 Signature Verification..."
        v2_file=$(find "$FW_WORK_DIR" -name "ApkSignatureSchemeV2Verifier.smali" -type f | head -1)
        if [ -n "$v2_file" ]; then
            awk '
            BEGIN { found = 0; done = 0 }
            {
                print $0
                if (!done && /MessageDigest;->isEqual/) { found = 1 }
                else if (found && !done && /move-result (v[0-9]+)/) {
                    clean_line = $0
                    match(clean_line, /move-result (v[0-9]+)/, m)
                    reg = m[1]
                    print "    const/4 " reg ", 0x1"
                    done = 1; found = 0
                }
            }
            ' "$v2_file" > "${v2_file}.tmp" && mv "${v2_file}.tmp" "$v2_file"
        fi

        # V3 Signature Verification
        echo "[*] Patch 4.2: Bypass V3 Signature Verification..."
        v3_file=$(find "$FW_WORK_DIR" -name "ApkSignatureSchemeV3Verifier.smali" -type f | head -1)
        if [ -n "$v3_file" ]; then
            awk '
            BEGIN { found = 0; done = 0 }
            {
                print $0
                if (!done && /MessageDigest;->isEqual/) { found = 1 }
                else if (found && !done && /move-result (v[0-9]+)/) {
                    clean_line = $0
                    match(clean_line, /move-result (v[0-9]+)/, m)
                    reg = m[1]
                    print "    const/4 " reg ", 0x1"
                    done = 1; found = 0
                }
            }
            ' "$v3_file" > "${v3_file}.tmp" && mv "${v3_file}.tmp" "$v3_file"
        fi

        # V4 Signature Verification (Android 11-17)
        echo "[*] Patch 4.3: Bypass V4 Signature Verification..."
        v4_file=$(find "$FW_WORK_DIR" -name "ApkSignatureSchemeV4Verifier.smali" -type f | head -1)
        if [ -n "$v4_file" ]; then
            awk '
            BEGIN { found = 0; done = 0 }
            {
                print $0
                if (!done && /MessageDigest;->isEqual/) { found = 1 }
                else if (found && !done && /move-result (v[0-9]+)/) {
                    clean_line = $0
                    match(clean_line, /move-result (v[0-9]+)/, m)
                    reg = m[1]
                    print "    const/4 " reg ", 0x1"
                    done = 1; found = 0
                }
            }
            ' "$v4_file" > "${v4_file}.tmp" && mv "${v4_file}.tmp" "$v4_file"
        fi

        # ApkSignatureVerifier patches
        echo "[*] Patch 5.1: Set Minimum Signature Scheme to V1..."
        smali_kit -c -m "getMinimumSignatureSchemeVersionForTargetSdk" -re "$return_false" -d "$FW_WORK_DIR" -name "ApkSignatureVerifier.smali"

        echo "[*] Patch 5.2: Disable V1 Signature Verification..."
        smali_kit -c -m "verify" -bl "ApkSignatureVerifier;->verifyV1Signature" "    const p3, 0x0" -d "$FW_WORK_DIR" -name "ApkSignatureVerifier.smali"

        # ApkSigningBlockUtils
        echo "[*] Patch 6.1: Bypass Signing Block Verification..."
        block_file=$(find "$FW_WORK_DIR" -name "ApkSigningBlockUtils.smali" -type f | head -1)
        if [ -n "$block_file" ]; then
            awk '
            BEGIN { found = 0; done = 0 }
            {
                print $0
                if (!done && /MessageDigest;->isEqual/) { found = 1 }
                else if (found && !done && /move-result (v[0-9]+)/) {
                    clean_line = $0
                    match(clean_line, /move-result (v[0-9]+)/, m)
                    reg = m[1]
                    print "    const/4 " reg ", 0x1"
                    done = 1; found = 0
                }
            }
            ' "$block_file" > "${block_file}.tmp" && mv "${block_file}.tmp" "$block_file"
        fi

        # StrictJarVerifier
        echo "[*] Patch 7.1: Bypass JAR Digest Verification..."
        verifier_file=$(find "$FW_WORK_DIR" -name "StrictJarVerifier.smali" -type f | head -1)
        if [ -n "$verifier_file" ]; then
            awk '
            BEGIN { in_target_method = 0; skip_body = 0 }
            /\.method private static.*verifyMessageDigest\(\[B\[B\)Z/ {
                in_target_method = 1; skip_body = 1
                print $0
                print "    .locals 1"
                print ""
                print "    const/4 v0, 0x1"
                print ""
                print "    return v0"
                next
            }
            in_target_method && /\.end method/ {
                in_target_method = 0; skip_body = 0
                print $0
                next
            }
            !skip_body { print $0 }
            ' "$verifier_file" > "${verifier_file}.tmp" && mv "${verifier_file}.tmp" "$verifier_file"
        fi

        # StrictJarFile
        echo "[*] Patch 8.1: Remove Manifest Entry Check..."
        smali_kit -c -m "<init>" -dim "if-eqz v6, :cond_56" -d "$FW_WORK_DIR" -name "StrictJarFile.smali"
        smali_kit -c -m "<init>" -dim ":cond_56" -d "$FW_WORK_DIR" -name "StrictJarFile.smali"

        # ParsingPackageUtils
        echo "[*] Patch 9.1: Bypass Shared User (ParsingPackageUtils)..."
        smali_kit -c -m "parseBaseApkTag" -bl "if-eqz v4," "    const/4 v4, 0x0" -d "$FW_WORK_DIR" -name "ParsingPackageUtils.smali"

        if [ "$FW_STANDALONE" -eq 1 ]; then
            echo "[*] Recompiling framework.jar..."
            dynamic_apktool -recompile "$FW_WORK_DIR" -o "$FRAMEWORK"
            delete_recursive "$FW_WORK_DIR"
        fi
    else
        echo "[!] FATAL ERROR: Framework workspace directory not found: $FW_WORK_DIR"
        return 1
    fi
fi

# ==================== SERVICES.JAR ====================
if [ -n "$SERVICES" ]; then
    if [ "$SVC_STANDALONE" -eq 1 ]; then
        echo "[*] Decompiling services.jar..."
        dynamic_apktool -decompile "$SERVICES" -o "$SVC_WORK_DIR"
    fi

    if [ -d "$SVC_WORK_DIR" ]; then
        echo "[*] Applying patches to services.jar..."

        # 1. Downgrade bypass across Android 10-17
        echo "[*] Patch 1.1: Disable checkDowngrade..."
        smali_kit -c -m "checkDowngrade" -re "$return_void" -d "$SVC_WORK_DIR" -name "InstallPackageHelper.smali"
        smali_kit -c -m "checkDowngrade" -re "$return_void" -d "$SVC_WORK_DIR" -name "PackageManagerServiceUtils.smali"
        smali_kit -c -m "checkDowngrade" -re "$return_void" -d "$SVC_WORK_DIR" -name "PackageManagerService.smali"

        # 2. Signature verification bypass (must return TRUE on success!)
        echo "[*] Patch 1.2: Bypass verifySignatures..."
        smali_kit -c -m "verifySignatures" -re "$return_true" -d "$SVC_WORK_DIR" -name "PackageManagerServiceUtils.smali"
        smali_kit -c -m "verifySignatures" -re "$return_true" -d "$SVC_WORK_DIR" -name "InstallPackageHelper.smali"

        # 3. Signature compare bypass (returns 0 for SIGNATURE_MATCH)
        echo "[*] Patch 1.3: Bypass compareSignatures..."
        smali_kit -c -m "compareSignatures" -re "$return_false" -d "$SVC_WORK_DIR" -name "PackageManagerServiceUtils.smali"
        smali_kit -c -m "compareSignatures" -re "$return_false" -d "$SVC_WORK_DIR" -name "PackageManagerService.smali"

        # 4. Compat signature matching
        echo "[*] Patch 1.4: Force matchSignaturesCompat..."
        smali_kit -c -m "matchSignaturesCompat" -re "$return_true" -d "$SVC_WORK_DIR" -name "PackageManagerServiceUtils.smali"

        # 5. KeySet verification
        echo "[*] Patch 2.1: Skip KeySet verification..."
        smali_kit -c -m "shouldCheckUpgradeKeySetLocked" -re "$return_false" -d "$SVC_WORK_DIR" -name "KeySetManagerService.smali"

        # 6. Shared User Leaving Check
        echo "[*] Patch 3.1: Bypass Shared User Leaving Check..."
        smali_kit -c -m "adjustScanFlags" -bl "if-eqz v3," "    const/4 v3, 0x1" -d "$SVC_WORK_DIR" -name "InstallPackageHelper.smali"

        # 7. Reconciliation Bypass
        echo "[*] Patch 4.1: Enable Reconciliation Bypass..."
        smali_kit -c -m "<clinit>" -rim "const/4 v0, 0x0" "const/4 v0, 0x1" -d "$SVC_WORK_DIR" -name "ReconcilePackageUtils.smali"

        if [ "$SVC_STANDALONE" -eq 1 ]; then
            echo "[*] Recompiling services.jar..."
            dynamic_apktool -recompile "$SVC_WORK_DIR" -o "$SERVICES"
            delete_recursive "$SVC_WORK_DIR"
        fi
    else
        echo "[!] FATAL ERROR: Services workspace directory not found: $SVC_WORK_DIR"
        return 1
    fi
fi

# ==================== MIUI-SERVICES.JAR (HYPEROS) ====================
if [ -n "$MIUI_SERVICES" ]; then
    if [ "$MIUI_STANDALONE" -eq 1 ]; then
        echo "[*] Decompiling miui-services.jar..."
        dynamic_apktool -decompile "$MIUI_SERVICES" -o "$MIUI_WORK_DIR"
    fi

    if [ -d "$MIUI_WORK_DIR" ]; then
        echo "[*] Applying HyperOS / MIUI signature bypass patches..."

        echo "[*] Patch 1: Disable Isolation Violation Check..."
        smali_kit -c -m "verifyIsolationViolation" -re "$return_void" -d "$MIUI_WORK_DIR" -name "PackageManagerServiceImpl.smali"

        echo "[*] Patch 2: Allow Critical System App Updates..."
        smali_kit -c -m "canBeUpdate" -re "$return_true" -d "$MIUI_WORK_DIR" -name "PackageManagerServiceImpl.smali"

        if [ "$MIUI_STANDALONE" -eq 1 ]; then
            echo "[*] Recompiling miui-services.jar..."
            dynamic_apktool -recompile "$MIUI_WORK_DIR" -o "$MIUI_SERVICES"
            delete_recursive "$MIUI_WORK_DIR"
        fi
    else
        echo "[!] FATAL ERROR: MIUI services workspace directory not found: $MIUI_WORK_DIR"
        return 1
    fi
fi

echo "[*] Signature verification patches complete."
