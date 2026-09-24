#@name DSV / CorePatch
#@description Bypasses signature verification, digest comparisons, and APK downgrade protection across framework.jar, services.jar, and miui-services.jar
#@requires framework.jar,services.jar,miui-services.jar

FRAMEWORK="$FRAMEWORK_JAR"
SERVICES="$SERVICES_JAR"
MIUI_SERVICES="$MIUI_SERVICES_JAR"

echo "[*] Initializing DSV / CorePatch (Disable Signature Verification & Downgrade)..."

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

if [ -n "$MIUI_SERVICES_WORKSPACE" ] && [ -d "$MIUI_SERVICES_WORKSPACE" ]; then
    MIUI_DIR="$MIUI_SERVICES_WORKSPACE"
else
    MIUI_DIR="$TMP/smali_workspaces/miui-services"
fi

# ==================== 1. FRAMEWORK.JAR ====================
if [ -d "$FW_DIR" ]; then
    echo "[*] Applying CorePatch to framework.jar..."

    # 1. PackageParser.smali
    for pp in $(find "$FW_DIR" -name "PackageParser.smali" -type f 2>/dev/null); do
        echo "  -> Patching $pp..."
        awk '
        /unsafeGetCertsWithoutVerification/ {
            print "    const/4 v1, 0x1"
        }
        /<manifest> specifies bad sharedUserId name/ {
            found_bad_shared = 1
        }
        found_bad_shared && /if-nez[ \t]+v[0-9]+/ {
            print "    const/4 v14, 0x1"
            found_bad_shared = 0
        }
        { print $0 }
        ' "$pp" > "${pp}.tmp" && mv "${pp}.tmp" "$1" 2>/dev/null || rm -f "${pp}.tmp"
    done

    # 2. PackageParser$PackageParserException.smali
    for ppe in $(find "$FW_DIR" -name "PackageParser\$PackageParserException.smali" -type f 2>/dev/null); do
        echo "  -> Patching $ppe..."
        awk '
        /iput[ \t]+p1,[ \t]+p0,.*->error:I/ {
            print "    const/4 p1, 0x0"
        }
        { print $0 }
        ' "$ppe" > "${ppe}.tmp" && mv "${ppe}.tmp" "$1" 2>/dev/null || rm -f "${ppe}.tmp"
    done

    # 3. PackageParser$SigningDetails.smali & SigningDetails.smali
    for sd in $(find "$FW_DIR" \( -name "PackageParser\$SigningDetails.smali" -o -name "SigningDetails.smali" \) -type f 2>/dev/null); do
        echo "  -> Patching $sd..."
        awk '
        BEGIN { in_target = 0 }
        /\.method public.*checkCapability\(/ {
            in_target = 1
            print $0
            next
        }
        /\.method public.*hasAncestorOrSelf\(/ {
            in_target = 1
            print $0
            next
        }
        in_target && /^[ \t]*\.(registers|locals)[ \t]+([0-9]+)/ {
            print $0
            print "    const/4 v0, 0x1"
            print "    return v0"
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$sd" > "${sd}.tmp" && mv "${sd}.tmp" "$1" 2>/dev/null || rm -f "${sd}.tmp"
    done

    # 4. ApkSignatureSchemeV2Verifier.smali & ApkSignatureSchemeV3Verifier.smali & ApkSigningBlockUtils.smali
    for v2 in $(find "$FW_DIR" -name "ApkSignatureSchemeV2Verifier.smali" -type f 2>/dev/null); do
        echo "  -> Patching $v2..."
        awk '
        BEGIN { in_equal = 0 }
        /invoke-static.*MessageDigest;->isEqual/ {
            in_equal = 1
            print $0
            next
        }
        in_equal && /move-result[ \t]+(v[0-9]+)/ {
            reg = $2
            print "    const/4 " reg ", 0x1"
            in_equal = 0
            next
        }
        { print $0 }
        ' "$v2" > "${v2}.tmp" && mv "${v2}.tmp" "$1" 2>/dev/null || rm -f "${v2}.tmp"
    done

    for v3 in $(find "$FW_DIR" -name "ApkSignatureSchemeV3Verifier.smali" -type f 2>/dev/null); do
        echo "  -> Patching $v3..."
        awk '
        BEGIN { in_equal = 0 }
        /invoke-static.*MessageDigest;->isEqual/ {
            in_equal = 1
            print $0
            next
        }
        in_equal && /move-result[ \t]+(v[0-9]+)/ {
            reg = $2
            print "    const/4 " reg ", 0x1"
            in_equal = 0
            next
        }
        { print $0 }
        ' "$v3" > "${v3}.tmp" && mv "${v3}.tmp" "$1" 2>/dev/null || rm -f "${v3}.tmp"
    done

    for sbu in $(find "$FW_DIR" -name "ApkSigningBlockUtils.smali" -type f 2>/dev/null); do
        echo "  -> Patching $sbu..."
        awk '
        BEGIN { in_equal = 0 }
        /invoke-static.*MessageDigest;->isEqual/ {
            in_equal = 1
            print $0
            next
        }
        in_equal && /move-result[ \t]+(v[0-9]+)/ {
            reg = $2
            print "    const/4 " reg ", 0x1"
            in_equal = 0
            next
        }
        { print $0 }
        ' "$sbu" > "${sbu}.tmp" && mv "${sbu}.tmp" "$1" 2>/dev/null || rm -f "${sbu}.tmp"
    done

    # 5. ApkSignatureVerifier.smali
    for asv in $(find "$FW_DIR" -name "ApkSignatureVerifier.smali" -type f 2>/dev/null); do
        echo "  -> Patching $asv..."
        awk '
        BEGIN { in_min = 0 }
        /\.method public static.*getMinimumSignatureSchemeVersionForTargetSdk/ {
            in_min = 1
            print $0
            next
        }
        in_min && /^[ \t]*\.(registers|locals)[ \t]+([0-9]+)/ {
            print $0
            print "    const/4 v0, 0x0"
            print "    return v0"
            in_min = 0
            next
        }
        in_min && /\.end method/ { in_min = 0 }
        /invoke-static.*verifyV1Signature/ {
            print "    const/4 p3, 0x0"
        }
        { print $0 }
        ' "$asv" > "${asv}.tmp" && mv "${asv}.tmp" "$1" 2>/dev/null || rm -f "${asv}.tmp"
    done

    # 6. StrictJarVerifier.smali & StrictJarFile.smali
    for sjv in $(find "$FW_DIR" -name "StrictJarVerifier.smali" -type f 2>/dev/null); do
        echo "  -> Patching $sjv..."
        awk '
        BEGIN { in_vmd = 0 }
        /\.method.*verifyMessageDigest/ {
            in_vmd = 1
            print $0
            next
        }
        in_vmd && /^[ \t]*\.(registers|locals)[ \t]+([0-9]+)/ {
            print $0
            print "    const/4 v0, 0x1"
            print "    return v0"
            in_vmd = 0
            next
        }
        in_vmd && /\.end method/ { in_vmd = 0 }
        { print $0 }
        ' "$sjv" > "${sjv}.tmp" && mv "${sjv}.tmp" "$1" 2>/dev/null || rm -f "${sjv}.tmp"
    done

    # 7. ParsingPackageUtils.smali
    for ppu in $(find "$FW_DIR" -name "ParsingPackageUtils.smali" -type f 2>/dev/null); do
        echo "  -> Patching $ppu..."
        awk '
        /<manifest> specifies bad sharedUserId name/ {
            found_bad = 1
        }
        found_bad && /if-eqz[ \t]+(v[0-9]+)/ {
            reg = $2
            gsub(/,/, "", reg)
            print "    const/4 " reg ", 0x0"
            found_bad = 0
        }
        { print $0 }
        ' "$ppu" > "${ppu}.tmp" && mv "${ppu}.tmp" "$1" 2>/dev/null || rm -f "${ppu}.tmp"
    done

    command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "framework.jar"
fi

# ==================== 2. SERVICES.JAR ====================
if [ -d "$SVC_DIR" ]; then
    echo "[*] Applying CorePatch to services.jar..."

    # 1. PackageManagerServiceUtils.smali
    for pmsu in $(find "$SVC_DIR" -name "PackageManagerServiceUtils.smali" -type f 2>/dev/null); do
        echo "  -> Patching $pmsu..."
        awk '
        BEGIN { in_target = 0; target_type = "" }
        /\.method.*checkDowngrade\(/ {
            in_target = 1; target_type = "void"; print $0; next
        }
        /\.method.*shouldCheckUpgradeKeySetLocked\(/ {
            in_target = 1; target_type = "int_0"; print $0; next
        }
        /\.method.*compareSignatures\(/ {
            in_target = 1; target_type = "int_0"; print $0; next
        }
        /\.method.*verifySignatures\(/ {
            in_target = 1; target_type = "bool_1"; print $0; next
        }
        /\.method.*matchSignaturesCompat\(/ {
            in_target = 1; target_type = "bool_1"; print $0; next
        }
        in_target && /^[ \t]*\.(registers|locals)[ \t]+([0-9]+)/ {
            print $0
            if (target_type == "void") {
                print "    return-void"
            } else if (target_type == "int_0") {
                print "    const/4 v0, 0x0"
                print "    return v0"
            } else if (target_type == "bool_1") {
                print "    const/4 v0, 0x1"
                print "    return v0"
            }
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$pmsu" > "${pmsu}.tmp" && mv "${pmsu}.tmp" "$1" 2>/dev/null || rm -f "${pmsu}.tmp"
    done

    # 2. KeySetManagerService.smali
    for ks in $(find "$SVC_DIR" -name "KeySetManagerService.smali" -type f 2>/dev/null); do
        echo "  -> Patching $ks..."
        awk '
        BEGIN { in_target = 0 }
        /\.method.*shouldCheckUpgradeKeySetLocked\(/ {
            in_target = 1
            print $0
            next
        }
        in_target && /^[ \t]*\.(registers|locals)[ \t]+([0-9]+)/ {
            print $0
            print "    const/4 v0, 0x0"
            print "    return v0"
            in_target = 0
            next
        }
        in_target && /\.end method/ { in_target = 0 }
        { print $0 }
        ' "$ks" > "${ks}.tmp" && mv "${ks}.tmp" "$1" 2>/dev/null || rm -f "${ks}.tmp"
    done

    # 3. InstallPackageHelper.smali (A13-A17)
    for iph in $(find "$SVC_DIR" -name "InstallPackageHelper.smali" -type f 2>/dev/null); do
        echo "  -> Patching $iph..."
        awk '
        BEGIN { in_method = 0 }
        /\.method.*checkDowngrade\(/ {
            in_method = 1
            print $0
            next
        }
        in_method && /^[ \t]*\.(registers|locals)[ \t]+([0-9]+)/ {
            print $0
            print "    return-void"
            in_method = 0
            next
        }
        in_method && /\.end method/ { in_method = 0 }
        /invoke-interface.*isLeavingSharedUser\(\)Z/ {
            print $0
            print "    const/4 v0, 0x1"
            next
        }
        { print $0 }
        ' "$iph" > "${iph}.tmp" && mv "${iph}.tmp" "$1" 2>/dev/null || rm -f "${iph}.tmp"
    done

    # 4. ReconcilePackageUtils.smali
    for rpu in $(find "$SVC_DIR" -name "ReconcilePackageUtils.smali" -type f 2>/dev/null); do
        echo "  -> Patching $rpu..."
        awk '
        /sput-boolean.*ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS/ {
            print "    const/4 v0, 0x1"
        }
        { print $0 }
        ' "$rpu" > "${rpu}.tmp" && mv "${rpu}.tmp" "$1" 2>/dev/null || rm -f "${rpu}.tmp"
    done

    command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "services.jar"
fi

# ==================== 3. MIUI-SERVICES.JAR ====================
if [ -d "$MIUI_DIR" ]; then
    echo "[*] Applying CorePatch to miui-services.jar..."

    for mf in $(find "$MIUI_DIR" -name "*.smali" -type f 2>/dev/null); do
        if grep -q "verifyIsolationViolation\|canBeUpdate" "$mf" 2>/dev/null; then
            echo "  -> Patching $mf..."
            awk '
            BEGIN { in_target = 0; return_type = "" }
            /\.method.*verifyIsolationViolation\(/ {
                in_target = 1; return_type = "void"; print $0; next
            }
            /\.method.*canBeUpdate\(/ {
                in_target = 1
                if ($0 ~ /\)Z/) { return_type = "bool" } else { return_type = "void" }
                print $0
                next
            }
            in_target && /^[ \t]*\.(registers|locals)[ \t]+([0-9]+)/ {
                print $0
                if (return_type == "bool") {
                    print "    const/4 v0, 0x1"
                    print "    return v0"
                } else {
                    print "    return-void"
                }
                in_target = 0
                next
            }
            in_target && /\.end method/ { in_target = 0 }
            { print $0 }
            ' "$mf" > "${mf}.tmp" && mv "${mf}.tmp" "$1" 2>/dev/null || rm -f "${mf}.tmp"
        fi
    done

    command -v mark_workspace_modified >/dev/null 2>&1 && mark_workspace_modified "miui-services.jar"
fi

echo "[+] DSV / CorePatch applied successfully."
