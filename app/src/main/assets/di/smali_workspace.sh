#!/system/bin/sh
# Smali Workspace Management Library
# Provides centralized JAR decompilation/recompilation to eliminate redundant operations

# Global workspace tracking
declare -A WORKSPACE_PATHS
declare -A WORKSPACE_MODIFIED
WORKSPACE_BASE="$TMP/smali_workspaces"

# Initialize workspace system
init_workspace() {
    echo "[*] Initializing workspace system..."
    mkdir -p "$WORKSPACE_BASE"
    
    # Clear any previous workspace state
    WORKSPACE_PATHS=()
    WORKSPACE_MODIFIED=()
    
    # Clean stale Apktool framework caches to prevent resource parsing errors across ROM updates
    rm -rf "$TMP/zbin/apktool" "$HOME/.local/share/apktool/framework" /data/local/tmp/apktool 2>/dev/null || true
    
    echo "[+] Workspace system ready at: $WORKSPACE_BASE"
}

# Decompile a JAR to its workspace (no-op if already decompiled)
# Usage: decompile_jar "jar_name" "jar_path"
# Example: decompile_jar "framework.jar" "$FRAMEWORK_JAR"
decompile_jar() {
    local jar_name="$1"
    local jar_path="$2"
    
    if [ -z "$jar_name" ] || [ -z "$jar_path" ]; then
        echo "[!] decompile_jar: Missing jar_name or jar_path"
        return 1
    fi
    
    if [ ! -f "$jar_path" ]; then
        echo "[!] decompile_jar: JAR not found: $jar_path"
        return 1
    fi
    
    # Check if already decompiled
    if [ -n "${WORKSPACE_PATHS[$jar_name]}" ]; then
        echo "[*] Workspace for $jar_name already exists, skipping decompilation"
        return 0
    fi
    
    # Create workspace directory
    local workspace_dir="$WORKSPACE_BASE/${jar_name%.jar}"
    mkdir -p "$workspace_dir"
    
    echo "[*] Decompiling $jar_name..."
    
    # Use dynamic_apktool to decompile with signature preservation
    if dynamic_apktool -decompile "$jar_path" -o "$workspace_dir" -ps; then
        WORKSPACE_PATHS[$jar_name]="$workspace_dir"
        WORKSPACE_MODIFIED[$jar_name]=0
        echo "[+] Decompiled $jar_name to workspace: $workspace_dir"
        return 0
    else
        echo "[!] Failed to decompile $jar_name"
        rm -rf "$workspace_dir"
        return 1
    fi
}

# Get workspace path for a JAR
# Usage: workspace=$(get_workspace_path "framework.jar")
get_workspace_path() {
    local jar_name="$1"
    
    if [ -z "$jar_name" ]; then
        echo "[!] get_workspace_path: Missing jar_name" >&2
        return 1
    fi
    
    local workspace="${WORKSPACE_PATHS[$jar_name]}"
    
    if [ -z "$workspace" ]; then
        echo "[!] get_workspace_path: No workspace found for $jar_name (was it decompiled?)" >&2
        return 1
    fi
    
    echo "$workspace"
}

# Mark a workspace as modified (called automatically by smali patching, or can be manual)
# Usage: mark_workspace_modified "framework.jar"
mark_workspace_modified() {
    local jar_name="$1"
    
    if [ -z "$jar_name" ]; then
        echo "[!] mark_workspace_modified: Missing jar_name"
        return 1
    fi
    
    WORKSPACE_MODIFIED[$jar_name]=1
    echo "[*] Marked $jar_name workspace as modified"
}

