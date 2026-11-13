# Battery evidence

- **Date:** 2026-11-13  
- **Environment:** POCO F7 / PixelOS 17 / KernelSU / module v1.1.2  
- **Procedure:** Reset batterystats; capture `capture_snap.sh` at T0; idle ~8 minutes with module enabled; capture T1  
- **Power source:** **USB powered** throughout — discharge not measurable  
- **Expected:** Document sensor residency and memory; avoid false mAh claims  
- **Actual:** See `baseline_T0.txt` and `enabled_idle_T1_8min.txt` (when present)  
- **Result:** **INSTRUMENTED / LIMITED** — see `docs/BATTERY_AND_PERFORMANCE.md`  
- **NOT TESTED:** Unplugged drain baseline vs enabled; %/day estimates  
