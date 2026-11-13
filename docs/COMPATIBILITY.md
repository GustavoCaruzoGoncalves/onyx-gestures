# Compatibility

## Verified

| Dimension | Value |
|-----------|-------|
| Device | POCO F7 |
| Codename | `onyx` |
| ROM | PixelOS Android 17 (`CP2A.260605.016`) |
| Kernel | 6.6.x (`6.6.142-4k-â€¦`) |
| ABI | arm64-v8a |
| Root | KernelSU LKM (`ksud` 3.3.x) |
| Module | `f7_moto_actions` v1.1.2 |

## Explicitly unverified

- Magisk
- APatch
- Other Xiaomi/POCO devices
- HyperOS / MIUI stock
- Android versions other than 17 on this device
- Emulators

## Hardware dependency

Chop/Twist classifiers assume portrait-ish handheld motion on LSM6DSV-class accel/gyro with Mot-inspired thresholds. Other IMUs may need retuning.

## Camera apps

Preferred: `com.android.MGC` (SecureCamera / CameraImage). Other apps may open via `STILL_IMAGE_CAMERA(_SECURE)` but are less validated.

