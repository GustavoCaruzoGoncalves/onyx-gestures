# Testing

## Automated

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Unit tests cover synthetic streams for `ChopChopDetector` and `TwistDetector` (positive-shaped and negative-shaped sequences). Passing unit tests **does not** prove physical torch/camera actions on hardware.

## Manual smoke (device)

Environment: POCO F7 / PixelOS 17 / KernelSU.

| ID | Procedure | Expected |
|----|-----------|----------|
| M1 | Screen on, Chop-Chop | Torch toggles |
| M2 | Screen on, Twist | Camera UI becomes resumed (MGC preferred) |
| M3 | App backgrounded, Twist (v1.1.2+) | Camera opens without needing app UI |
| M4 | Walking / pocket | No torch/camera |
| M5 | Screen off Chop/Twist | **Optional / flaky** — document outcome honestly |
| M6 | Disable module + reboot | Gestures stop; stock behavior restored |

## Correlating detection with action

Prefer evidence that ties:

1. `logcat` detection line (`F7MotoActions`)
2. Torch callback / camera activity resume (`dumpsys activity activities`)

Classifier-only logs are insufficient as sole proof of hardware action.

## Evidence pack

See [evidence/README.md](evidence/README.md). Mark categories **NOT TESTED** when no correlated capture exists.
