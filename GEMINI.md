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
