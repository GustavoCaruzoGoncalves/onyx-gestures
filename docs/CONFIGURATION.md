# Configuration

Settings are stored in SharedPreferences (`f7_moto_actions`, schema **12**). Opening the app after an older schema migrates defaults.

## User toggles

| Key | Default | Effect |
|-----|---------|--------|
| `service_enabled` | `true` | Master enable for the gesture engine |
| `chop_enabled` | `true` | Chop-Chop → torch |
| `twist_enabled` | `true` | Twist → camera |
| `screen_off_enabled` | `true` | Keep listening when display is off (higher power) |
| `verbose_logs` | `false` | Extra `logcat` diagnostics |

## Sensitivity and timing

| Key | Default | Valid range | Effect |
|-----|---------|-------------|--------|
| `sensitivity` | `1.0` | `0.5`–`2.0` | Divides chop/twist magnitude gates (higher = easier) |
| `cooldown_ms` | `1000` | `400`–`5000` | Minimum time between successful gestures |
| `sampling_hz` | `100` | `25`–`200` | Target IMU rate (screen-off path may raise rate further) |

## Chop detector

| Key | Default | Effect |
|-----|---------|--------|
| `chop_ax_p2p` | `70` | Min accel-X peak-to-peak in window |
| `chop_peak_mag` | `20` | Min peak magnitude |
| `chop_window_ms` | `800` | Analysis window |
| `chop_max_gap_ms` | `140` | Max gap between chop peaks |

Effective chop threshold: `chop_ax_p2p / sensitivity`.

## Twist detector

Engine: `TemporalTwistDetector` (angular integrals / reversal state machine).  
Legacy magnitude prefs below remain stored for schema compatibility / sensitivity scaling context but are not an alternate user-selectable engine:

| Key | Default | Effect |
|-----|---------|--------|
| `twist_edge` | `8` | Legacy edge gate (unused by temporal path) |
| `twist_gy_p2p` | `25` | Legacy gyro-Y p2p (unused by temporal path) |
| `twist_peak_w` | `15` | Legacy peak score (unused by temporal path) |
| `twist_window_ms` | `900` | Legacy window |
| `twist_max_gap_ms` | `350` | Legacy gap |
| `twist_accel_veto` | `22` | Floor for temporal accel abort (pipeline uses ≥28) |

## Camera launch path

On Twist success, `CameraLauncher`:

1. Writes `request_camera` under the app files dir for the KernelSU `service.sh` root helper.
2. Attempts BAL-friendly / full-screen intent trampoline.
3. Falls back through MGC explicit components and `STILL_IMAGE_CAMERA(_SECURE)` intents.

No separate preference currently selects the camera package; MGC (`com.android.MGC`) is preferred when installed.

## Calibration UI

The app can capture labeled samples and propose thresholds. Defaults already match Mot-inspired feature gates from the paired capture set (`experiments/FEATURE_TABLE.csv`). Re-calibrate if your grip or ROM differs.
