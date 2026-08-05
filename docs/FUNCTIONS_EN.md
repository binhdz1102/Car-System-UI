# Car-System-UI functions and architecture

## Scope

This project supplies a package-compatible `com.android.systemui` system application for the
matching AAOS development image. It recreates the visible Automotive system surface with Android
XML layouts and connects the controls to the same platform services used by the AOSP application
where those services are available in an isolated Gradle application.

The implementation is organized as:

```text
UI (XML views, coordinator, fragments)
        ↓
Presentation (MVVM state/actions, service-scoped coordinators)
        ↓
Domain (models, repository contracts, use cases)
        ↓
Data/platform (Car VHAL, Bluetooth, network, audio, notifications, PackageManager)
```

Hilt wires the module boundaries. Coroutine scopes use `Dispatchers.Main` for view binding and an
injected IO dispatcher for platform/VHAL work. State is exposed as `StateFlow`; time and platform
callbacks are adapted to `callbackFlow`/Flow. Timber is planted by `SystemUIApplication` and
includes app version and the standalone build identifier in each message.

## Implemented surface

### System bars

- Top bar: Bluetooth, connectivity/Wi-Fi, display, volume, clock, location, notifications and
  current user.
- Bottom bar: Home, driver/passenger HVAC targets, app grid, My-System-App, Settings, Phone and
  voice-assistant actions.
- Privileged AOSP-compatible window types are used for the top bar, bottom bar and dialogs:
  `2041`, `2024` and `2008` respectively.
- The bar dimensions are resolved from the platform Automotive resources when available. On the
  tested AVD the measured frames are top `[0,0][1920,101]` and bottom `[0,952][1920,1080]`.
- User-facing activity launches are routed with `startActivityAsUser` to the foreground driver
  user. This is required because the persistent SystemUI service runs as the system user.

### Quick-control panels

The XML quick panels open below the top bar and can be dismissed with Back or the close button:

- Bluetooth: current adapter state, toggle and Bluetooth Settings.
- Connectivity: network description and Wireless Settings.
- Display: display description and Display Settings.
- Volume: media volume slider and Sound Settings.

### Climate/HVAC

- Driver and passenger target temperatures are read from `HVAC_TEMPERATURE_SET`.
- AOSP target masks are mapped to the individual VHAL areas exposed by the emulator. On the test
  image the driver mask `49` maps to areas `1`, `16`, `32` and the passenger mask `68` maps to
  areas `4`, `64`.
- Unit display follows `HVAC_TEMPERATURE_DISPLAY_UNITS`; Fahrenheit values are rendered as whole
  degrees to match the Automotive bar.
- `+`/`-`, A/C, AUTO and fan speed write through `CarPropertyManager` and update the StateFlow.
- VHAL configuration is discovered at runtime. Missing properties are represented as unavailable
  state instead of crashing the SystemUI process.

### Notifications and user panel

- A `NotificationListenerService` feeds an in-memory notification repository through Flow.
- The notification panel supports an empty state and Clear All action.
- The user panel shows the current user name and provides a user-settings entry point.
- Notification listener access is an OS-controlled setting and may need to be granted manually on
  a clean image before positive notification-content testing.

### Launcher and TaskView diagnostics

- Launcher diagnostics query launchable activities using `PackageManager` and launch them through a
  repository/use-case boundary.
- The app-grid action resolves to the launcher component present in the tested AVD:
  `com.android.car.carlauncher/.feature.launcher.presentation.AppGridActivity`.
- `TaskViewTestActivity` is an XML diagnostic host based on the `ControlledRemoteCarTaskView`
  contract used by `My-System-App`. It reports controller/surface/task lifecycle transitions and
  logs them with Timber.
- The full AOSP TaskView server side (`CarSystemUIProxyImpl`, Shell task organizer and remote-task
  transitions) is not reproduced by this standalone APK. Therefore the diagnostic activity can
  connect to the controller, but the tested image does not create an embedded task until that
  AOSP WM/Shell integration is ported or retained in the system image.

### Diagnostic activity

`MainActivity` is an XML/Navigation entry point intended for Android Studio/manual checks. It
exposes Climate diagnostics, launcher diagnostics and the TaskView test without requiring Compose.
It does not start a second system-bar service when launched under the foreground user, avoiding
duplicate privileged windows while the boot-started system service is running.

## Deliberate non-inclusions

Room is not useful for the current contract because there is no durable feature data to persist.
The project also does not claim byte-for-byte parity with every AOSP CarSystemUI pod. Keyguard,
privacy chips, full media controls, multi-display WM/Shell orchestration, profile switching,
scalable UI panels and the complete CarSystemUI proxy remain image/AOSP integration work rather
than isolated feature modules.
