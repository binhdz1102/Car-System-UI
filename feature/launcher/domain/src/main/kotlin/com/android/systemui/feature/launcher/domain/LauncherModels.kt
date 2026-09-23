package com.android.systemui.feature.launcher.domain

import kotlinx.coroutines.flow.StateFlow

data class LaunchableApp(
    val packageName: String,
    val componentName: String,
    val label: String,
)

interface LauncherRepository {
    val apps: StateFlow<List<LaunchableApp>>

    suspend fun refresh()

    fun launch(app: LaunchableApp): Result<Unit>
}
