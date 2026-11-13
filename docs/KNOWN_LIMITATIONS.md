# Known limitations â€” v1.1.2

1. **Not Mot SensorHub.** Recognition is AP-side software on LSM6DSV samples.
2. **No wake-up IMU** on onyx â†’ cannot claim Mot-equivalent deep sleep. Screen-off uses renewable `PARTIAL_WAKE_LOCK` + high-rate IMU. Long idle / Doze survival is not proven.
3. **Camera from background:** Android BAL blocks naive `startActivity`. v1.1.2 uses a KernelSU root helper (`request_camera` flag) plus full-screen intent / explicit MGC components. Without the module helper loop, camera may show a notification only.
4. **Experimental thresholds** from a small Mot-paired set â€” not production-certified.
5. **Portrait / standard hold** assumed for ax / gy dominance.
6. **Slow Mot-reject gestures** (`CHOP_SLOW`, `TWIST_SLOW`) remain hard negatives; residual false-positive risk exists.
7. **Training caveats:** F7 `CHOP_POSITIVE_002` invalid; some Mot burst windows are artifacts.
8. **`/system` full** on PixelOS onyx â†’ `pm install` path rather than durable priv-app overlay.
9. **SELinux Enforcing** kept; no custom policy shipped.
10. **Root managers other than KernelSU** are untested.
11. **Battery:** continuous IMU has measurable cost; no claim of negligible impact. See `BATTERY_AND_PERFORMANCE.md`.

