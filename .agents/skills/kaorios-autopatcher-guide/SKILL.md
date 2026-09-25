---
name: kaorios-autopatcher-guide
description: >-
  Actionable guide for developing and maintaining KaoriOS Toolbox integrations,
  CorePatch signature/downgrade bypasses, and Dynamic Patching features for Android 17 / HyperOS 4.
  Use when modifying patcher scripts, framework smali hooks, or flashable module templates.
---

# KaoriOS & AutoPatcher Developer Guide

This guide provides procedures for maintaining and updating framework patches for Android 17 and HyperOS 4.

## 1. Smali Injection Procedures

### KeyGen Hook Injection
When patching `AndroidKeyStoreKeyPairGeneratorSpi.generateKeyPair()`:
1. Parse the existing `.registers <N>` or `.locals <M>` directive.
2. Increment the total registers by 1: `.registers <N+1>`.
3. Set the target register for the software KeyPair generation to `<N-1>`.
4. Call `invoke-static {p0}, Landroid/security/kaorios/KaoriosHook;->initGenerateSoftwareKeyPair(Ljava/lang/Object;)Ljava/security/KeyPair;`.
5. Store result via `move-result-object <N-1>`.
6. If non-null, return `<N-1>`; if null, jump to stock code without clobbering `v0`.

### Dynamic FLAG_SECURE Suite
Always maintain the 5-point dynamic interception:
- `DevicePolicyCacheImpl.isScreenCaptureAllowed(I)Z` -> returns `1` if `isSecureFlag()` is true.
- `WindowState.isSecureLocked()Z` -> returns `0` if `isSecureFlag()` is true.
- `WindowStateAnimator.isSecureLocked()Z` -> returns `0` if `isSecureFlag()` is true.
- `WindowState.setSecureLocked(Z)V` -> early `return-void` if `isSecureFlag()` is true.
- `WindowManagerService*.notAllowCaptureDisplay` -> returns `0` if `isSecureFlag()` is true.

## 2. CorePatch Invariants
- `verifySignatures`: Always returns `1` (true) on success.
- `compareSignatures`: Always returns `0` (`SIGNATURE_MATCH`).
- In Android 13-17, target `InstallPackageHelper.smali` for both `checkDowngrade` and `verifySignatures`.

## 3. Flashable Module Packaging
- Ensure `action.sh` is present in the module root for on-demand KernelSU/APatch action triggering.
- Always include `SKIP_BOOT_WAIT=1` when sourcing `service.sh` from `customize.sh` or `action.sh`.
- Set `ro.control_privapp_permissions=` in `system.prop` to allow `com.kousei.kaorios` to acquire privileged permissions.

## 4. Multi-DEX Recompilation & Extra Options Isolation
When enabling patches beyond Step 1 (Step 2 DSV, Step 3 Privacy, Step 4 Identity):
1. **Isolate Target Workspaces**:
   - Verify which JARs are modified by each step:
     - Step 1: `framework.jar` (classes7.dex staging + KeyGen hooks), `services.jar` (SystemServer).
     - Step 2: `framework.jar` (PackageParser, SigningDetails, ApkSignature*Verifier), `services.jar` (InstallPackageHelper), `miui-services.jar` (canBeUpdate).
     - Step 3: `services.jar` (AppsFilter, ComputerEngine, WindowManager*).
     - Step 4: `services.jar` (SettingsProvider).
2. **Recompilation Resource Guardrails**:
   - `framework.jar` contains 6+ DEXes. Recompiling with `-j 2` causes `app_process` to be killed by LMKD (`line 2885: Killed`).
   - Recompile `framework.jar` strictly with `-j 1`.
   - Ensure `echo -1000 > /proc/$$/oom_score_adj` is executed before launching `app_process`.
3. **Smali Awk Script Scoping**:
   - In all feature awk scripts, state variables must reset on `/\.end method/ { state=0 }`.
   - Never assume line ordering between string constants and conditional branches (e.g. `if-nez` can precede the string constant).
