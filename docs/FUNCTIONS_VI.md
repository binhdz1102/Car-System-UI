# Chức năng và kiến trúc Car-System-UI

## Phạm vi

Project này cung cấp ứng dụng system tương thích package `com.android.systemui` cho image AAOS
đang dùng. Các bề mặt Automotive nhìn thấy trên máy ảo được dựng lại bằng Android XML và kết nối
đến các platform service tương ứng khi những service đó có thể dùng trong một Gradle app độc lập.

Kiến trúc được chia như sau:

```text
UI (XML view, coordinator, fragment)
        ↓
Presentation (MVVM state/action, coordinator theo service)
        ↓
Domain (model, repository contract, use case)
        ↓
Data/platform (Car VHAL, Bluetooth, network, audio, notification, PackageManager)
```

Hilt thực hiện dependency injection giữa các module. Coroutine dùng `Dispatchers.Main` cho việc
bind view và IO dispatcher được inject cho VHAL/platform work. State dùng `StateFlow`; callback
của platform và clock được chuyển thành `callbackFlow`/Flow. Timber được plant bởi
`SystemUIApplication`, mỗi log có version app và build identifier.

## Các bề mặt đã xây dựng

### System bar

- Thanh trên: Bluetooth, connectivity/Wi-Fi, display, volume, clock, location, notification và
  user hiện tại.
- Thanh dưới: Home, nhiệt độ driver/passenger, app grid, My-System-App, Settings, Phone và voice
  assistant.
- Dùng các window type tương thích AOSP cho top bar, bottom bar và dialog: lần lượt `2041`, `2024`
  và `2008`.
- Kích thước bar được lấy từ resource Automotive của platform nếu có. Trên AVD kiểm thử, frame đo
  được là top `[0,0][1920,101]`, bottom `[0,952][1920,1080]`.
- Activity hướng tới người dùng được gọi bằng `startActivityAsUser` theo foreground driver user,
  vì service SystemUI persistent chạy ở system user.

### Quick-control panel

Các panel XML mở bên dưới top bar và đóng được bằng Back hoặc nút close:

- Bluetooth: trạng thái adapter, switch và Bluetooth Settings.
- Connectivity: mô tả network và Wireless Settings.
- Display: mô tả display và Display Settings.
- Volume: thanh chỉnh media volume và Sound Settings.

### Climate/HVAC

- Đọc nhiệt độ mục tiêu driver/passenger từ `HVAC_TEMPERATURE_SET`.
- Target mask của AOSP được ánh xạ vào các area VHAL mà emulator expose. Trên image kiểm thử,
  driver mask `49` ánh xạ các area `1`, `16`, `32`; passenger mask `68` ánh xạ `4`, `64`.
- Đơn vị hiển thị theo `HVAC_TEMPERATURE_DISPLAY_UNITS`; Fahrenheit được hiển thị số nguyên như
  thanh Automotive.
- Nút `+`/`-`, A/C, AUTO và fan speed ghi xuống `CarPropertyManager`, sau đó cập nhật StateFlow.
- Cấu hình VHAL được discover lúc chạy. Property không có sẽ thành trạng thái unavailable thay
  vì làm crash process SystemUI.

### Notification và user panel

- `NotificationListenerService` đưa notification vào repository trong memory bằng Flow.
- Notification panel có empty state và thao tác Clear All.
- User panel hiển thị tên user hiện tại và có nút mở user settings.
- Quyền notification listener do OS quản lý; trên image mới cần grant thủ công trước khi test
  notification content dương tính.

### Launcher và TaskView diagnostics

- Launcher diagnostics query các activity có thể launch bằng `PackageManager`, rồi launch thông qua
  repository/use-case.
- App-grid action dùng component có trong AVD kiểm thử:
  `com.android.car.carlauncher/.feature.launcher.presentation.AppGridActivity`.
- `TaskViewTestActivity` là host XML dựa trên contract `ControlledRemoteCarTaskView` giống phần
  triển khai trong `My-System-App`, có hiển thị lifecycle và log Timber.
- Full server-side của AOSP TaskView (`CarSystemUIProxyImpl`, Shell task organizer và remote-task
  transitions) chưa được port vào APK độc lập. Vì vậy activity test kết nối được controller,
  nhưng trên image kiểm thử chưa tạo embedded task cho đến khi port/giữ lại WM/Shell integration
  của AOSP.

### Diagnostic activity

`MainActivity` là entry point XML/Navigation phục vụ Android Studio và kiểm thử thủ công. Activity
có các nút Climate diagnostics, launcher diagnostics và TaskView test, không dùng Compose. Khi chạy
ở foreground user, activity không start thêm một SystemUI service; việc này tránh tạo duplicate
privileged window trong khi system service đã được boot-start.

## Các phần cố ý chưa bao gồm

Room chưa được dùng vì contract hiện tại không có dữ liệu bền vững cần lưu. Project cũng không
khẳng định parity byte-for-byte với mọi AOSP CarSystemUI pod. Keyguard, privacy chips, media đầy
đủ, WM/Shell orchestration nhiều display, profile switching, scalable UI panel và full
CarSystemUI proxy vẫn cần tích hợp ở image/AOSP thay vì chỉ trong một Gradle feature module.
