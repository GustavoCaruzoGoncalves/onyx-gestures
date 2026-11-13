# Evidence index

All device identifiers in this tree are anonymized (no USB serials).  
Environment unless noted otherwise:

| Field | Value |
|-------|-------|
| Device | POCO F7 (`onyx`) |
| ROM | PixelOS Android 17 (`CP2A.260605.016`) |
| Kernel | 6.6.142-4k-… |
| Root | KernelSU LKM (`ksud` 3.3.x) |
| Module / app | `f7_moto_actions` / `org.f7.motoactions` **1.1.2** (versionCode 19) |
| Date | 2026-11-13 |

## Category status

| Category | Status | Notes |
|----------|--------|-------|
| Installation / FGS | **PASS** | Module prop + `isForeground=true` — see `installation/` |
| Camera background helper | **PASS** | `request_camera` consumed; MGC `SecureCameraActivity` resumed — `camera-background/` |
| Chop → torch (physical) | **OPERATOR / PARTIAL** | Unit tests PASS; correlated torch hardware log **not archived in this pack** — mark as needing operator confirmation |
| Twist → camera (physical gesture) | **OPERATOR / PARTIAL** | Helper path PASS; gesture→detect→camera chain confirmed in development but full gesture log not archived here |
| Chop/Twist negatives (walk/pocket) | **FEATURE TABLE ONLY** | Offline Mot/F7 feature CSV — not a live FP campaign |
| Screen-off gestures | **NOT TESTED (archived)** | Implementation present; deep-sleep durability unproven |
| Battery quantitative drain | **LIMITED** | USB-powered idle snapshots only — see `battery/` + `../BATTERY_AND_PERFORMANCE.md` |
| Performance (sensors/mem) | **PASS (snapshot)** | 100 Hz IMU listeners + ~13 MB PSS — `performance/` |

## Integrity rule

A classifier `logcat` line alone is **not** treated as proof that torch or camera hardware acted. Prefer correlation with `CameraManager` / resumed activities / helper file consumption.
