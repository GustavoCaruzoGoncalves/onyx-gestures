#!/system/bin/sh
# Try to expose SYSTEM priv-app. PixelOS onyx /system is often 100% full, so
# magic-mount / copy may fail — service.sh + user keep-alive still apply.
MODDIR=${0%/*}
APK_SRC="$MODDIR/system/priv-app/F7MotoActions"
APK_DST="/system/priv-app/F7MotoActions"

mount -o rw,remount / 2>/dev/null

# Tiny whitelist files (ignore ENOSPC).
if [ -f "$MODDIR/system/etc/sysconfig/org.f7.motoactions.xml" ]; then
  cp -f "$MODDIR/system/etc/sysconfig/org.f7.motoactions.xml" \
    /system/etc/sysconfig/org.f7.motoactions.xml 2>/dev/null
fi
if [ -f "$MODDIR/system/etc/permissions/privapp-permissions-org.f7.motoactions.xml" ]; then
  cp -f "$MODDIR/system/etc/permissions/privapp-permissions-org.f7.motoactions.xml" \
    /system/etc/permissions/privapp-permissions-org.f7.motoactions.xml 2>/dev/null
fi

# Full APK only if there is room (or bind works).
if [ -d "$APK_SRC" ]; then
  if mkdir -p "$APK_DST" 2>/dev/null; then
    mount --bind "$APK_SRC" "$APK_DST" 2>/dev/null \
      || cp -f "$APK_SRC/F7MotoActions.apk" "$APK_DST/F7MotoActions.apk" 2>/dev/null
    chmod 0644 "$APK_DST"/*.apk 2>/dev/null
  fi
fi

exit 0
