# Contributing

Thanks for interest in Onyx Gestures.

## Scope

This module targets **POCO F7 (`onyx`)** on **KernelSU**. PRs that break onyx KernelSU support for untested Magisk/APatch abstractions will be scrutinized carefully.

## Development setup

1. JDK 17+
2. Android SDK (compileSdk 37)
3. Clone the repo and open the Gradle project at the repository root
4. Copy `local.properties.example` to `local.properties` and set `sdk.dir` (never commit `local.properties`)

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
.\build.ps1
```

## Guidelines

- Prefer evidence over claims. Update `docs/evidence/` when you change gesture behavior.
- Keep user-facing docs in **English**.
- Do not commit device serials, personal logs, or API tokens.
- Add or update unit tests under `app/src/test` for detector logic when changing thresholds or gates.
- Document battery impact changes in `docs/BATTERY_AND_PERFORMANCE.md` when altering sampling rates or wake locks.

## Pull requests

1. Describe device / ROM / KernelSU version used for testing.
2. State what was verified (screen on/off, torch, camera, negatives).
3. Attach anonymized log excerpts when relevant.

## Issues

Use the issue templates. Include ROM build id, kernel string, KernelSU version, and module version â€” **not** your device serial number.

