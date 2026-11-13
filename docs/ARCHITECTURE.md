# Architecture

## Why software recognition?

LineageOS / Motorola `MotoActions` listens to OEM virtual sensors such as chop-chop (`65546`) and camera-activate (`65540`). Those sensors are **absent** on POCO F7 (`onyx`). The IMU is **LSM6DSV** exposed as **non-wakeup** accel/gyro.

This project therefore:

1. Samples accel + gyro in a foreground service.
2. Runs Mot-inspired feature detectors in Kotlin.
3. Executes torch / camera actions through Android APIs + a KernelSU root helper for background camera starts on Android 17.

## Components

| Piece | Role |
|-------|------|
| `GestureService` | Foreground service (`specialUse`), hosts pipeline, Doze nudges |
| `SensorPipeline` | Registers sensors, applies screen-on/off policy, wake locks |
| `ChopChopDetector` | Dual-peak chop on accel features (`ax_p2p`, timing) |
| `TwistDetector` | Gyro-Y dominant twist with accel veto |
| `TorchController` | `CameraManager.setTorchMode` |
| `CameraLauncher` | Flag file + intents + trampoline |
| `module/service.sh` | Boot wait, `pm install`, FGS watchdog, root camera loop |
| Compose `MainActivity` | Toggles, thresholds, energy UI, logs |

## Screen-on path

- Target ~100 Hz IMU (configurable).
- No continuous wake lock required while the display is interactive.
- Chop → torch; Twist → camera launch stack.

## Screen-off path

- F7 lacks wake-up IMU → cannot claim Mot SensorHub deep-sleep behavior.
- When `screen_off_enabled`, the pipeline renews a `PARTIAL_WAKE_LOCK` (`F7MotoActions:ScreenOffImu`) and keeps high-rate sampling.
- Proximity covered → ignore chop (pocket guard).
- Long idle / deep Doze survival is **not fully proven**; expect dropouts.

## Camera and Android BAL

Android 17 Background Activity Launch restrictions often block `startActivity` from a background FGS. Mitigations:

1. Module root helper: `am start` on MGC secure/camera activities when `request_camera` appears.
2. Full-screen intent / trampoline activity when the screen is on.
3. Explicit component names for MGC when present.

## Module layout

```text
module/
  module.prop
  service.sh
  post-fs-data.sh
  customize.sh
  uninstall.sh
  system/priv-app/F7MotoActions/F7MotoActions.apk
  system/etc/permissions/...
  system/etc/sysconfig/...
```

## Security posture

- No permanent boot/vendor edits.
- SELinux Enforcing on the tested device.
- Root is used only for keep-alive / camera `am start` helper, not for disabling SELinux.
