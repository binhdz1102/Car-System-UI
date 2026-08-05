# Custom Car-System-UI

`Car-System-UI` is a standalone, platform-signed Android Gradle replacement for the
`com.android.systemui` package used by the Automotive AVD. It is based on the visible
system-bar/HVAC contracts in AOSP `packages/apps/Car/SystemUI` and on the Android Car and
system-feature integrations already present in `custom-system-apps/My-System-App`.

The implementation is deliberately XML-only at the view layer. It uses a multi-module
Clean Architecture/MVVM layout, Hilt dependency injection, Kotlin Coroutines/Flow, Android
Navigation for the diagnostic activity, Timber logging, and Android Car VHAL APIs for HVAC.
Room is not included because this surface has no persistent domain data; transient UI state is
provided by `StateFlow`.

## Modules

```text
app                                  package-compatible entry points
core:common                          dispatchers, results, Flow helpers
core:platform                        privileged WindowManager context factory
feature:systembar:{domain,data,presentation}
                                     top/bottom bars, panels, status state and actions
feature:climate:{domain,data,presentation}
                                     HVAC state/use cases, VHAL adapter and XML controls
feature:launcher:{domain,data,presentation}
                                     launcher query/launch and TaskView diagnostic host
feature:notifications:{domain,data,presentation}
                                     notification listener state and XML panel
build-logic/convention               shared Gradle convention plugins
```

See the bilingual feature documentation:

- [Functions and architecture (English)](docs/FUNCTIONS_EN.md)
- [Chức năng và kiến trúc (Tiếng Việt)](docs/FUNCTIONS_VI.md)
- [AVD test report](docs/TEST_REPORT.md)

## Build and deploy

Build from Android Studio or the command line:

```bash
cd /home/binh/Desktop/aosp/custom-system-apps/Car-System-UI
./gradlew --no-daemon testDebugUnitTest :app:assembleDebug
```

`com.android.systemui` is a persistent system package. For an actual system replacement, the
emulator must be started with a writable system overlay. The original command supplied for the
AVD can be used with `-writable-system` added:

```bash
systemd-run --user \
  --unit=mysystemapp-avd-carsystemui-test.service \
  --collect \
  --property=Restart=on-failure \
  --setenv=DISPLAY="$DISPLAY" \
  --setenv=XDG_RUNTIME_DIR="$XDG_RUNTIME_DIR" \
  --setenv=ANDROID_AVD_HOME="$HOME/.aaos-mysystemapp-20260802/.android/avd" \
  --setenv=ANDROID_SDK_ROOT="$HOME/Android/Sdk" \
  "$HOME/Android/Sdk/emulator/emulator" \
  -avd my_car_avd_mysystemapp_20260802 \
  -writable-system \
  -no-snapshot
```

After the AVD is booted and visible as `emulator-5554`:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew --no-daemon :app:replaceSystemApp
```

The deployment task performs `adb root`, `adb remount`, pushes the signed APK to
`/system_ext/priv-app/CarSystemUI/CarSystemUI.apk`, sets ownership/mode, and reboots the AVD.
It requires the writable-system emulator overlay; a locked/non-writable image cannot be updated
with `adb remount`.

The final test report records the exact AVD checks and remaining limitations. The APK uses the
local AOSP development platform keystore in `keystore/platform.p12`; it is suitable for this
development image only, not for production distribution.
