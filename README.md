# Onyx Gestures

An independent Android gesture-control project for compatible devices.

Software recognition of:

- **Double chopping motion** → toggle flashlight  
- **Double wrist rotation** → launch camera  

Delivered as a reversible **KernelSU** module plus a Kotlin foreground service. Intended for **any Android ROM** that can load KernelSU modules on a compatible device — not tied to a single custom ROM.

Primary validation so far: **POCO F7** (`onyx`) with an LSM6DSV IMU (reference build used PixelOS 17). Other ROMs on the same device are expected to work the same way; please report results.

> **Disclaimer:** This is an independent community-developed project. It is not affiliated with, endorsed by, or sponsored by Motorola, Lenovo, Xiaomi, or any other device manufacturer.

> **Status:** Usable on the validated device. Thresholds come from a small labeled capture set. Treat this as an enthusiast module, not an OEM feature.

## Features

| Feature | Status |
|---------|--------|
| Double chopping motion → flashlight | **Implemented** (screen on verified; screen off partial / hardware-limited) |
| Double wrist rotation → camera | **Implemented** — temporal sequence detector; background launch via root helper + BAL / full-screen intent |
| Screen-on continuous IMU sampling (~100 Hz default) | **Implemented** |
| Screen-off listening | **Implemented with caveats** — F7 IMU is non-wakeup; uses renewable `PARTIAL_WAKE_LOCK` + high-rate sampling. Long idle / deep Doze **not proven** |
| Proximity pocket guard | **Implemented** |
| In-app sensitivity / theme / language + update from GitHub | **Implemented** |
| KernelSU module keep-alive (`service.sh`) | **Implemented** |
| Magisk / APatch | **Not tested** |

## Device & ROM compatibility

**Goal:** work on **any ROM** (AOSP / PixelOS / Lineage / HyperOS ports / etc.) as long as KernelSU modules and standard Android camera/torch APIs are available.

**Validated so far (reference):**

| Item | Value |
|------|-------|
| Device | POCO F7 |
| Codename | `onyx` |
| ROM (reference) | PixelOS (Android **17**) — *not* a requirement |
| Root | **KernelSU** LKM (`ksud` 3.3.x) |
| SELinux | Enforcing (on the reference device) |

Other ROMs on `onyx` (and similar LSM6DSV devices) should install the same ZIP; gesture thresholds may need in-app sensitivity tuning.

### Root manager statement

This project has **only** been tested with **KernelSU**.

It has **NOT** been tested with Magisk, APatch, or other root managers. The ZIP uses a Magisk/KernelSU-compatible layout so other managers *may* install it, but **compatibility is unverified**.

## Requirements

- Compatible device with accel + gyro (POCO F7 / `onyx` validated; other devices: try and report)
- **Any ROM** with KernelSU + manager able to load modules
- Camera permission for torch / camera intents

## Installation

1. Download the latest **`f7_moto_actions-vX.Y.Z.zip`** from [Releases](https://github.com/GustavoCaruzoGoncalves/onyx-gestures/releases).  
   *(Module ID remains `f7_moto_actions` for upgrade compatibility with earlier installs.)*
2. Open **KernelSU Manager** → Modules → Install from storage → select the ZIP.
3. Reboot.
4. Open **Onyx Gestures**, grant **Camera**, **Notifications**, and unrestricted / ignore battery optimizations if prompted.
5. Leave the foreground service notification running.
6. Test flashlight (double chop) and camera (double wrist rotation) with the screen on.

Authorize **Onyx Gestures** for root in KernelSU Manager if you use in-app module updates.

## Configuration

In-app toggles (schema 12 defaults):

| Setting | Default | Notes |
|---------|---------|--------|
| Service enabled | on | Master switch |
| Double chop → flashlight | on | |
| Double wrist rotation → camera | on | Temporal detector |
| Screen-off gestures | on | Higher power; limited by non-wakeup IMU |
| Theme / language | system | |
| Sensitivity | `1.0` | `0.5`–`2.0` |
| Cooldown | `1000` ms | |

## Updates

- **In-app:** Updates → check / install from GitHub (requires root grant in KernelSU Manager).  
- **KernelSU Manager:** module `updateJson` points at this repository’s `module/update.json`.

## Uninstallation

1. KernelSU Manager → disable or remove `f7_moto_actions` → reboot.
2. If the APK remains: `adb shell pm uninstall org.f7.motoactions`.

## Build

```powershell
cd onyx-gestures
.\build.ps1
```

## License

See [LICENSE](LICENSE) and [NOTICE](NOTICE).

Author: **GustavoCaruzo**
