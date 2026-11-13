#!/system/bin/sh
# KernelSU / Magisk module installer hooks
SKIPUNZIP=0
ui_print "- Onyx Gestures (by GustavoCaruzo)"
ui_print "- Independent gesture module — not affiliated with device OEMs"
ui_print "- APK is also installed via pm on boot when possible"
ui_print "- Keep-alive + GestureService start in service.sh"

set_perm_recursive "$MODPATH" 0 0 0755 0644
if [ -d "$MODPATH/system/priv-app/F7MotoActions" ]; then
  set_perm_recursive "$MODPATH/system/priv-app/F7MotoActions" 0 0 0755 0644
  set_perm "$MODPATH/system/priv-app/F7MotoActions/F7MotoActions.apk" 0 0 0644
fi

# Do NOT pm uninstall here — /system has no room for priv-app on this device,
# so removing /data would leave the user with no app after reboot.
if [ -n "$BOOTMODE" ] && command -v pm >/dev/null 2>&1; then
  APK="$MODPATH/system/priv-app/F7MotoActions/F7MotoActions.apk"
  if [ -f "$APK" ]; then
    pm install -r -g "$APK" >/dev/null 2>&1 \
      && ui_print "- Installed org.f7.motoactions from module APK" \
      || ui_print "- pm install deferred to service.sh (ok after reboot)"
  fi
fi
