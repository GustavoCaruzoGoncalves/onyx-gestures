# Twist experiment protocol (POCO F7 primary)

## Preconditions (every session)

1. POCO F7 USB connected (`adb devices` shows `onyx` / serial).  
2. SensorLab installed and able to write CSV (accel, gyro, gravity, linear accel, rotation / game rotation vectors).  
3. Target sample period ≈ **5000 µs** (∼200 Hz request; record **actual** median gap).  
4. Screen **on**, portrait, natural grip unless session says otherwise.  
5. Operator confirms readiness **before** arming capture.  
6. Unique session ID; raw CSV never overwritten.

## Physical gesture definition (positive)

**Double wrist twist (Quick Capture-like):** phone in one hand, screen facing user; rotate wrist about the forearm axis roughly along device **Y** (or the dominant axis for that hold) in one direction, reverse, and complete a second rotation of similar character within ~0.2–0.6 s between peaks. Do **not** force Mot-identical copies — capture natural variation.

### Initial pose (default)

- Portrait, screen toward face.  
- Elbow relaxed, phone at chest–face height.  
- Thumb along one long edge if comfortable.  
- Stay still 0.5 s after arming, then perform **one** gesture, then still 0.5 s.

## Capture matrix (proposed)

### Positives (F7) — ≥3 reps each class when possible

| ID pattern | Motion |
|------------|--------|
| `F7_TWIST_POS_NORMAL_###` | Double twist, normal speed |
| `F7_TWIST_POS_SLOW_###` | Double twist, deliberately slow |
| `F7_TWIST_POS_FAST_###` | Double twist, fast |
| `F7_TWIST_POS_SMALL_###` | Small amplitude |
| `F7_TWIST_POS_LARGE_###` | Large amplitude |
| `F7_TWIST_POS_HOLD_A/B/C_###` | Alternate comfortable holds / slight yaw of start pose |

### Negatives (F7) — challenging

| ID pattern | Motion |
|------------|--------|
| `F7_TWIST_NEG_SINGLE_###` | Single wrist rotation (no return double) |
| `F7_TWIST_NEG_CONTINUOUS_###` | Continuous rotate without reverse pair |
| `F7_TWIST_NEG_HANDSPIN_###` | Spin phone in hand |
| `F7_TWIST_NEG_LANDSCAPE_###` | Rotate to landscape |
| `F7_TWIST_NEG_PICKUP_###` | Pick up from table |
| `F7_TWIST_NEG_PUTDOWN_###` | Place on table |
| `F7_TWIST_NEG_WALK_###` | Walk while holding |
| `F7_TWIST_NEG_SHAKE_###` | Shake |
| `F7_TWIST_NEG_RANDOM_WRIST_###` | Random wrist fidget |
| `F7_TWIST_NEG_TYPING_###` | Normal UI use / typing |
| `F7_TWIST_NEG_CAMERA_PREP_###` | Raise phone as if opening camera |

### Optional Mot reference (G42)

Re-capture Mot-accepted / Mot-rejected twists with SensorLab Mot markers when G42 is available — for semantics, not for F7 thresholds.

## Interactive flow (mandatory)

1. Announce session ID + motion + initial pose.  
2. Wait for operator **“ready”**.  
3. Arm SensorLab.  
4. Cue gesture.  
5. Stop, pull CSV, validate rows / gaps / sensors.  
6. Write metadata; mark INVALID if needed (keep raw).

## Recommended order

1. SensorLab health check (short idle CSV).  
2. `POS_NORMAL` ×3  
3. `NEG_SINGLE`, `NEG_LANDSCAPE`, `NEG_WALK`  
4. `POS_SLOW` / `POS_FAST`  
5. Remaining positives (amplitude / holds)  
6. Remaining hard negatives  
7. Feature study + classifier prototypes (offline)

## Invalid session policy

Keep raw + metadata with `status: INVALID` and reason (drop phone, wrong gesture, capture gap storm, etc.).
