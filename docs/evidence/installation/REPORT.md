# Installation evidence

- **Device / software:** POCO F7 onyx, PixelOS 17, KernelSU 3.3.x  
- **Date:** 2026-11-13  
- **Procedure:** Module already installed via KernelSU; inspect `module.prop`, `pm path`, `dumpsys activity services`  
- **Expected:** Module v1.1.2 present; package installed; `GestureService` foreground  
- **Actual:** See `module_and_service.txt`  
- **Result:** **PASS**  
- **Limitations:** Does not re-demonstrate first-time ZIP flash in this capture (module was already active)
