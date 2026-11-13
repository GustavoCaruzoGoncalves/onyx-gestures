#!/system/bin/sh
# Ensure the APK exists (pm) then keep GestureService alive.
# Also serve Twist→camera requests as root (app cannot BAL-start camera on A17).
MODDIR=${0%/*}
PKG=org.f7.motoactions
SVC="$PKG/.service.GestureService"
APK="$MODDIR/system/priv-app/F7MotoActions/F7MotoActions.apk"
REQ="/data/data/$PKG/files/request_camera"
REQ_DE="/data/user_de/0/$PKG/files/request_camera"

i=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ $i -lt 60 ]; do
  sleep 2
  i=$((i + 1))
done

ensure_pkg() {
  if pm path "$PKG" >/dev/null 2>&1; then
    return 0
  fi
  if [ -f "$APK" ]; then
    pm install -r -g "$APK" >/dev/null 2>&1
    log -t F7MotoActions "pm install from module APK (exit $?)"
  fi
}

launch_camera() {
  am start -n com.android.MGC/com.android.camera.SecureCameraActivity --activity-clear-top >/dev/null 2>&1 && return 0
  am start -n com.android.MGC/com.android.camera.CameraImageActivity --activity-clear-top >/dev/null 2>&1 && return 0
  am start -a android.media.action.STILL_IMAGE_CAMERA_SECURE --activity-clear-top >/dev/null 2>&1 && return 0
  am start -a android.media.action.STILL_IMAGE_CAMERA --activity-clear-top >/dev/null 2>&1 && return 0
  am start -n org.lineageos.aperture/.CameraActivity --activity-clear-top >/dev/null 2>&1 && return 0
  return 1
}

serve_camera_request() {
  for f in "$REQ" "$REQ_DE"; do
    if [ -f "$f" ]; then
      rm -f "$f"
      if launch_camera; then
        log -t F7MotoActions "camera launched (root helper)"
      else
        log -t F7MotoActions "camera launch FAILED (root helper)"
      fi
    fi
  done
}

ensure_pkg

pm enable "$PKG" >/dev/null 2>&1
dumpsys deviceidle whitelist +"$PKG" >/dev/null 2>&1
cmd appops set "$PKG" RUN_ANY_IN_BACKGROUND allow >/dev/null 2>&1
cmd appops set "$PKG" RUN_IN_BACKGROUND allow >/dev/null 2>&1
cmd appops set "$PKG" SCHEDULE_EXACT_ALARM allow >/dev/null 2>&1
cmd appops set "$PKG" USE_FULL_SCREEN_INTENT allow >/dev/null 2>&1

start_svc() {
  am start-foreground-service -n "$SVC" >/dev/null 2>&1 \
    || am startservice -n "$SVC" >/dev/null 2>&1
}

pidof "$PKG" >/dev/null 2>&1 || start_svc
log -t F7MotoActions "service.sh boot start done"

# Fast camera request loop (root).
(
  while true; do
    serve_camera_request
    sleep 0.35
  done
) &

# Slow keep-alive for the APK / FGS.
(
  while true; do
    sleep 60
    ensure_pkg
    dumpsys deviceidle whitelist +"$PKG" >/dev/null 2>&1
    if ! pidof "$PKG" >/dev/null 2>&1; then
      start_svc
      log -t F7MotoActions "watchdog restarted GestureService"
    fi
  done
) &
