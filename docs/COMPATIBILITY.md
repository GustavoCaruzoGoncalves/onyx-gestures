# Compatibility

## Intent

Onyx Gestures is **ROM-agnostic**: it does not depend on PixelOS-specific APIs. Any Android ROM that can run a KernelSU module and expose standard camera / torch / sensor APIs is in scope.

## Validated (reference)

| Dimension | Value |
|-----------|-------|
| Device | POCO F7 |
| Codename | `onyx` |
| ROM (reference only) | PixelOS Android 17 (`CP2A.260605.016`) |
| Kernel | 6.6.x |
| ABI | arm64-v8a |
| Root | KernelSU LKM (`ksud` 3.3.x) |
| Module ID | `f7_moto_actions` |

PixelOS here means “where we tested,” **not** “only supported ROM.”

## Expected elsewhere

| Target | Expectation |
|--------|-------------|
| Other custom ROMs on `onyx` (Lineage, crDroid, HyperOS ports, …) | Should work; please report |
| Stock / vendor ROMs with KernelSU on `onyx` | Should work if sensors + torch APIs behave normally |
| Other devices with LSM6DSV-class IMU | Likely needs sensitivity retuning |
| Magisk / APatch | Untested (ZIP layout is Magisk-compatible for packaging only) |
| Emulators | Untested / not useful |

## Hardware dependency

Chop / twist classifiers assume portrait-ish handheld motion on LSM6DSV-class accel/gyro. Other IMUs may need retuning via in-app sensitivity.

## Camera apps

Preferred when present: `com.android.MGC` (SecureCamera / CameraImage). Other camera packages may open via `STILL_IMAGE_CAMERA(_SECURE)` but are less validated — this varies by ROM.
