# Experimental classifier design (not production)

**Status:** research prototypes only. **No KernelSU / app threshold freeze.**  
**Code:** `SensorLab/scripts/run_classifier_study.py`  
**Report JSON:** `experiments/analysis/validation_report.json`

---

## 1. Design principles

1. Separate **Chop** and **Twist** detectors (Mot uses distinct virtual sensors).
2. Prefer **interpretable multivariate rules** grounded in Mot-paired IMU.
3. Validate with **leave-one-session-out (LOSO)** — never split samples from the same CSV into train/test.
4. Treat metrics as **indicative**, not deployable, while \(n_\mathrm{pos}\lesssim 6\).
5. Do not ship Random Forest as authority with this n (overfits; omitted as primary).

---

## 2. Proposed Chop detector (experimental rule v0)

**Inputs on a ~0.8–1.0 s activity window** (Mot-aligned when available):

```
ACCEPT chop if ALL:
  dominant_lin == ax
  ax_p2p >= 70 m/s²
  n_peaks20 >= 2
  interpeak_med_ms <= 160   # use a FIXED median definition in code
REJECT otherwise
```

**Motivation**

- Mot positives: `ax_p2p` 96–122, interpeak ~90–125 ms, dominant ax.
- Walk: `ax_p2p` ~7–10 even when `peak_a≈45`.
- Captures Mot “double impulse on X with short spacing” better than `peak_a`.

### Resubstitution (all sessions, enriched labels)

| Metric | Value |
|--------|-------|
| Confusion `[[TN,FP],[FN,TP]]` | `[[7,1],[0,6]]` |
| Precision / Recall / F1 | 0.86 / 1.00 / 0.92 |

**Only error:** `CHOP_SLOW_001` (Mot reject) → FP under this rule (`ax_p2p=71`, interpeak=130).

### LOSO Logistic Regression (`ax_p2p`, `interpeak_med_ms`, `n_peaks20`, `peak_a`)

| Metric | Value |
|--------|-------|
| Accuracy | 0.79 |
| F1 | 0.77 |
| Errors | PICKUP FP, SLOW FP, F7 POSITIVE_001 FN |

### LOSO Decision Tree (depth≤3)

| Metric | Value |
|--------|-------|
| Accuracy | 0.86 |
| F1 | 0.83 |

Full-data tree collapsed to **`ax_p2p ≤ 75.46`** only — a symptom of tiny n, not a Mot theory. Prefer the multivariate rule.

### Missing for Chop (next features)

- Strike **pair** scoring: two opposite-sign \(a_x\) peaks within 70–150 ms.
- Explicit **gyro veto** magnitude/template (Mot firmware logs historically mention gyro aborts — not quantified here).
- Rate feature distinguishing SLOW Mot-reject from POSITIVE.

---

## 3. Proposed Twist detector (experimental rule v0)

```
ACCEPT twist if ALL:
  dom == gy
  gy_p2p >= 25 rad/s
  peak_w >= 15 rad/s
REJECT otherwise
```

### Resubstitution

| Metric | Value |
|--------|-------|
| Confusion | `[[4,1],[0,5]]` |
| F1 | 0.91 |

**Only error:** `TWIST_SLOW_001` (Mot reject) → FP (`gy_p2p=30.8`, `peak_w=20.7`).

### LOSO LogReg / Tree

| Model | Acc | F1 | Notable errors |
|-------|-----|----|----------------|
| LogReg | 0.80 | 0.80 | POSITIVE_003 FN; SLOW FP |
| Tree | 0.70 | 0.73 | RANDOM FP; POSITIVE_003 FN; SLOW FP |

Full-data tree split on **`gx_p2p ≤ 14.3`** — unstable / non-physical primary. Prefer gy-based rule; add **oscillation rate** next.

### Missing for Twist

- Half-cycle duration / frequency of \(\omega_y\) sign alternation (SLOW vs POSITIVE).
- Mot-window-tightened duration (not fixed 1.2 s).
- Cross-veto vs chop-like \(|a|\) bursts (optional).

---

## 4. Model comparison stance

| Approach | Use now? | Why |
|----------|----------|-----|
| Multivariate rules | **Yes (primary)** | Matches Mot evidence; debugable |
| Logistic Regression | Exploratory | LOSO OK; coefficients unstable |
| Shallow Decision Tree | Exploratory | Over-simplifies / wrong axis |
| Random Forest | **No as authority** | n too small; black-box |

---

## 5. Uncertainty / borderline sessions

Closest to rule frontiers (see JSON `borderline`):

| Session | Issue |
|---------|-------|
| `CHOP_SLOW_001` | Mot reject inside/near chop rule |
| `TWIST_SLOW_001` | Mot reject inside twist magnitude rule |
| `CHOP_SMALL_001` | Mot accept with lower `peak_a` (~50) — defines soft lower energy |
| `CHOP_POSITIVE_001` (F7) | Timing messy; FN risk under strict interpeak |
| `TWIST_POSITIVE_003` (G42) | Lowest `peak_w` Mot accept (~19) — soft lower bound |
| `TWIST_NEGATIVE_RANDOM_001` | Bad segmentation; model-hostile |

---

## 6. What we will **not** do yet

- Freeze thresholds into `F7-Moto-Actions` detectors.
- Claim production precision/recall.
- Assume F7 intent labels equal Mot.
- Use `burst_ms` from Mot windows as a learned duration gate without redesign.

---

## 7. Path to a defensible classifier

1. Collect prioritized sessions in `EXPERIMENT_GAPS.md` (SLOW boundary, clean F7 chop, gyro-veto probes).
2. Add timing features that separate SLOW Mot-rejects.
3. Re-run LOSO; only then consider app integration with conservative defaults + user sensitivity.
