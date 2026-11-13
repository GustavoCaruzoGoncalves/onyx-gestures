# Twist implementation

**App / module:** `1.2.0` (versionCode 21)

## Engines

| Pref `twist_temporal` | Class | Role |
|-----------------------|-------|------|
| `true` (**default**) | `TemporalTwistDetector` | State machine IDLE→ROT1→REVERSAL→ROT2→CONFIRM |
| `false` | `TwistDetector` | Legacy magnitude gates (`gy_p2p` / peak_w / opposite edges) |

UI toggle: **Temporal Twist detector**.

## Why temporal is default

Legacy accepts only when a 900 ms window has `gy_p2p≥25` **and** opposite \|gy\| edges.  
Softer but valid double twists (e.g. F7 `NORMAL_005`, `gy_p2p≈24.7`) are false negatives.  
The temporal engine confirms via angular integrals + reversal on the dominant axis at onset.

## Logging

`SensorPipeline` emits EventLog lines:

- `TWIST_SM ROTATION_1_DETECTED …`
- `TWIST_SM REVERSAL_DETECTED …`
- `TWIST_SM ROTATION_2_DETECTED …`
- `TWIST_SM GESTURE_CONFIRMED …` / `GESTURE_REJECTED …` / timeouts  
- `TWIST GESTURE_CONFIRMED …` on fire (camera launch)

## Chop / sampling

Chop-Chop untouched. IMU target rate unchanged (`sampling_hz`, default 100). Temporal work is O(1) per gyro sample.
