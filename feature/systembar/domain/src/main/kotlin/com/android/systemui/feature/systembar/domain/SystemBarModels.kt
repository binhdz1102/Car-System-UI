package com.android.systemui.feature.systembar.domain

import kotlinx.coroutines.flow.StateFlow

data class SystemBarState(
    val time: String = "--:--",
    val userName: String = "Driver",
    val bluetoothEnabled: Boolean = false,
    val wifiConnected: Boolean = false,
    val locationEnabled: Boolean = false,
    val mediaVolume: Int = 0,
    val mediaVolumeMax: Int = 100,
    val notificationCount: Int = 0,
)

interface SystemBarRepository {
    val state: StateFlow<SystemBarState>
}

enum class QuickControl {
    BLUETOOTH,
    CONNECTIVITY,
    DISPLAY,
    VOLUME,
}

sealed interface SystemBarAction {
    data class OpenQuickControl(val control: QuickControl) : SystemBarAction
    data object OpenNotifications : SystemBarAction
    data object OpenClimate : SystemBarAction
    data object OpenUserPicker : SystemBarAction
    data object LaunchHome : SystemBarAction
    data object LaunchAppGrid : SystemBarAction
    data object LaunchMySystemApp : SystemBarAction
    data object LaunchSettings : SystemBarAction
    data object LaunchPhone : SystemBarAction
    data object LaunchAssistant : SystemBarAction
    data class AdjustTemperature(val areaId: Int, val step: Int) : SystemBarAction
    data class AdjustVolume(val step: Int) : SystemBarAction
}
