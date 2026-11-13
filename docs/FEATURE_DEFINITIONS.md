# Feature definitions (SensorLab experimental pipeline)

**Scope:** definitions of features used in `experiments/FEATURE_TABLE.csv` and `experiments/analysis/*_features_enriched.csv`.  
**Raw data are never modified.** Features are derived views.

---

## Coordinate frames and sensors

| Device | Accel / Gyro | Typical rate in captures | Wake-up IMU |
|--------|--------------|--------------------------|-------------|
| Moto G42 | `icm4x6xa` | ~200.4 Hz (`gap_med≈4.99 ms`) | yes (used) |
| POCO F7 | `lsm6dsv` | ~237.4 Hz (`gap_med≈4.21 ms`) | **no** (non-wakeup only) |

Values are Android `SensorEvent.values` in the device coordinate frame (screen facing user, portrait):

- Accel: \(a_x,a_y,a_z\) in **m/s²** (includes gravity).
- Gyro: \(\omega_x,\omega_y,\omega_z\) in **rad/s**.

**No filtering** (no low-pass / high-pass / Kalman) is applied before these features.  
**No axis remapping** between G42 and F7 in this pipeline (empirical: chop ↔ \(a_x\), twist ↔ \(\omega_y\) under the standard hold).

---

## Segmentation (critical)

Features are computed on a **window** \(W\), not the whole recording.

### Mot-aligned window (G42 only, when OEM event exists)

| Gesture | Trigger | Window |
|---------|---------|--------|
| Chop | sensor type `65546` (`com.motorola.sensor.chopchop`) | \([t_\mathrm{mot}-800\,\mathrm{ms},\, t_\mathrm{mot}]\) |
| Twist | sensor type `65540` (`com.motorola.sensor.camera_activate`) | \([t_\mathrm{mot}-1000\,\mathrm{ms},\, t_\mathrm{mot}+200\,\mathrm{ms}]\) |

`src=mot` in tables.

### Burst heuristic (F7 always; G42 when no Mot event)

1. Build series of samples with activity gate: chop \(|a|>15\), twist \(|\omega|>3\).
2. Split into bursts where consecutive active samples are separated by **>400 ms**.
3. Take the burst with maximum peak magnitude.

`src=burst`.

**Limitations**

- Burst merge can glue unrelated motion (see `TWIST_NEGATIVE_RANDOM_001`, `burst_ms≈9673`).
- Mot twist windows force `burst_ms≈1198` — that is the **fixed window length**, not the physical wiggle duration.
- Chop Mot windows force `burst_ms≈799` similarly.

---

## Scalar magnitudes

\[
|a| = \sqrt{a_x^2+a_y^2+a_z^2},\qquad
|\omega| = \sqrt{\omega_x^2+\omega_y^2+\omega_z^2}.
\]

Units: m/s² and rad/s.

---

## Peak-to-peak features

Computed on window \(W\):

\[
\mathrm{ax\_p2p} = \max_{t\in W} a_x(t) - \min_{t\in W} a_x(t)
\]

Analogously `ay_p2p`, `az_p2p`, `gx_p2p`, `gy_p2p`, `gz_p2p`.

| Feature | Unit | Meaning |
|---------|------|---------|
| `ax_p2p` | m/s² | Swing of linear accel along device X |
| `gy_p2p` | rad/s | Swing of angular rate about device Y |

**Comparable across devices?** Same physical units and similar IMU ranges; rates differ (~200 vs ~237 Hz) but p2p extrema are largely rate-insensitive if the gesture is captured. Residual device scale/noise not yet calibrated.

---

## Peak magnitude features

\[
\mathrm{peak\_a} = \max_{t\in W} |a(t)|,\qquad
\mathrm{peak\_w} = \max_{t\in W} |\omega(t)|.
\]

**Limitation:** walking and pocket can reach `peak_a≈45–69` without Mot chop — **not Mot-discriminative alone**.

---

## Chop peak counting (`n_peaks20`, `interpeak_med_ms`)

On window \(W\), detect peaks of \(|a|\) with:

1. Candidate if \(|a|\ge 20\,\mathrm{m/s²}\).
2. Local refine in \(\pm 40\,\mathrm{ms}\).
3. Refractory **80 ms** between accepted peaks.

Then:

\[
\mathrm{n\_peaks20} = \text{number of accepted peaks},
\]

\[
\Delta_i = t_{i}-t_{i-1}\ \mathrm{(ms)},\qquad
\mathrm{interpeak\_med\_ms} = \mathrm{median}\{\Delta_i\}.
\]

### Median implementation difference (documented)

| Table | Even-length median rule | Example F7 `CHOP_POSITIVE_001` Δ=[80, 93, 219, 286] |
|-------|-------------------------|------------------------------------------------------|
| `FEATURE_TABLE.csv` (`feature_table.py`) | `sorted[n//2]` (upper middle) | **219.1 ms** |
| `chop_features_enriched.csv` (`numpy.median`) | average of two central | **155.9 ms** |

Both are valid; **do not mix them** when comparing sessions. Prefer documenting which estimator was used.

**Limitations:** threshold 20 / refractory 80 are analysis choices, not Mot firmware constants. Multi-chop or rebound creates 4–6 peaks; median then mixes strike spacing with pause-between-strikes.

---

## `burst_ms`

\[
\mathrm{burst\_ms} = (t_\mathrm{last}-t_\mathrm{first})\ \text{of samples in } W,\ \mathrm{ms}.
\]

Interpret only with `src`:

- `mot` → mostly window length artifact.
- `burst` → activity-gated duration (can be over-merged).

---

## Derived / enrichment-only

| Feature | Definition |
|---------|------------|
| `dominant_lin` | \(\arg\max\{\mathrm{ax\_p2p},\mathrm{ay\_p2p},\mathrm{az\_p2p}\}\) |
| `dom` (twist) | \(\arg\max\{\mathrm{gx\_p2p},\mathrm{gy\_p2p},\mathrm{gz\_p2p}\}\) |
| `gy_zc` | count of sign changes of \(\omega_y\) inside \(W\) (zero crossings) |
| `interpeak_min_ms` | \(\min\Delta_i\) |

---

## Cross-device comparability checklist

| Aspect | Status |
|--------|--------|
| Units | Same SI units |
| Axis meaning under standard hold | Empirically aligned (chop→X, twist→Y) |
| Sampling rate | Different; p2p OK; fine timing ±1 sample differs |
| Segmentation | Mot vs burst — **not fully comparable** for `burst_ms` / some timing stats |
| Absolute thresholds | Must not be copied blindly; use relative structure + margins |

---

## What these features are **not**

- Not Mot SensorHub internal state.
- Not gravity-compensated linear acceleration (unless separately using type 10 stream — current p2p uses type 1 accel).
- Not energy/RMS/DTW features (candidates for a later revision).
