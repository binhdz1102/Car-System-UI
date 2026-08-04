# Custom CarSystemUI

Standalone Gradle-built, package-compatible development replacement for the
`com.android.systemui` package in the matching AAOS development emulator image.

The project is signed with the AOSP development platform certificate stored in
`keystore/platform.p12`. Do not use this key for a production image.

Build and run from Android Studio or the command line:

```bash
bash ./gradlew :app:assembleDebug
```

`com.android.systemui` is a persistent system package. A normal Android Studio
Run/`adb install -r` is rejected by Package Manager, even with the correct
platform key. For an actual system replacement, boot the AVD with a writable
system overlay (`-writable-system`), then run:

```bash
ANDROID_SERIAL=emulator-5554 bash ./gradlew :app:replaceSystemApp
```

The task runs `adb root`, `adb remount`, pushes to
`/system_ext/priv-app/CarSystemUI/CarSystemUI.apk`, fixes ownership/mode, and
reboots the AVD. The currently running locked instance will report
`Device must be bootloader unlocked` for `adb remount`; that is an emulator
launch configuration limitation, not a signing failure.

This project deliberately supplies only a package-compatible development
`SystemUIService` and a visible diagnostic activity. The full AOSP CarSystemUI
and its HVAC overlay are Soong-built from many internal SystemUI/AAOS modules;
they are not source-compatible with an isolated Android Gradle project. Thus
this APK proves signing, package update, service startup, and Android Studio
iteration, but is not a feature-for-feature replacement for the AOSP UI.

Restore the image APK used for testing immediately after experiments:

```bash
adb -s emulator-5554 root
adb -s emulator-5554 remount
adb -s emulator-5554 push \
  "/home/binh/Desktop/aosp/custom-system-apps/original system apks/CarSystemUI.apk" \
  /system_ext/priv-app/CarSystemUI/CarSystemUI.apk
adb -s emulator-5554 shell chown 0:0 /system_ext/priv-app/CarSystemUI/CarSystemUI.apk
adb -s emulator-5554 shell chmod 0644 /system_ext/priv-app/CarSystemUI/CarSystemUI.apk
adb -s emulator-5554 reboot
```

After replacing a custom manifest with the full image manifest, re-grant the
Bluetooth runtime permissions if SystemUI logs a `BLUETOOTH_CONNECT` denial:

```bash
adb -s emulator-5554 shell pm grant com.android.systemui android.permission.BLUETOOTH_CONNECT
adb -s emulator-5554 shell pm grant com.android.systemui android.permission.BLUETOOTH_SCAN
adb -s emulator-5554 shell am force-stop com.android.systemui
adb -s emulator-5554 shell am startservice -n com.android.systemui/.SystemUIService
```

Expected platform certificate SHA-256:

```text
c8a2e9bccf597c2fb6dc66bee293fc13f2fc47ec77bc6b2b0d52c11f51192ab8
```
