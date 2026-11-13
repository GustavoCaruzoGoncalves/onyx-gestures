# Twist feature definitions (research)

Units use Android device frame unless noted. Prefer **timestamp windows** over fixed sample counts.

## Windowing

| Symbol | Meaning |
|--------|---------|
| \(W\) | Analysis window, typically activity-gated burst or fixed 0.6–1.2 s around gesture |
| \(t\) | `SensorEvent.timestamp` or capture CSV time base (document which) |

## Angular rate features

| Feature | Definition | Unit |
|---------|------------|------|
| `gx_p2p`, `gy_p2p`, `gz_p2p` | \(\max \omega_i - \min \omega_i\) on \(W\) | rad/s |
| `peak_w` | \(\max \|\boldsymbol{\omega}\|\) | rad/s |
| `dom` | \(\arg\max\) of `{gx,gy,gz}_p2p` | categorical |
| `rms_w` | RMS of \(\|\boldsymbol{\omega}\|\) | rad/s |
| `energy_w` | \(\sum \|\boldsymbol{\omega}\|^2 \Delta t\) | rad²/s |
| `zc_gy` | Zero-crossings of \(\omega_y\) (or dominant axis) | count |
| `peak_pos`, `peak_neg` | Max positive / min negative on dominant axis | rad/s |
| `peak_order` | Chronological order of signed peaks | sequence |
| `interpeak_ms` | Time between consecutive opposite peaks | ms |
| `prominence` | Peak prominence vs surrounding valleys | rad/s |
| `int_gy` | \(\int \omega_y\,dt\) (trapezoid on timestamps) | rad |
| `int_dom` | Integral on dominant axis | rad |
| `dir_changes` | Sign changes of \(\omega\) after deadband | count |
| `half_similarity` | Correlation / DTW distance between first and second half | 0–1 or distance |
| `return_score` | Orientation change after gesture vs start (RV / quaternion) | rad |

## Orientation features (when captured)

| Feature | Source | Notes |
|---------|--------|-------|
| `dtheta` | Game Rotation Vector / RV | Relative rotation magnitude over \(W\) |
| `axis_of_rot` | Eigen-axis of relative quaternion | Compare to device Y |
| `gravity_tilt` | Gravity vector | Reject large pose changes (landscape) |

## Accel veto features

| Feature | Definition |
|---------|------------|
| `dyn_a_p2p` | Peak-to-peak of \(\|a - g\|\) or linear accel |
| `ax_p2p` | Linear chop confound |

## Implementation note

Current production detector uses only `gy_p2p`, `peak_w`, gy dominance, opposite \|gy\| edges, and accel abort — see `TWIST_RESEARCH.md`.
