# Camera background helper evidence

- **Device / software:** POCO F7 onyx, PixelOS 17, KernelSU, module v1.1.2  
- **Date:** 2026-11-13  
- **Procedure:**  
  1. Ensure `service.sh` camera loop is running  
  2. `echo 1 > /data/data/org.f7.motoactions/files/request_camera` (as root)  
  3. Observe file removal and resumed activity  
- **Expected:** Flag consumed; MGC secure camera becomes top resumed activity  
- **Actual:** `topResumedActivity=…com.android.MGC/com.android.camera.SecureCameraActivity`; files dir no longer contains `request_camera`  
- **Result:** **PASS** (root helper → camera UI)  
- **Limitations:** This proves the **helper path**, not a physical Twist gesture end-to-end. Gesture→detect→flag is covered by app code + operator testing; archive a Twist `logcat` session when available.
