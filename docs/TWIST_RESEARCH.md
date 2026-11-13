# Twist / Quick Capture — Research Phase

**Status:** Investigation / data collection (no threshold changes to public `v1.1.2`).  
**Branch (implementation later):** `feature/twist-classifier-research`  
**Primary device:** POCO F7 (`onyx`) · PixelOS 17 · LSM6DSV non-wakeup  
**Reference device:** Moto G42 (Mot sensor `65540`) when available  

Chop-Chop detectors are **out of scope** unless a shared pipeline bug is proven.

---

## 1. Diagnosis of the current Twist implementation

### Pipeline (v1.1.2)

| Stage | Behavior |
|-------|----------|
| Sensors | `TYPE_ACCELEROMETER` + `TYPE_GYROSCOPE` (+ proximity unused for twist) |
| Rate | Target ~100 Hz (`sampling_hz`); screen-off ≥80 Hz, batchUs=0 |
| Filtering | **None** (raw `SensorEvent.values`) |
| Frame | Device coordinates; **Y assumed dominant** under portrait hold |
| Features | Rolling window `gy_p2p`, `peak_w=\|\ω\|`, dominance vs gx/gz |
| Segmentation | Implicit 900 ms prune window |
| Peaks | Rising-edge hysteresis on **\|gy\| only** (≥ edge, then valley, opposite sign) |
| Sequence | Exactly **two** opposite-sign half-peaks with gap ∈ [70, 350] ms |
| Classification | Hard AND of magnitude + dominance + half-sequence + accel veto |
| Cooldown | `refractoryAfterGestureMs` ≈ prefs cooldown (default 1000 ms) + re-arm on low \|gy\| |
| Action | `CameraLauncher` (root helper / BAL / MGC) — orthogonal to recognition quality |

### What it actually recognizes

The live detector is **not** a full double-wrist-rotation kinematic model. It accepts when:

1. Recent window has large **gy** swing and large \|ω\| peak, **and**
2. Two opposite-sign **\|gy\|** edges appear with a short gap.

That is closer to “energetic bidirectional gy wiggle” than to “two similar half-rotations with return / angular displacement constraints.”

### Likely causes of poor precision

| Cause | Evidence / mechanism |
|-------|----------------------|
| **Magnitude-first gating** | Mot `TWIST_SLOW` already showed Mot reject with `gy_p2p≈31`, `peak_w≈21` inside the magnitude band — rate/shape matters more than p2p alone |
| **Axis lock to gy** | Only `y` drives half-tracking; natural grips can move energy to gx/gz → FN or wrong dominance reject |
| **No angular integral / direction quality** | Single landscape rotate or continuous spin can still produce opposite edges + high p2p |
| **Gap window too permissive / too tight** | 70–350 ms may miss slow doubles or accept chaotic shakes |
| **Tiny F7 labeled set** | Only **2** F7 positives + **1** F7 rotate-negative in `FEATURE_TABLE.csv` — thresholds Mot-transferred, not F7-validated |
| **Bad / sparse negatives** | Missing pickup, landscape, camera-handle, typing, single-twist, continuous rotate campaigns on F7 |
| **No state machine** | No explicit IDLE → ROT1 → REVERSE → ROT2 → DONE with timeouts; half-state is fragile |
| **Shared accel veto only** | Walk/shake rejection depends on chop’s dynamic accel; camera-prep motions may slip through |

### Camera path

Background camera open (v1.1.2 helper) is **separate**. Research focuses on **when** `TwistDetector` returns true, not on BAL.

---

## 2. Existing data inventory (preserve; do not overwrite)

| Set | Sessions | Notes |
|-----|----------|-------|
| G42 Mot-aligned positives | `TWIST_POSITIVE_001…003` | OEM `65540` windows |
| G42 Mot-reject slow | `TWIST_SLOW_001` | High magnitude, Mot fail — critical |
| G42 negatives | rotate / walk / random | Random = bad segmentation |
| F7 positives | `TWIST_POSITIVE_001…002` | Burst-segmented only |
| F7 negatives | `TWIST_NEGATIVE_ROTATE_001` | Insufficient |

**Conclusion:** prior data are useful for Mot semantics hypotheses but **not sufficient** for a robust F7 Twist classifier.

---

## 3. Research goals

1. Characterize true **double wrist twist** on F7 across speeds/amplitudes/holds.  
2. Collect hard negatives that look like high \|ω\|.  
3. Engineer timestamp-based features (peaks, order, integral, zero-crossings, orientation).  
4. Compare rule / multivariate / **temporal state machine** / optional DTW.  
5. Validate with **LOSO by session**, not shuffled samples.  
6. Only then propose detector changes on the feature branch (never silently patch `v1.1.2` release).

See also: `TWIST_EXPERIMENT_PROTOCOL.md`, `TWIST_FEATURES.md`, `TWIST_VALIDATION.md`.
