#!/system/bin/sh
# Remove materialized SYSTEM copy if present.
mount -o rw,remount / 2>/dev/null
rm -rf /system/priv-app/F7MotoActions
rm -f /system/etc/sysconfig/org.f7.motoactions.xml
rm -f /system/etc/permissions/privapp-permissions-org.f7.motoactions.xml
pm uninstall org.f7.motoactions >/dev/null 2>&1 || true
