# Development

## Project layout

| Path | Purpose |
|------|---------|
| `app/` | Kotlin / Compose Android application |
| `module/` | KernelSU module scripts + system overlays |
| `scripts/` | Host helpers (battery reports, packaging aids) |
| `docs/` | User and engineering documentation |
| `docs/evidence/` | Anonymized test evidence |
| `experiments/` | Feature tables / Mot↔F7 analysis artifacts |
| `tools/` | Module APK signing keystore for reproducible builds |
| `tests/` | Host-side notes / future integration harness |
| `native/` | Optional / experimental native bits (not required for v1.1.2) |

## Build dependencies

- JDK 17
- Android SDK Platform 37 + Build-Tools
- Gradle wrapper 8.13
- Python 3 (ZIP packing with POSIX paths in `build.ps1`)

## Packaging

`build.ps1`:

1. Runs unit tests (unless `-SkipTests`)
2. `assembleRelease`
3. Signs APK into `module/system/priv-app/F7MotoActions/F7MotoActions.apk`
4. Packs `dist/f7_moto_actions-<version>.zip` with forward-slash entries

Version is read from `module/module.prop` (`version=` / `versionCode=`). Keep `app/build.gradle.kts` `versionName` / `versionCode` in sync.

## CI

`.github/workflows/ci.yml` runs unit tests and assembles release on Ubuntu runners. Full device tests remain manual.
