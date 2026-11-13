# F7 Moto Actions

Reimplementation of Motorola-style **Chop-Chop** (flashlight) and **Twist / Quick Capture** (camera) gestures for the **POCO F7** (`onyx`), delivered as a reversible **KernelSU** module plus a Kotlin foreground service.

Motorola OEM phones expose dedicated SensorHub virtual sensors for these gestures. The POCO F7 does **not**. This project recognizes the same *user motions* in software from the on-device **LSM6DSV** accelerometer and gyroscope via Android `SensorManager`.

> **Status:** Experimental but usable on the tested device. Thresholds come from a small Mot-paired capture set. Treat this as a research / enthusiast module, not a production OEM feature.

## Features

| Feature | Status |
|---------|--------|
| Chop-Chop → flashlight toggle | **Implemented** (screen on verified; screen off partial / hardware-limited) |
| Twist → camera launch (MGC / secure still intent) | **Implemented** (background launch via root helper + BAL / full-screen intent path) |
| Screen-on continuous IMU sampling (~100 Hz default) | **Implemented** |
| Screen-off listening | **Implemented with caveats** — F7 IMU is non-wakeup; uses renewable `PARTIAL_WAKE_LOCK` + high-rate sampling. Not Motorola SensorHub parity. Long idle / deep Doze **not proven** |
| Proximity pocket guard | **Implemented** |
| In-app sensitivity / threshold prefs + diagnostic logging | **Implemented** |
| KernelSU module keep-alive (`service.sh`) | **Implemented** |
| Magisk / APatch | **Not tested** |

## Device compatibility

**Tested environment (only):**

| Item | Value |
|------|-------|
| Device | POCO F7 |
| Codename | `onyx` |
| ROM | PixelOS (Android **17**) |
| Build display | `CP2A.260605.016` |
| Kernel | `6.6.x` (`6.6.142-4k-…` on test device) |
| ABI | `arm64-v8a` |
| Root | **KernelSU** LKM (`ksud` 3.3.x) |
| SELinux | Enforcing |

### Root manager statement (critical)

This project has **only** been tested with **KernelSU**.

It has **NOT** been tested with:

- Magisk
- APatch
- Other root managers

The module ZIP uses a Magisk/KernelSU-compatible layout so other managers *may* install it, but **compatibility is unverified**. Do not assume Magisk/APatch support.

## Requirements

- POCO F7 (`onyx`) or a closely related device with LSM6DSV (other devices: **unsupported / untested**)
- KernelSU + manager able to load modules
- Camera permission for torch / camera intents
- Optional: Google Camera (MGC) or another still-image camera for Twist

## Installation

1. Download the latest **`f7_moto_actions-vX.Y.Z.zip`** from [Releases](https://github.com/GustavoCaruzoGoncalves/moto-actions-poco-f7/releases).
2. Open **KernelSU Manager** → Modules → Install from storage → select the ZIP.
3. Reboot.
4. Open **F7 Moto Actions**, grant **Camera**, **Notifications**, and unrestricted / ignore battery optimizations if prompted.
5. Leave the foreground service notification running.
6. Test Chop-Chop (torch) and Twist (camera) with the screen on.

Full steps, recovery, and diagnostics: [docs/INSTALLATION.md](docs/INSTALLATION.md).

## Configuration

In-app toggles and advanced thresholds (schema 12 defaults):

| Setting | Default | Range / notes |
|---------|---------|----------------|
| Service enabled | on | Master switch |
| Chop enabled | on | Flashlight |
| Twist enabled | on | Camera |
| Screen-off gestures | on | Higher power; limited by non-wakeup IMU |
| Sensitivity | `1.0` | `0.5`–`2.0` (higher = easier) |
| Cooldown | `1000` ms | `400`–`5000` |
| Sampling rate | `100` Hz | `25`–`200` |
| Chop `ax_p2p` | `70` | Peak-to-peak on accel X |
| Chop peak magnitude | `20` | |
| Chop window / max gap | `800` / `140` ms | |
| Twist edge / `gy_p2p` / peak W | `8` / `25` / `15` | |
| Twist window / max gap | `900` / `350` ms | |
| Twist accel veto | `22` | Reject twist-like motion during high linear accel |
| Verbose logs | off | `logcat` tag `F7MotoActions` |

Details: [docs/CONFIGURATION.md](docs/CONFIGURATION.md).

## Uninstallation

1. KernelSU Manager → disable or remove `f7_moto_actions` → reboot.
2. If the APK remains: `adb shell pm uninstall org.f7.motoactions`.
3. No boot/vendor partition is permanently patched; removal reverses systemless overlays.

## Architecture (short)

```text
IMU (LSM6DSV accel+gyro) + proximity
        │
        ▼
 SensorPipeline (FGS + optional ScreenOff wake lock)
        │
        ├─ ChopChopDetector ──► TorchController
        └─ TwistDetector ────► CameraLauncher
                                 ├─ flag file → module root helper (am start MGC)
                                 ├─ BAL / full-screen intent trampoline
                                 └─ STILL_IMAGE_CAMERA(_SECURE)
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Limitations

- Not Motorola SensorHub / DSP recognition.
- Screen-off cannot match OEM wake-up sensors.
- Camera from background depends on KernelSU root helper and/or full-screen intent grants on Android 17 BAL.
- Classifier thresholds are experimental (small paired Mot/F7 dataset).
- PixelOS onyx often has a full `/system` — module prefers `pm install` of the bundled APK rather than a durable priv-app overlay.

Full list: [docs/KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md).

## Evidence

Functional and instrumentation evidence (anonymized): [docs/evidence/](docs/evidence/README.md).

Battery / performance methodology and measured snapshots: [docs/BATTERY_AND_PERFORMANCE.md](docs/BATTERY_AND_PERFORMANCE.md).

**Do not assume “negligible battery impact.”** Continuous non-wakeup IMU + optional partial wake locks have a real cost; see the battery document for what was measured and what was not.

## Build

```powershell
# Windows — JDK 17+, Android SDK, Python 3 for ZIP packing
cd moto-actions-poco-f7
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
.\build.ps1   # signs APK into module/ and packs dist/f7_moto_actions-<version>.zip
```

CI: A GitHub Actions workflow is prepared locally; enabling it on the remote requires a `gh` token with the `workflow` scope (see CONTRIBUTING / maintainer notes).

## License

Apache License 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).

Motorola / POCO / PixelOS / KernelSU names are used for compatibility description only.
