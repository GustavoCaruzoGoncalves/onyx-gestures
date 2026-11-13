# Changelog

All notable changes to this project are documented in this file.

## [1.3.0] — 2026-11-13

### Changed
- Project identity: **Onyx Gestures** (independent community project).
- Update endpoints → `GustavoCaruzoGoncalves/onyx-gestures`.
- KernelSU Manager launch uses Android package visibility (`<queries>`); no GitHub fallback.
- Root probe messaging: authorize Onyx Gestures in KernelSU Manager when denied.
- SHA-256 verification when `update.json` includes `sha256`.

### Notes
- Module ID remains `f7_moto_actions` for upgrade compatibility with existing installs.
- Disclaimer: not affiliated with Motorola, Lenovo, Xiaomi, or other OEMs.

## [1.2.3] — 2026-11-13

### Added
- In-app **Check for updates** / **Install latest from GitHub** (root + `ksud module install`) with optional reboot prompt.
- KernelSU `updateJson` pointing at GitHub releases (`module/update.json`).
- **Open KernelSU** shortcut for Manager-based module updates.

## [1.2.2] — 2026-11-13

### Added
- Dark / light / system theme.
- Locales: English, Portuguese, Spanish, Simplified Chinese, Traditional Chinese, Hindi, Russian (in-app picker + system).
- Clear **GustavoCaruzo** authorship in the UI and module metadata.

### Fixed
- Content drawing under the status bar (edge-to-edge + system bar padding).

### Changed
- Settings UI restyled (amber / charcoal palette, section cards).

## [1.2.1] — 2026-11-13

### Changed
- Twist uses only the temporal sequence detector (`IDLE→ROT1→REVERSAL→ROT2→CONFIRM`); no user-facing engine switch.
- Module author metadata: **GustavoCaruzo**.

## [1.2.0] — 2026-11-13

### Added
- Temporal Twist detector as the double wrist rotation engine (`IDLE→ROT1→REVERSAL→ROT2→CONFIRM`).
- State-transition EventLog lines (`TWIST_SM …`) for diagnosis.

### Changed
- Twist recognition prioritizes kinematic sequence (integrals, reversal, timing) over a single `gy_p2p` gate — recovers softer executions such as F7 SensorLab `NORMAL_005` (`gy_p2p≈24.7`).

### Notes
- Chop-Chop unchanged.
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
