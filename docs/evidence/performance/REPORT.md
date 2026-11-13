# Performance evidence

- **Date:** 2026-11-13  
- **Files:** `sensorservice_listeners.txt`, `meminfo_snapshot.txt`  
- **Findings:**  
  - Accel/gyro registered at `10000µs` (~100 Hz) for `org.f7.motoactions.service.SensorPipeline`  
  - Proximity at `200000µs`  
  - Approx. **13 MB PSS** / **120 MB RSS** while FGS idle  
- **Result:** **PASS (snapshot)**  
- **NOT TESTED:** CPU % over time, `simpleperf` profiles, Doze entry with screen-off listening for ≥30 min  