# Recompile all modified workspaces back to their original JAR paths
# Usage: recompile_all
recompile_all() {
    local jar_name workspace modified jar_path
    local recompiled_count=0
    local failed_count=0
    
    if [ ${#WORKSPACE_PATHS[@]} -eq 0 ]; then
        echo "[!] FATAL ERROR: No workspaces registered for recompilation!"
        return 1
    fi

    echo "[*] Recompiling modified workspaces (${#WORKSPACE_PATHS[@]} total)..."
    
    for jar_name in "${!WORKSPACE_PATHS[@]}"; do
        workspace="${WORKSPACE_PATHS[$jar_name]}"
        
        # Always recompile (we assume if workspace exists, it was used)
        # In the future, we could use modified flag for optimization
        
        # Determine original JAR path from environment
        case "$jar_name" in
            framework.jar)
                jar_path="$FRAMEWORK_JAR"
                ;;
            services.jar)
                jar_path="$SERVICES_JAR"
                ;;
            miui-services.jar)
                jar_path="$MIUI_SERVICES_JAR"
                ;;
            *)
                echo "[!] Unknown JAR: $jar_name, skipping recompilation"
                continue
                ;;
        esac
        
        if [ -z "$jar_path" ]; then
            echo "[!] No path defined for $jar_name, skipping"
            continue
        fi
        
        echo "[*] Recompiling $jar_name..."
        
        # OOM mitigation: Clear cache before every recompilation to prevent kills
        sync
        echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true
        
        # Use dynamic_apktool to recompile with limited threads (-j) and preserve signature (-ps)
        if dynamic_apktool -recompile "$workspace" -o "$jar_path" -j 2 -ps; then
            echo "[+] Successfully recompiled $jar_name"
            
            # Direct multi-dex injection: if any .extra_dex/*.dex exist for this workspace, bundle them directly
            if [ -d "$workspace/.extra_dex" ]; then
                local extra_dex_count=0
                
                # Locate zip archiver
                local zip_bin=""
                if [ -x "$DI_BIN/zip" ]; then
                    zip_bin="$DI_BIN/zip"
                elif [ -x "$DI_TMP/bin/zip" ]; then
                    zip_bin="$DI_TMP/bin/zip"
                elif [ -x "/data/tmp/di/bin/zip" ]; then
                    zip_bin="/data/tmp/di/bin/zip"
                elif [ -n "$ARCH" ] && [ -x "$DI_ROOT/META-INF/zbin/arch/$ARCH/zip" ]; then
                    zip_bin="$DI_ROOT/META-INF/zbin/arch/$ARCH/zip"
                elif [ -x "$DI_ROOT/META-INF/zbin/arch/arm64-v8a/zip" ]; then
                    zip_bin="$DI_ROOT/META-INF/zbin/arch/arm64-v8a/zip"
                elif command -v zip >/dev/null 2>&1; then
                    zip_bin=$(command -v zip)
                fi

                # Locate aapt archiver as fallback
                local aapt_bin=""
                if [ -x "$DI_BIN/aapt" ]; then
                    aapt_bin="$DI_BIN/aapt"
                elif [ -x "$DI_TMP/bin/aapt" ]; then
                    aapt_bin="$DI_TMP/bin/aapt"
                elif [ -x "/data/tmp/di/bin/aapt" ]; then
                    aapt_bin="/data/tmp/di/bin/aapt"
                elif [ -n "$ARCH" ] && [ -x "$DI_ROOT/META-INF/zbin/arch/$ARCH/aapt" ]; then
                    aapt_bin="$DI_ROOT/META-INF/zbin/arch/$ARCH/aapt"
                elif [ -x "$DI_ROOT/META-INF/zbin/arch/arm64-v8a/aapt" ]; then
                    aapt_bin="$DI_ROOT/META-INF/zbin/arch/arm64-v8a/aapt"
                elif command -v aapt >/dev/null 2>&1; then
                    aapt_bin=$(command -v aapt)
                fi

                # Locate zipalign
                local zipalign_bin=""
                if [ -x "$DI_BIN/zipalign" ]; then
                    zipalign_bin="$DI_BIN/zipalign"
                elif [ -x "$DI_TMP/bin/zipalign" ]; then
                    zipalign_bin="$DI_TMP/bin/zipalign"
                elif [ -x "/data/tmp/di/bin/zipalign" ]; then
                    zipalign_bin="/data/tmp/di/bin/zipalign"
                elif [ -n "$ARCH" ] && [ -x "$DI_ROOT/META-INF/zbin/arch/$ARCH/zipalign" ]; then
                    zipalign_bin="$DI_ROOT/META-INF/zbin/arch/$ARCH/zipalign"
                elif [ -x "$DI_ROOT/META-INF/zbin/arch/arm64-v8a/zipalign" ]; then
                    zipalign_bin="$DI_ROOT/META-INF/zbin/arch/arm64-v8a/zipalign"
                elif command -v zipalign >/dev/null 2>&1; then
                    zipalign_bin=$(command -v zipalign)
                fi

                for extra_dex in "$workspace/.extra_dex"/*.dex; do
                    [ -f "$extra_dex" ] || continue
                    local dex_name=$(basename "$extra_dex")
                    echo "[*] Bundling extra multi-dex $dex_name into $jar_name..."
                    
                    local added=false
                    if [ -n "$zip_bin" ] && [ -x "$zip_bin" ]; then
                        echo "[*] Using zip archiver: $zip_bin"
                        (cd "$workspace/.extra_dex" && "$zip_bin" -qu "$jar_path" "$dex_name") || "$zip_bin" -qju "$jar_path" "$extra_dex"
                        added=true
                    elif [ -n "$aapt_bin" ] && [ -x "$aapt_bin" ]; then
                        echo "[*] Fallback: Using aapt archiver: $aapt_bin"
                        (cd "$workspace/.extra_dex" && "$aapt_bin" add "$jar_path" "$dex_name")
                        added=true
                    else
                        echo "[!] ERROR: No zip or aapt binary available to bundle $dex_name!"
                    fi

                    if unzip -l "$jar_path" 2>/dev/null | grep -q "$dex_name"; then
                        echo "[+] Verified $dex_name successfully bundled in $jar_name"
                        ((extra_dex_count++))
                    else
                        echo "[!] ERROR: Failed to bundle $dex_name into $jar_name!"
                        return 1
                    fi
                done
                
                # Zipalign the JAR archive after adding extra multi-dex files
                if [ $extra_dex_count -gt 0 ]; then
                    if [ -n "$zipalign_bin" ] && [ -x "$zipalign_bin" ]; then
                        echo "[*] Zipaligning $jar_name after multi-dex injection..."
                        "$zipalign_bin" -f 4 "$jar_path" "$jar_path.aligned" 2>/dev/null && mv -f "$jar_path.aligned" "$jar_path" || true
                    fi
                fi
            fi
            
            ((recompiled_count++))
        else
            echo "[!] Failed to recompile $jar_name"
            ((failed_count++))
        fi
    done
    
    echo "[*] Recompilation summary: $recompiled_count succeeded, $failed_count failed"
    
    if [ $failed_count -gt 0 ] || [ $recompiled_count -eq 0 ]; then
        echo "[!] FATAL ERROR: Recompilation failed (succeeded: $recompiled_count, failed: $failed_count)"
        return 1
    fi
    
    return 0
}

# Clean up all workspaces (called at end of job)
# Usage: cleanup_workspaces
cleanup_workspaces() {
    echo "[*] Cleaning up workspaces..."
    
    if [ -d "$WORKSPACE_BASE" ]; then
        rm -rf "$WORKSPACE_BASE"
        echo "[+] Workspaces cleaned"
    fi
    
    WORKSPACE_PATHS=()
    WORKSPACE_MODIFIED=()
}

# Export functions so they're available to feature scripts
export -f init_workspace
export -f decompile_jar
export -f get_workspace_path
export -f mark_workspace_modified
export -f recompile_all
export -f cleanup_workspaces
