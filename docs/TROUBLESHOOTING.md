# Troubleshooting

## Gestures never fire

1. Confirm FGS is up: `adb shell dumpsys activity services org.f7.motoactions | grep isForeground`
2. Confirm sensors: `adb shell dumpsys sensorservice | grep org.f7.motoactions`
3. Enable verbose logs in-app; watch `adb logcat -s F7MotoActions`
4. Raise sensitivity slightly or re-calibrate
5. Ensure Chop/Twist toggles are enabled

## Chop detected but torch does not change

- Grant **Camera** permission (required for `setTorchMode` on many ROMs)
- Another app may hold the camera / torch exclusively

## “Opening camera…” notification but camera stays closed

Android BAL blocked the activity. With **v1.1.2+**:

1. Ensure KernelSU module is enabled and rebooted so `service.sh` camera loop runs
2. Confirm MGC or another still-image camera is installed
3. Grant full-screen intent / unrestricted battery for `org.f7.motoactions`
4. Smoke-test the helper:

   ```bash
   adb shell su -c 'echo 1 > /data/data/org.f7.motoactions/files/request_camera'
   ```

   The file should disappear and MGC should open within ~1 s if the loop is alive.

## Works only with the app in the foreground

Usually the same BAL issue — update to ≥1.1.2 and verify the root helper loop.

## Screen-off unreliable

Expected limitation: non-wakeup LSM6DSV. Keep screen-off enabled only if you accept wake-lock cost; deep sleep parity with Motorola is **not** claimed.

## Module installed but app missing after reboot

`/system` may be full. Check `service.sh` / `customize.sh` `pm install` logs:

```bash
adb logcat -d -s F7MotoActions
```

Reinstall the ZIP or `pm install` the APK from the module path manually.

## High battery drain

Disable screen-off gestures; lower `sampling_hz`; see [BATTERY_AND_PERFORMANCE.md](BATTERY_AND_PERFORMANCE.md).
