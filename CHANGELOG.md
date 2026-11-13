# Changelog

All notable changes to this project are documented in this file.

## [1.2.0] — 2026-11-13

### Added
- **Temporal Twist detector** as the default Quick Capture engine (`IDLE→ROT1→REVERSAL→ROT2→CONFIRM`).
- Pref / UI toggle to fall back to the legacy magnitude-based `TwistDetector`.
- State-transition EventLog lines (`TWIST_SM …`) for diagnosis.

### Changed
- Twist recognition prioritizes kinematic sequence (integrals, reversal, timing) over a single `gy_p2p` gate — recovers softer executions such as F7 SensorLab `NORMAL_005` (`gy_p2p≈24.7`).

### Notes
- Chop-Chop unchanged. Legacy Twist remains available for rollback.
- Small labeled F7 set; further negatives still welcome.

## [1.1.2] — 2026-11-13

### Added
- Root camera helper loop in `module/service.sh` consuming `request_camera` flag files.
- Explicit MGC component launch (`SecureCameraActivity` / `CameraImageActivity`) for Twist when the app is backgrounded.

### Fixed
- “Opening camera…” heads-up notification without actually starting the camera under Android 17 background activity limits.

### Notes
- First public GitHub release packaging of the working KernelSU module for POCO F7 / PixelOS 17.

## [1.1.1] — 2026-11-13

### Changed
- Stronger screen-off keep-alive: renewable `PARTIAL_WAKE_LOCK` (~45 s), higher IMU rate when screen off, Doze nudges.
- Camera path prefers elevated `am start` / full-screen intent trampoline.

## [1.1.0] — 2026-11-13

### Changed
- Chop / Twist detectors rewritten around Mot-inspired features (`ax_p2p`, `gy_p2p`, peaks, timing gates) from paired Mot G42 ↔ F7 captures.
- Prefs schema 12 defaults aligned with feature table thresholds.

## [1.0.x] — 2026-11

### Added
- Initial KernelSU module + Compose settings app, torch + camera actions, FGS sensor pipeline, calibration UI, Magisk-layout ZIP packer (`build.ps1`).
