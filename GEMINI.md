# AutoPatcher & KaoriOS Framework Modification Guidelines

## 1. Smali AST Hooking & Register Safety
- **Register Expansion Rule**: When hooking methods in `framework.jar` or `services.jar` that call static helpers (e.g. `KaoriosHook.initGenerateSoftwareKeyPair`):
  - Always increment `.registers` by +1 (or allocate safe local register).
  - Use target register at `new_registers - 2`.
  - Never clobber existing method argument or return registers (`p0`, `v0`) without checking fallback execution branches.
- **Boolean vs Match Return Types**:
  - `verifySignatures` is a boolean method: returning `1` (`const/4 vX, 0x1` / `return vX`) indicates success.
  - `compareSignatures` is an int comparator: returning `0` indicates `SIGNATURE_MATCH`.
  - `canBeUpdate` in Xiaomi `PackageManagerServiceImpl` returns boolean (`return_true`), never `return_void`.
- **Target Class Resolution for Android 13–17**:
  - Package installation, downgrade verification (`checkDowngrade`), and signature checking reside in `InstallPackageHelper.smali` in `services.jar` for Android 13+.
- **Method-Scoped State Invariant in Scripted Hooks**:
  - Any awk/sed script modifying smali must explicitly clear state flags on `/\.end method/` (e.g. `BEGIN { flag=0 } ... /\.end method/ { flag=0 }`).
  - Never allow matching flags to persist past the method boundary, which causes illegal register injection into unrelated methods.
- **Dalvik Format 35c Nibble Restrictions**:
  - Format 35c (`invoke-static {vA, vB, ...}`) is strictly limited to 4-bit register indices (`v0..v15`).
  - In Android 17 HyperOS methods with >16 registers (e.g. `Settings$NameValueCache` with 28 registers), client-side cache hooking must be avoided; use server-side `SettingsProvider.smali` with `invoke-static/range`.

## 2. KaoriOS Suite Specifications (hzzmonetvn & Kousei)
- **Version Parity**: Injected framework DEX (`classes7.dex`) method `KaoriosFramework.getFrameworkVersion()` reports internal version `2.0.5.0` (or `2.0.6.0`).
- **Dynamic FLAG_SECURE**:
  - Intercepts `DevicePolicyCacheImpl.isScreenCaptureAllowed(I)Z` (returns 1).
  - Intercepts `WindowState.isSecureLocked()Z` & `WindowStateAnimator.isSecureLocked()Z` (returns 0).
  - Intercepts `WindowState.setSecureLocked(Z)V` (returns void).
  - Intercepts `WindowManagerService*.notAllowCaptureDisplay` & HyperOS `WindowManagerServiceImpl.notAllowCaptureDisplay` (returns 0).
  - Controlled dynamically at runtime by `KaoriosHook.isSecureFlag()Z`.
- **Developer Options & ADB Stealth**:
  - Injected into `Settings$Global`, `Settings$Secure`, `Settings$System`, and `NameValueCache` via `KaoriosHook.shouldHideDevStatus(...)`.

## 3. Module Scripts & Packaging
- **`action.sh`**: Required for KernelSU / APatch / Magisk on-demand Action button. Purges GMS PIF caches and re-runs `service.sh`.
- **Recovery Safe Execution**: Any script sourcing `service.sh` from `customize.sh` or `action.sh` must export `SKIP_BOOT_WAIT=1` to prevent hanging in recovery where `sys.boot_completed` is never 1.
- **Priv-App Permissions**: Always bundle `system/etc/permissions/com.kousei.kaorios.xml` and include `ro.control_privapp_permissions=` in `system.prop`.

## 4. Multi-DEX Recompilation & Resource Limits
- **Single-Thread Limit for Large Multi-DEX Archives**:
  - When recompiling `framework.jar` (>40MB with 6+ DEXes and >200k smali files), always use `-j 1` (single-thread mode) in `dynamic_apktool`.
  - Dual-thread (`-j 2`) smaling on large archives causes Dalvik/ART heap memory spikes exceeding 2.5 GB, triggering Android LMKD termination (`SIGKILL`).
- **LMK Immunity**:
  - Root shell execution and `app_process` wrappers must configure `echo -1000 > /proc/$$/oom_score_adj` and `echo -1000 > /proc/self/oom_score_adj` to prevent `SIGKILL` termination.
- **Drop Caches & Self-Healing Retry**:
  - Always execute `sync; echo 3 > /proc/sys/vm/drop_caches` prior to every JAR recompilation.
  - If recompilation fails on the initial attempt, the workspace manager must clear caches and retry in single-thread safe mode (`-j 1`) before declaring failure.
