# Car-System-UI test report

Date: 2026-08-04  
Device: `emulator-5554`  
AVD: `my_car_avd_mysystemapp_20260802`  
Image: AAOS API 37 / Baklava, 1920×1080, 213 dpi, foreground user 10  
Deployment mode: `-writable-system`, APK copied to `/system_ext/priv-app/CarSystemUI`

## Build and deployment

Commands used:

```bash
./gradlew --no-daemon :app:replaceSystemApp --console=plain
./gradlew --no-daemon testDebugUnitTest :app:assembleDebug --console=plain
```

The replacement task completed successfully multiple times. The final deployment rebooted the
AVD and the package reported `versionCode=1000`, `versionName=custom-dev`, platform signature and
`com.android.systemui` process/service.

Final debug APK SHA-256: `02b2f1b31a71607bbe1608e70de92ed10f1fedafaa732e1e23948fa30ce38ac7`.
The final host verification `testDebugUnitTest :app:assembleDebug` also completed successfully.
After reboot, WindowManager showed one type-`2041` top-bar window and one type-`2024`
bottom-bar window; opening the user-10 diagnostic Activity did not create a second service.

## Verified on the AVD

| Area | Result | Evidence/notes |
| --- | --- | --- |
| Boot-started service | PASS | `Car-System-UI service created`; process remained alive after reboot. |
| Top/bottom bars | PASS | Windows type `2041` and `2024`; final AVD frames were top `[0,0][1920,101]`, bottom `[0,952][1920,1080]`. |
| Clock/status/user | PASS | Clock, location, notification icon and `Driver` rendered in the top XML bar. |
| Bluetooth panel | PASS | Panel opened below top bar, switch rendered and no crash. |
| Connectivity panel | PASS | XML panel opened/dismissed successfully. |
| Display panel | PASS | XML panel opened/dismissed successfully. |
| Volume panel | PASS | Media slider rendered with `15/15`; panel opened/dismissed successfully. |
| Climate panel | PASS | Driver/passenger temperatures, A/C, AUTO and fan controls rendered. Platform `Switch` was used because the AVD system window theme did not render `SwitchCompat` correctly. |
| HVAC temperature write | PASS | Driver `+` wrote 18.50°C → 19.00°C to the exposed areas and the UI changed 65° → 66°. |
| HVAC A/C toggle | PASS | A/C switch changed checked state and remained stable; no SystemUI crash. |
| App grid | PASS | Bottom app-grid action launched `com.android.car.carlauncher/.feature.launcher.presentation.AppGridActivity` in user 10. |
| Settings | PASS | Settings action launched `com.android.car.settings` in user 10; the image displayed its initial-user notice. |
| My-System-App | PASS | Bottom `M` action launched `com.android.mysystemapp/.MainActivity` in user 10. |
| Phone | PASS | Bottom phone action launched `com.android.car.dialer/.ui.TelecomActivity` in user 10. |
| Home | PASS | Home action returned to `com.android.car.carlauncher/.CarLauncher` in user 10. |
| Notifications panel | PASS/EMPTY | Panel opened with `No active notifications`; positive content was not available without granting listener access and creating a test notification. |
| User panel | PASS | `Driver` panel opened and dismissed. |
| XML diagnostics/navigation | PASS | `MainActivity` launched; Climate diagnostics showed `Connected`, Driver 65° and Passenger 65° using Navigation/Fragments. |
| Timber | PASS | Runtime launch logs were observed, for example `CarSystemUI.Coordinator` recorded user-10 activity launches with version/build metadata. |
| TaskView diagnostic activity | LIMITED | `TaskViewTestActivity` launched without an AppCompat/theme crash and connected to `CarTaskViewController`, but no embedded task surface appeared. |

## Defects found and fixed during verification

1. The emulator exposed individual HVAC areas while AOSP uses composite target masks. The VHAL
   adapter now maps a target mask to every supported area intersecting that mask.
2. The emulator rejected a Fahrenheit-converted write (`19.055555`) because the AOSP contract
   writes the Celsius step. The adapter now reads `configArray[2]` and writes the Celsius step.
3. `SwitchCompat` did not render correctly in the system-dialog window theme and crashed during
   measurement in one quick-panel path. The final XML panels use platform `android.widget.Switch`
   with explicit empty on/off text.
4. Activity launches from the persistent system-user service initially resolved to user 0. The
   coordinator now invokes `Context.startActivityAsUser` and uses the current foreground user.
5. The current AVD's AppGrid component differs from the generic AOSP configuration string. The
   implementation resolves the component present in this API-37 image.
6. The diagnostic `MainActivity` previously used `AppCompatActivity` with a platform-only theme.
   It now uses `FragmentActivity`; the TaskView diagnostic uses `Activity`, so both XML screens
   launch with the system-app theme.

## TaskView result and remaining limitations

The actual AVD showed the native CarLauncher error:

```text
TaskView unavailable
CarSystemUI did not register the car activity proxy on this device.
```

The custom `TaskViewTestActivity` was then launched through the diagnostic button. Its UI state
reached `CarTaskViewController connected; waiting for TaskView…`, but
`ControlledRemoteCarTaskViewCallback.onTaskViewCreated`/`onTaskAppeared` did not arrive. This is
consistent with the missing AOSP `CarSystemUIProxyImpl` and WMShell task-organizer integration;
the standalone APK does not contain the server-side remote-task host needed to create that surface.
This case is recorded as a tested limitation, not as a passing embedded-task case.

Other known limits are the full AOSP keyguard/privacy/media pods, profile switching, multi-display
WM/Shell orchestration and byte-for-byte resource parity. The APK is signed with a development
platform key and requires a writable emulator system overlay for replacement.

## Local visual evidence

During the run, screenshots were captured under `/tmp`, including:

- `/tmp/car-systemui-climate-switch-platform.png`
- `/tmp/car-systemui-hvac-driver-plus-final.png`
- `/tmp/car-systemui-bluetooth-panel-final.png`
- `/tmp/car-systemui-connectivity-panel-final.png`
- `/tmp/car-systemui-display-panel-final.png`
- `/tmp/car-systemui-volume-panel-final.png`
- `/tmp/car-systemui-notifications-clean.png`
- `/tmp/car-systemui-custom-taskview-final.png`
- `/tmp/car-systemui-final-normal.png`
- `/tmp/car-systemui-final-climate-panel.png`
- `/tmp/car-systemui-final-taskview.png`

These files are emulator evidence from the shared development environment and are not runtime
application assets.
