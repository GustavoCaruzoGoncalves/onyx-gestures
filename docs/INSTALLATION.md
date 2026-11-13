# Installation

## Requirements

- POCO F7 (`onyx`)
- **KernelSU** (LKM) + KernelSU Manager â€” **only tested root solution**
- SELinux may remain **Enforcing**
- Camera permission for the app
- Reboot after module install

## Install from GitHub Release

1. Download `f7_moto_actions-vX.Y.Z.zip` and the matching `.sha256` file from Releases.
2. Optionally verify:

   ```powershell
   Get-FileHash .\f7_moto_actions-v1.1.2.zip -Algorithm SHA256
   ```

3. Copy the ZIP to the phone (Downloads is fine).
4. KernelSU Manager â†’ **Modules** â†’ **Install from storage** â†’ select the ZIP.
5. **Reboot**.
6. Open **Onyx Gestures**:
   - Grant **Camera** and **Notifications**
   - Allow unrestricted battery / ignore optimizations if the ROM offers it
   - Confirm the persistent â€œListening for chop-chop and twist gesturesâ€ notification
7. Test Chop-Chop (torch) and Twist (camera) with the screen on.

On PixelOS onyx, `/system` is often full. The module still bundles a priv-app path, but `customize.sh` / `service.sh` also `pm install` the APK so the package remains available after reboot.

## Verify

```bash
adb shell pm path org.f7.motoactions
adb shell dumpsys activity services org.f7.motoactions | grep isForeground
adb logcat -s F7MotoActions SensorPipeline GestureService
```

Confirm module version:

```bash
adb shell su -c 'cat /data/adb/modules/f7_moto_actions/module.prop'
```

## Uninstall / recovery

1. KernelSU Manager â†’ disable or remove `f7_moto_actions` â†’ reboot.
2. Residual APK: `adb shell pm uninstall org.f7.motoactions`
3. If the camera helper loop misbehaves after a partial update, reboot once so `service.sh` restarts cleanly.
4. The module does not permanently modify `boot` / `vendor`. Removing it reverses systemless overlays.

### Boot / service problems

- Safe recovery: disable the module in KernelSU Manager from a working session, or use recovery/KSUD module disable if available, then reboot.
- Force-stop the app: `adb shell am force-stop org.f7.motoactions`
- Clear app data if prefs are corrupt: `adb shell pm clear org.f7.motoactions`

## Magisk / APatch

**Not tested.** The ZIP layout is Magisk-compatible for packaging convenience only. Do not assume support.

