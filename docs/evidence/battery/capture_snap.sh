#!/system/bin/sh
# Device snapshot for battery/perf evidence (no serial numbers).
echo "=== ENV ==="
echo "device=$(getprop ro.product.device)"
echo "release=$(getprop ro.build.version.release)"
echo "display=$(getprop ro.build.display.id)"
echo "kernel=$(uname -r)"
echo "abi=$(getprop ro.product.cpu.abi)"
echo "ksud=$(ksud -V 2>/dev/null || echo unknown)"
echo "=== MODULE ==="
cat /data/adb/modules/f7_moto_actions/module.prop 2>/dev/null
echo "=== BATTERY ==="
dumpsys battery
echo "=== PACKAGE ==="
dumpsys package org.f7.motoactions 2>/dev/null | grep -E 'userId=|versionName|versionCode|pkgFlags' | head -30
echo "=== SENSOR LISTENERS (package) ==="
dumpsys sensorservice 2>/dev/null | grep -E 'org.f7.motoactions|Connection' | head -60
echo "=== POWER / WAKELOCK HINTS ==="
dumpsys power 2>/dev/null | grep -E 'PARTIAL_WAKE_LOCK|F7Moto|motoactions|ScreenOff' | head -40
echo "=== DEVICEIDLE ==="
dumpsys deviceidle 2>/dev/null | grep -E 'mState|Whitelist|org.f7' | head -40
echo "=== PROCESS ==="
ps -A 2>/dev/null | grep -E 'motoactions|f7_moto' | head -20
echo "=== MEMINFO ==="
dumpsys meminfo org.f7.motoactions 2>/dev/null | head -40
echo "=== BATTERYSTATS UID ==="
UID=$(dumpsys package org.f7.motoactions 2>/dev/null | grep userId= | head -1 | sed 's/.*userId=\([0-9]*\).*/\1/')
echo "uid=$UID"
if [ -n "$UID" ]; then
  dumpsys batterystats --charged 2>/dev/null | grep -A 80 "Uid $UID:" | head -100
fi
echo "=== DONE ==="
