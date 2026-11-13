# Battery and performance

## Summary

Continuous LSM6DSV sampling at ~100 Hz plus an optional screen-off `PARTIAL_WAKE_LOCK` **will consume power**. This project does **not** claim negligible battery impact.

What we have today is a **short, USB-powered instrumentation snapshot** (module enabled, service running). It is **not** a multi-hour unplugged drain study and must not be converted into %/day or precise mAh/day claims.

## Tested environment

| Field | Value |
|-------|-------|
| Device | POCO F7 (`onyx`) |
| ROM | PixelOS Android 17 (`CP2A.260605.016`) |
| Kernel | 6.6.142-4k-… |
| Module | v1.1.2 |
| Root | KernelSU 3.3.x |
| Date | 2026-11-13 |

## Methodology

1. `dumpsys batterystats --reset`
2. Capture script: `docs/evidence/battery/capture_snap.sh` (env, battery, sensors, power, meminfo, deviceidle)
3. Idle with module **enabled** and GestureService foreground
4. Re-capture after ~8 minutes (`enabled_idle_T1_8min.txt`)

### Constraints (important)

- Device remained **USB powered** (`USB powered: true`, charging status). Charge counter / battery % **cannot** demonstrate discharge under these conditions.
- No matched “module disabled” multi-hour unplugged baseline was completed for this release.
- App-side timer estimates (if shown in UI) are **order-of-magnitude models**, not coulomb truth for the package alone.

## Measured values (enabled, interactive / USB)

From `docs/evidence/battery/baseline_T0.txt`:

| Metric | Value | Type |
|--------|-------|------|
| Battery level | 30% | System estimate |
| Charge counter | 1 812 000 (µAh units as reported by `dumpsys battery`) | Counter while USB powered |
| Design / max capacity | 6 500 000 / 6 427 000 | Device property |
| Temperature | 33.6 °C (`336`) | Sensor |
| IMU registration | accel+gyro `samplingPeriod=10000µs` (~100 Hz), proximity 200 000µs | `dumpsys sensorservice` |
| Process RSS / PSS | ~119 MB RSS / ~13 MB PSS | `dumpsys meminfo` |
| DeviceIdle whitelist | `org.f7.motoactions` present | `dumpsys deviceidle` |
| Screen-off wake lock name | `F7MotoActions:ScreenOffImu` appears in power history | Acquisition/release events |

T1 (≈8 min later) is archived beside T0. Under USB charging, level/counter are **not** interpretable as idle drain.

## What was NOT measured

| Item | Status |
|------|--------|
| Unplugged baseline (module off) ≥30–60 min | **NOT TESTED** |
| Unplugged module-on screen-off ≥30–60 min | **NOT TESTED** |
| Package-attributed mAh from `batterystats` over a clean window | **NOT RELIABLE** in this capture (UID section incomplete / USB charging) |
| % per day extrapolation | **NOT CLAIMED** |
| Comparison vs Magisk build | N/A (KernelSU only) |

## Performance notes

- Foreground service + 100 Hz non-wakeup IMU is the dominant continuous cost while listening.
- Screen-off mode renews partial wake locks → expect higher cost than screen-on listening without a wake lock.
- Memory footprint ~13 MB PSS is modest relative to camera apps; CPU cost is sampling/classify bound and was not profiled with `simpleperf` for this release.

## How to reproduce better measurements

```powershell
# On device: unplug USB, disable always-on display, fix brightness, airplane mode optional
adb shell su -c 'dumpsys batterystats --reset'
# Wait N minutes in the desired screen state
adb shell su -c 'sh /data/local/tmp/capture_snap.sh' > T_after.txt
adb shell dumpsys batterystats org.f7.motoactions
```

Compare charge counter delta and uid wake lock / sensor time **only when unplugged**. Repeat with the module disabled for a baseline.

## Conclusions

1. Sensor registration and service residency are **confirmed**.
2. Quantitative battery **drain** under realistic unplugged conditions is **not established** for v1.1.2.
3. Users who need maximum idle life should disable screen-off gestures and/or lower `sampling_hz`.
