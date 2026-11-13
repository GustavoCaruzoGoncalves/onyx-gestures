# Twist implementation

**App / module:** `1.2.3` (versionCode 24) · author **GustavoCaruzo**

## Engine

`TemporalTwistDetector` only — state machine `IDLE→ROT1→REVERSAL→ROT2→CONFIRM`.  
No in-app switch to the old magnitude-based detector.

## Logging

`SensorPipeline` emits EventLog lines:

- `TWIST_SM ROTATION_1_DETECTED …`
- `TWIST_SM REVERSAL_DETECTED …`
- `TWIST_SM ROTATION_2_DETECTED …`
- `TWIST_SM GESTURE_CONFIRMED …` / `GESTURE_REJECTED …` / timeouts  
- `TWIST GESTURE_CONFIRMED …` on fire (camera launch)

## Chop / sampling

Chop-Chop untouched. IMU target rate unchanged (`sampling_hz`, default 100).
