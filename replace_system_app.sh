#!/usr/bin/env bash
set -euo pipefail

apk_path="${1:?APK path is required}"
serial="${ANDROID_SERIAL:-emulator-5554}"
remote="/system_ext/priv-app/CarSystemUI/CarSystemUI.apk"

wait_for_device() {
    adb -s "$serial" wait-for-device
}

adb -s "$serial" root
wait_for_device

remount_output="$(adb -s "$serial" remount 2>&1)" || {
    printf '%s\n' "$remount_output" >&2
    exit 1
}
printf '%s\n' "$remount_output"

# On the first writable-system boot, adb remount enables overlayfs and asks for
# one reboot before the mount becomes writable. Handle that bootstrap here.
if ! adb -s "$serial" shell mount | grep -qE 'overlay on /system_ext .*\(rw,'; then
    adb -s "$serial" reboot
    wait_for_device
    adb -s "$serial" root
    wait_for_device
    adb -s "$serial" remount
fi

if ! adb -s "$serial" shell mount | grep -qE 'overlay on /system_ext .*\(rw,'; then
    echo "ERROR: /system_ext is not writable. Start the emulator with -writable-system." >&2
    exit 1
fi

adb -s "$serial" push "$apk_path" "$remote"
adb -s "$serial" shell chown 0:0 "$remote"
adb -s "$serial" shell chmod 0644 "$remote"
adb -s "$serial" reboot
echo "CarSystemUI pushed to $remote; the AVD is rebooting."
